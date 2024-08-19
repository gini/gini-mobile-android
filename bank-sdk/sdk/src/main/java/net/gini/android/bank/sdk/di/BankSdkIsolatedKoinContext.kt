package net.gini.android.bank.sdk.di

import net.gini.android.bank.sdk.capture.captureFlowFragmentModule
import net.gini.android.bank.sdk.capture.skonto.invoice.skontoInvoiceScreenModule
import net.gini.android.bank.sdk.capture.skonto.skontoScreenModule
import org.koin.dsl.koinApplication

object BankSdkIsolatedKoinContext {

    private val koinApp = koinApplication {
        modules(
            screenModules
        )
    }

    val koin = koinApp.koin
}

private val screenModules = listOf(
    skontoScreenModule,
    skontoInvoiceScreenModule,
    captureFlowFragmentModule
)

fun getGiniBankKoin() = BankSdkIsolatedKoinContext.koin
