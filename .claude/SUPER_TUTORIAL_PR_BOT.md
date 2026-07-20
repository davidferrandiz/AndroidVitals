# 🤖 Super Tutorial: PR bot con IA local para proyectos mobile (de 0 a CI)

Guía completa para montar, **desde cero y en cualquier proyecto mobile** (iOS/Swift o
Android/Kotlin), un **revisor de código automático** que:

1. Corre **100% en local** (privacidad, coste cero) con Ollama.
2. Usa **RAG** para revisar según los estándares y tipos de TU repo.
3. Se **evalúa** con golden tests (precision/recall).
4. Acaba **corriendo en un job de CI** (GitHub Actions) que comenta cada PR.

> **Idea de fondo:** el bot revisa un **diff** (los cambios de un PR). RAG le da
> **contexto** del repo; el prompt le da **criterios**; el eval te dice si mejora;
> y CI lo pone a trabajar en cada PR. La IA es un **primer revisor, no la última palabra**.

**Prerrequisitos:** una Mac/Linux, Python 3, y un repo mobile en GitHub.

---

## Parte 0 — Setup (10 min)

**1. Instala Ollama y un modelo de código.**

```bash
brew install ollama          # o descarga desde ollama.com
ollama run qwen3-coder       # descarga y arranca el modelo (deja el servidor en :11434)
ollama list                  # apunta el TAG exacto (p. ej. qwen3-coder:latest)
```

> Elige el modelo por licencia y tamaño: Apache-2.0/MIT para uso comercial, variante
> `instruct`, y 7B-14B para equilibrio calidad/latencia. Verás el tamaño con `ollama show <modelo>`.

**2. Entorno de Python aislado.**

```bash
mkdir pr-bot && cd pr-bot
python3 -m venv .venv
source .venv/bin/activate
pip install openai
```

> Si te sale `externally-managed-environment`, es por Homebrew: el venv lo resuelve.
> Para no activar cada vez: `.venv/bin/python script.py`.

---

## Parte 1 — Revisor local

El corazón: leer un diff → pedir review al LLM → imprimirlo. Crea `review.py`:

```python
import sys
from openai import OpenAI

client = OpenAI(base_url="http://localhost:11434/v1", api_key="ollama")  # api_key dummy (local)
MODEL = "qwen3-coder:latest"     # OJO: el TAG exacto de `ollama list`

# 👇 Adapta el rol a tu stack (Swift/iOS o Kotlin/Android)
SYSTEM_PROMPT = """Eres un revisor de código {LENGUAJE} senior. Revisa SOLO el diff.
Busca, por prioridad: 1) bugs/crashes  2) seguridad  3) casos límite.
IGNORA estilo (ya lo cubre el linter).
Formato: - [alta|media] archivo:línea — problema — sugerencia
Si no hay nada relevante, responde: "LGTM, sin problemas críticos."
No inventes problemas para rellenar."""

def review_diff(diff: str) -> str:
    resp = client.chat.completions.create(
        model=MODEL, temperature=0,      # temperature 0 = reviews reproducibles
        messages=[{"role": "system", "content": SYSTEM_PROMPT},
                  {"role": "user", "content": f"Revisa este diff:\n{diff}"}])
    return resp.choices[0].message.content

if __name__ == "__main__":
    print(review_diff(open(sys.argv[1], encoding="utf-8").read()))
```

**Notas por stack** (cambia `{LENGUAJE}` y los ejemplos de bugs):

| | iOS / Swift | Android / Kotlin |
|---|---|---|
| Bugs típicos a cazar | force-unwrap `!`, retain cycles, índices fuera de rango, race conditions | `!!` not-null assertion, NPE, lateinit sin init, leaks de coroutines |
| Linter que ya cubre estilo | SwiftLint | ktlint / detekt |

**Probar:** crea un `ejemplo.diff` con un bug a propósito y córrelo:

```bash
python review.py ejemplo.diff
```

Deberías ver el bug cazado. Si dice "model not found", usa el tag exacto de `ollama list`.

---

## Parte 2 — RAG (contexto del repo)

Sin contexto, el bot comenta genérico y se le escapan violaciones de TUS convenciones.
RAG recupera lo relevante del repo y lo mete en el prompt.

