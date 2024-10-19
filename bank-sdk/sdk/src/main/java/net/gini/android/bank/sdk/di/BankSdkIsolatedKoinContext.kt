package net.gini.android.bank.sdk.di

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import net.gini.android.bank.sdk.capture.captureFlowFragmentModule
import net.gini.android.bank.sdk.capture.di.skonto.skontoCommonModule
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.digitalInvoiceSkontoScreenModule
import net.gini.android.bank.sdk.capture.skonto.skontoScreenModule
import net.gini.android.bank.sdk.capture.skonto.usecase.di.skontoUseCaseModule
import net.gini.android.bank.sdk.invoice.invoicePreviewScreenModule
import net.gini.android.bank.sdk.transactiondocs.di.transactionListModule
import net.gini.android.bank.sdk.transactiondocs.ui.invoice.transactionDocInvoicePreviewScreenModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.Koin
import org.koin.dsl.koinApplication

@SuppressLint("StaticFieldLeak")
object BankSdkIsolatedKoinContext {

    private var context: Context? = null

    val koin: Koin by lazy {
        koinApplication {
            val ctx = context
            check(ctx != null) {
                "Koin needs to be initialized first. " +
                        "Call BankSdkIsolatedKoinContext.init(context)!"
            }
            androidContext(ctx)

            modules(
                screenModules
                    .plus(useCaseModules)
                    .plus(commonModules)
                    .plus(handlerModule)
                    .plus(transactionListModule)
            )
        }.koin
    }

    @Synchronized
    fun init(context: Context) {
        if (this.context == null) {
            this.context = context
        } else {
            Log.d("BankIsolatedKoinContext", "Koin already initialized")
        }
    }

    fun clean() {
        context = null
    }
}

private val commonModules = listOf(
    skontoCommonModule,
)

private val useCaseModules = listOf(
    skontoUseCaseModule,
)

private val screenModules = listOf(
    skontoScreenModule,
    invoicePreviewScreenModule,
    transactionDocInvoicePreviewScreenModule,
    captureFlowFragmentModule,
    digitalInvoiceSkontoScreenModule
)

fun getGiniBankKoin() = BankSdkIsolatedKoinContext.koin
