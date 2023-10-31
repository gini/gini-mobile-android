package net.gini.android.capture.internal.textrecognition

import android.graphics.Rect

internal data class RecognizedText(val text: String, val blocks: List<RecognizedTextBlock>)

internal data class RecognizedTextBlock(val lines: List<RecognizedTextLine>)

internal data class RecognizedTextLine(val elements: List<RecognizedTextElement>)

internal data class RecognizedTextElement(val text: String, val boundingBox: Rect?)