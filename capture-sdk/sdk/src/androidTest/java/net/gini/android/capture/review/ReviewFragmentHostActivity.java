package net.gini.android.capture.review;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureError;

/**
 * Created by Alpar Szotyori on 21.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

public class ReviewFragmentHostActivity extends
        ReviewFragmentHostActivityNotListener implements ReviewFragmentListener {

    @Override
    public void onError(@NonNull final GiniCaptureError error) {

    }

    @Override
    public void onProceedToAnalysisScreen(@NonNull final Document document,
            @Nullable final String errorMessage) {

    }
}
