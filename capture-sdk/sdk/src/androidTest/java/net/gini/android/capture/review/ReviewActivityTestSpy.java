package net.gini.android.capture.review;

import android.content.Intent;

import net.gini.android.capture.Document;

import androidx.annotation.NonNull;

public class ReviewActivityTestSpy extends ReviewActivity {

    private ListenerHook mListenerHook;

    private Document mShouldAnalyzeDocument = null;
    private Intent mAddDataToResultIntent = null;
    private Document mProceedToAnalysisDocument = null;
    private Document mDocumentReviewedAndAnalyzedDocument = null;
    private Document mDocumentWasRotatedDocument = null;
    private int mDocumentWasRotatedDegreesOld = Integer.MAX_VALUE;
    private int mDocumentWasRotatedDegreesNew = Integer.MAX_VALUE;

    public void setListenerHook(final ListenerHook listenerHook) {
        mListenerHook = listenerHook;
        if (mShouldAnalyzeDocument != null) {
            mListenerHook.onShouldAnalyzeDocument(mShouldAnalyzeDocument);
        }
        if (mAddDataToResultIntent != null) {
            mListenerHook.onAddDataToResult(mAddDataToResultIntent);
        }
        if (mProceedToAnalysisDocument != null) {
            mListenerHook.onProceedToAnalysisScreen(mProceedToAnalysisDocument);
        }
        if (mDocumentReviewedAndAnalyzedDocument != null) {
            mListenerHook.onDocumentReviewedAndAnalyzed(mDocumentReviewedAndAnalyzedDocument);
        }
        if (mDocumentWasRotatedDocument != null
                && mDocumentWasRotatedDegreesOld != Integer.MAX_VALUE &&
                mDocumentWasRotatedDegreesNew != Integer.MAX_VALUE) {
            mListenerHook.onDocumentWasRotated(mDocumentWasRotatedDocument,
                    mDocumentWasRotatedDegreesOld, mDocumentWasRotatedDegreesNew);
        }
    }

    public static abstract class ListenerHook {

        public void onShouldAnalyzeDocument(@NonNull final Document document) {
        }

        public void onAddDataToResult(@NonNull final Intent result) {
        }

        public void onProceedToAnalysisScreen(@NonNull final Document document) {
        }

        public void onDocumentReviewedAndAnalyzed(@NonNull final Document document) {
        }

        public void onDocumentWasRotated(@NonNull final Document document, final int oldRotation,
                final int newRotation) {
        }
    }
}
