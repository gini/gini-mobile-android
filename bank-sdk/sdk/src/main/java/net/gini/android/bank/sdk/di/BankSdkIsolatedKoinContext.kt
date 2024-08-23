package net.gini.android.bank.sdk.di

import net.gini.android.bank.sdk.capture.captureFlowFragmentModule
import net.gini.android.bank.sdk.capture.digitalinvoice.skonto.digitalInvoiceSkontoScreenModule
import net.gini.android.bank.sdk.capture.skonto.invoice.skontoInvoiceScreenModule
import net.gini.android.bank.sdk.capture.skonto.skontoScreenModule
import net.gini.android.bank.sdk.capture.skonto.usecase.di.skontoUseCaseModule
import org.koin.dsl.koinApplication

object BankSdkIsolatedKoinContext {

    private val koinApp = koinApplication {
        modules(
            screenModules
                .plus(useCaseModules)
        )
    }

    val koin = koinApp.koin
}

private val useCaseModules = listOf(
    skontoUseCaseModule,
)

private val screenModules = listOf(
    skontoScreenModule,
    skontoInvoiceScreenModule,
    captureFlowFragmentModule,
    digitalInvoiceSkontoScreenModule
)

fun getGiniBankKoin() = BankSdkIsolatedKoinContext.koin
