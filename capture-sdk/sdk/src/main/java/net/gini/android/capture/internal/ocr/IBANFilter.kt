package net.gini.android.capture.internal.ocr

import android.graphics.Rect
import android.util.Log
import net.gini.android.capture.internal.qrcode.IBANValidator

/**
 * Created by Alpár Szotyori on 27.04.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */
class IBANFilter {

    private val ibanValidator: IBANValidator = IBANValidator()

    private val ibanRegex = """[A-Z]{2}\d{2}.*\d""".toRegex()

    fun process(text: OCRText): OCRText {
        return OCRText(text.elements.map { element ->
            val filteredElement = element.text.filter { it.isLetterOrDigit() }
            Log.d("filteredElement", filteredElement)
            ibanRegex.find(filteredElement)?.let { firstMatchResult ->
                var matchResult: MatchResult? = firstMatchResult
                do {
                    if (matchResult == null) {
                        return@let
                    }
                    try {
                        Log.d("matchResult", matchResult.value)
                        ibanValidator.validate(matchResult.value)
                        return@map OCRElement(matchResult.value, element.box)
                    } catch (ignore: Exception) {
                    }
                    matchResult = matchResult.next()
                } while (matchResult != null)
            }
            OCRElement("", Rect())
        }.filter { it.text.isNotBlank() })
    }
}