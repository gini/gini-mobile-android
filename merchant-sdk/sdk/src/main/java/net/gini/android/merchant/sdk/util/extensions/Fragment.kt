package net.gini.android.merchant.sdk.util.extensions

import android.app.PendingIntent
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File

internal fun Fragment.startSharePdfIntent(file: File, pendingIntent: PendingIntent? = null) {
    val uriForFile = FileProvider.getUriForFile(
        requireContext(),
        requireContext().packageName + ".merchant.sdk.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        putExtra(Intent.EXTRA_STREAM, uriForFile)
    }
    startActivity(Intent.createChooser(shareIntent, uriForFile.lastPathSegment, pendingIntent?.intentSender))
}