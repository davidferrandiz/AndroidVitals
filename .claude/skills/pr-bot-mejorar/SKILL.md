---
name: pr-bot-mejorar
description: Orchestrator that improves an existing AI PR review bot. It diagnoses the bot's current state, asks the user what they want to improve, explains the options and trade-offs, and then runs the right complementary skills in the right order (pr-bot-ampliar-evaluacion, pr-bot-afinar-prompt, pr-bot-actualizar-estandares, pr-bot-rag-embeddings, pr-bot-seguridad-inyeccion, pr-bot-comparar-modelos, pr-bot-recoger-feedback, pr-bot-comentarios-en-linea). Use when the user wants to "mejorar mi PR bot", "improve my code review bot", "optimize my PR bot", "qué mejoro en mi bot", "mi bot mete ruido / se come bugs", or isn't sure what to improve and wants guidance.
---

# pr-bot-mejorar (super-agente / orquestador)

El "manager" del bot: **diagnostica → pregunta el objetivo → explica opciones →
delega en la skill correcta → reporta**, y repite. No hace el trabajo fino él mismo:
**orquesta** las skills complementarias en el orden adecuado.

## Paso 1 — Diagnosticar (antes de preguntar)
- Detecta qué existe: ¿hay `eval.py` + `golden/`? ¿RAG (`retrieve.py`)? ¿deploy (webhook/CI)?
- Si hay eval, **córrelo** (`eval.py` y `--rag`) para tener un baseline de precision/recall.
- Resume el estado en 2 líneas ("tienes RAG y eval con 5 casos; recall 75, precision 80").

## Paso 2 — Preguntar el objetivo (AskUserQuestion)
Ofrece opciones en lenguaje de problema, no de herramienta:

| El usuario dice… | Problema | Skill(s) que orquesta |
|---|---|---|
| "mete demasiado ruido" | precision baja | `pr-bot-afinar-prompt` (tras `pr-bot-ampliar-evaluacion`) |
| "se come bugs / no cita nuestras reglas" | recall bajo / contexto | `pr-bot-actualizar-estandares` → `pr-bot-afinar-prompt` / `pr-bot-rag-embeddings` |
| "no me fío del eval" | medición pobre | `pr-bot-ampliar-evaluacion` |
| "el RAG se queda corto" | retrieval débil | `pr-bot-rag-embeddings` |
| "¿es seguro?" | prompt injection | `pr-bot-seguridad-inyeccion` |
| "¿qué modelo uso?" | elección de motor | `pr-bot-comparar-modelos` |
| "quiero datos para fine-tuning" | dataset | `pr-bot-recoger-feedback` |
| "feedback más accionable" | UX | `pr-bot-comentarios-en-linea` |
| "no sé, recomiéndame" | — | recomienda según el diagnóstico |

## Paso 3 — Explicar antes de actuar
Di **qué** skill vas a usar, **por qué**, el **trade-off** y los **prerrequisitos**.
Regla de dependencias clave: **sin un golden set fiable, no se afina nada** → si el eval
es pobre, propón `pr-bot-ampliar-evaluacion` primero aunque el objetivo sea otro.

## Paso 4 — Delegar y gestionar
- Invoca la(s) skill(s) en orden; pásales el contexto (objetivo, baseline).
- Al terminar, **re-mide** y reporta antes/después.
- Pregunta "¿seguimos con otra mejora?" y vuelve al Paso 2.

## Orden por defecto (si "recomiéndame")
`pr-bot-ampliar-evaluacion` → `pr-bot-afinar-prompt` → `pr-bot-actualizar-estandares` → `pr-bot-rag-embeddings` →
`pr-bot-seguridad-inyeccion` → `pr-bot-comparar-modelos` → `pr-bot-recoger-feedback` → `pr-bot-comentarios-en-linea`.

## Límite humano
Aprobar prompts finales, políticas de estilo/permisos y curación de datos. El super-agente
**recomienda y ejecuta lo verificable; el humano decide** lo de criterio, secretos e infra.

## Ejemplo
```
Tú:    "quiero mejorar mi PR bot"
Skill: [diagnostica] "Tienes RAG + eval de 5 casos, recall 75/precision 80."
Skill: ¿Qué te molesta más? → "se come algún bug"
Skill: "Antes de afinar, tu eval es pequeño (5 casos) y no es fiable.
        Propongo: 1) pr-bot-ampliar-evaluacion para medir bien, 2) pr-bot-afinar-prompt. ¿Vamos?"
Tú:    "vale"
→ ejecuta pr-bot-ampliar-evaluacion (eval a 20 casos) → pr-bot-afinar-prompt (recall 75→92)
Skill: "Recall 75→92, precision se mantiene. ¿Otra mejora?"
```
