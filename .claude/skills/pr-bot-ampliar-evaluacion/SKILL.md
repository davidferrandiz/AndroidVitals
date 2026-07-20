---
name: pr-bot-ampliar-evaluacion
description: Grow the evaluation golden set of an AI PR review bot with realistic, labeled test cases mined from the repo's history. Use when the bot's eval is too small or unreliable, before tuning prompts, or when the user wants "más casos de evaluación", "ampliar el golden set", "grow the eval set", "better test coverage for my PR bot". Produces new golden/*.diff files (bug cases + clean cases) and labels them in eval.py.
---

# pr-bot-ampliar-evaluacion

Amplía el set de golden tests del PR bot con casos **realistas y etiquetados**, sacados
de la historia del repo. Un eval fiable es la base de TODO lo demás.

## Cuándo usarlo
- El eval tiene pocos casos (p. ej. 5) → 100% no es fiable.
- Antes de `pr-bot-afinar-prompt` (no puedes optimizar contra una medición pobre).
- Todo pasa siempre → faltan casos difíciles/negativos.

## Preguntas (AskUserQuestion)
1. **Fuente de casos:** commits de fix / PRs cerrados / reverts / bugs de issues.
2. **Cuántos casos** añadir.
3. **¿Incluir casos limpios** (deben callar) para medir precision? ¿cuántos?

## Pasos
1. Escanea `git log` buscando commits tipo *fix/bug/revert*; extrae el diff **anterior al fix**
   (el que contenía el bug) como caso con bug.
2. Para casos limpios, coge diffs de refactors/features sin incidencias posteriores.
3. Escribe cada uno en `golden/<nombre>.diff` y añádelo a la lista `GOLDEN` de `eval.py`
   con `must_find` (palabras esperadas) o `silent: True`.
4. Mantén equilibrio: varios con bug + algunos limpios.
5. Corre `eval.py` para confirmar que cargan bien.

## Límite humano
Revisar/confirmar las etiquetas: **la 'verdad' la pone una persona**, no se auto-etiqueta a ciegas.

## Ejemplo
```
Tú:    "amplía el golden set con los fixes del último trimestre"
Skill: ¿Fuente? → commits de fix   ¿Cuántos? → 10   ¿Limpios? → 3
→ crea 13 golden/*.diff etiquetados + los registra en eval.py
🙋 Confirmas etiquetas dudosas.
```
