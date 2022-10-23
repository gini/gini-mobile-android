package net.gini.android.health.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.KSessionManager
import net.gini.android.core.api.authorization.apimodels.SessionToken
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
class GiniHealthAPIBuilderTest {

    @Test
    fun constructors_useTheHealthAPI() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        var healthAPIBuilder = GiniHealthAPIBuilder(targetContext, "", "", "")
        assertEquals(healthAPIBuilder.getGiniApiType(), GiniHealthApiType( apiVersion = 3))

        healthAPIBuilder = GiniHealthAPIBuilder(targetContext, sessionManager = object: KSessionManager {
            override suspend fun getSession(): Resource<SessionToken> {
                return mock()
            }
        })
        assertEquals(healthAPIBuilder.getGiniApiType(), GiniHealthApiType(apiVersion = 3))
    }

}