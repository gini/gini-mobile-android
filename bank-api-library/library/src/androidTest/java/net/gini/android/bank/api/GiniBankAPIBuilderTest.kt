package net.gini.android.bank.api

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.Session
import net.gini.android.core.api.authorization.SessionManager
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
class GiniBankAPIBuilderTest {

    @Test
    fun constructors_useTheBankAPI() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

        var bankAPIBuilder = GiniBankAPIBuilder(targetContext, "", "", "")
        assertEquals(bankAPIBuilder.getGiniApiType(), GiniBankApiType(apiVersion = 1))

        bankAPIBuilder = GiniBankAPIBuilder(targetContext, sessionManager = object: SessionManager {
            override suspend fun getSession(): Resource<Session> {
                return mock()
            }
        })
        assertEquals(bankAPIBuilder.getGiniApiType(), GiniBankApiType(apiVersion = 1))
    }
}