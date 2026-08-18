#!/usr/bin/env python3
"""Render a synthetic German invoice image for the PP-3301 UI test fixture.

Variant WITHOUT any due-date line: the API must extract NO paymentDueDate (fixture for R6).
"""
import os

from PIL import Image, ImageDraw, ImageFont

W, H = 1240, 1754  # A4 at 150 dpi
img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)

# (regular path, bold path, ttc index regular, ttc index bold) per platform.
# NOTE: a different font produces different image bytes — a regenerated fixture
# must be re-validated against the Gini API before replacing the committed one.
FONTS = [
    ("/System/Library/Fonts/Helvetica.ttc", "/System/Library/Fonts/Helvetica.ttc", 0, 1),  # macOS
    ("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
     "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 0, 0),  # Linux
    ("C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/arialbd.ttf", 0, 0),  # Windows
]


def f(size, bold=False):
    for regular, bold_path, regular_index, bold_index in FONTS:
        path, index = (bold_path, bold_index) if bold else (regular, regular_index)
        if os.path.exists(path):
            return ImageFont.truetype(path, size, index=index)
    raise SystemExit("No known font found — add your platform's font paths to FONTS.")


x = 110
y = 90
d.text((x, y), "Muster Bau GmbH", font=f(34, True), fill="black")
y += 46
d.text((x, y), "Musterstraße 12 · 80333 München", font=f(24), fill="black")
y += 34
d.text((x, y), "Tel. 089 1234560 · info@musterbau.example", font=f(24), fill="black")

y += 110
d.text((x, y), "Max Mustermann", font=f(26), fill="black")
y += 36
d.text((x, y), "Beispielweg 3", font=f(26), fill="black")
y += 36
d.text((x, y), "10115 Berlin", font=f(26), fill="black")

y += 100
d.text((x, y), "Rechnung Nr. 2028-0918", font=f(40, True), fill="black")
y += 66
d.text((x, y), "Rechnungsdatum: 05.08.2028", font=f(26), fill="black")
y += 38
d.text((x, y), "Kundennummer: 44821", font=f(26), fill="black")

y += 90
d.text((x, y), "Pos.  Beschreibung", font=f(26, True), fill="black")
d.text((x + 780, y), "Betrag", font=f(26, True), fill="black")
y += 44
d.line((x, y, x + 1000, y), fill="black", width=2)
y += 26
d.text((x, y), "1      Malerarbeiten Wohnzimmer", font=f(26), fill="black")
d.text((x + 780, y), "450,00 €", font=f(26), fill="black")
y += 44
d.text((x, y), "2      Materialkosten", font=f(26), fill="black")
d.text((x + 780, y), "120,50 €", font=f(26), fill="black")
y += 54
d.line((x, y, x + 1000, y), fill="black", width=2)
y += 26
d.text((x, y), "Gesamtbetrag", font=f(30, True), fill="black")
d.text((x + 780, y), "570,50 €", font=f(30, True), fill="black")

y += 110
d.text((x, y), "Bitte überweisen Sie den Betrag auf folgendes Konto:", font=f(26), fill="black")
y += 50
d.text((x, y), "Empfänger: Muster Bau GmbH", font=f(26), fill="black")
y += 38
d.text((x, y), "IBAN: DE02 1203 0000 0000 2020 51", font=f(26), fill="black")
y += 38
d.text((x, y), "BIC: BYLADEM1001", font=f(26), fill="black")
y += 38
d.text((x, y), "Verwendungszweck: RE 2028-0918", font=f(26), fill="black")

y += 120
d.text((x, y), "Vielen Dank für Ihren Auftrag!", font=f(26), fill="black")
y += 60
d.line((x, y, x + 1000, y), fill="black", width=1)
y += 20
d.text(
    (x, y),
    "Muster Bau GmbH · Amtsgericht München HRB 123456 · USt-IdNr. DE123456789",
    font=f(20),
    fill="black",
)

out = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "invoice_no_due_date.jpeg")
img.save(out, "JPEG", quality=92)
print(out)
