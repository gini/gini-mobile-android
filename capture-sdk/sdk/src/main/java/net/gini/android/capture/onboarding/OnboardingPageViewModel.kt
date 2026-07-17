package net.gini.android.capture.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.gini.android.capture.onboarding.view.OnboardingIllustrationAdapter

/**
 * Internal use only.
 *
 * ViewModel for a single onboarding page. Exposes the page's illustration, title and message
 * and forwards page visibility changes to the illustration adapter.
 *
 * @suppress
 */
internal class OnboardingPageViewModel : ViewModel() {

    private var page: OnboardingPage? = null

    private val mutableIllustrationAdapter = MutableLiveData<OnboardingIllustrationAdapter>()

    /**
     * The illustration adapter to be shown for the page.
     */
    val illustrationAdapter: LiveData<OnboardingIllustrationAdapter> = mutableIllustrationAdapter

    private val mutableTitleResId = MutableLiveData<Int>()

    /**
     * The string resource id of the page's title.
     */
    val titleResId: LiveData<Int> = mutableTitleResId

    private val mutableMessageResId = MutableLiveData<Int>()

    /**
     * The string resource id of the page's message.
     */
    val messageResId: LiveData<Int> = mutableMessageResId

    fun setPage(page: OnboardingPage) {
        this.page = page
    }

    fun start() {
        showImage()
        showText()
    }

    fun onPageIsVisible() {
        page?.illustrationAdapter?.onVisible()
    }

    fun onPageIsHidden() {
        page?.illustrationAdapter?.onHidden()
    }

    private fun showImage() {
        val adapter = page?.illustrationAdapter ?: return
        mutableIllustrationAdapter.value = adapter
    }

    private fun showText() {
        val page = this.page ?: return
        if (page.titleResId != 0) {
            mutableTitleResId.value = page.titleResId
        }
        if (page.messageResId != 0) {
            mutableMessageResId.value = page.messageResId
        }
    }
}
