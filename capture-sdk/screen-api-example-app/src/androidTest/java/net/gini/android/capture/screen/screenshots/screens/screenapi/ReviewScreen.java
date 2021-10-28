package net.gini.android.capture.screen.screenshots.screens.screenapi;

import static net.gini.android.capture.screen.screenshots.Helper.isObjectAvailable;

import net.gini.android.capture.screen.screenshots.screens.Screen;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

public class ReviewScreen implements Screen {

    private static final String ROTATE_BUTTON_RES_ID =
            "net.gini.android.capture.screenapiexample:id/gc_button_rotate";
    private static final String NEXT_BUTTON_RES_ID =
            "net.gini.android.capture.screenapiexample:id/gc_button_next";

    private final UiDevice mUiDevice;

    public ReviewScreen(final UiDevice uiDevice) {
        mUiDevice = uiDevice;
    }

    @Override
    public boolean isVisible() {
        return isObjectAvailable(new UiSelector().resourceId(ROTATE_BUTTON_RES_ID), mUiDevice);
    }

    public void proceedToAnalysis() throws UiObjectNotFoundException {
        final UiObject button = mUiDevice.findObject(new UiSelector().resourceId(NEXT_BUTTON_RES_ID));
        button.clickAndWaitForNewWindow();
    }
}
