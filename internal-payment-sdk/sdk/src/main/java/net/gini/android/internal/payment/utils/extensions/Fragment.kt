package net.gini.android.internal.payment.utils.extensions

import android.app.PendingIntent
import android.content.Intent
import android.view.LayoutInflater
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import net.gini.android.internal.payment.GiniInternalPaymentModule
import java.io.File
import java.util.Locale

internal fun Fragment.getLayoutInflaterWithGiniPaymentThemeAndLocale(inflater: LayoutInflater, locale: Locale? = null): LayoutInflater {
    return inflater.cloneInContext(requireContext().wrappedWithGiniPaymentThemeAndLocale(locale))
}

internal fun Fragment.getLocaleStringResource(resourceId: Int, giniPaymentModule: GiniInternalPaymentModule?): String {
    if (giniPaymentModule?.localizedContext == null) {
        giniPaymentModule?.localizedContext = context?.createConfigurationContext(resources.configuration)
    }

    return giniPaymentModule?.localizedContext?.getText(resourceId).toString()
}

fun Fragment.startSharePdfIntent(file: File, pendingIntent: PendingIntent? = null) {
    val uriForFile = FileProvider.getUriForFile(
        requireContext(),
        requireContext().packageName + ".internal.payment.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        putExtra(Intent.EXTRA_STREAM, uriForFile)
    }
    startActivity(
        Intent.createChooser(
            shareIntent,
            uriForFile.lastPathSegment,
            pendingIntent?.intentSender
        )
    )
}
