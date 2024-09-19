package net.gini.android.internal.payment.utils.extensions

import android.R
import android.content.res.ColorStateList
import android.widget.Button
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.core.graphics.ColorUtils

internal fun Button.setBackgroundTint(@ColorInt color: Int, @IntRange(from = 0x0, to = 0xFF) nonEnabledAlpha: Int = 100) {
    backgroundTintList = ColorStateList(
        arrayOf(
            intArrayOf(R.attr.state_enabled),
            intArrayOf()
        ),
        intArrayOf(
            color,
            ColorUtils.setAlphaComponent(color, nonEnabledAlpha)
        )
    )
}
 