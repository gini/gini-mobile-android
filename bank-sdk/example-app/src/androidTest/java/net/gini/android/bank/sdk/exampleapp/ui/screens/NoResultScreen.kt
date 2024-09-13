package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText

class NoResultScreen {

    fun checkNoResultTitleIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_title_no_results)).check(matches(
            isDisplayed()
        ))
    }

    fun checkNoResultHeaderIsDisplay(){
        onView(withText(net.gini.android.capture.R.string.gc_noresults_header)).check(matches(
            isDisplayed()
        ))

    }

    fun checkSupportedFormatIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_supported_format_section_header)).check(matches(
            isDisplayed()
        ))
    }

    fun checkComputerGeneratedInvoiceIsDisplayed() {
        onView(withText(net.gini.android.capture.R.string.gc_supported_format_printed_invoices)).check(matches(
            isDisplayed()
        ))
    }

    fun checkNotSupportedFormatIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_unsupported_format_section_header)).check(matches(
            isDisplayed()
        ))
    }

    fun checkHandwritingIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_unsupported_format_handwriting)).check(matches(
            isDisplayed()
        ))
    }

    fun checkEnterManuallyButtonIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_noresults_enter_manually)).check(matches(
            isDisplayed()
        ))
    }

    fun clickEnterManuallyButton() {
        onView(withText(net.gini.android.capture.R.string.gc_noresults_enter_manually)).perform(
            click())
    }

    fun checkUsefulTipsIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_useful_tips)).check(matches(
            isDisplayed()
        ))
    }

    fun checkGoodLightingTitleIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_photo_tip_good_lighting_title)).check(matches(
            isDisplayed()
        ))
    }

    fun checkFlattenTitleIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_photo_tip_flatten_the_page_title)).check(matches(
            isDisplayed()
        ))
    }

    fun checkParallelTitleIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_photo_tip_parallel_title)).check(matches(
            isDisplayed()
        ))
    }

    fun checkPositionInTheFrameTitleIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_photo_tip_align_title)).check(matches(
            isDisplayed()
        ))
    }

    fun checkMultiPagesTitleIsDisplay() {
        onView(withText(net.gini.android.capture.R.string.gc_photo_tip_multiple_pages_title)).check(matches(
            isDisplayed()
        ))
    }
}