"""Genera el icono de Vitals (capas adaptativas en SVG y VectorDrawable) y previews.

Uso: python3 gen_icon.py <carpeta_salida> "#EAF6FC"  (luego copiar ic_launcher_*.xml a app/src/main/res/drawable)

Geometría en el viewport 108x108 de los iconos adaptativos (zona segura: círculo r=33 en el centro).
"""
import math, sys, os

OUT = sys.argv[1]
PULSE_COLOR = sys.argv[2] if len(sys.argv) > 2 else "#EAF6FC"

C = 54.0
R = 27.0          # radio del anillo
RING_W = 5.6
GAP = 7.0         # grados entre segmentos
START, SWEEP = 135.0, 270.0   # hueco abajo, como un indicador
COLORS = ["#4FC3F7", "#CE93D8", "#81C784", "#FFB74D"]  # cpu, memoria, batería, red
PULSE = [(34.5, 56), (43, 56), (46.5, 49), (51, 66), (56.5, 37), (62, 60), (65, 54), (73.5, 54)]
PULSE_W = 5.0
BG_IN, BG_OUT = "#1A2631", "#0A0F14"


def pt(a):
    r = math.radians(a)
    return C + R * math.cos(r), C + R * math.sin(r)


def segments():
    n = len(COLORS)
    seg = (SWEEP - GAP * (n - 1)) / n
    out = []
    for i, col in enumerate(COLORS):
        a0 = START + i * (seg + GAP)
        a1 = a0 + seg
        x0, y0 = pt(a0)
        x1, y1 = pt(a1)
        large = 1 if seg > 180 else 0
        d = f"M{x0:.3f},{y0:.3f}A{R},{R} 0 {large},1 {x1:.3f},{y1:.3f}"
        out.append((d, col))
    return out


def pulse_d():
    return "M" + "L".join(f"{x},{y}" for x, y in PULSE)


# ---------- SVG ----------
def svg_layers(mono=False, bg=True, pulse=PULSE_COLOR):
    s = []
    if bg:
        s.append(f'<defs><radialGradient id="g" cx="54" cy="42" r="72" gradientUnits="userSpaceOnUse">'
                 f'<stop offset="0" stop-color="{BG_IN}"/><stop offset="1" stop-color="{BG_OUT}"/></radialGradient></defs>'
                 f'<rect width="108" height="108" fill="url(#g)"/>')
    for d, col in segments():
        s.append(f'<path d="{d}" fill="none" stroke="{"#fff" if mono else col}" stroke-width="{RING_W}" stroke-linecap="butt"/>')
    s.append(f'<path d="{pulse_d()}" fill="none" stroke="{"#fff" if mono else pulse}" stroke-width="{PULSE_W}" '
             f'stroke-linecap="round" stroke-linejoin="round"/>')
    return "".join(s)


def svg_full(size, crop=True, mask=None, mono=False, pulse=PULSE_COLOR):
    vb = "18 18 72 72" if crop else "0 0 108 108"
    inner = svg_layers(mono=mono, bg=not mono, pulse=pulse)
    clip = ""
    if mask == "circle":
        clip = '<clipPath id="m"><circle cx="54" cy="54" r="36"/></clipPath>'
    elif mask == "squircle":
        clip = '<clipPath id="m"><rect x="18" y="18" width="72" height="72" rx="16"/></clipPath>'
    body = f'<g clip-path="url(#m)">{inner}</g>' if clip else inner
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="{vb}">'
            f'<defs>{clip}</defs>{body}</svg>')


# ---------- VectorDrawable ----------
VD_HEAD = ('<?xml version="1.0" encoding="utf-8"?>\n'
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
           '    xmlns:aapt="http://schemas.android.com/aapt"\n'
           '    android:width="108dp"\n    android:height="108dp"\n'
           '    android:viewportWidth="108"\n    android:viewportHeight="108">\n')


def vd_background():
    return (VD_HEAD.replace('    xmlns:aapt="http://schemas.android.com/aapt"\n', '    xmlns:aapt="http://schemas.android.com/aapt"\n') +
            '    <path android:pathData="M0,0h108v108h-108z">\n'
            '        <aapt:attr name="android:fillColor">\n'
            f'            <gradient android:type="radial" android:centerX="54" android:centerY="42" android:gradientRadius="72"\n'
            f'                android:startColor="{BG_IN}" android:endColor="{BG_OUT}" />\n'
            '        </aapt:attr>\n    </path>\n</vector>\n')


def vd_foreground(mono=False):
    head = VD_HEAD.replace('    xmlns:aapt="http://schemas.android.com/aapt"\n', '')
    s = [head]
    for d, col in segments():
        s.append(f'    <path\n        android:pathData="{d}"\n        android:strokeColor="{"#FFFFFFFF" if mono else "#FF" + col[1:]}"\n'
                 f'        android:strokeWidth="{RING_W}"\n        android:strokeLineCap="butt" />\n')
    s.append(f'    <path\n        android:pathData="{pulse_d()}"\n        android:strokeColor="{"#FFFFFFFF" if mono else "#FF" + PULSE_COLOR[1:]}"\n'
             f'        android:strokeWidth="{PULSE_W}"\n        android:strokeLineCap="round"\n        android:strokeLineJoin="round" />\n')
    s.append('</vector>\n')
    return "".join(s)


os.makedirs(OUT, exist_ok=True)
open(f"{OUT}/ic_launcher_background.xml", "w").write(vd_background())
open(f"{OUT}/ic_launcher_foreground.xml", "w").write(vd_foreground())
open(f"{OUT}/ic_launcher_monochrome.xml", "w").write(vd_foreground(mono=True))
open(f"{OUT}/store.svg", "w").write(svg_full(512, crop=True, pulse=PULSE_COLOR))
open(f"{OUT}/icon_layers.svg", "w").write(svg_layers())  # para incrustar en la cabecera

# Preview: máscaras de launcher a varios tamaños + monocromo temático
def themed(size):
    # Android 13 themed: fondo de color claro, glifo en color de acento
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" viewBox="18 18 72 72">'
            f'<defs><clipPath id="t"><circle cx="54" cy="54" r="36"/></clipPath></defs>'
            f'<g clip-path="url(#t)"><rect width="108" height="108" fill="#D6E7F2"/>'
            f'<g style="filter:none">{svg_layers(mono=True, bg=False).replace("#fff", "#1F4D66")}</g></g></svg>')

html = ['<html><body style="margin:0;padding:24px;background:#3a3f45;display:flex;gap:28px;align-items:end;flex-wrap:wrap;width:1100px">']
for sz in (192, 96, 48):
    html.append(svg_full(sz, mask="circle"))
    html.append(svg_full(sz, mask="squircle"))
html.append(themed(192))
html.append(themed(48))
html.append('</body></html>')
open(f"{OUT}/preview.html", "w").write("".join(html))
