package net.gini.android.merchant.sdk.review.openWith

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import net.gini.android.merchant.sdk.R

@RunWith(AndroidJUnit4::class)
class OpenWithBottomSheetTest {
    @Test
    fun `listener method called when 'Forward' button tapped`() = runTest {
        // Given
        val listener: OpenWithForwardListener = mockk()
        every { listener.onForwardSelected() } returns mockk()

        launchFragmentInContainer(themeResId = R.style.GiniMerchantTheme) {
            OpenWithBottomSheet.newInstance(
                mockk(relaxed = true),
                listener
            )
        }

        // When
        onView(withId(R.id.gms_forward_button)).perform(ViewActions.click())

        // Then
        verify { listener.onForwardSelected() }
    }
}