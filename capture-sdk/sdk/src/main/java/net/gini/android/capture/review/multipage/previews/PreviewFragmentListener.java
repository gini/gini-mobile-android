package net.gini.android.capture.review.multipage.previews;

import net.gini.android.capture.document.ImageDocument;

import androidx.annotation.NonNull;

/**
 * Created by Alpar Szotyori on 11.05.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

/**
 * Internal use only.
 *
 * @suppress
 */
public interface PreviewFragmentListener {

    void onRetryUpload(@NonNull final ImageDocument document);

    void onDeleteDocument(@NonNull final ImageDocument document);
}
