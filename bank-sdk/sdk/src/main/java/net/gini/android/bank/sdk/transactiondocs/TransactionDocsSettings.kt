package net.gini.android.bank.sdk.transactiondocs

interface TransactionDocsSettings {

    fun getAlwaysAttachSetting(): Boolean

    fun setAlwaysAttachSetting(value: Boolean)
}