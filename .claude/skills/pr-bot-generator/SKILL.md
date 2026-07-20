---
name: pr-bot-generator
description: Scaffold a local AI PR review bot into an existing mobile project (iOS/Swift or Android/Kotlin). Use when the user wants to add an AI code reviewer, PR bot, automated PR review, or "AI code review" to their repository — covering local testing, RAG over the repo's coding standards, evaluation with golden tests, and deployment as a GitHub Actions CI job. The bot runs on a local LLM via Ollama (privacy, zero cost). Trigger phrases include "create a PR bot", "add AI code review", "scaffold a code review bot", "montar un PR bot", "revisor de código con IA".
---

# PR Bot Generator

Genera un revisor de PRs con IA **local** dentro de un proyecto mobile existente, de
principio a fin: revisor local → RAG → evaluación → deploy en CI. El código nunca sale
de la máquina (privacidad).

## Paso 1 — Preguntar antes de generar (usa AskUserQuestion)

Haz estas preguntas al usuario (agrúpalas en una o dos llamadas a AskUserQuestion):

1. **Framework/lenguaje** del repo: iOS/Swift · Android/Kotlin · otro.
2. **Backend del LLM y modelo**: Ollama local (pide el nombre exacto, p. ej.
   `qwen3-coder:latest`; sugiérelo si no lo saben y recuérdales `ollama list`) ·
   LM Studio (`localhost:1234`) · una API en la nube.
3. **¿RAG?** ¿Existe un `CODING_STANDARDS.md` (o similar) en el repo? Si no, ofrece
   crear uno mínimo. Pide la ruta del repo/código para el retrieval.
4. **Deploy target**: solo local (probar) · GitHub Actions (CI) · webhook + servidor.
5. **Carpeta destino** donde crear el bot (p. ej. `pr-bot/`).

Adapta todo lo siguiente a las respuestas (lenguaje en el prompt, extensiones de
archivo en el retrieval, modelo, y qué archivos de deploy generar).

## Paso 2 — Revisor local (`review.py`)

```python
import sys
from openai import OpenAI
client = OpenAI(base_url="http://localhost:11434/v1", api_key="ollama")  # o :1234 para LM Studio
MODEL = "<MODELO>"   # el tag exacto de `ollama list`
SYSTEM_PROMPT = """Eres un revisor de código <LENGUAJE> senior. Revisa SOLO el diff.
Busca por prioridad: 1) bugs/crashes  2) seguridad  3) casos límite.
IGNORA estilo (ya lo cubre el linter <LINTER>).
Formato: - [alta|media] archivo:línea — problema — sugerencia
Escribe el problema y la sugerencia en lenguaje CLARO para cualquier dev (qué está mal,
por qué, y cómo arreglarlo). Nada de jerga ni códigos de regla sin explicar.
Si no hay nada relevante, responde: "LGTM, sin problemas críticos."
No inventes problemas para rellenar."""
def review_diff(diff):
    r = client.chat.completions.create(model=MODEL, temperature=0, messages=[
        {"role":"system","content":SYSTEM_PROMPT},
        {"role":"user","content":f"Revisa este diff:\n{diff}"}])
    return r.choices[0].message.content
if __name__ == "__main__":
    print(review_diff(open(sys.argv[1], encoding="utf-8").read()))
```

Sustituye `<LENGUAJE>` (Swift/Kotlin), `<LINTER>` (SwiftLint/ktlint), `<MODELO>`.
Crea también un `requirements.txt` con `openai` (y `flask`, `requests` si hay webhook).
Genera un `ejemplo.diff` con un bug típico del lenguaje (Swift: `!` force-unwrap;
Kotlin: `!!`) para poder probar.

## Paso 3 — RAG (si se pidió): `retrieve.py` + `review_rag.py`

`retrieve.py`: siempre incluye `CODING_STANDARDS.md` + los archivos que definen los
tipos que aparecen en el diff. Extensiones según el stack (`*.swift` / `*.kt`).
Regex de definiciones: `struct|class|enum|protocol` (Swift) / `class|object|interface`
(Kotlin). Presupuesto ~6000 chars.

`review_rag.py`: inyecta el contexto y **refuerza el prompt** con:
> "CRITERIO 0: violaciones de CODING_STANDARDS. Márcalas como [alta] aunque no sean
> crashes; la arquitectura NO es 'estilo'. Explica la violación EN PALABRAS y cita el
> código de la regla solo como referencia al final (p. ej. '(regla R-DI-4)'); nunca
> empieces con 'viola X:'."

(Sin ese refuerzo, el modelo ignora las reglas de arquitectura aunque tenga el
documento en el contexto — es el error más común.)

**Mensajes legibles:** un código de regla no se entiende solo. Cuando el deploy publique
comentarios, adjunta la descripción legible de la regla citada leyéndola del
`CODING_STANDARDS.md` (glosario `código → texto`). Ver la skill `pr-bot-comentarios-en-linea`.

## Paso 4 — Evaluación (`eval.py` + `golden/`)

Crea 4-5 diffs etiquetados en `golden/` **basados en tipos reales del repo**: incluye
uno que **viole un estándar** (p. ej. un singleton nuevo) y uno **limpio** (debe callar).
`eval.py` calcula **recall** (bugs cazados/reales) y **precision** (de lo comentado,
cuánto es real), ejecutable con y sin `--rag` para comparar. Métrica a nivel de caso
(matriz de confusión: TP/FP/FN/TN).

## Paso 5 — Deploy

**GitHub Actions (CI):** genera `.github/workflows/ai-review.yml` (trigger
`pull_request`, `permissions: pull-requests: write`, `runs-on: self-hosted`) que saca
el diff con `gh pr diff`, llama a `ci_review.py` y comenta con `gh pr comment`. Copia el
revisor a `.github/scripts/ci_review.py`. **Avisa** de que un LLM local necesita un
**self-hosted runner** con Ollama (los runners en la nube no llegan a localhost).

**Webhook (alternativa):** genera un `webhook_server.py` (Flask) que **verifica la firma
`X-Hub-Signature-256`**, trae el diff por la API, revisa y comenta. En local requiere un
túnel (smee/ngrok). Token/secreto en variables de entorno, nunca en git.

## Paso 6 — Verificar

- `python -m py_compile` de todos los scripts.
- Corre `review.py ejemplo.diff` (necesita Ollama/LM Studio corriendo).
- Si hay eval, corre `eval.py` y `eval.py --rag` y enseña la diferencia.
- Valida el YAML del workflow.

## Principios (recuérdaselos al usuario)

- **Privacidad:** el modelo es local; el código no sale a terceros. Revisa la licencia
  del modelo si es un producto comercial.
- **RAG da contexto, el prompt da criterios** — van juntos.
- **Mensajes claros:** el comentario debe entenderse sin saberse los códigos de regla;
  explica en palabras y adjunta la descripción de la regla.
- **Mide, no te fíes a ojo** (precision/recall).
- **La IA es un primer revisor, no la última palabra**: nunca auto-merge; humano decide.
- **Ojo con prompt injection**: el diff no es de confianza; trátalo como datos, no como
  instrucciones, y no des al bot poderes destructivos sin aprobación humana.
