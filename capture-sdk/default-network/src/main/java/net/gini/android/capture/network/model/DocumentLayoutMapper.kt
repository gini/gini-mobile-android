package net.gini.android.capture.network.model

import net.gini.android.core.api.models.DocumentLayout
import net.gini.android.capture.internal.network.model.DocumentLayout as CaptureApiDocumentLayout
import net.gini.android.capture.internal.network.model.DocumentLayout.Page.Region as CaptureApiRegion
import net.gini.android.capture.internal.network.model.DocumentLayout.Page.TextZone.Paragraph as CaptureApiParagraph
import net.gini.android.capture.internal.network.model.DocumentLayout.Page.TextZone.Paragraph.Line as CaptureApiLine
import net.gini.android.capture.internal.network.model.DocumentLayout.Page.TextZone.Paragraph.Line.Word as CaptureApiWord
import net.gini.android.capture.internal.network.model.DocumentLayout.Page as CaptureApiPage
import net.gini.android.capture.internal.network.model.DocumentLayout.Page.TextZone as CaptureApiTextZone


fun DocumentLayout.toCaptureDocumentLayout() = CaptureApiDocumentLayout(
    pages = pages.map { it.toPage() }
)

fun DocumentLayout.Page.toPage() = CaptureApiPage(
    number = number,
    sizeX = sizeX,
    sizeY = sizeY,
    textZones = textZones.map { it.toTextZone() },
    regions = regions.map { it.toRegion() }
)

fun DocumentLayout.Page.TextZone.toTextZone() = CaptureApiTextZone(
    paragraphs = paragraphs.map { it.toParagraph() }
)

fun DocumentLayout.Page.TextZone.Paragraph.toParagraph() =
    CaptureApiParagraph(
        left = left,
        top = top,
        width = width,
        height = height,
        lines = lines.map { it.toLine() })

fun DocumentLayout.Page.TextZone.Paragraph.Line.toLine() =
    CaptureApiLine(
        words = words.map { it.toWord() },
        top = top,
        left = left,
        width = width,
        height = height,
    )

fun DocumentLayout.Page.TextZone.Paragraph.Line.Word.toWord() =
    CaptureApiWord(
        text = text,
        left = left,
        top = top,
        width = width,
        height = height,
        fontSize = fontSize,
        bold = bold,
        fontFamily = fontFamily
    )

fun DocumentLayout.Page.Region.toRegion() =
    CaptureApiRegion(
        width = width,
        height = height,
        left = left,
        top = top,
        type = type
    )