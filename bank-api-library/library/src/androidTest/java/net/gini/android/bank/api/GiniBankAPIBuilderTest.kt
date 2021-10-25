package net.gini.android.bank.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.gini.android.core.api.GiniApiType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/**
 * Created by Alpár Szotyori on 25.10.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */
@RunWith(AndroidJUnit4::class)
class GiniBankAPIBuilderTest {

    @Test
    fun constructors_useTheHealthAPI() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        var healthAPIBuilder = GiniBankAPIBuilder(targetContext, "", "", "")
        assertEquals(healthAPIBuilder.giniApiType, GiniApiType.BANK)

        healthAPIBuilder = GiniBankAPIBuilder(targetContext, mock())
        assertEquals(healthAPIBuilder.giniApiType, GiniApiType.BANK)
    }

}