package net.gini.android.capture.analysis;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import net.gini.android.capture.AsyncCallback;
import net.gini.android.capture.Document;
import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureError;
import net.gini.android.capture.analysis.warning.WarningPaymentState;
import net.gini.android.capture.analysis.warning.WarningType;
import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.GiniCaptureDocumentError;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.document.PdfDocument;
import net.gini.android.capture.error.ErrorType;
import net.gini.android.capture.internal.camera.photo.ParcelableMemoryCache;
import net.gini.android.capture.internal.document.DocumentRenderer;
import net.gini.android.capture.internal.document.DocumentRendererFactory;
import net.gini.android.capture.internal.network.FailureException;
import net.gini.android.capture.internal.qreducation.model.InvoiceEducationType;
import net.gini.android.capture.internal.storage.ImageDiskStore;
import net.gini.android.capture.internal.util.FileImportHelper;
import net.gini.android.capture.logging.ErrorLog;
import net.gini.android.capture.logging.ErrorLogger;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;
import net.gini.android.capture.tracking.AnalysisScreenEvent;
import net.gini.android.capture.tracking.AnalysisScreenEvent.ERROR_DETAILS_MAP_KEY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import jersey.repackaged.jsr166e.CompletableFuture;
import kotlin.Unit;

import static net.gini.android.capture.tracking.EventTrackingHelper.trackAnalysisScreenEvent;

/**
 * Created by Alpar Szotyori on 08.05.2019.
 * <p>
 * Copyright (c) 2019 Gini GmbH.
 */

/**
 * Internal use only
 */
class AnalysisScreenPresenter extends AnalysisScreenContract.Presenter {

    @VisibleForTesting
    static final String PARCELABLE_MEMORY_CACHE_TAG = "ANALYSIS_FRAGMENT";

    private static final String EXTRACTION_PAYMENT_STATE = "paymentState";

    private static final Logger LOG = LoggerFactory.getLogger(AnalysisScreenPresenter.class);

    @VisibleForTesting
    final AnalysisScreenPresenterExtension extension;

    private final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
            mMultiPageDocument;
    private final String mDocumentAnalysisErrorMessage;
    private final AnalysisInteractor mAnalysisInteractor;
    private final List<AnalysisHint> mHints;
    @VisibleForTesting
    DocumentRenderer mDocumentRenderer;
    private boolean mStopped;
    private boolean mAnalysisCompleted;
    private final boolean mIsInvoiceSavingEnabled;
    private boolean isSavingInvoicesInProgress = false;

    private AnalysisInteractor.ResultHolder successResultHolder;
    AnalysisScreenPresenter(
            @NonNull final Activity activity,
            @NonNull final AnalysisScreenContract.View view,
            @NonNull final Document document,
            @Nullable final String documentAnalysisErrorMessage,
            @NonNull final Boolean mIsInvoiceSavingEnabled) {
        this(activity, view, document, documentAnalysisErrorMessage,
                new AnalysisInteractor(activity.getApplication()), mIsInvoiceSavingEnabled);
    }

    @VisibleForTesting
    AnalysisScreenPresenter(
            @NonNull final Activity activity,
            @NonNull final AnalysisScreenContract.View view,
            @NonNull final Document document,
            @Nullable final String documentAnalysisErrorMessage,
            @NonNull final AnalysisInteractor analysisInteractor,
            @NonNull final Boolean mIsInvoiceSavingEnabled) {
        super(activity, view);
        view.setPresenter(this);
        mMultiPageDocument = asMultiPageDocument(document);
        // Tag the documents to be able to clean up the automatically parcelled data
        tagDocumentsForParcelableMemoryCache(document, mMultiPageDocument);
        mDocumentAnalysisErrorMessage = documentAnalysisErrorMessage;
        mAnalysisInteractor = analysisInteractor;
        mHints = generateRandomHintsList();
        extension = new AnalysisScreenPresenterExtension(view);
        this.mIsInvoiceSavingEnabled = mIsInvoiceSavingEnabled;
    }

