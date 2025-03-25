package net.gini.android.internal.payment.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextPaint
import android.text.style.URLSpan
import android.view.View


class UrlSpanNoUnderline(private val context: Context, url: String) : URLSpan(url) {
    override fun onClick(widget: View) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        // Check if the context is an Activity
        if (context is Activity) {
            context.startActivity(intent)
        } else {
            // If not, add the FLAG_ACTIVITY_NEW_TASK flag
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
    }
}