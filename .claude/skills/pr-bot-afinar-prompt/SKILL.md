---
name: pr-bot-afinar-prompt
description: Iteratively improve an AI PR review bot's system prompt by running its golden-test eval, proposing prompt changes, and re-measuring — a decide/act/observe loop. Use after a reliable golden set exists, when the user wants to "afinar el prompt", "mejorar recall/precision", "reduce noise", "tune the prompt", or reduce false positives/misses. Reports before/after precision and recall.
---

# pr-bot-afinar-prompt (agente)

Optimiza el `SYSTEM_PROMPT` del bot **midiendo**, no a ojo. Es el learning loop en bucle:
correr eval → proponer cambio → re-evaluar → quedarte si mejora.

## Cuándo usarlo
- **Después** de tener un golden set fiable (usa `pr-bot-ampliar-evaluacion` primero).
- El bot mete ruido (precision baja) o se come bugs (recall bajo).

## Preguntas (AskUserQuestion)
1. **Objetivo:** subir recall / bajar ruido / equilibrio.
2. **Iteraciones máximas** (p. ej. 5) — freno anti-loop.

## Pasos (loop decidir → actuar → observar)
1. Corre `eval.py` (y `eval.py --rag`) → baseline de precision/recall.
2. **Decide** un cambio concreto del prompt (añadir criterio, afinar "qué ignorar", el "out"…).
3. **Actúa:** aplica el cambio.
4. **Observa:** re-evalúa. Si mejora el objetivo sin degradar lo otro, consérvalo; si no, revierte.
5. Repite hasta el tope de iteraciones. Reporta tabla antes/después.

## Límite humano
Aprobar el prompt final y **vigilar el overfitting**: mejorar 5 casos no es mejorar en la realidad.

## Ejemplo
```
Tú:    "sube el recall sin disparar el ruido, 5 iteraciones"
Skill: iter0 R80/P85 → iter2 R90/P86 → iter4 R95/P92   (guarda el mejor)
🙋 Apruebas el prompt final.
```