    private List<AnalysisHint> generateRandomHintsList() {
        final List<AnalysisHint> list = AnalysisHint.getArray();
        Collections.shuffle(list, new Random());
        switch (mMultiPageDocument.getType()) {
            case IMAGE_MULTI_PAGE:
            case PDF_MULTI_PAGE:
            case QR_CODE_MULTI_PAGE:
                break;
            default:
                list.remove(AnalysisHint.MULTIPAGE);
        }
        return list;
    }

    private void tagDocumentsForParcelableMemoryCache(
            @NonNull final Document document,
            @NonNull final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                    multiPageDocument) {
        if (document instanceof GiniCaptureDocument) {
            ((GiniCaptureDocument) document).setParcelableMemoryCacheTag(
                    PARCELABLE_MEMORY_CACHE_TAG);
        }
        multiPageDocument.setParcelableMemoryCacheTag(PARCELABLE_MEMORY_CACHE_TAG);
    }

    @SuppressWarnings("unchecked")
    private GiniCaptureMultiPageDocument<GiniCaptureDocument,
            GiniCaptureDocumentError> asMultiPageDocument(@NonNull final Document document) {
        if (!(document instanceof GiniCaptureMultiPageDocument)) {
            return DocumentFactory.newMultiPageDocument((GiniCaptureDocument) document);
        } else {
            return (GiniCaptureMultiPageDocument) document;
        }
    }

    @Override
    void finish() {
        clearParcelableMemoryCache();
    }

    @VisibleForTesting
    void clearParcelableMemoryCache() {
        // Remove data from the memory cache. The data had been added when the document in the
        // arguments was automatically parcelled when the activity was stopped
        ParcelableMemoryCache.getInstance().removeEntriesWithTag(PARCELABLE_MEMORY_CACHE_TAG);
    }

    private void startScanAnimation() {
        getView().showScanAnimation(mIsInvoiceSavingEnabled);
    }

    private void stopScanAnimation() {
        getView().hideScanAnimation();
    }

    @Override
    public void setListener(@NonNull final AnalysisFragmentListener listener) {
        extension.setListener(listener);
    }

    @VisibleForTesting
    void clearSavedImages() {
        ImageDiskStore.clear(getActivity());
    }

    @Override
    public void start() {
        mStopped = false;
        checkGiniCaptureInstance();
        if (mMultiPageDocument.getType() != Document.Type.XML &&
                mMultiPageDocument.getType() != Document.Type.XML_MULTI_PAGE) {
            createDocumentRenderer();
        }
        clearParcelableMemoryCache();
        getView().showScanAnimation(mIsInvoiceSavingEnabled);
        loadDocumentData();
        showHintsForImage();
    }

    @Override
    public void stop() {
        mStopped = true;
        stopScanAnimation();
        if (!mAnalysisCompleted) {
            deleteUploadedDocuments();
        } else if (GiniCapture.hasInstance()) {
            GiniCapture.getInstance().internal().getImageMultiPageDocumentMemoryStore()
                    .clear();
        }
    }

    private void checkGiniCaptureInstance() {
        if (!GiniCapture.hasInstance()) {
            getView().showError(ErrorType.GENERAL, mMultiPageDocument);
        }
    }

    private void deleteUploadedDocuments() {
        if (mMultiPageDocument.getType() == Document.Type.PDF_MULTI_PAGE) {
            // Delete PDF partial documents here because the Camera Screen
            // doesn't keep references to them
            mAnalysisInteractor.deleteMultiPageDocument(mMultiPageDocument);
        } else {
            // Delete only the composite document
            mAnalysisInteractor.deleteDocument(mMultiPageDocument);
        }
    }

    @VisibleForTesting
    boolean isStopped() {
        return mStopped;
    }

    @VisibleForTesting
    GiniCaptureMultiPageDocument<
            GiniCaptureDocument, GiniCaptureDocumentError> getMultiPageDocument() {
        return mMultiPageDocument;
    }

    @VisibleForTesting
    List<AnalysisHint> getHints() {
        return mHints;
    }

