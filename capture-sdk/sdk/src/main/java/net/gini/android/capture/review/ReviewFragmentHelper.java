package net.gini.android.capture.review;

import android.content.Context;
import android.os.Bundle;

import net.gini.android.capture.Document;
import net.gini.android.capture.internal.ui.FragmentImplCallback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Internal use only.
 *
 * @suppress
 */
final class ReviewFragmentHelper {

    private static final String ARGS_DOCUMENT = "GC_ARGS_DOCUMENT";

    public static Bundle createArguments(@NonNull final Document document) {
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARGS_DOCUMENT, document);
        return arguments;
    }

    static ReviewFragmentImpl createFragmentImpl(@NonNull final FragmentImplCallback fragment,
            @NonNull final Bundle arguments) {
        final Document document = arguments.getParcelable(ARGS_DOCUMENT);
        if (document != null) {
            return new ReviewFragmentImpl(fragment, document);
        } else {
            throw new IllegalStateException(
                    "ReviewFragmentCompat requires a Document. Use the createInstance() method of these classes for instantiating.");
        }
    }

    public static void setListener(@NonNull final ReviewFragmentImpl fragmentImpl,
            @NonNull final Context context, @Nullable final ReviewFragmentListener listener) {
        if (context instanceof ReviewFragmentListener) {
            fragmentImpl.setListener((ReviewFragmentListener) context);
        } else if (listener != null) {
            fragmentImpl.setListener(listener);
        } else {
            throw new IllegalStateException(
                    "ReviewFragmentListener not set. "
                            + "You can set it with ReviewFragmentCompat#setListener() or "
                            + "by making the host activity implement the ReviewFragmentListener.");
        }
    }

    private ReviewFragmentHelper() {
    }
}
