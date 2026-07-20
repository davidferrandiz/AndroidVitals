---
name: pr-bot-actualizar-estandares
description: Keep an AI PR review bot's CODING_STANDARDS.md (its RAG context) in sync with the repo's linter config and evolving conventions, so the bot never judges with stale rules. Use when linter rules change, new conventions appear, or the user wants to "sincronizar standards", "actualizar CODING_STANDARDS", "keep the RAG fresh", "update coding standards".
---

# pr-bot-actualizar-estandares

Mantiene `CODING_STANDARDS.md` (el contexto que consume el RAG) alineado con la config
del linter y las convenciones reales del repo. Un contexto viejo degrada el bot **en silencio**.

## Cuándo usarlo
- Cambió `.swiftlint.yml` / `detekt` / `ktlint`.
- Aparecieron convenciones nuevas (arquitectura, DI, concurrencia).
- El bot cita reglas que ya no aplican, o se pierde reglas nuevas.

## Preguntas (AskUserQuestion)
1. **Fuente de la verdad:** el linter, un doc, o ambos.
2. ¿Escanear el código para inferir convenciones nuevas?

## Pasos
1. Lee la config del linter (reglas activas, límites).
2. (Opcional) Escanea el repo en busca de patrones dominantes (p. ej. DI por init, actores).
3. Actualiza `CODING_STANDARDS.md`: reglas duras del linter + convenciones de arquitectura.
4. Muestra el diff de cambios del documento.

## Límite humano
Aprobar cambios de **política de estilo** (qué es regla dura vs recomendación).

## Ejemplo
```
Tú:    "sincroniza el estándar con el linter"
Skill: ¿Fuente? → .swiftlint.yml + escanear código
→ añade "sin try!/as!", detecta "repos como actor" como convención → actualiza el doc
🙋 Apruebas la política.
```
