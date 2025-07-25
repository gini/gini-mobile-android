package net.gini.android.capture.di

import net.gini.android.capture.internal.qreducation.UpdateFlowTypeUseCase
import net.gini.android.capture.internal.storage.FlowTypeStorage
import net.gini.android.capture.education.GetEducationFeatureEnabledUseCase
import org.koin.dsl.module

internal val educationModule = module {
    single {
        FlowTypeStorage()
    }
    factory {
        UpdateFlowTypeUseCase(
            flowTypeStorage = get()
        )
    }
    factory {
        GetEducationFeatureEnabledUseCase(
            giniBankConfigurationProvider = get(),
        )
    }
}
