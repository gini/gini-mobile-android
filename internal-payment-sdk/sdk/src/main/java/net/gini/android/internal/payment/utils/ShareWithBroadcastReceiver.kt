package net.gini.android.internal.payment.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


internal class ShareWithBroadcastReceiver : BroadcastReceiver() {
    val SHARE_WITH_INTENT_FILTER = "share_intent_filter"
    override fun onReceive(context: Context?, intent: Intent?) {
       context?.sendBroadcast(Intent().also { it.action = SHARE_WITH_INTENT_FILTER })
    }
}