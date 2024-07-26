package net.gini.android.merchant.sdk.util

import androidx.core.content.FileProvider

/**
 * Own FileProvider so we avoid manifest merger errors if clients use their own FileProviders.
 * Not used anywhere except in Manifest declaration
 */
internal class MerchantSDKFileProvider: FileProvider() {
}