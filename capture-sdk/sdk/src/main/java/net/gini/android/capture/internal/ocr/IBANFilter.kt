package net.gini.android.capture.internal.ocr

import net.gini.android.capture.internal.qrcode.IBANValidator

/**
 * Created by Alpár Szotyori on 27.04.23.
 *
 * Copyright (c) 2023 Gini GmbH.
 */
class IBANFilter {

    private val ibanValidator: IBANValidator = IBANValidator()

    fun process(text: OCRText): OCRText {
        return OCRText(text.elements.filter { element ->
            try {
                ibanValidator.validate(element.text)
                true
            } catch (ignore: Exception) {
                false
            }
        })
    }
}