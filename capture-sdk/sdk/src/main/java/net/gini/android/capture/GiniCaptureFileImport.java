package net.gini.android.capture;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import net.gini.android.capture.analysis.AnalysisActivity;
import net.gini.android.capture.document.DocumentFactory;
import net.gini.android.capture.document.ImageMultiPageDocument;
import net.gini.android.capture.internal.util.ActivityHelper;
import net.gini.android.capture.internal.util.DeviceHelper;
import net.gini.android.capture.internal.util.FileImportValidator;
import net.gini.android.capture.internal.util.MimeType;
import net.gini.android.capture.review.ReviewActivity;
import net.gini.android.capture.review.multipage.MultiPageReviewActivity;
import net.gini.android.capture.util.CancellationToken;
import net.gini.android.capture.util.IntentHelper;
import net.gini.android.capture.util.NoOpCancellationToken;
import net.gini.android.capture.util.UriHelper;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This class contains methods for preparing launching the Gini Capture SDK with a file received
 * from another app.
 */
public final class GiniCaptureFileImport {

    @NonNull
    private final GiniCapture mGiniCapture;

    /**
     * Screen API
     *
     * <p> When your application receives a file from another application you can use this method to
     * create an Intent for launching the Gini Capture SDK.
     *
     * <p> Start the Intent with {@link android.app.Activity#startActivityForResult(Intent, int)} to
     * receive the {@link GiniCaptureError} in case there was an error.
     *
     * @param intent                the Intent your app received
     * @param context               Android context
     * @param reviewActivityClass   the class of your application's {@link ReviewActivity} subclass
     * @param analysisActivityClass the class of your application's {@link AnalysisActivity}
     *                              subclass
     *
     * @return an Intent for launching the Gini Capture SDK
     *
     * @throws ImportedFileValidationException if the file didn't pass validation
     * @throws IllegalArgumentException        if the Intent's data is not valid or the mime type is
     *                                         not supported
     * @Deprecated Use {@link GiniCapture#createIntentForImportedFile(Intent, Context, Class, Class)}
     * instead.
     */
    @Deprecated
    @NonNull
    public static Intent createIntentForImportedFile(@NonNull final Intent intent,
            @NonNull final Context context,
            @NonNull final Class<? extends ReviewActivity> reviewActivityClass,
            @NonNull final Class<? extends AnalysisActivity> analysisActivityClass)
            throws ImportedFileValidationException {
        final Document document = createDocumentForImportedFile(intent, context);
        return createIntentForSingleDocument(context, reviewActivityClass, analysisActivityClass,
                document);
    }

    @NonNull
    private static Intent createIntentForSingleDocument(@NonNull final Context context,
            @NonNull final Class<? extends ReviewActivity> reviewActivityClass,
            @NonNull final Class<? extends AnalysisActivity> analysisActivityClass,
            final Document document) {
        final Intent giniCaptureIntent;
        if (document.isReviewable()) {
            giniCaptureIntent = createReviewActivityIntent(context, reviewActivityClass,
                    analysisActivityClass, document);
        } else {
            giniCaptureIntent = new Intent(context, analysisActivityClass);
            giniCaptureIntent.putExtra(AnalysisActivity.EXTRA_IN_DOCUMENT, document);
            giniCaptureIntent.setExtrasClassLoader(GiniCaptureFileImport.class.getClassLoader());
        }
        return giniCaptureIntent;
    }

    @NonNull
    private static Intent createReviewActivityIntent(@NonNull final Context context,
            @Nullable final Class<? extends ReviewActivity> reviewActivityClass,
            @Nullable final Class<? extends AnalysisActivity> analysisActivityClass,
            final Document document) {
        final Intent giniCaptureIntent;
        giniCaptureIntent = new Intent(context, getReviewActivityClass(reviewActivityClass));
        giniCaptureIntent.putExtra(ReviewActivity.EXTRA_IN_DOCUMENT, document);
        giniCaptureIntent.setExtrasClassLoader(GiniCaptureFileImport.class.getClassLoader());
        ActivityHelper.setActivityExtra(giniCaptureIntent,
                ReviewActivity.EXTRA_IN_ANALYSIS_ACTIVITY, context,
                getAnalysisActivityClass(analysisActivityClass));
        return giniCaptureIntent;
    }

    @NonNull
    private static Class<? extends ReviewActivity> getReviewActivityClass(
            @Nullable final Class<? extends ReviewActivity> reviewActivityClass) {
        return reviewActivityClass == null ? ReviewActivity.class : reviewActivityClass;
    }

    @NonNull
    private static Class<? extends AnalysisActivity> getAnalysisActivityClass(
            @Nullable final Class<? extends AnalysisActivity> analysisActivityClass) {
        return analysisActivityClass == null ? AnalysisActivity.class : analysisActivityClass;
    }

