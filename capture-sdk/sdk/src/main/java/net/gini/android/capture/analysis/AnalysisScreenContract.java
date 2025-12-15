package net.gini.android.capture.analysis;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCaptureBasePresenter;
import net.gini.android.capture.GiniCaptureBaseView;
import net.gini.android.capture.analysis.education.EducationCompleteListener;
import net.gini.android.capture.analysis.paymentDueHint.PaymentDueHintDismissListener;
import net.gini.android.capture.analysis.warning.WarningType;
import net.gini.android.capture.error.ErrorType;
import net.gini.android.capture.internal.util.Size;

import java.util.List;

import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Created by Alpar Szotyori on 08.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 *
 */
interface AnalysisScreenContract {

    abstract class View implements GiniCaptureBaseView<Presenter>, AnalysisFragmentInterface {

        private Presenter mPresenter;

        @Override
        public void setPresenter(@NonNull final Presenter presenter) {
            mPresenter = presenter;
        }

        public Presenter getPresenter() {
            return mPresenter;
        }

        abstract void showScanAnimation(Boolean isSavingInvoicesLocallyEnabled);

        abstract void hideScanAnimation();

        abstract CompletableFuture<Void> waitForViewLayout();

        abstract void showPdfInfoPanel();

        abstract void showPdfTitle(@NonNull final String title);

        abstract Size getPdfPreviewSize();

        abstract void showBitmap(@Nullable final Bitmap bitmap, final int rotationForDisplay);

        abstract void showAlertDialog(@NonNull final String message,
                @NonNull final String positiveButtonTitle,
                @NonNull final DialogInterface.OnClickListener positiveButtonClickListener,
                @Nullable final String negativeButtonTitle,
                @Nullable final DialogInterface.OnClickListener negativeButtonClickListener,
                @Nullable final DialogInterface.OnCancelListener cancelListener);

        abstract void showHints(List<AnalysisHint> hints);

        abstract void showError(String errorMessage, Document document);
        abstract void showAlreadyPaidWarning(@NonNull WarningType warningType, @NonNull Runnable onProceed);
        abstract void showCreditNoteWarning(@NonNull WarningType warningType, @NonNull Runnable onProceed);
        abstract void showPaymentDueHint(PaymentDueHintDismissListener listener, String dueDate);
        abstract void showError(ErrorType errorType, Document document);

        abstract void showEducation(EducationCompleteListener listener);
        abstract void processInvoiceSaving();
    }

    abstract class Presenter extends GiniCaptureBasePresenter<View> implements
            AnalysisFragmentInterface {

        Presenter(@NonNull final Activity activity,
                @NonNull final View view) {
            super(activity, view);
        }

        abstract void finish();

        abstract void resumeInterruptedFlow();

        abstract List<Uri> assembleMultiPageDocumentUris();

        abstract void updateInvoiceSavingState(Boolean isInProgress);

        abstract void releaseMutexForEducation();
    }
}
