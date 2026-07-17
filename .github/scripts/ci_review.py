"""Revisor para CI (GitHub Actions).

Autónomo: lee un archivo diff (pr.diff), carga CODING_STANDARDS.md desde la raíz del
repo si existe (RAG ligero), llama al LLM local vía Ollama e imprime la review.
Lo impreso va al comentario del PR.

Uso en el workflow:  python .github/scripts/ci_review.py pr.diff > review.txt
"""
import os
import sys
from openai import OpenAI

client = OpenAI(base_url="http://localhost:11434/v1", api_key="ollama")
MODEL = os.environ.get("PR_BOT_MODEL", "qwen3-coder:latest")

# raíz del repo = dos niveles por encima de .github/scripts/
REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
STANDARDS = os.path.join(REPO, "CODING_STANDARDS.md")

SYSTEM_PROMPT = """Eres un revisor de código Kotlin/Android senior. Revisa SOLO el diff.
Busca, por prioridad: 1) bugs/crashes  2) seguridad  3) casos límite.
Bugs típicos de Kotlin/Android: `!!`, NullPointerException, lateinit sin init, leaks de
coroutines/Flows, fugas de Context/View, trabajo pesado en el hilo principal.
IGNORA el estilo (ya lo cubre ktlint / detekt).

CRITERIO 0 (lo primero): violaciones de CODING_STANDARDS. Márcalas como [alta] AUNQUE no
sean crashes; la arquitectura NO es "estilo". CITA la regla (p. ej. "viola R-DI-4").

FORMATO DE SALIDA (obligatorio, para que un script lo publique línea a línea):
- Un hallazgo por línea, EXACTAMENTE así: - [alta|media] archivo:línea — problema — sugerencia
- Usa la ruta y el número de línea reales del diff (la del lado nuevo, con '+').
- Si hay al menos un hallazgo, NO escribas ninguna línea de "LGTM".
- Escribe únicamente "LGTM, sin problemas críticos." (y nada más) SOLO si no hay hallazgos.
No inventes problemas para rellenar."""


def review(diff: str) -> str:
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
    return resp.choices[0].message.content


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python ci_review.py <pr.diff>")
        sys.exit(1)
    diff = open(sys.argv[1], encoding="utf-8").read()
    if not diff.strip():
        print("LGTM, sin problemas críticos.")
        sys.exit(0)
    print(review(diff))
