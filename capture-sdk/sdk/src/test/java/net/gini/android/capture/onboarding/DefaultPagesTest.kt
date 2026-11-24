package net.gini.android.capture.onboarding

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import net.gini.android.capture.GiniCapture
import net.gini.android.capture.GiniCaptureHelper
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DefaultPagesTest {

    @After
    fun tearDown() {
        GiniCaptureHelper.setGiniCaptureInstance(null)
    }

    @Test
    fun `has the two basic pages when multi-page and QR code scanning are disabled`() {
        assertThat(DefaultPages.asArrayList(false, false)).hasSize(2)
    }

    @Test
    fun `adds a page for multi-page and one for QR code scanning when they are enabled`() {
        assertThat(DefaultPages.asArrayList(true, false)).hasSize(3)
        assertThat(DefaultPages.asArrayList(false, true)).hasSize(3)
        assertThat(DefaultPages.asArrayList(true, true)).hasSize(4)
    }

    @Test
    fun `keeps the default illustrations when the client configured none`() {
        val pagesWithoutInstance = DefaultPages.asArrayList(true, true)
        GiniCapture.Builder().build()

        val pagesWithInstance = DefaultPages.asArrayList(true, true)

        assertThat(pagesWithInstance.map { it.illustrationAdapter?.javaClass })
            .isEqualTo(pagesWithoutInstance.map { it.illustrationAdapter?.javaClass })
    }

    @Test
    fun `uses the illustrations configured by the client`() {
        val alignCorners = mock<OnboardingIllustrationAdapter>()
        val lighting = mock<OnboardingIllustrationAdapter>()
        val multiPage = mock<OnboardingIllustrationAdapter>()
        val qrCode = mock<OnboardingIllustrationAdapter>()
        GiniCapture.Builder()
            .setOnboardingAlignCornersIllustrationAdapter(alignCorners)
            .setOnboardingLightingIllustrationAdapter(lighting)
            .setOnboardingMultiPageIllustrationAdapter(multiPage)
            .setOnboardingQRCodeIllustrationAdapter(qrCode)
            .build()

        val pages = DefaultPages.asArrayList(true, true)

        assertThat(pages.map { it.illustrationAdapter })
            .containsExactly(alignCorners, lighting, multiPage, qrCode).inOrder()
    }
}
