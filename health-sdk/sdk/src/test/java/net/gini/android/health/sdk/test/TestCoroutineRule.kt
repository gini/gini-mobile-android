package net.gini.android.health.sdk.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Created by Alpár Szotyori on 13.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

/**
 * Source: https://blog.mindorks.com/unit-testing-viewmodel-with-kotlin-coroutines-and-livedata
 */
@ExperimentalCoroutinesApi
class TestCoroutineRule : TestRule {

    private val dispatcher = TestCoroutineDispatcher()

    val scope = TestCoroutineScope(dispatcher)

    override fun apply(base: Statement, description: Description?) = object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            Dispatchers.setMain(dispatcher)

            base.evaluate()

            Dispatchers.resetMain()
            scope.cleanupTestCoroutines()
        }
    }

}