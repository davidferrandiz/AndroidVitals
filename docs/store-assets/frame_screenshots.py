"""Pone titular y marco de móvil a las capturas de Play Store.

Entrada: screenshots/raw/<idioma>/<nombre>.png (las genera el workflow "Store screenshots").
Salida:  screenshots/<idioma>/<nombre>.png a 1080 × 1920 (9:16, lo que pide Play para móvil).

Uso (necesita Python con Playwright y Chromium; usa la fuente Inter si está instalada):
    python3 docs/store-assets/frame_screenshots.py
"""
import base64
import html
import pathlib
import sys

from PIL import Image
from playwright.sync_api import sync_playwright

HERE = pathlib.Path(__file__).resolve().parent
RAW = HERE / "screenshots" / "raw"
OUT = HERE / "screenshots"
W, H = 1080, 1920

# Titular y subtítulo por captura e idioma. Mismo orden que los tests de StoreScreenshots.
CAPTIONS = {
    "en-US": {
        "01_panel": ("Your phone's vitals at a glance", "Battery, CPU, RAM, storage and network on one screen"),
        "02_battery": ("Charging power and battery health", "Time to full, 24-hour history and estimated wear"),
        "03_hardware": ("Know your real hardware", "Chip, GPU, display, sensors and Widevine level"),
        "04_panel_light": ("Light or dark theme", "Also in your wallpaper colors (Material You)"),
    },
    "es-ES": {
        "01_panel": ("Todo tu móvil de un vistazo", "Batería, CPU, RAM, almacenamiento y red en una pantalla"),
        "02_battery": ("Potencia de carga y salud de la batería", "Tiempo hasta la carga completa, historial de 24 h y desgaste estimado"),
        "03_hardware": ("Conoce el hardware real", "Procesador, GPU, pantalla, sensores y nivel de Widevine"),
        "04_panel_light": ("Tema claro u oscuro", "También con los colores de tu fondo (Material You)"),
    },
    "de-DE": {
        "01_panel": ("Dein Handy auf einen Blick", "Akku, CPU, RAM, Speicher und Netzwerk auf einem Bildschirm"),
        "02_battery": ("Ladeleistung und Akkuzustand", "Restladezeit, 24-Stunden-Verlauf und geschätzter Verschleiß"),
        "03_hardware": ("Deine echte Hardware", "Prozessor, GPU, Display, Sensoren und Widevine-Level"),
        "04_panel_light": ("Hell oder dunkel", "Auch in den Farben deines Hintergrunds (Material You)"),
    },
    "it-IT": {
        "01_panel": ("Il tuo telefono a colpo d'occhio", "Batteria, CPU, RAM, memoria e rete in una schermata"),
        "02_battery": ("Potenza di ricarica e salute della batteria", "Tempo alla carica completa, cronologia di 24 ore e usura stimata"),
        "03_hardware": ("Scopri l'hardware reale", "Processore, GPU, display, sensori e livello Widevine"),
        "04_panel_light": ("Tema chiaro o scuro", "Anche con i colori del tuo sfondo (Material You)"),
    },
    "fr-CA": {
        "01_panel": ("Votre téléphone en un coup d'œil", "Batterie, processeur, mémoire, stockage et réseau sur un seul écran"),
        "02_battery": ("Puissance de charge et santé de la batterie", "Temps avant la charge complète, historique de 24 h et usure estimée"),
        "03_hardware": ("Découvrez le vrai matériel", "Processeur, GPU, écran, capteurs et niveau Widevine"),
        "04_panel_light": ("Thème clair ou sombre", "Aussi aux couleurs de votre fond d'écran (Material You)"),
    },
    "hi-IN": {
        "01_panel": ("आपके फ़ोन की पूरी जानकारी, एक नज़र में", "बैटरी, CPU, RAM, स्टोरेज और नेटवर्क एक ही स्क्रीन पर"),
        "02_battery": ("चार्जिंग पावर और बैटरी हेल्थ", "फ़ुल चार्ज होने का समय, 24 घंटे का इतिहास और अनुमानित घिसावट"),
        "03_hardware": ("अपने फ़ोन का असली हार्डवेयर जानें", "प्रोसेसर, GPU, डिस्प्ले, सेंसर और Widevine लेवल"),
        "04_panel_light": ("लाइट या डार्क थीम", "आपके वॉलपेपर के रंगों के साथ भी (Material You)"),
    },
    "fa-IR": {
        "01_panel": ("همه‌چیز گوشی‌تان در یک نگاه", "باتری، CPU، رم، حافظه و شبکه در یک صفحه"),
        "02_battery": ("توان شارژ و سلامت باتری", "زمان تا شارژ کامل، تاریخچهٔ ۲۴ ساعته و فرسودگی تخمینی"),
        "03_hardware": ("سخت‌افزار واقعی گوشی‌تان را بشناسید", "پردازنده، GPU، نمایشگر، حسگرها و سطح Widevine"),
        "04_panel_light": ("تم روشن یا تیره", "با رنگ‌های والپیپر شما هم (Material You)"),
    },
}

