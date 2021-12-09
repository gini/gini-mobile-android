package net.gini.android.bank.sdk.screenapiexample.util

import android.view.View
import android.widget.AdapterView

interface SimpleSpinnerSelectListener : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}