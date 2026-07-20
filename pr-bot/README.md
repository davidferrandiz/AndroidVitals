# pr-bot — Revisor de PRs con IA local

Revisor de código automático para Vitals que corre **100% en local** con Ollama (privacidad,
coste cero), revisa según los estándares del repo vía **RAG**, se **evalúa** con golden tests
y comenta cada PR desde **GitHub Actions**.

> La IA es un **primer revisor, no la última palabra**: nunca auto-merge, el humano decide.

## Requisitos

- [Ollama](https://ollama.com) corriendo con un modelo de código:
  ```bash
  ollama run qwen3-coder      # deja el servidor en localhost:11434
  ollama list                 # confirma el TAG exacto (p. ej. qwen3-coder:latest)
  ```
- Python 3 + el paquete `openai`.

## Estructura

| Archivo | Qué es |
|---|---|
| `review.py` | Revisor local: lee un diff → LLM → comentarios. `temperature 0` (reproducible). |
| `retrieve.py` | RAG: recupera `CODING_STANDARDS.md` + los `.kt` que definen los tipos del diff. |
| `review_rag.py` | Revisor con RAG (añade el **CRITERIO 0**: hacer cumplir el estándar). |
| `eval.py` + `golden/` | Medición con golden tests: recall, precision y `cita_regla`. |
| `ejemplo.diff` | Diff con un bug a propósito para probar. |
| `../CODING_STANDARDS.md` | Las reglas del equipo (la fuente de verdad del RAG). |
| `../.github/workflows/ai-review.yml` | El workflow de CI. |
| `../.github/scripts/ci_review.py` | Revisor para CI (salida JSON). |
| `../.github/scripts/post_review.py` | Publica los hallazgos como comentarios **inline** + glosario de reglas. |

## Uso local

```bash
cd pr-bot
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt

python review.py ejemplo.diff      # revisor pelado
python eval.py                     # medición SIN RAG
python eval.py --rag               # medición CON RAG (compara)
```

`eval.py` usa rutas relativas a `golden/`, así que córrelo **desde dentro de `pr-bot/`**.

## Deploy en CI

El workflow `ai-review.yml` se dispara en cada `pull_request`, saca el diff, lo revisa con
el LLM local y publica **un review con comentarios línea a línea** (umbral configurable,
tipo `COMMENT`).

⚠️ **Necesita un self-hosted runner con Ollama**: los runners en la nube de GitHub no llegan
a `localhost:11434`. Regístralo en *Settings → Actions → Runners* en una máquina con Ollama +
Python + `gh`. El comentario usa el `GITHUB_TOKEN` que inyecta Actions (no hace falta token
propio para la v1).

## Cómo se mantiene y mejora

Este bot se montó y se mejora con las skills de `.claude/skills/` (ver su
[`README.md`](../.claude/skills/README.md)): ampliar el eval, afinar el prompt, sincronizar
estándares, subir el RAG a embeddings, comparar modelos, blindar contra prompt injection, etc.

## Trampas a evitar

1. Tag del modelo mal → error 404. Usa el exacto de `ollama list`.
2. RAG sin reforzar el prompt → ignora las reglas de arquitectura.
3. Medir solo recall → no ves el ruido (precision).
4. Runner cloud + Ollama local → no se ven. Usa self-hosted.
5. Prompt injection → el diff son datos, no instrucciones.
6. Auto-merge por el bot → nunca; el humano decide.
