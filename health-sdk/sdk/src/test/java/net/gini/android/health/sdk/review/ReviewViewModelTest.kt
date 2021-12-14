package net.gini.android.health.sdk.review

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import net.gini.android.health.sdk.GiniHealth
import net.gini.android.health.sdk.preferences.UserPreferences
import net.gini.android.health.sdk.test.TestCoroutineRule
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Created by Alpár Szotyori on 13.12.21.
 *
 * Copyright (c) 2021 Gini GmbH.
 */

@ExperimentalCoroutinesApi
class ReviewViewModelTest {

    @get:Rule
    val testCoroutineRule = TestCoroutineRule()

    private var giniHealth: GiniHealth? = null
    private var userPreferences: UserPreferences? = null

    @Before
    fun setup() {
        giniHealth = mockk(relaxed = true)
        userPreferences = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        giniHealth = null
        userPreferences = null
    }

    @Test
    fun `shows info bar on launch`() = testCoroutineRule.scope.runBlockingTest {
        // Given
        val viewModel = ReviewViewModel(giniHealth!!, userPreferences!!)

        // When
        val isVisible = viewModel.isInfoBarVisible.first()

        // Then
        assertThat(isVisible).isTrue()
    }

    @Test
    fun `hides info bar after a delay`() = testCoroutineRule.scope.runBlockingTest {
        // Given
        val viewModel = ReviewViewModel(giniHealth!!, userPreferences!!)

        // When
        testCoroutineRule.scope.advanceTimeBy(ReviewViewModel.SHOW_INFO_BAR_MS)

        val isVisible = viewModel.isInfoBarVisible.first()

        // Then
        assertThat(isVisible).isFalse()
    }

}