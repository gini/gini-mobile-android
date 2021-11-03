package net.gini.android.capture.review.multipage.previews;

import net.gini.android.capture.document.GiniCaptureDocumentError;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 14.05.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public interface PreviewsAdapterListener {

    PreviewFragment.ErrorButtonAction getErrorButtonAction(
            @NonNull final GiniCaptureDocumentError documentError);
}
