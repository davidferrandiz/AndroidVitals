"""Revisor de código local para Vitals (Android/Kotlin).

Lee un diff, se lo pasa a un LLM local vía Ollama e imprime la review.
El código NUNCA sale de tu máquina (privacidad, coste cero).
"""
import sys
from openai import OpenAI

# Ollama expone una API compatible con OpenAI en :11434. api_key es dummy (es local).
client = OpenAI(base_url="http://localhost:11434/v1", api_key="ollama")

# OJO: usa el TAG exacto que te dé `ollama list`.
MODEL = "qwen3-coder:latest"

SYSTEM_PROMPT = """Eres un revisor de código Kotlin/Android senior. Revisa SOLO el diff.
Busca, por prioridad: 1) bugs/crashes  2) seguridad  3) casos límite.
Bugs típicos de Kotlin/Android a cazar: `!!` (not-null assertion), NullPointerException,
lateinit usado antes de init, leaks de coroutines/Flows, fugas de Context/View, trabajo
pesado en el hilo principal.
IGNORA el estilo (ya lo cubre el linter: ktlint / detekt).
Formato de salida: - [alta|media] archivo:línea — problema — sugerencia
Si no hay nada relevante, responde exactamente: "LGTM, sin problemas críticos."
No inventes problemas para rellenar."""


def review_diff(diff: str) -> str:
    resp = client.chat.completions.create(
        model=MODEL,
        temperature=0,  # temperature 0 = reviews reproducibles
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": f"Revisa este diff:\n{diff}"},
        ],
    )
    return resp.choices[0].message.content


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python review.py <archivo.diff>")
        sys.exit(1)
    print(review_diff(open(sys.argv[1], encoding="utf-8").read()))
