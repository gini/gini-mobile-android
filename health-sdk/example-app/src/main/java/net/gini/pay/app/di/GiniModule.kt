package net.gini.pay.app.di

import net.gini.pay.ginipaybusiness.GiniBusiness
import net.gini.pay.ginipaybusiness.ginipayapi.getGiniApi
import org.koin.dsl.module

val giniModule = module {
    single { getGiniApi(get(), getProperty("clientId"), getProperty("clientSecret"), "example.com") }
    single { GiniBusiness(get()) }
}