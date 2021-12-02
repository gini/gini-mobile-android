package net.gini.android.bank.sdk.componentapiexample.util

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import java.util.*
import net.gini.android.capture.util.UriHelper

fun Context.hasLessThan5MB(uri: Uri): Boolean =
    UriHelper.getFileSizeFromUri(uri, this) <= 5 * 1024 * 1024

fun <T : Parcelable> Map<String, T>.toBundle(): Bundle = Bundle().apply {
    this@toBundle.forEach { (key, value) ->
        putParcelable(key, value)
    }
}

fun <T : Parcelable> Bundle.toMap(): Map<String, T> {
    return mutableMapOf<String, T>().apply {
        for (key in this@toMap.keySet()) {
            this[key] = this@toMap.getParcelable<T>(key) as T
        }
    }
}