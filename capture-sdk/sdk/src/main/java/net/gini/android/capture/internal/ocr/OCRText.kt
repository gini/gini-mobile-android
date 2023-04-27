package net.gini.android.capture.internal.ocr

import android.graphics.Point
import android.graphics.Rect

/**
 * Created by Alpár Szotyori on 27.04.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */

data class OCRText(val elements: List<OCRElement>)

data class OCRElement(val text: String, val box: Rect)