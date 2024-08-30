package net.gini.android.core.api.mapper

import net.gini.android.core.api.models.DocumentLayout
import net.gini.android.core.api.response.DocumentLayoutResponse


fun DocumentLayoutResponse.toDocumentLayout() = DocumentLayout(
    pages = pages.map { it.toPage() }
)

fun DocumentLayoutResponse.PageResponse.toPage() = DocumentLayout.Page(number = number,
    sizeX = sizeX,
    sizeY = sizeY,
    textZones = textZones.map { it.toTextZone() },
    regions = regions.map { it.toRegion() })

fun DocumentLayoutResponse.PageResponse.TextZoneResponse.toTextZone() =
    DocumentLayout.Page.TextZone(paragraphs = paragraphs.map { it.toParagraph() })

fun DocumentLayoutResponse.PageResponse.TextZoneResponse.ParagraphResponse.toParagraph() =
    DocumentLayout.Page.TextZone.Paragraph(width = width,
        height = height,
        top = top,
        left = left,
        lines = lines.map { it.toLine() })

fun DocumentLayoutResponse.PageResponse.TextZoneResponse.ParagraphResponse.LineResponse.toLine() =
    DocumentLayout.Page.TextZone.Paragraph.Line(width = width,
        height = height,
        top = top,
        left = left,
        words = words.map { it.toWord() })

fun DocumentLayoutResponse.PageResponse.TextZoneResponse.ParagraphResponse.LineResponse.WordResponse.toWord() =
    DocumentLayout.Page.TextZone.Paragraph.Line.Word(
        width = width,
        height = height,
        top = top,
        left = left,
        fontSize = fontSize,
        fontFamily = fontFamily,
        bold = bold,
        text = text
    )

fun DocumentLayoutResponse.PageResponse.RegionResponse.toRegion() = DocumentLayout.Page.Region(
    width = width, height = height, top = top, left = left, type = type
)