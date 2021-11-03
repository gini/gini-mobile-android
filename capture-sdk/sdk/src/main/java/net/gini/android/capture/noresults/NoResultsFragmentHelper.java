package net.gini.android.capture.noresults;

import android.content.Context;
import android.os.Bundle;

import net.gini.android.capture.Document;
import net.gini.android.capture.internal.ui.FragmentImplCallback;

import androidx.annotation.NonNull;

final class NoResultsFragmentHelper {

    private static final String ARGS_DOCUMENT = "GC_ARGS_DOCUMENT";

    public static Bundle createArguments(@NonNull final Document document) {
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARGS_DOCUMENT, document);
        return arguments;
    }

    static NoResultsFragmentImpl createFragmentImpl(@NonNull final FragmentImplCallback fragment,
            @NonNull final Bundle arguments) {
        final Document document = arguments.getParcelable(ARGS_DOCUMENT);
        if (document != null) {
            return new NoResultsFragmentImpl(fragment, document);
        } else {
            throw new IllegalStateException(
                    "NoResultsFragmentCompat requires a Document. Use the createInstance() method of these classes for instantiating.");
        }
    }

    public static void setListener(@NonNull final NoResultsFragmentImpl fragmentImpl,
            @NonNull final Context context) {
        if (context instanceof NoResultsFragmentListener) {
            fragmentImpl.setListener((NoResultsFragmentListener) context);
        } else {
            throw new IllegalStateException(
                    "Hosting activity must implement NoResultsFragmentListener.");
        }
    }

    private NoResultsFragmentHelper() {
    }
}
