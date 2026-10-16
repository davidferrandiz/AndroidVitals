"""Publica los hallazgos como UN review con comentarios inline (API de reviews de GitHub).

Lee el JSON de ci_review.py y el diff (pr.diff), mapea cada hallazgo a su línea del lado
nuevo del diff (la API exige que la línea esté en el diff), aplica el umbral de severidad
y publica un único review con comments[] línea a línea. Opcionalmente añade un resumen de
cabecera. Solo stdlib.

Config por variables de entorno:
  GITHUB_TOKEN, GITHUB_REPOSITORY (owner/repo), PR_NUMBER, COMMIT_ID (head sha)
  PR_BOT_MIN_SEVERITY = alta|media|baja   (umbral; default media)
  PR_BOT_REVIEW_EVENT = COMMENT|REQUEST_CHANGES|APPROVE   (default COMMENT)
  PR_BOT_SUMMARY = 1|0   (comentario resumen de cabecera; default 1)

Uso:  python post_review.py review.json pr.diff
"""
import json
import os
import re
import sys
import urllib.error
import urllib.request

TOKEN = os.environ["GITHUB_TOKEN"]
REPO = os.environ["GITHUB_REPOSITORY"]
PR = os.environ["PR_NUMBER"]
COMMIT = os.environ["COMMIT_ID"]
MIN_SEV = os.environ.get("PR_BOT_MIN_SEVERITY", "media").lower()
EVENT = os.environ.get("PR_BOT_REVIEW_EVENT", "COMMENT")
SUMMARY = os.environ.get("PR_BOT_SUMMARY", "1") == "1"
API = "https://api.github.com"
RANK = {"baja": 0, "media": 1, "alta": 2}

# --- Glosario de reglas: para que cada código (R-DI-4, K-NULL-1...) venga con su
# descripción legible sacada del CODING_STANDARDS.md ---
STANDARDS = os.path.join(os.path.dirname(__file__), "..", "..", "CODING_STANDARDS.md")
RULE_RE = re.compile(r"\b([A-Z]{1,3}-[A-Z]{2,6}-\d+)\b")


def load_glossary():
    g = {}
    try:
        text = open(STANDARDS, encoding="utf-8", errors="ignore").read()
    except FileNotFoundError:
        return g
    # **CÓDIGO** — descripción (la descripción puede continuar hasta el próximo bullet/regla)
    for m in re.finditer(
        r"\*\*([A-Z]{1,3}-[A-Z]{2,6}-\d+)\*\*\s*[—-]\s*(.+?)(?=\n\s*[-*]\s*\*\*|\n\s*\n|\Z)",
        text, re.S,
    ):
        desc = re.sub(r"\s+", " ", m.group(2)).strip()
        if len(desc) > 240:
            desc = desc[:237].rstrip() + "…"
        g[m.group(1)] = desc
    return g


GLOSSARY = load_glossary()


def explain_rules(note: str) -> str:
    """Texto extra con la definición legible de las reglas citadas en la nota."""
    seen = []
    for code in RULE_RE.findall(note or ""):
        if code in GLOSSARY and code not in seen:
            seen.append(code)
    return "".join(f"\n\n> 📖 **{c}** — {GLOSSARY[c]}" for c in seen)


def api_post(path: str, payload: dict):
    req = urllib.request.Request(
        API + path, data=json.dumps(payload).encode("utf-8"), method="POST",
        headers={
            "Authorization": f"Bearer {TOKEN}",
            "Accept": "application/vnd.github+json",
            "X-GitHub-Api-Version": "2022-11-28",
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, r.read().decode("utf-8", "ignore")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "ignore")


def diff_new_lines(diff_text: str):
    """Conjunto de (archivo, línea) presentes en el lado NUEVO del diff (líneas '+' y
    contexto). La API de review comments solo acepta comentar sobre esas líneas."""
    valid = set()
    path = None
    newno = 0
    for ln in diff_text.splitlines():
        if ln.startswith("+++ b/"):
            path = ln[6:]
            continue
        if ln.startswith("@@"):
            m = re.search(r"\+(\d+)", ln)
            newno = int(m.group(1)) if m else 0
            continue
        if path is None:
            continue
        if ln.startswith("+") and not ln.startswith("+++"):
            valid.add((path, newno)); newno += 1
        elif ln.startswith(" "):
            valid.add((path, newno)); newno += 1
        # las líneas '-' (borradas) no avanzan el contador del lado nuevo
    return valid


def badge(sev: str) -> str:
    return {"alta": "🔴 **Prioridad alta**", "media": "🟠 **Prioridad media**"}.get(
        sev, f"**[{sev}]**")


def main():
    findings = json.load(open(sys.argv[1], encoding="utf-8"))
    valid = diff_new_lines(open(sys.argv[2], encoding="utf-8").read())

    # 1) Umbral de severidad
    kept = [f for f in findings
            if RANK.get(str(f.get("severidad", "")).lower(), 0) >= RANK.get(MIN_SEV, 1)]
    dropped = len(findings) - len(kept)

    # 2) Mapear a línea del diff; lo que no encaje va al resumen
    comments, unanchored = [], []
    for f in kept:
        path = f.get("archivo")
        sev = str(f.get("severidad", "")).lower()
        try:
            line = int(f.get("linea"))
        except (TypeError, ValueError):
            line = None
        nota = str(f.get("nota", "")).strip()
        body = f"🤖 {badge(sev)}\n\n{nota}{explain_rules(nota)}"
        if path and line and (path, line) in valid:
            comments.append({"path": path, "line": line, "side": "RIGHT", "body": body})
        else:
            unanchored.append(f)

    # 3) Resumen de cabecera (opcional)
    body = ""
    if SUMMARY:
        parts = []
        if kept or unanchored:
            resumen = f"🤖 **AI review** — {len(comments)} comentario(s) inline"
            if unanchored:
                resumen += f", {len(unanchored)} fuera del diff"
            if dropped:
                resumen += f", {dropped} bajo umbral omitido(s)"
            parts.append(resumen + ".")
        else:
            parts.append("🤖 **AI review:** LGTM, sin problemas críticos.")
        for f in unanchored:
            parts.append(f"- {badge(str(f.get('severidad', '')).lower())} "
                         f"`{f.get('archivo')}:{f.get('linea')}` — {str(f.get('nota', '')).strip()}")
        body = "\n".join(parts)

    # 4) Publicar UN review (COMMENT no bloquea el merge)
    payload = {"commit_id": COMMIT, "event": EVENT}
    if body:
        payload["body"] = body
    if comments:
        payload["comments"] = comments
    # Un review COMMENT necesita al menos body o comments
    if not body and not comments:
        payload["body"] = "🤖 **AI review:** LGTM, sin problemas críticos."

    status, resp = api_post(f"/repos/{REPO}/pulls/{PR}/reviews", payload)
    if status in (200, 201):
        print(f"OK review publicado: {len(comments)} inline, "
              f"{len(unanchored)} en resumen, {dropped} bajo umbral.")
    else:
        print(f"ERROR {status}: {resp[:400]}")
        sys.exit(1)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Uso: python post_review.py <review.json> <pr.diff>")
        sys.exit(1)
    main()
