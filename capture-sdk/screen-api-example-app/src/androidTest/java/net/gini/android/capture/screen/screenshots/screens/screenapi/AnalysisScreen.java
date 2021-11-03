package net.gini.android.capture.screen.screenshots.screens.screenapi;

import static net.gini.android.capture.screen.screenshots.Helper.isObjectAvailable;

import net.gini.android.capture.screen.screenshots.screens.Screen;

import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiSelector;

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
