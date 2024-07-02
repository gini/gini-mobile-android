package net.gini.android.bank.sdk.exampleapp.ui.screens

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import net.gini.android.bank.sdk.exampleapp.R

class MainScreen {

    fun assertWelcomeTitle(): MainScreen {
        onView(withId(R.id.tv_welcomeToGini)).check(matches(isDisplayed()))
        return this
    }

    fun assertDescriptionTitle(): MainScreen {
        onView(withId(R.id.tv_exampleOfPhotoPayment)).check(matches(isDisplayed()))
        return this
    }

    fun checkCheckIconDisplayed(): MainScreen {
        onView(withId(R.id.til_fieldEntryPoint)).check(matches(isDisplayed()))
        return this
    }

    fun clickCameraIcon(): MainScreen {
        onView(withId(R.id.til_fieldEntryPoint)).perform(click())
        return this
    }

    fun checkScannerButtonDisplayed(): MainScreen {
        onView(withId(R.id.button_startScanner)).check(matches(isDisplayed()))
        return this
    }

    fun clickPhotoPaymentButton(): MainScreen {
        onView(withId(R.id.button_startScanner)).perform(click())
        return this
    }

    fun clickSettingButton(): MainScreen {
        onView(withId(R.id.text_giniBankVersion)).perform(click())
        return this
    }
}