package net.gini.android.capture;

import androidx.annotation.Nullable;

/**
 * Created by Alpar Szotyori on 13.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
public class GiniCaptureHelper {

    public static void setGiniCaptureInstance(@Nullable final GiniCapture giniCapture) {
        GiniCapture.setInstance(giniCapture);
    }

}
