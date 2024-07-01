package net.gini.android.merchant.sdk.util.extensions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import androidx.annotation.VisibleForTesting
import net.gini.android.merchant.sdk.paymentprovider.PaymentProviderApp
import net.gini.android.merchant.sdk.util.ShareWithBroadcastReceiver

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
            // Bitmap.createScaledBitmap returns original bitmap when width and height is the same as bitmap.width and bitmap.height. In one case (Pixel 7 Pro) iconSizePx was being resolved to exactly the same value as some of the bitmap's widths
            // Recycling this bitmap in this case leads to the app crashing when attempting to load the drawable into an ImageView
            // Only recycle if width is different than bitmap.width (can only check for width, as it's the same dimension for both with and height)
            if (bitmap.width != iconSizePx) bitmap.recycle()
            BitmapDrawable(this.resources, scaledBitmap)
        }
}

internal fun Context.getFontScale() = resources.configuration.fontScale

internal fun Context.createShareWithPendingIntent() = PendingIntent.getBroadcast(
    this, 123,
    Intent(this, ShareWithBroadcastReceiver::class.java),
    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
)
