package net.gini.android.bank.sdk.transactiondocs

import kotlinx.coroutines.flow.Flow

interface TransactionDocsSettings {

    fun getAlwaysAttachSetting(): Flow<Boolean>

    suspend fun setAlwaysAttachSetting(value: Boolean)
}
