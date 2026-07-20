---
name: pr-bot-recoger-feedback
description: Capture developers' accept/reject decisions on the PR bot's comments and turn them into a preference dataset (chosen/rejected pairs for DPO, plus SFT examples) — the "quality dataset" for later fine-tuning. Use when the team wants to move toward fine-tuning, close the learning loop, or the user asks "recoger feedback", "dataset DPO", "accept/reject dataset", "collect training data from reviews".
---

# pr-bot-recoger-feedback (agente)

Convierte cada **accept/reject** de los devs sobre los comentarios del bot en **datos de
preferencia** (pares elegido/rechazado para DPO + ejemplos SFT). Es el "dataset de calidad"
con el que, más adelante, afinas (LoRA/DPO). El eslabón que cierra el learning loop.

## Cuándo usarlo
- Hay volumen de PRs y quieres empezar a acumular datos.
- Piensas en fine-tuning **a futuro** (antes: prompt y RAG suelen bastar).

## Preguntas (AskUserQuestion)
1. **Fuente del feedback:** reacciones al comentario (👍/👎) / resolución del hilo /
   si el dev editó la sugerencia.
2. **Dónde guardo** el dataset (archivo JSONL local, repo interno).

## Pasos
1. Lee el feedback de los PRs (API de GitHub: reacciones, hilos resueltos).
2. Construye registros: para SFT `(diff → comentario aceptado)`; para DPO
   `(diff, comentario_elegido, comentario_rechazado)`.
3. Escribe el dataset en JSONL, deduplicando.

## Límite humano
**Curar** el dataset (basura entra, basura sale) y decidir **cuándo** afinar. La calidad manda.

## Ejemplo
```
Tú:    "empieza a recoger feedback de los reviews del bot"
Skill: ¿Fuente? → reacciones + edición del dev   ¿Dónde? → data/preferences.jsonl
→ genera pares elegido/rechazado a partir de los PRs cerrados
🙋 Curas el dataset; decides el umbral para afinar.
```
