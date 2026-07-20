---
name: pr-bot-comentarios-en-linea
description: Evolve an AI PR review bot from a single summary comment to inline, line-by-line review comments on the PR. Use when the user wants "comentarios inline", "línea a línea", "inline comments", "review comments on the diff", or more actionable feedback next to the code. Maps findings to file/line/position and posts via the GitHub review API.
---

# pr-bot-comentarios-en-linea

Evoluciona el deploy del bot para dejar comentarios **línea a línea** (junto al código)
en vez de un único comentario resumen. Feedback más accionable.

## Cuándo usarlo
- El comentario resumen se queda corto: quieres el apunte pegado a la línea.
- El equipo pide feedback más granular en el PR.

## Preguntas (AskUserQuestion)
1. **Umbral de severidad** para comentar inline (p. ej. solo [alta]) — evita saturar el PR.
2. ¿Mantener también un comentario resumen arriba?

## Pasos
1. Pide al LLM la salida en **formato estructurado** (JSON con `archivo`, `linea`, `severidad`, `nota`).
   La `nota` va SIEMPRE en lenguaje claro (qué está mal, por qué, y cómo arreglarlo); si cita
   una regla del estándar, va como referencia al final entre paréntesis (p. ej. "(regla R-DI-4)"),
   nunca empezando con "viola X:".
2. Mapea cada hallazgo a la **posición en el diff** (hunk/línea) que exige la API de GitHub.
3. Publica un **review** con `comments[]` inline vía la API (`POST .../pulls/{n}/reviews`).
4. Aplica el umbral: descarta lo que esté por debajo para no hacer ruido.
5. **Mensajes auto-explicativos (glosario de reglas).** Un código como `R-DI-4` o `K-NULL-1`
   no se entiende solo. Construye un glosario `código → descripción` leyendo el
   `CODING_STANDARDS.md` (regex sobre las líneas `**R-XXX-N** — ...`) y, cuando un comentario
   cite una regla, **adjunta su descripción legible debajo** (una cita, p. ej.
   `> 📖 **R-DI-4** — Prohibidos los singletons ad-hoc...`). Así nadie necesita saberse los
   códigos de memoria y el comentario se entiende por sí solo.

## Límite humano
Fijar el **umbral** y el tono; decidir si el review es `COMMENT` (no bloquea) o `REQUEST_CHANGES`.

## Ejemplo
```
Tú:    "pasa a comentarios inline, solo severidad alta"
Skill: ¿Umbral? → alta   ¿Resumen arriba? → sí
→ el bot comenta en la línea exacta del force-unwrap, en lenguaje claro, con la
   definición de la regla citada adjunta, y deja un resumen arriba
🙋 Ajustas el umbral si satura.
```
