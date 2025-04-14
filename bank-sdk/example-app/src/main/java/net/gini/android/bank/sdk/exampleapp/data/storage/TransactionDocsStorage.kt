package net.gini.android.bank.sdk.exampleapp.data.storage

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.gini.android.bank.sdk.exampleapp.ui.transactiondocs.docs.model.Transaction
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


@Suppress("TooGenericExceptionCaught")
internal class TransactionDocsStorage(context: Context) {

    private val file = File(context.cacheDir.toString(), "transaction_docs_data.json")

    suspend fun update(data: List<Transaction>?) {
        delete()
        return suspendCoroutine {
            try {
                file.writeText(data?.let { data ->
                    Json.encodeToString(data)
                } ?: "")
                it.resume(Unit)
            } catch (e: Exception) {
                it.resumeWithException(e)
            }
        }
    }

    suspend fun delete() = suspendCoroutine {
        file.delete()
        it.resume(Unit)
    }

    suspend inline fun get(): List<Transaction>? = suspendCoroutine {
        it.resume(kotlin.runCatching {
            if (!file.exists()) return@runCatching null
            Json.decodeFromString<List<Transaction>>(file.readText())
        }.onFailure {
            it.printStackTrace()
        }.getOrNull())
    }
}
