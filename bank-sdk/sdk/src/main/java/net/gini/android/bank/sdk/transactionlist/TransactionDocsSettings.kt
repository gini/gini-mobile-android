package net.gini.android.bank.sdk.transactionlist

interface TransactionDocsSettings {

    fun getAlwaysAttachSetting(): Boolean

    fun setAlwaysAttachSetting(value: Boolean)
}