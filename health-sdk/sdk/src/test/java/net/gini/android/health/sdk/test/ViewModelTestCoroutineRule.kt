package net.gini.android.health.sdk.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
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
 *
 * Updated according to the migration guide for 1.6.0:
 * https://github.com/Kotlin/kotlinx.coroutines/blob/1.6.0/kotlinx-coroutines-test/MIGRATION.md#simplify-code-by-removing-unneeded-entities
 */
@ExperimentalCoroutinesApi
class ViewModelTestCoroutineRule : TestRule {

    override fun apply(base: Statement, description: Description?) = object : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            Dispatchers.setMain(UnconfinedTestDispatcher())

            base.evaluate()

            Dispatchers.resetMain()
        }
    }

}