package net.gini.android.bank.sdk.capture.digitalinvoice.details

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Created by Alpar Szotyori on 18.12.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */

@JvmSynthetic
internal fun EditText.doAfterTextChanged(afterTextChanged: (String) -> Unit): TextWatcher =
    object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            // No action needed - only afterTextChanged is used
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            // No action needed - only afterTextChanged is used
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged(editable.toString())
        }
    }.also {
        this.addTextChangedListener(it)
    }