# Color de acento del titular, uno por pantalla (los de categoría de VitalsPalette).
ACCENT = {"01_panel": "#4FC3F7", "02_battery": "#81C784", "03_hardware": "#CE93D8", "04_panel_light": "#FFB74D"}


def font_face(family, file, weight):
    data = base64.b64encode((HERE / "fonts" / file).read_bytes()).decode()
    return f"@font-face{{font-family:'{family}';src:url(data:font/woff2;base64,{data}) format('woff2');font-weight:{weight}}}"


FONTS = "".join([
    font_face("NotoDeva", "noto-sans-devanagari-devanagari-600-normal.woff2", 600),
    font_face("NotoDeva", "noto-sans-devanagari-devanagari-700-normal.woff2", 700),
    font_face("Vazirmatn", "vazirmatn-arabic-600-normal.woff2", 600),
    font_face("Vazirmatn", "vazirmatn-arabic-700-normal.woff2", 700),
])


def page(locale, name, shot_png):
    title, sub = CAPTIONS[locale][name]
    rtl = locale.startswith("fa")
    family = "'Inter Display', Inter, NotoDeva, Vazirmatn, sans-serif"
    if rtl:
        family = "Vazirmatn, Inter, sans-serif"
    light = name.endswith("_light")
    img = base64.b64encode(shot_png).decode()
    bg = ("radial-gradient(circle at 50% 18%, #2A3846 0, #141C24 45%, #0A0F14 100%)" if not light else
          "radial-gradient(circle at 50% 18%, #FFFFFF 0, #E8EEF3 50%, #D5DEE6 100%)")
    fg, fg2 = ("#F2F6F9", "#AEBCC7") if not light else ("#14202B", "#4C5B67")
    return f"""<!doctype html><html dir="{'rtl' if rtl else 'ltr'}"><head><meta charset="utf-8"><style>{FONTS}
*{{margin:0;box-sizing:border-box}}
body{{width:{W}px;height:{H}px;overflow:hidden;background:{bg};font-family:{family};text-align:center}}
.top{{position:absolute;left:80px;right:80px;top:96px}}
.bar{{width:72px;height:8px;border-radius:4px;background:{ACCENT[name]};margin:0 auto 34px}}
h1{{color:{fg};font-size:64px;line-height:1.12;font-weight:700;letter-spacing:{'0' if rtl else '-1px'}}}
p{{color:{fg2};font-size:34px;line-height:1.3;margin-top:22px;font-weight:600}}
.phone{{position:absolute;left:50%;transform:translateX(-50%);top:470px;width:676px;padding:16px;border-radius:72px;
  background:#1B2127;box-shadow:0 40px 90px rgba(0,0,0,{0.55 if not light else 0.25}),inset 0 0 0 2px #2E363E}}
.phone img{{display:block;width:100%;border-radius:58px}}
</style></head><body>
<div class="top"><div class="bar"></div><h1>{html.escape(title)}</h1><p>{html.escape(sub)}</p></div>
<div class="phone"><img src="data:image/png;base64,{img}"></div>
</body></html>"""


def main():
    shots = sorted(RAW.glob("*/*.png"))
    if not shots:
        sys.exit(f"No hay capturas en {RAW}")
    with sync_playwright() as p:
        browser = p.chromium.launch()
        tab = browser.new_page(viewport={"width": W, "height": H})
        for shot in shots:
            locale, name = shot.parent.name, shot.stem
            if locale not in CAPTIONS or name not in CAPTIONS[locale]:
                print("sin titular, se omite:", shot)
                continue
            tab.set_content(page(locale, name, shot.read_bytes()))
            tab.wait_for_timeout(150)
            out = OUT / locale / f"{name}.png"
            out.parent.mkdir(parents=True, exist_ok=True)
            tab.screenshot(path=str(out), clip={"x": 0, "y": 0, "width": W, "height": H})
            Image.open(out).convert("RGB").save(out)  # Play pide PNG de 24 bits, sin alfa
            print(out.relative_to(HERE))
        browser.close()


if __name__ == "__main__":
    main()