**Prerrequisito:** ten un `CODING_STANDARDS.md` en tu repo con las reglas del equipo
(reglas duras del linter, arquitectura, DI, concurrencia...). Es el contexto más valioso.

Crea `retrieve.py`:

```python
import os, re, glob

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.join(HERE, "..", "ruta", "a", "tu", "repo")   # ajústalo
STANDARDS = os.path.join(REPO, "CODING_STANDARDS.md")
EXT = ("*.swift", "*.kt", "*.kts")      # extensiones de tu stack
BUDGET = 6000

def find_context(diff: str) -> str:
    parts = []
    if os.path.exists(STANDARDS):
        parts.append(open(STANDARDS, encoding="utf-8", errors="ignore").read())
    ids = set(re.findall(r"\b[A-Z][A-Za-z0-9]+\b", diff))      # tipos del diff
    files = [f for p in EXT for f in glob.glob(os.path.join(REPO, "**", p), recursive=True)]
    for path in files:
        text = open(path, encoding="utf-8", errors="ignore").read()
        # busca definiciones de esos tipos (Swift: struct/class/enum; Kotlin: class/object/interface)
        if any(re.search(rf"\b(struct|class|enum|protocol|object|interface)\s+{i}\b", text) for i in ids):
            parts.append(f"### {os.path.basename(path)}\n{text}")
    return "\n\n".join(parts)[:BUDGET]
```

En `review_rag.py`, inyecta ese contexto y **refuerza el prompt para que haga cumplir
los estándares** (si no, tratará las reglas de arquitectura como "estilo" y las ignorará):

```python
from review import client, MODEL, SYSTEM_PROMPT
from retrieve import find_context

RAG_SYSTEM = SYSTEM_PROMPT + """
CRITERIO 0 (lo primero): violaciones de CODING_STANDARDS. Márcalas como [alta]
AUNQUE no sean crashes; la arquitectura (singletons, DI) NO es "estilo". Cita la regla."""

def review_diff_rag(diff):
    ctx = find_context(diff)
    resp = client.chat.completions.create(model=MODEL, temperature=0, messages=[
        {"role": "system", "content": RAG_SYSTEM},
        {"role": "user", "content": f"CONTEXTO DEL REPO:\n{ctx}\n\n---\nRevisa este diff:\n{diff}"}])
    return resp.choices[0].message.content
```

> **Lección clave:** RAG da el **contexto**, pero el **prompt** decide qué hacer con él.
> Meter el documento no basta si no le dices explícitamente que lo haga cumplir.

---

## Parte 3 — Evaluación (golden tests)

No te fíes a ojo: **mídelo**. Un set fijo de diffs etiquetados, y calculas
**precision** (¿mete ruido?) y **recall** (¿se come bugs?). Crea `eval.py`:

```python
import sys
GOLDEN = [   # 4 diffs con bug conocido + 1 limpio (debe callar)
    {"file": "golden/force_unwrap.diff", "must_find": ["force", "unwrap"], "silent": False},
    {"file": "golden/new_singleton.diff", "must_find": ["singleton", "shared", "inyec"], "silent": False},
    {"file": "golden/clean.diff", "must_find": [], "silent": True},
]
def is_silent(r): return "lgtm" in r.lower() or not any(l.strip().startswith("-") for l in r.splitlines())
def caught(r, kws): return any(k.lower() in r.lower() for k in kws)

def run(reviewer):
    tp = fp = fn = 0
    for c in GOLDEN:
        r = reviewer(open(c["file"]).read())
        if c["silent"]:
            fp += 0 if is_silent(r) else 1
        else:
            if caught(r, c["must_find"]): tp += 1
            else: fn += 1
    print(f"recall={tp/(tp+fn):.0%}  precision={tp/(tp+fp) if tp+fp else 1:.0%}")

if __name__ == "__main__":
    if "--rag" in sys.argv:
        from review_rag import review_diff_rag as rev
    else:
        from review import review_diff as rev
    run(rev)
```

Crea los diffs de `golden/` **basados en tus tipos reales** (uno debe violar un estándar,
p.ej. un singleton nuevo, para ver el efecto de RAG). Corre las dos versiones:

```bash
python eval.py            # sin RAG
python eval.py --rag      # con RAG
```

