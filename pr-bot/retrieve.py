"""RAG para el revisor de Vitals.

Recupera el contexto relevante del repo para un diff dado:
  1) CODING_STANDARDS.md (siempre) — las reglas del equipo.
  2) Los archivos .kt que DEFINEN los tipos que aparecen en el diff.

Así el bot revisa según TUS convenciones y conoce los tipos reales, en vez de
comentar de forma genérica.
"""
import os
import re
import glob

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, ".."))          # raíz del repo (pr-bot/ está dentro)
STANDARDS = os.path.join(REPO, "CODING_STANDARDS.md")
EXT = ("*.kt", "*.kts")                                    # stack Kotlin/Android
BUDGET = 6000                                             # ~presupuesto de chars para el contexto

# Carpetas de código a escanear (evitamos build/, .git/, etc.)
SRC_DIRS = ("app", "core", "feature")


def _kotlin_files():
    files = []
    for d in SRC_DIRS:
        base = os.path.join(REPO, d)
        for pat in EXT:
            files += glob.glob(os.path.join(base, "**", pat), recursive=True)
    # descarta artefactos de build
    return [f for f in files if "/build/" not in f.replace("\\", "/")]


def find_context(diff: str) -> str:
    parts = []

    # 1) Los estándares del equipo, siempre.
    if os.path.exists(STANDARDS):
        parts.append("### CODING_STANDARDS.md\n" +
                     open(STANDARDS, encoding="utf-8", errors="ignore").read())

    # 2) Tipos que aparecen en el diff (identificadores CamelCase: VitalsSnapshot, etc.)
    ids = set(re.findall(r"\b[A-Z][A-Za-z0-9]+\b", diff))

    for path in _kotlin_files():
        text = open(path, encoding="utf-8", errors="ignore").read()
        # ¿este archivo DEFINE alguno de esos tipos? (Kotlin: class/object/interface/enum class)
        if any(re.search(rf"\b(class|object|interface|enum\s+class)\s+{re.escape(i)}\b", text)
               for i in ids):
            parts.append(f"### {os.path.relpath(path, REPO)}\n{text}")

    return "\n\n".join(parts)[:BUDGET]


if __name__ == "__main__":
    import sys
    if len(sys.argv) < 2:
        print("Uso: python retrieve.py <archivo.diff>  (imprime el contexto recuperado)")
        sys.exit(1)
    print(find_context(open(sys.argv[1], encoding="utf-8").read()))
