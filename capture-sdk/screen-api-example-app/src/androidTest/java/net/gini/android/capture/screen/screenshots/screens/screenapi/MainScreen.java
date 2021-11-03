package net.gini.android.capture.screen.screenshots.screens.screenapi;

import static net.gini.android.capture.screen.screenshots.Helper.isObjectAvailable;

import net.gini.android.capture.screen.screenshots.screens.Screen;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

public class MainScreen implements Screen {

    private static final String START_SCANNER_BUTTON_RES_ID =
            "net.gini.android.capture.screenapiexample:id/button_start_scanner";

    private final UiDevice mUiDevice;

    public MainScreen(final UiDevice uiDevice) {
        mUiDevice = uiDevice;
    }

    @Override
    public boolean isVisible() {
        return isObjectAvailable(new UiSelector().resourceId(START_SCANNER_BUTTON_RES_ID),
                mUiDevice);
    }

    public void startGiniCapture() throws UiObjectNotFoundException {
        final UiObject scannerButton = mUiDevice.findObject(
                new UiSelector().resourceId(START_SCANNER_BUTTON_RES_ID));
        scannerButton.clickAndWaitForNewWindow();
    }
}
