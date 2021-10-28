package net.gini.android.capture.logging

import android.os.Build
import net.gini.android.capture.BuildConfig

/**
 * Contains an error description along with useful metadata for logging.
 */
data class ErrorLog @JvmOverloads constructor(
    val deviceModel: String = Build.MODEL,
    val osName: String = "Android",
    val osVersion: String = Build.VERSION.RELEASE,
    val captureVersion: String = BuildConfig.VERSION_NAME,
    val description: String,
    val exception: Throwable?,
)
