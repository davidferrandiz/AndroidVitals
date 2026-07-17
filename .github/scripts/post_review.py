"""Publica la review como comentarios INLINE separados en el PR (estilo Gemini).

Lee el texto del revisor (líneas '- [sev] archivo:línea — mensaje') y publica CADA
hallazgo como un comentario de revisión anclado a su archivo:línea vía la API de GitHub.
Los hallazgos cuya línea no esté en el diff (no anclables) se agrupan en un comentario
resumen para no perderlos.

Sin dependencias externas: solo stdlib (urllib). Variables de entorno esperadas:
  GITHUB_TOKEN, GITHUB_REPOSITORY (owner/repo), PR_NUMBER, COMMIT_ID (head sha).
"""
import json
import os
import re
import sys
import urllib.error
import urllib.request

TOKEN = os.environ["GITHUB_TOKEN"]
REPO = os.environ["GITHUB_REPOSITORY"]          # owner/repo (lo inyecta Actions)
PR = os.environ["PR_NUMBER"]
COMMIT = os.environ["COMMIT_ID"]                 # head sha del PR
API = "https://api.github.com"

# - [alta] ruta/al/archivo.kt:42 — problema — sugerencia
FINDING = re.compile(
    r"^\s*[-*]\s*\[(?P<sev>[^\]]+)\]\s+(?P<path>\S+?):(?P<line>\d+)\s*[—:-]+\s*(?P<body>.+)$"
)


def api_post(path: str, payload: dict):
    req = urllib.request.Request(
        API + path,
        data=json.dumps(payload).encode("utf-8"),
        method="POST",
        headers={
            "Authorization": f"Bearer {TOKEN}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, None
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


def badge(sev: str) -> str:
    s = sev.strip().lower()
    if s in ("alta", "high"):
        return "🔴 **Prioridad alta**"
    if s in ("media", "medium"):
        return "🟠 **Prioridad media**"
    return f"**[{sev.strip()}]**"


def main():
    text = open(sys.argv[1], encoding="utf-8").read()
    findings = [m.groupdict() for m in map(FINDING.match, text.splitlines()) if m]

    # Sin hallazgos → un único comentario LGTM.
    if not findings:
        api_post(f"/repos/{REPO}/issues/{PR}/comments",
                 {"body": "🤖 **AI review:** LGTM, sin problemas críticos."})
        print("Sin hallazgos: publicado LGTM.")
        return

    unanchored = []
    for f in findings:
        body = f"🤖 {badge(f['sev'])}\n\n{f['body'].strip()}"
        status, err = api_post(
            f"/repos/{REPO}/pulls/{PR}/comments",
            {"body": body, "commit_id": COMMIT, "path": f["path"],
             "line": int(f["line"]), "side": "RIGHT"},
        )
        if status in (200, 201):
            print(f"OK inline  {f['path']}:{f['line']}")
        else:
            print(f"WARN inline {status} en {f['path']}:{f['line']} -> resumen  ({err[:120] if err else ''})")
            unanchored.append(f)

    # Los que no se pudieron anclar (línea fuera del diff) → un comentario resumen.
    if unanchored:
        summary = "\n".join(
            f"- 🤖 {badge(f['sev'])} `{f['path']}:{f['line']}` — {f['body'].strip()}"
            for f in unanchored
        )
        api_post(f"/repos/{REPO}/issues/{PR}/comments",
                 {"body": "🤖 **AI review** — hallazgos fuera del diff:\n\n" + summary})

    print(f"Publicados {len(findings) - len(unanchored)} inline, {len(unanchored)} en resumen.")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python post_review.py <review.txt>")
        sys.exit(1)
    main()
