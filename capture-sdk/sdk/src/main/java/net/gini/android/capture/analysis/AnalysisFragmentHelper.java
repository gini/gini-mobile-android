package net.gini.android.capture.analysis;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.BankSDKBridge;
import net.gini.android.capture.Document;

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

    @NonNull
    static Document requireDocument(@Nullable final Bundle arguments) {
        final Document document = arguments != null
                ? arguments.getParcelable(ARGS_DOCUMENT)
                : null;
        if (document == null) {
            throw new IllegalStateException(
                    "AnalysisFragmentCompat requires a Document. Use the createInstance() method of these classes for instantiating.");
        }
        return document;
    }

    @Nullable
    static String getDocumentAnalysisErrorMessage(@Nullable final Bundle arguments) {
        return arguments != null
                ? arguments.getString(ARGS_DOCUMENT_ANALYSIS_ERROR_MESSAGE)
                : null;
    }

    static boolean isInvoiceSavingEnabled(@Nullable final Bundle arguments) {
        return arguments != null && arguments.getBoolean(GC_ARGS_SAVE_INVOICES, false);
    }

    @NonNull
    static AnalysisFragmentListener resolveListener(@Nullable final Context context,
            @Nullable final AnalysisFragmentListener listener) {
        if (context instanceof AnalysisFragmentListener) {
            return (AnalysisFragmentListener) context;
        } else if (listener != null) {
            return listener;
        } else {
            throw new IllegalStateException(
                    "AnalysisFragmentListener not set. "
                            + "You can set it with AnalysisFragmentCompat#setListener() or "
                            + "by making the host activity implement the AnalysisFragmentListener.");
        }
    }

    @NonNull
    static BankSDKBridge resolveBankSDKBridge(@Nullable final Context context,
            @Nullable final BankSDKBridge bankSDKBridge) {
        if (context instanceof BankSDKBridge) {
            return (BankSDKBridge) context;
        } else if (bankSDKBridge != null) {
            return bankSDKBridge;
        } else {
            throw new IllegalStateException(
                    "BankSDKBridge not set. "
                            + "You can set it with AnalysisFragmentCompat#setBankSDKBridge() or "
                            + "by making the host activity implement the BankSDKBridge.");
        }
    }

    private AnalysisFragmentHelper() {
    }
}
