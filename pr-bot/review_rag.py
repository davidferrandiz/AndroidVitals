"""Revisor con RAG: mismo motor que review.py, pero inyectando el contexto del repo
(CODING_STANDARDS.md + tipos del diff) y REFORZANDO el prompt para que haga cumplir
los estándares.

Lección clave: RAG da el CONTEXTO, pero el PROMPT decide qué hacer con él. Sin este
refuerzo, el modelo trata las reglas de arquitectura como "estilo" y las ignora aunque
tenga el documento delante.
"""
from review import client, MODEL, SYSTEM_PROMPT
from retrieve import find_context

RAG_SYSTEM = SYSTEM_PROMPT + """

CRITERIO 0 (lo primero, antes que los bugs): violaciones de CODING_STANDARDS.
Márcalas como [alta] AUNQUE no sean crashes. La arquitectura (grafo de módulos, DI,
singletons, concurrencia) NO es "estilo". CITA la regla incumplida (p. ej. "viola R-DI-4").
Usa los archivos del repo que se te dan como CONTEXTO para entender los tipos reales."""


def review_diff_rag(diff: str) -> str:
    ctx = find_context(diff)
    resp = client.chat.completions.create(
        model=MODEL,
        temperature=0,
        messages=[
            {"role": "system", "content": RAG_SYSTEM},
            {"role": "user",
             "content": f"CONTEXTO DEL REPO:\n{ctx}\n\n---\nRevisa este diff:\n{diff}"},
        ],
    )
    return resp.choices[0].message.content


if __name__ == "__main__":
    import sys
    if len(sys.argv) < 2:
        print("Uso: python review_rag.py <archivo.diff>")
        sys.exit(1)
    print(review_diff_rag(open(sys.argv[1], encoding="utf-8").read()))
