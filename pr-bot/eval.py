"""Evaluación del revisor con golden tests.

No te fíes a ojo: mídelo. Un set fijo de diffs etiquetados y calculamos:
  - recall    = bugs cazados / bugs reales   (¿se come problemas?)
  - precision = de lo que comenta, cuánto es real (¿mete ruido?)

Corre con y sin RAG para comparar:
  python eval.py            # sin RAG
  python eval.py --rag      # con RAG (debería subir el recall sin disparar el ruido)
"""
import re
import sys

# 'must_find': basta con que la review contenga UNA de estas señales para dar el caso por
# cazado. Ojo con keywords demasiado cortas/genéricas ("di", "main"): coincidirían como
# subcadena dentro de otras palabras (có-di-go, do-main) e inflarían el recall. Usa
# términos específicos y deja que 'caught' los busque como PALABRA COMPLETA.
GOLDEN = [
    # bug/crash clásico: !! -> NPE. Regla del estándar: K-NULL-1 (evitar !!).
    {"file": "golden/force_nonnull.diff",
     "must_find": ["!!", "npe", "nullpointer", "not-null"], "rule": "K-NULL-1", "silent": False},
    # viola R-DI-4 (singleton ad-hoc con estado global). La señal fuerte es citar la regla
    # o nombrar el problema real (singleton/Hilt/inyección).
    {"file": "golden/new_singleton.diff",
     "must_find": ["r-di-4", "r-di", "singleton", "hilt", "inyección", "inyectar"],
     "rule": "R-DI-4", "silent": False},
    # viola R-COR-1 (Flow recogido en Main).
    {"file": "golden/flow_on_main.diff",
     "must_find": ["r-cor-1", "r-cor", "dispatchers.main", "main thread", "hilo principal"],
     "rule": "R-COR-1", "silent": False},
    # cambio limpio (solo un comentario): debe CALLAR.
    {"file": "golden/clean.diff", "must_find": [], "silent": True},
]


def is_silent(r: str) -> bool:
    low = r.lower()
    return "lgtm" in low or not any(l.strip().startswith("-") for l in r.splitlines())


def caught(r: str, kws) -> bool:
    """True si la review contiene alguna keyword. Las keywords puramente alfabéticas se
    exigen como PALABRA COMPLETA (word boundary) para no matchear subcadenas; las que
    llevan símbolos/espacios (`!!`, `r-di-4`, `dispatchers.main`, `main thread`) se
    buscan literalmente."""
    low = r.lower()
    for k in kws:
        kl = k.lower()
        if kl.isalpha():                      # p.ej. "singleton", "hilt"
            if re.search(rf"\b{re.escape(kl)}\b", low):
                return True
        else:                                  # p.ej. "!!", "r-di-4", "main thread"
            if kl in low:
                return True
    return False


def run(reviewer):
    tp = fp = fn = tn = 0
    cited = rule_total = 0          # 2ª métrica: ¿cita la regla del estándar?
    for c in GOLDEN:
        r = reviewer(open(c["file"], encoding="utf-8").read())
        if c["silent"]:
            if is_silent(r):
                tn += 1
            else:
                fp += 1  # comentó en un diff limpio = ruido
        else:
            if caught(r, c["must_find"]):
                tp += 1
            else:
                fn += 1  # se comió el problema
        # ¿mencionó el ID de la regla? (esto SOLO puede hacerlo con el RAG: sin el
        # CODING_STANDARDS delante, el modelo no sabe que R-DI-4 existe)
        rule = c.get("rule")
        if rule:
            rule_total += 1
            if rule.lower() in r.lower():
                cited += 1
    recall = tp / (tp + fn) if (tp + fn) else 1.0
    precision = tp / (tp + fp) if (tp + fp) else 1.0
    cite_rate = cited / rule_total if rule_total else 0.0
    print(f"TP={tp} FP={fp} FN={fn} TN={tn}")
    print(f"recall={recall:.0%}  precision={precision:.0%}  "
          f"cita_regla={cited}/{rule_total} ({cite_rate:.0%})")


if __name__ == "__main__":
    if "--rag" in sys.argv:
        from review_rag import review_diff_rag as rev
        print("== Evaluando CON RAG ==")
    else:
        from review import review_diff as rev
        print("== Evaluando SIN RAG ==")
    run(rev)
