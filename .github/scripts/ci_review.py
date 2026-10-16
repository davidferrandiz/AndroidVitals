"""Revisor para CI — salida en JSON estructurado (para publicar comentarios inline).

Autónomo: lee un diff (pr.diff), carga CODING_STANDARDS.md desde la raíz del repo (RAG
ligero), llama al LLM local vía Ollama y devuelve un ARRAY JSON de hallazgos. Ese JSON lo
consume post_review.py para publicar un review con comentarios línea a línea.

Uso:  python ci_review.py pr.diff > review.json
"""
import json
import os
import re
import sys
from openai import OpenAI

client = OpenAI(base_url="http://localhost:11434/v1", api_key="ollama")
MODEL = os.environ.get("PR_BOT_MODEL", "qwen3-coder:latest")

# raíz del repo = dos niveles por encima de .github/scripts/
REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
STANDARDS = os.path.join(REPO, "CODING_STANDARDS.md")

SYSTEM_PROMPT = """Eres un revisor de código Kotlin/Android senior. Revisa SOLO el diff.
Prioridad: 1) bugs/crashes  2) seguridad  3) casos límite. Bugs típicos de Kotlin/Android:
`!!`, NullPointerException, lateinit sin init, leaks de coroutines/Flows, fugas de Context,
trabajo pesado en el hilo principal. IGNORA el estilo (lo cubre ktlint/detekt).

CRITERIO 0 (lo primero): violaciones de CODING_STANDARDS → severidad "alta" AUNQUE no sean
crashes; la arquitectura NO es "estilo".

Escribe la "nota" SIEMPRE en lenguaje claro que entienda cualquier desarrollador, en este
orden: 1) QUÉ está mal y por qué es un problema (consecuencia concreta), 2) CÓMO arreglarlo.
Si aplica una regla del estándar, menciónala AL FINAL entre paréntesis con su código,
p. ej. "(regla R-DI-4)". NUNCA empieces la nota con "viola X:" ni uses el código como si se
entendiera por sí solo — el código es solo una referencia, la explicación va en palabras.

DEVUELVE EXCLUSIVAMENTE un array JSON válido (sin texto alrededor, sin ```), un objeto por
hallazgo, con estas claves exactas:
  {"archivo": "<ruta tal cual aparece en el diff>", "linea": <entero, línea del lado nuevo>,
   "severidad": "alta" | "media", "nota": "<explicación clara + (regla X) al final si aplica>"}
Si no hay hallazgos, devuelve exactamente: []
No inventes problemas para rellenar."""


def _extract_json(text: str):
    text = text.strip()
    text = re.sub(r"^```(?:json)?", "", text).strip()
    text = re.sub(r"```$", "", text).strip()
    i, j = text.find("["), text.rfind("]")
    if i != -1 and j != -1:
        text = text[i:j + 1]
    return json.loads(text)


def review(diff: str):
    standards = ""
    if os.path.exists(STANDARDS):
        standards = "CODING_STANDARDS:\n" + open(STANDARDS, encoding="utf-8", errors="ignore").read()[:6000]
    resp = client.chat.completions.create(
        model=MODEL, temperature=0,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"{standards}\n\n---\nRevisa este diff:\n{diff}"},
        ],
    )
    raw = resp.choices[0].message.content
    try:
        findings = _extract_json(raw)
        return findings if isinstance(findings, list) else []
    except Exception:
        # Si el modelo no devolvió JSON válido, no rompemos el CI: cero hallazgos.
        return []


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("[]")
        sys.exit(0)
    diff = open(sys.argv[1], encoding="utf-8").read()
    findings = review(diff) if diff.strip() else []
    print(json.dumps(findings, ensure_ascii=False, indent=2))