    /**
     * Component API
     *
     * <p> When your application receives a file from another application you can use this method to
     * create a Document for launching the Gini Capture SDK's Review Fragment or Analysis
     * Fragment.
     *
     * <p> If the Document can be reviewed ({@link Document#isReviewable()}) launch the
     * Review Fragment ({@link net.gini.android.capture.review.ReviewFragmentCompat}).
     *
     * <p> If the Document cannot be reviewed you must launch the Analysis Fragments ({@link
     * net.gini.android.capture.analysis.AnalysisFragmentCompat}).
     *
     * @param intent  the Intent your app received
     * @param context Android context
     *
     * @return a Document for launching the Gini Capture SDK's Review Fragment or
     * Analysis Fragment
     *
     * @throws ImportedFileValidationException if the file didn't pass validation
     * @Deprecated Use {@link GiniCapture#createDocumentForImportedFile(Intent, Context)} instead.
     */
    @Deprecated
    @NonNull
    public static Document createDocumentForImportedFile(@NonNull final Intent intent,
            @NonNull final Context context) throws ImportedFileValidationException {
        final Uri uri = IntentHelper.getUri(intent);
        if (uri == null) {
            throw new ImportedFileValidationException("Intent data did not contain a Uri");
        }
        if (!UriHelper.isUriInputStreamAvailable(uri, context)) {
            throw new ImportedFileValidationException(
                    "InputStream not available for Intent's data Uri");
        }
        final FileImportValidator fileImportValidator = new FileImportValidator(context);
        if (fileImportValidator.matchesCriteria(intent, uri)) {
            return DocumentFactory.newDocumentFromIntent(intent, context,
                    DeviceHelper.getDeviceOrientation(context), DeviceHelper.getDeviceType(context),
                    Document.ImportMethod.OPEN_WITH);
        } else {
            throw new ImportedFileValidationException(fileImportValidator.getError());
        }
    }

    GiniCaptureFileImport(@NonNull final GiniCapture giniCapture) {
        mGiniCapture = giniCapture;
    }

    CancellationToken createIntentForImportedFiles(@NonNull final Intent intent,
            @NonNull final Context context,
            @NonNull final AsyncCallback<Intent, ImportedFileValidationException> callback) {
        final CancellationToken cancellationToken =
                createDocumentForImportedFiles(intent, context,
                        new AsyncCallback<Document, ImportedFileValidationException>() {
                            @Override
                            public void onSuccess(final Document result) {
                                final Intent giniCaptureIntent;
                                if (result.getType() == Document.Type.IMAGE_MULTI_PAGE) {
                                    // The new ImageMultiPageDocument was already added to the memory store
                                    giniCaptureIntent = MultiPageReviewActivity.createIntent(
                                            context);
                                } else {
                                    giniCaptureIntent = createIntentForSingleDocument(context,
                                            ReviewActivity.class, AnalysisActivity.class,
                                            result);
                                }
                                callback.onSuccess(giniCaptureIntent);
                            }

                            @Override
                            public void onError(final ImportedFileValidationException exception) {
                                callback.onError(exception);
                            }

                            @Override
                            public void onCancelled() {
                                callback.onCancelled();
                            }
                        });
        return new CancellationToken() {
            @Override
            public void cancel() {
                cancellationToken.cancel();
            }
        };
    }

    CancellationToken createDocumentForImportedFiles(@NonNull final Intent intent,
            @NonNull final Context context,
            @NonNull final AsyncCallback<Document, ImportedFileValidationException> callback) {
        if (!GiniCapture.hasInstance()) {
            callback.onError(createNoGiniCaptureFileValidationException());
            return new NoOpCancellationToken();
        }
        final List<Uri> uris = IntentHelper.getUris(intent);
        if (uris == null) {
            callback.onError(
                    new ImportedFileValidationException("Intent data did not contain Uris"));
            return new NoOpCancellationToken();
        }
        if (uris.size() == 1 && UriHelper.hasMimeType(uris.get(0), context,
                MimeType.APPLICATION_PDF.asString())) {
            try {
                final Document document = createDocumentForImportedFile(intent,
                        context);
                callback.onSuccess(document);
            } catch (final ImportedFileValidationException e) {
                callback.onError(e);
            }
            return new NoOpCancellationToken();
        } else {
            final ImportImageFileUrisAsyncTask asyncTask = new ImportImageFileUrisAsyncTask(context,
                    intent,
                    mGiniCapture, Document.Source.newExternalSource(),
                    Document.ImportMethod.OPEN_WITH,
                    new AsyncCallback<ImageMultiPageDocument, ImportedFileValidationException>() {
                        @Override
                        public void onSuccess(final ImageMultiPageDocument result) {
                            if (!GiniCapture.hasInstance()) {
                                callback.onError(createNoGiniCaptureFileValidationException());
                                return;
                            }
                            GiniCapture.getInstance().internal()
                                    .getImageMultiPageDocumentMemoryStore()
                                    .setMultiPageDocument(result);
                            callback.onSuccess(result);
                        }

                        @Override
                        public void onError(final ImportedFileValidationException exception) {
                            callback.onError(exception);
                        }

                        @Override
                        public void onCancelled() {
                            callback.onCancelled();
                        }
                    });
            asyncTask.execute(uris.toArray(new Uri[uris.size()]));
            return new CancellationToken() {
                @Override
                public void cancel() {
                }
            };
        }
    }

    @NonNull
    private static ImportedFileValidationException createNoGiniCaptureFileValidationException() {
        return new ImportedFileValidationException(
                "Cannot import files. GiniCapture instance not available. Create it with GiniCapture.newInstance().");
    }

}