    @VisibleForTesting
    void createDocumentRenderer() {
        final GiniCaptureDocument documentToRender = getFirstDocument();
        if (documentToRender != null) {
            mDocumentRenderer = DocumentRendererFactory.fromDocument(documentToRender);
        }
    }

    @Nullable
    @VisibleForTesting
    String getPdfFilename(final PdfDocument pdfDocument) {
        return pdfDocument.getFilename();
    }

    @VisibleForTesting
    void analyzeDocument() {
        showAlertIfOpenWithDocumentAndAppIsDefault(mMultiPageDocument,
                (message, positiveButtonTitle, positiveButtonClickListener, negativeButtonTitle, negativeButtonClickListener, cancelListener) -> getView().showAlertDialog(message, positiveButtonTitle,
                        positiveButtonClickListener, negativeButtonTitle,
                        negativeButtonClickListener, cancelListener))
                .handle((CompletableFuture.BiFun<Void, Throwable, Void>) (aVoid, throwable) -> {
                    if (throwable != null) {
                        extension.getAnalysisFragmentListenerOrNoOp()
                                .onDefaultPDFAppAlertDialogCancelled();
                    } else {
                        showErrorIfAvailableAndAnalyzeDocument();
                    }
                    return null;
                });
    }

    @VisibleForTesting
    CompletableFuture<Void> showAlertIfOpenWithDocumentAndAppIsDefault(
            @NonNull final GiniCaptureDocument document,
            @NonNull final FileImportHelper.ShowAlertCallback showAlertCallback) {
        return FileImportHelper.showAlertIfOpenWithDocumentAndAppIsDefault(getActivity(), document,
                showAlertCallback);
    }

    @VisibleForTesting
    void doAnalyzeDocument() {
        startScanAnimation();
        extension.showLoadingIndicator(() -> {
            stopScanAnimation();
            return Unit.INSTANCE;
        });
        mAnalysisInteractor.analyzeMultiPageDocument(mMultiPageDocument)
                .handle(new CompletableFuture.BiFun<
                        AnalysisInteractor.ResultHolder, Throwable, Void>() {
                    @Override
                    public Void apply(final AnalysisInteractor.ResultHolder resultHolder,
                                      final Throwable throwable) {

                        stopScanAnimation();
                        if (isStopped()) {
                            return null;
                        }
                        if (throwable != null) {
                            handleAnalysisError(throwable);
                            return null;
                        }
                        RemoteAnalyzedDocument remoteAnalyzedDocument =
                                new RemoteAnalyzedDocument(
                                        resultHolder.getDocumentId(),
                                        resultHolder.getDocumentFileName()
                                );
                        final AnalysisInteractor.Result result = resultHolder.getResult();
                        boolean shouldClearImageCaches = true;
                        switch (result) {
                            case SUCCESS_NO_EXTRACTIONS:
                                mAnalysisCompleted = true;
                                extension.getLastAnalyzedDocumentProvider()
                                        .update(remoteAnalyzedDocument);
                                try {
                                    extension.getAttachDocToTransactionDialogProvider()
                                            .update(remoteAnalyzedDocument);
                                } catch (Exception ignored) {

                                }
                                proceedSuccessNoExtractions();
                                break;
                            case SUCCESS_WITH_EXTRACTIONS: {

                                mAnalysisCompleted = true;
                                extension.getLastAnalyzedDocumentProvider()
                                        .update(remoteAnalyzedDocument);
                                try {
                                    extension.getAttachDocToTransactionDialogProvider()
                                            .update(remoteAnalyzedDocument);
                                } catch (Exception ignored) {

                                }

                                if (resultHolder.getExtractions().isEmpty()) {
                                    proceedSuccessNoExtractions();
                                } else if (shouldShowPaidInvoiceWarning(resultHolder)
                                && !isSavingInvoicesInProgress) {
                                    shouldClearImageCaches = false;
                                    getView().showPaidWarningThen(
                                            WarningType.DOCUMENT_MARKED_AS_PAID,
                                            () -> proceedWithExtractions(resultHolder)
                                    );
                                } else {
                                    proceedWithExtractions(resultHolder);
                                    shouldClearImageCaches = false;
                                }
                            }
                            case NO_NETWORK_SERVICE:
                                break;
                            default:
                                throw new UnsupportedOperationException(
                                        "Unknown AnalysisInteractor result: " + result);
                        }
                        if (result != AnalysisInteractor.Result.NO_NETWORK_SERVICE
                                && shouldClearImageCaches) {
                            clearSavedImages();
                        }
                        return null;
                    }
                });
    }

