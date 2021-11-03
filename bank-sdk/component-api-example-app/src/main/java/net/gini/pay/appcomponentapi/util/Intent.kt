package net.gini.pay.appcomponentapi.util

import android.content.Intent

fun isIntentActionViewOrSend(intent: Intent): Boolean =
    Intent.ACTION_VIEW == intent.action || Intent.ACTION_SEND == intent.action || Intent.ACTION_SEND_MULTIPLE == intent.action