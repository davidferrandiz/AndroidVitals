---
name: pr-bot-comparar-modelos
description: Benchmark several local LLMs on the PR bot's own golden set to pick the best model for this repo, using data instead of hype. Use when choosing or switching the review model, or when the user asks "qué modelo uso", "comparar modelos", "benchmark models", "which local model is best for my repo". Outputs a precision/recall/latency table and a recommendation.
---

# pr-bot-comparar-modelos

Compara varios modelos locales en **TU golden set** y te dice cuál rinde mejor para tu
repo. El "mejor modelo" depende de tu código: se elige con datos, no por hype.

## Cuándo usarlo
- Vas a elegir o cambiar el modelo del bot.
- Salió un modelo nuevo y quieres saber si merece la pena.

## Preguntas (AskUserQuestion)
1. **¿Qué modelos comparo?** (p. ej. qwen3-coder, deepseek-coder, codestral, qwen2.5-coder).
2. **¿Priorizas calidad o velocidad?** (para la recomendación).
3. ¿Con RAG, sin RAG, o ambos?

## Pasos
1. Para cada modelo: `ollama pull <m>` si falta, apunta `MODEL` a él, corre `eval.py`.
2. Registra **precision, recall y latencia** (tiempo por review) de cada uno.
3. Muestra una **tabla comparativa** y **recomienda** según el criterio elegido.

## Límite humano
**Validar la licencia** del modelo ganador (uso comercial — ¡ojo Codestral!) y su tamaño/RAM.

## Ejemplo
```
Tú:    "compara qwen3-coder vs deepseek-coder vs codestral, priorizo calidad"
Skill: | modelo | recall | precision | latencia |
       ...tabla... → recomienda deepseek-coder (mejor recall) 
🙋 Revisas la licencia de Codestral antes de descartarlo/elegirlo.
```
