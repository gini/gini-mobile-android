#!/usr/bin/env python3
"""Render a synthetic German invoice carrying line items AND a claimable skonto discount.

Fixture for the Return Assistant + Skonto case (DigitalInvoiceSkontoTests), where the
digital invoice screen shows a skonto row below the other charges.

Two fixtures cover this case and they are not interchangeable:

- `Testrechnung-RA-Skonto.pdf` is a real OTTO invoice with two line items and genuine
  skonto terms, but its invoice date is 15.08.2024, so its discount deadline is long
  past and the SDK renders the row with the toggle OFF (`SkontoEdgeCase.SkontoExpired`).
  That is the expired half of the case.
- this generated invoice carries a 2028 invoice date, so the discount is still
  claimable and the toggle arrives ON. See `gen_invoice_skonto_valid.py` for why a
  real invoice cannot cover this half.

Rechnungsdatum: 05.08.2028, 14-day window -> skonto deadline 19.08.2028.
Both dates must match SkontoFixtures in the tests.

TWO position rows, not one: `DigitalInvoiceSkontoTests` switches one line item off and
asserts the total fell. With a single item, switching it off leaves nothing enabled and
the total sits at an edge case instead of dropping by a readable amount.
"""
import os

from PIL import Image, ImageDraw, ImageFont

W, H = 1240, 1754  # A4 at 150 dpi
img = Image.new("RGB", (W, H), "white")
d = ImageDraw.Draw(img)

# (regular path, bold path, ttc index regular, ttc index bold) per platform.
# NOTE: a different font produces different image bytes -- a regenerated fixture
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
    raise SystemExit("No known font found -- add your platform's font paths to FONTS.")


x = 110
y = 90
d.text((x, y), "Muster Handel GmbH", font=f(34, True), fill="black")
y += 46
d.text((x, y), "Musterstraße 12 · 80333 München", font=f(24), fill="black")
y += 34
d.text((x, y), "Tel. 089 1234560 · info@musterhandel.example", font=f(24), fill="black")

y += 110
d.text((x, y), "Max Mustermann", font=f(26), fill="black")
y += 36
d.text((x, y), "Beispielweg 3", font=f(26), fill="black")
y += 36
d.text((x, y), "10115 Berlin", font=f(26), fill="black")

y += 100
d.text((x, y), "Rechnung Nr. 2028-0782", font=f(40, True), fill="black")
y += 66
d.text((x, y), "Rechnungsdatum: 05.08.2028", font=f(26), fill="black")
y += 38
d.text((x, y), "Kundennummer: 44821", font=f(26), fill="black")

# The position table is what the backend turns into `lineItems`, which is what hands the
# flow to the Return Assistant. Quantity and unit price are both printed because
# `LineItemsValidator.validate` needs an int-parsable quantity and a parsable gross price.
y += 90
d.text((x, y), "Pos.  Artikelbezeichnung", font=f(26, True), fill="black")
d.text((x + 620, y), "Menge", font=f(26, True), fill="black")
d.text((x + 800, y), "Einzelpreis", font=f(26, True), fill="black")
y += 44
d.line((x, y, x + 1000, y), fill="black", width=2)
y += 26
d.text((x, y), "1      Bürostuhl Modell A", font=f(26), fill="black")
d.text((x + 620, y), "1", font=f(26), fill="black")
d.text((x + 800, y), "300,00 €", font=f(26), fill="black")
y += 44
d.text((x, y), "2      Schreibtischlampe", font=f(26), fill="black")
d.text((x + 620, y), "1", font=f(26), fill="black")
d.text((x + 800, y), "200,00 €", font=f(26), fill="black")
y += 54
d.line((x, y, x + 1000, y), fill="black", width=2)
y += 26
d.text((x, y), "Gesamtbetrag", font=f(30, True), fill="black")
d.text((x + 780, y), "500,00 €", font=f(30, True), fill="black")

y += 120
d.text((x, y), "Zahlungsbedingung:", font=f(28, True), fill="black")
y += 46
d.text(
    (x, y),
    "Rechnungsbetrag zahlbar innerhalb von 30 Tagen ab Rechnungsdatum.",
    font=f(26),
    fill="black",
)
y += 40
d.text(
    (x, y),
    "Bei Zahlung innerhalb von 14 Tagen werden 3 % Skonto gewährt.",
    font=f(26, True),
    fill="black",
)
y += 40
d.text((x, y), "Skonto-Frist: 19.08.2028 · Skontobetrag: 485,00 €", font=f(26), fill="black")
y += 40
d.text((x, y), "Fälligkeitsdatum: 04.09.2028", font=f(26), fill="black")

y += 90
d.text((x, y), "Bitte überweisen Sie den Betrag auf folgendes Konto:", font=f(26), fill="black")
y += 50
d.text((x, y), "Empfänger: Muster Handel GmbH", font=f(26), fill="black")
y += 38
d.text((x, y), "IBAN: DE02 1203 0000 0000 2020 51", font=f(26), fill="black")
y += 38
d.text((x, y), "BIC: BYLADEM1001", font=f(26), fill="black")
y += 38
d.text((x, y), "Verwendungszweck: RE 2028-0782", font=f(26), fill="black")

y += 100
d.line((x, y, x + 1000, y), fill="black", width=1)
y += 20
d.text(
    (x, y),
    "Muster Handel GmbH · Amtsgericht München HRB 123456 · USt-IdNr. DE123456789",
    font=f(20),
    fill="black",
)

out = os.path.join(os.path.dirname(__file__), "..", "..", "assets", "skonto_ra_valid.jpeg")
img.save(out, "JPEG", quality=92)
print(out)
