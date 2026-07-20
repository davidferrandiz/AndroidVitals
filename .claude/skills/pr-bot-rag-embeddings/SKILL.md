---
name: pr-bot-rag-embeddings
description: Upgrade an AI PR review bot's RAG retrieval from keyword/symbol matching to real semantic embeddings with similarity search. Use when symbol-based retrieval misses semantically related code, or the user wants "embeddings", "mejorar el RAG", "semantic search", "similarity search", "subir el retrieval". Sets up a local embedding model and a vector index.
---

# pr-bot-rag-embeddings

Sube el retrieval del bot de "por símbolos" (regex de nombres) a **embeddings reales**:
encuentra código relacionado **en significado**, no solo por nombre exacto.

## Cuándo usarlo
- El retrieval por símbolos se pierde código relevante que no comparte nombre.
- El repo es grande y quieres recuperación por similitud semántica.

## Preguntas (AskUserQuestion)
1. **Modelo de embeddings** (p. ej. `nomic-embed-text` en Ollama).
2. **¿Persistir el índice** en disco o reconstruirlo cada vez?
3. **Chunking:** por función/archivo o por ~512 tokens con solape.

## Pasos
1. `ollama pull <modelo-embeddings>`.
2. **Indexa (una vez):** trocea el repo, embebe cada chunk, guarda vectores (numpy/SQLite/Chroma).
   Prefija cada chunk con "archivo → tipo" (contextual retrieval) para menos fallos.
3. Reescribe `retrieve.py`: embebe el diff/consulta y trae los **k vecinos más cercanos** (coseno).
4. Verifica contra un diff de ejemplo que el contexto recuperado es más relevante.

## Límite humano
Decidir **cada cuánto reindexar** (coste vs frescura) y el modelo (licencia, tamaño).

## Ejemplo
```
Tú:    "sube el retrieval a embeddings"
Skill: ¿Modelo? → nomic-embed-text   ¿Índice en disco? → sí
→ pull + indexa el repo + reescribe retrieve.py (embed query → top-k por coseno)
```
