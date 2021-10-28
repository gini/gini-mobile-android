package net.gini.android.capture.review;

import static net.gini.android.capture.test.Helpers.createDocument;
import static net.gini.android.capture.test.Helpers.getTestJpeg;

import net.gini.android.capture.document.ImageDocument;
import net.gini.android.capture.test.FragmentHostActivity;

import java.io.IOException;

/**
 * Created by Alpar Szotyori on 21.02.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */

public class ReviewFragmentHostActivityNotListener extends
        FragmentHostActivity<ReviewFragmentCompat> {

    static ReviewFragmentListener sListener;

    @Override
    protected void setListener() {
        if (sListener != null) {
            getFragment().setListener(sListener);
        }
    }

    @Override
    protected ReviewFragmentCompat createFragment() {
        try {
            return ReviewFragmentCompat.createInstance(
                    createDocument(getTestJpeg(), 0, "portrait", "phone", ImageDocument.Source.newCameraSource()));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}
