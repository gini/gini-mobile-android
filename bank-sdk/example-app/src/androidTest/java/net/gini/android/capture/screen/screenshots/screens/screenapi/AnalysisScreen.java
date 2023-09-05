package net.gini.android.capture.screen.screenshots.screens.screenapi;

import net.gini.android.capture.screen.screenshots.screens.Screen;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiSelector;

import static net.gini.android.capture.screen.screenshots.Helper.isObjectAvailable;

public class AnalysisScreen implements Screen {

    private static final String IMAGE_RES_ID =
            "net.gini.android.capture.screenapiexample:id/gc_image_picture";

    private final UiDevice mUiDevice;

    public AnalysisScreen(final UiDevice uiDevice) {
        mUiDevice = uiDevice;
    }

    @Override
    public boolean isVisible() {
        return isObjectAvailable(
                new UiSelector().resourceId(IMAGE_RES_ID), mUiDevice);
    }

}
