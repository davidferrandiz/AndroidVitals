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
        body = f"🤖 {badge(sev)}\n\n{str(f.get('nota', '')).strip()}"
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