    private void proceedSuccessNoExtractions() {
        extension.proceedSuccessNoExtractions(mMultiPageDocument);
    }

    private void proceedWithExtractions(AnalysisInteractor.ResultHolder resultHolder) {
        successResultHolder = resultHolder;
        // This case will happen in case of screen rotation OR if the feature is disabled
        if (!mIsInvoiceSavingEnabled || isSavingInvoicesInProgress) {
            clearSavedImagesAndProceed();
            return;
        }
        // if the feature is enabled, Start the SAF flow
        getView().processInvoiceSaving();
    }
    private void clearSavedImagesAndProceed(){
        clearSavedImages();
        extension.proceedWithExtractions(successResultHolder);
    }

    private void loadDocumentData() {
        LOG.debug("Loading document data");
        mMultiPageDocument.loadData(getActivity(),
                new AsyncCallback<byte[], Exception>() {
                    @Override
                    public void onSuccess(final byte[] result) {
                        LOG.debug("Document data loaded");
                        if (isStopped()) {
                            return;
                        }
                        getView().waitForViewLayout()
                                .thenRun(new Runnable() {
                                    @Override
                                    public void run() {
                                        onViewLayoutFinished();
                                    }
                                });
                    }

                    @Override
                    public void onError(final Exception exception) {
                        LOG.error("Failed to load document data", exception);
                        if (isStopped()) {
                            return;
                        }
                        ErrorLogger.log(new ErrorLog("Failed to load document data", exception));
                        extension.getAnalysisFragmentListenerOrNoOp().onError(
                                new GiniCaptureError(GiniCaptureError.ErrorCode.ANALYSIS,
                                        "An error occurred while loading the document."));
                    }

                    @Override
                    public void onCancelled() {
                        // Not used
                    }
                });
    }

    private void onViewLayoutFinished() {
        LOG.debug("View layout finished");
        showPdfInfoForPdfDocument();
        showDocument();
        analyzeDocument();
    }

    private void showHintsForImage() {
        InvoiceEducationType invoiceEducationType = extension.getInvoiceEducationType();
        if (getFirstDocument().getType() == Document.Type.IMAGE &&
                invoiceEducationType == null) {
            getView().showHints(mHints);
        }
    }

    private GiniCaptureDocument getFirstDocument() {
        return mMultiPageDocument.getDocuments().get(0);
    }

    private void showPdfInfoForPdfDocument() {
        final GiniCaptureDocument documentToRender = getFirstDocument();
        if (documentToRender instanceof PdfDocument) {
            final PdfDocument pdfDocument = (PdfDocument) documentToRender;
            getView().showPdfInfoPanel();
            final String filename = getPdfFilename(pdfDocument);
            if (filename != null) {
                getView().showPdfTitle(filename);
            }
        }
    }

    private void showDocument() {
        LOG.debug("Rendering the document");
        if (mDocumentRenderer == null)
            return;
        mDocumentRenderer.toBitmap(getActivity(), getView().getPdfPreviewSize(),
                (bitmap, rotationForDisplay) -> {
                    LOG.debug("Document rendered");
                    if (isStopped()) {
                        return;
                    }

                    if (mMultiPageDocument.getType() == Document.Type.IMAGE_MULTI_PAGE || mMultiPageDocument.getType() == Document.Type.IMAGE) {
                        return;
                    }

                    getView().showBitmap(bitmap, rotationForDisplay);
                });
    }

