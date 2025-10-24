package net.gini.android.capture.analysis;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.Document;
import net.gini.android.capture.internal.ui.FragmentImplCallback;
import net.gini.android.capture.internal.util.CancelListener;

/**
 * Helper class for setting arguments to analysis fragment
 */
final class AnalysisFragmentHelper {

    private static final String ARGS_DOCUMENT = "GC_ARGS_DOCUMENT";
    private static final String GC_ARGS_SAVE_INVOICES = "GC_ARGS_SAVE_INVOICES";
    private static final String ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE =
            "GC_ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE";

    public static Bundle createArguments(@NonNull final Document document,
            @Nullable final String documentAnalysisErrorMessage,
                                         final Boolean saveInvoices) {
        final Bundle arguments = new Bundle();
        arguments.putParcelable(ARGS_DOCUMENT, document);
        arguments.putBoolean(GC_ARGS_SAVE_INVOICES, saveInvoices);
        if (documentAnalysisErrorMessage != null) {
            arguments.putString(ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE, documentAnalysisErrorMessage);
        }
        return arguments;
    }

    static AnalysisFragmentImpl createFragmentImpl(@NonNull final FragmentImplCallback fragment, @NonNull CancelListener cancelListener,
                                                   @NonNull final Bundle arguments) {
        final Document document = arguments.getParcelable(ARGS_DOCUMENT);
        final Boolean isInvoiceSavingEnabled = arguments.getBoolean(GC_ARGS_SAVE_INVOICES, false);
        if (document != null) {
            final String analysisErrorMessage = arguments.getString(
                    ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE);
            return new AnalysisFragmentImpl(fragment, cancelListener, document, analysisErrorMessage, isInvoiceSavingEnabled);
        } else {
            throw new IllegalStateException(
                    "AnalysisFragmentCompat requires a Document. Use the createInstance() method of these classes for instantiating.");
        }
    }

    public static void setListener(@NonNull final AnalysisFragmentImpl fragmentImpl,
            @NonNull final Context context, @Nullable final AnalysisFragmentListener listener) {
        if (context instanceof AnalysisFragmentListener) {
            fragmentImpl.setListener((AnalysisFragmentListener) context);
        } else if (listener != null) {
            fragmentImpl.setListener(listener);
        } else {
            throw new IllegalStateException(
                    "AnalysisFragmentListener not set. "
                            + "You can set it with AnalysisFragmentCompat#setListener() or "
                            + "by making the host activity implement the AnalysisFragmentListener.");
        }
    }

    private AnalysisFragmentHelper() {
    }
}