Verás cómo RAG **sube el recall** (caza la violación de estándar) **sin disparar el
ruido**. Esa es la prueba con números de que el sistema mejora.

---

## Parte 4 — Deploy en CI (GitHub Actions)

El objetivo: que el bot comente **cada PR automáticamente**, sin servidor que cuidar.

### El workflow (`.github/workflows/ai-review.yml` de tu repo)

```yaml
name: AI Code Review (LLM local)
on:
  pull_request:
    types: [opened, synchronize, reopened]
permissions:
  pull-requests: write
jobs:
  ai-review:
    runs-on: self-hosted          # ⚠️ ver abajo
    steps:
      - name: Sacar el diff
        env: { GH_TOKEN: "${{ secrets.GITHUB_TOKEN }}" }
        run: gh pr diff ${{ github.event.pull_request.number }} --repo ${{ github.repository }} > pr.diff
      - name: Revisar con el LLM local
        run: python .github/scripts/ci_review.py pr.diff > review.txt
      - name: Comentar el PR
        env: { GH_TOKEN: "${{ secrets.GITHUB_TOKEN }}" }
        run: gh pr comment ${{ github.event.pull_request.number }} --repo ${{ github.repository }} --body-file review.txt
```

`ci_review.py` (en `.github/scripts/`) es tu `review.py`: lee `pr.diff`, llama a Ollama
e imprime el review (lo impreso va al comentario).

### ⚠️ El punto crítico: `self-hosted`

El job corre en un **runner**. Los runners **en la nube de GitHub NO llegan a tu Ollama
local**. Para un LLM privado, registra un **self-hosted runner** (Settings → Actions →
Runners) en una máquina **con Ollama** (Python + `openai` + `gh`). El job corre ahí y
llama a `localhost:11434` en esa misma máquina → **la privacidad se mantiene**.

### Alternativa: webhook + servidor

Si prefieres un servidor 24/7 en vez de CI: un pequeño Flask que escucha el webhook de
GitHub (con la **firma verificada**), trae el diff, revisa y comenta. En local necesita
un **túnel** (smee/ngrok); en producción, un servidor con dirección propia. Elige CI si
quieres menos mantenimiento; webhook si quieres control fino.

---

## Parte 5 — De la demo al equipo (producción)

El montaje "en mi portátil" no escala. Para un equipo:

- **LLM compartido:** un servidor de inferencia interno (Ollama/vLLM, con GPU) dentro de
  la red de la empresa. Todo apunta a él. Sigue siendo **privado** (no sale a terceros).
- **Bot en máquina siempre encendida:** el self-hosted runner (o el servidor webhook) vive
  en infra de la empresa, no en un portátil.
- **GitHub App** en vez de token personal: identidad propia ("PR Bot"), permisos
  controlados, auditable.

---

## ✅ Checklist final

- [ ] Ollama corriendo, modelo con licencia comercial e `instruct`.
- [ ] `review.py` caza un bug en un diff de ejemplo.
- [ ] `CODING_STANDARDS.md` en el repo + `retrieve.py` lo recupera.
- [ ] Prompt de RAG con "CRITERIO 0: hacer cumplir el estándar".
- [ ] `eval.py` muestra recall/precision, y RAG mejora los números.
- [ ] Workflow en `.github/workflows/` + `ci_review.py` en `.github/scripts/`.
- [ ] Self-hosted runner con Ollama registrado.
- [ ] PR de prueba → el bot comenta.

## ⚠️ Trampas que evitar

1. Tag del modelo incorrecto (`:latest`) → 404.
2. RAG sin reforzar el prompt → ignora violaciones de estándar.
3. Medir solo recall e ignorar precision (ruido).
4. Runner en la nube + Ollama local → no se ven (usa self-hosted).
5. Diff enorme → no cabe en el contexto (recórtalo o trocéalo con RAG).
6. Token/secretos en el código → van en variables de entorno / secrets.
7. Auto-merge por la review del bot → no; humano decide (y ojo con prompt injection en diffs).

## 🗺️ De qué tema sale cada pieza

Local (privacidad) · Ollama/modelo/cuantización/licencias · prompt con criterios ·
RAG · evaluación (precision/recall) · arquitectura webhook/CI · buenas prácticas de
equipo. Es todo el sistema "AI for coding" en un solo proyecto.