    private void showErrorIfAvailableAndAnalyzeDocument() {
        if (mDocumentAnalysisErrorMessage != null && !mDocumentAnalysisErrorMessage.isEmpty()) {
            final Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put(ERROR_DETAILS_MAP_KEY.MESSAGE, mDocumentAnalysisErrorMessage);

            if (GiniCapture.hasInstance()) {
                final Throwable reviewScreenAnalysisError = GiniCapture.getInstance().internal().getReviewScreenAnalysisError();
                if (reviewScreenAnalysisError != null) {
                    errorDetails.put(ERROR_DETAILS_MAP_KEY.ERROR_OBJECT, reviewScreenAnalysisError);
                    trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR, errorDetails);
                }
            }

            getView().showError(mDocumentAnalysisErrorMessage, mMultiPageDocument);
        } else {
            doAnalyzeDocument();
        }
    }

    private void handleAnalysisError(final Throwable throwable) {
        final Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put(ERROR_DETAILS_MAP_KEY.MESSAGE, throwable.getMessage());
        errorDetails.put(ERROR_DETAILS_MAP_KEY.ERROR_OBJECT, throwable);
        trackAnalysisScreenEvent(AnalysisScreenEvent.ERROR, errorDetails);

        final ErrorType errorType;
        final FailureException failureException = FailureException.tryCastFromCompletableFutureThrowable(throwable);
        if (failureException != null) {
            errorType = failureException.getErrorType();
        } else {
            errorType = ErrorType.GENERAL;
        }
        getView().showError(errorType, mMultiPageDocument);
    }

    /**
     * We only have to add the files which are captured from camera, That's why we have to filter
     * out the files which are imported via picker or they are from "open with" flow.
     * */
    @Override
    public List<Uri> assembleMultiPageDocumentUris() {
        if (mMultiPageDocument == null || mMultiPageDocument.getDocuments() == null) {
            return Collections.emptyList();
        }

        return mMultiPageDocument.getDocuments().stream()
                .filter(doc -> doc.getUri() != null)
                .filter(doc -> doc.getImportMethod() != Document.ImportMethod.PICKER
                        && doc.getImportMethod() != Document.ImportMethod.OPEN_WITH)
                .map(Document::getUri)
                .collect(Collectors.toList());
    }

    @Override
    void updateInvoiceSavingState(Boolean isInProgress) {
        isSavingInvoicesInProgress = isInProgress;
    }

    @Override
    void releaseMutexForEducation() {
        extension.releaseMutex();
    }

    /**
     * Resumes the interrupted processing flow after the user selects a folder via SAF.
     * <p>
     * After a configuration change {@code successResultHolder} will be {@code null}.
     * In that case the flow is restarted by calling {@link #start()}, and rest of the
     * flow is handled by {@link #proceedWithExtractions(AnalysisInteractor.ResultHolder)}.
     * So, this method must not attempt to resume.
     * When {@code successResultHolder} is non-null, this method clears saved images and
     * continues processing.
     */
    @Override
    public void resumeInterruptedFlow() {
        if (successResultHolder == null) return;
        clearSavedImagesAndProceed();
    }


    private boolean shouldShowPaidInvoiceWarning(
            @NonNull final AnalysisInteractor.ResultHolder resultHolder) {
        // Feature flags / config
        final boolean hintsEnabled = extension.getGetPaymentHintsEnabledUseCase().invoke();

        final boolean showPaidWarningFlag = GiniCapture.hasInstance() && GiniCapture.getInstance().isPaymentHintsEnabled();

        if (!hintsEnabled || !showPaidWarningFlag) {
            return false;
        }

        // Payment state
        final Map<String, GiniCaptureSpecificExtraction> exts = resultHolder.getExtractions();
        final GiniCaptureSpecificExtraction ps = exts.get(EXTRACTION_PAYMENT_STATE);
        final String paymentStateValue = ps != null ? ps.getValue() : null;
        final WarningPaymentState state = WarningPaymentState.from(paymentStateValue);

        return state.isPaid();
    }

}
