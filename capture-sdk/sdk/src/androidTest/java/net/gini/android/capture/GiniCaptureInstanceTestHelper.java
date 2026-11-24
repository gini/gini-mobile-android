package net.gini.android.capture;

import androidx.annotation.Nullable;

/**
 * Helper class to set the {@link GiniCapture} instance for instrumentation tests.
 */
public class GiniCaptureInstanceTestHelper {
    public static void setGiniCaptureInstance(@Nullable final GiniCapture giniCapture) {
        GiniCapture.setInstance(giniCapture);
    }

}
