package net.gini.android.health.sdk.util.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import androidx.annotation.VisibleForTesting
import net.gini.android.health.sdk.paymentprovider.PaymentProviderApp

// In a future refactoring we can split extensions into files according to what component they extend
@VisibleForTesting
internal fun Context.generateBitmapDrawableIcon(icon: ByteArray, iconSize: Int): BitmapDrawable? {
    return BitmapFactory.decodeByteArray(icon, 0, iconSize)
        ?.let { bitmap ->
            val iconSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                PaymentProviderApp.ICON_SIZE,
                this.resources.displayMetrics
            ).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                iconSizePx,
                iconSizePx,
                true
            )
            bitmap.recycle()
            BitmapDrawable(this.resources, scaledBitmap)
        }
}

internal fun Context.getFontScale() = resources.configuration.fontScale