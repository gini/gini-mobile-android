package net.gini.android.capture.analysis;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import android.app.Application;

import net.gini.android.capture.GiniCapture;
import net.gini.android.capture.GiniCaptureHelper;
import net.gini.android.capture.document.GiniCaptureDocument;
import net.gini.android.capture.document.GiniCaptureDocumentError;
import net.gini.android.capture.document.GiniCaptureMultiPageDocument;
import net.gini.android.capture.internal.network.AnalysisNetworkRequestResult;
import net.gini.android.capture.internal.network.NetworkRequestResult;
import net.gini.android.capture.internal.network.NetworkRequestsManager;
import net.gini.android.capture.network.AnalysisResult;
import net.gini.android.capture.network.model.GiniCaptureSpecificExtraction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import jersey.repackaged.jsr166e.CompletableFuture;

/**
 * Created by Alpar Szotyori on 14.05.2019.
 *
 * Copyright (c) 2019 Gini GmbH.
 */
public class AnalysisInteractorTest {

    @Mock
    private Application mApp;

    private AnalysisInteractor mAnalysisInteractor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mAnalysisInteractor = new AnalysisInteractor(mApp);
    }

    @After
    public void tearDown() throws Exception {
        GiniCaptureHelper.setGiniCaptureInstance(null);
    }

    @Test
    public void should_completeWithNoNetworkService_whenGiniCapture_isNotAvailable()
            throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);

        // When
        final CompletableFuture<AnalysisInteractor.ResultHolder> future =
                mAnalysisInteractor.analyzeMultiPageDocument(multiPageDocument);

        // Then
        assertThat(future.get().getResult()).isEqualTo(
                AnalysisInteractor.Result.NO_NETWORK_SERVICE);
    }

    @Test
    public void should_completeWithNoNetworkService_whenNetworkRequestsManager_isNotAvailable()
            throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);

        createGiniCapture(null);

        // When
        final CompletableFuture<AnalysisInteractor.ResultHolder> future =
                mAnalysisInteractor.analyzeMultiPageDocument(multiPageDocument);

        // Then
        assertThat(future.get().getResult()).isEqualTo(
                AnalysisInteractor.Result.NO_NETWORK_SERVICE);
    }

    private void createGiniCapture(
            @Nullable final NetworkRequestsManager networkRequestsManager) {
        final GiniCapture.Internal internal = mock(GiniCapture.Internal.class);
        when(internal.getNetworkRequestsManager()).thenReturn(networkRequestsManager);

        final GiniCapture giniCapture = mock(GiniCapture.class);
        when(giniCapture.internal()).thenReturn(internal);

        GiniCaptureHelper.setGiniCaptureInstance(giniCapture);
    }

    @Test
    public void should_uploadEveryPage_ofMultiPageDocument() throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);
        final List<GiniCaptureDocument> documents = new ArrayList<>();
        documents.add(mock(GiniCaptureDocument.class));
        documents.add(mock(GiniCaptureDocument.class));
        documents.add(mock(GiniCaptureDocument.class));
        when(multiPageDocument.getDocuments()).thenReturn(documents);

        final NetworkRequestsManager networkRequestsManager = mock(NetworkRequestsManager.class);

        final CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>>
                analysisResult = CompletableFuture.completedFuture(null);

        when(networkRequestsManager.analyze(any(GiniCaptureMultiPageDocument.class))).thenReturn(
                analysisResult);

        createGiniCapture(networkRequestsManager);

        // When
        mAnalysisInteractor.analyzeMultiPageDocument(multiPageDocument);

        // Then
        for (final GiniCaptureDocument document : documents) {
            verify(networkRequestsManager).upload(mApp, document);
        }
    }

    @Test
    public void should_completeWithException_whenAnalysisFailed_withoutCancellation()
            throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);

        final RuntimeException analysisException = new RuntimeException();

        final NetworkRequestsManager networkRequestsManager =
                createtNetworkRequestsManagerWithAnalysisException(analysisException);

        createGiniCapture(networkRequestsManager);

        // When
        final CompletableFuture<AnalysisInteractor.ResultHolder> future =
                mAnalysisInteractor.analyzeMultiPageDocument(multiPageDocument);

        // Then
        Throwable exception = null;
        try {
            future.get();
        } catch (final ExecutionException e) {
            // Double wrapped: by CompletableFuture and by the handler in analyzeMultiPageDocument()
            exception = e.getCause().getCause();
        }

        assertThat(exception).isEqualTo(analysisException);
    }

    @NonNull
    private NetworkRequestsManager createtNetworkRequestsManagerWithAnalysisException(
            final RuntimeException analysisException) {
        final CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>>
                analysisResult = new CompletableFuture<>();
        analysisResult.completeExceptionally(analysisException);

        final NetworkRequestsManager networkRequestsManager = mock(NetworkRequestsManager.class);
        when(networkRequestsManager.analyze(any(GiniCaptureMultiPageDocument.class))).thenReturn(
                analysisResult);
        return networkRequestsManager;
    }

    @Test
    public void should_notCompleteWithException_whenAnalysisFailed_dueToCancellation()
            throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);

        final RuntimeException analysisException = new CancellationException();

        final NetworkRequestsManager networkRequestsManager =
                createtNetworkRequestsManagerWithAnalysisException(analysisException);

        createGiniCapture(networkRequestsManager);

        // When
        final CompletableFuture<AnalysisInteractor.ResultHolder> future =
                mAnalysisInteractor.analyzeMultiPageDocument(multiPageDocument);

        // Then
        Throwable exception = null;
        try {
            future.get();
        } catch (final ExecutionException e) {
            exception = e;
        }

        assertThat(exception).isNull();
    }

    @Test
    public void should_completeWithNoExtractions_whenAnalysisSucceeded_withoutExtractions()
            throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);

        final AnalysisResult analysisResult = new AnalysisResult("apiDocumentId",
                Collections.<String, GiniCaptureSpecificExtraction>emptyMap());

        final NetworkRequestsManager networkRequestsManager = createtNetworkRequestsManager(
                analysisResult);

        createGiniCapture(networkRequestsManager);

        // When
        final CompletableFuture<AnalysisInteractor.ResultHolder> future =
                mAnalysisInteractor.analyzeMultiPageDocument(multiPageDocument);

        // Then
        final AnalysisInteractor.ResultHolder resultHolder = future.get();

        assertThat(resultHolder.getResult()).isEqualTo(
                AnalysisInteractor.Result.SUCCESS_NO_EXTRACTIONS);
    }

    private NetworkRequestsManager createtNetworkRequestsManager(
            @NonNull final AnalysisResult analysisResult) {
        //noinspection unchecked
        final AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>
                analysisNetworkRequestResult = mock(AnalysisNetworkRequestResult.class);
        when(analysisNetworkRequestResult.getAnalysisResult()).thenReturn(analysisResult);

        final CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>>
                analysisResultFuture = new CompletableFuture<>();
        analysisResultFuture.complete(analysisNetworkRequestResult);

        final NetworkRequestsManager networkRequestsManager = mock(NetworkRequestsManager.class);
        when(networkRequestsManager.analyze(any(GiniCaptureMultiPageDocument.class))).thenReturn(
                analysisResultFuture);
        return networkRequestsManager;
    }

    @Test
    public void should_completeWithExtractions_whenAnalysisSucceeded_withExtractions()
            throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);

        final Map<String, GiniCaptureSpecificExtraction> extractions = new HashMap<>();
        extractions.put("amountToPay", mock(GiniCaptureSpecificExtraction.class));
        extractions.put("paymentRecipient", mock(GiniCaptureSpecificExtraction.class));
        extractions.put("iban", mock(GiniCaptureSpecificExtraction.class));
        extractions.put("paymentReference", mock(GiniCaptureSpecificExtraction.class));

        final AnalysisResult analysisResult = new AnalysisResult("apiDocumentId",
                extractions);

        final NetworkRequestsManager networkRequestsManager = createtNetworkRequestsManager(
                analysisResult);

        createGiniCapture(networkRequestsManager);

        // When
        final CompletableFuture<AnalysisInteractor.ResultHolder> future =
                mAnalysisInteractor.analyzeMultiPageDocument(multiPageDocument);

        // Then
        final AnalysisInteractor.ResultHolder resultHolder = future.get();

        assertThat(resultHolder.getResult()).isEqualTo(
                AnalysisInteractor.Result.SUCCESS_WITH_EXTRACTIONS);
        assertThat(resultHolder.getExtractions()).isEqualTo(extractions);
    }

    @Test
    public void should_completeWithoutResult_whenAnalysisSucceeded_withoutAnyResult()
            throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);

        final CompletableFuture<AnalysisNetworkRequestResult<GiniCaptureMultiPageDocument>>
                analysisResultFuture = new CompletableFuture<>();
        analysisResultFuture.complete(null);

        final NetworkRequestsManager networkRequestsManager = mock(NetworkRequestsManager.class);
        when(networkRequestsManager.analyze(any(GiniCaptureMultiPageDocument.class))).thenReturn(
                analysisResultFuture);

        createGiniCapture(networkRequestsManager);

        // When
        final CompletableFuture<AnalysisInteractor.ResultHolder> future =
                mAnalysisInteractor.analyzeMultiPageDocument(multiPageDocument);

        // Then
        final AnalysisInteractor.ResultHolder resultHolder = future.get();

        assertThat(resultHolder).isNull();
    }

    @Test
    public void should_notDeleteMultiPageDocument_whenNetworkRequestsManager_isNotAvailable()
            throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);

        createGiniCapture(null);

        // When
        final CompletableFuture<Void> future =
                mAnalysisInteractor.deleteMultiPageDocument(multiPageDocument);

        // Then
        assertThat(future.get()).isNull();
    }

    @Test
    public void should_deleteMultiPageDocument() throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);

        //noinspection unchecked
        final NetworkRequestResult<GiniCaptureDocument> deletionRequestResult = mock(
                NetworkRequestResult.class);

        final NetworkRequestsManager networkRequestsManager = createtNetworkRequestsManager(
                deletionRequestResult);

        createGiniCapture(networkRequestsManager);

        // When
        mAnalysisInteractor.deleteMultiPageDocument(multiPageDocument);

        // Then
        verify(networkRequestsManager).cancel(multiPageDocument);
        verify(networkRequestsManager).delete(multiPageDocument);
    }

    private NetworkRequestsManager createtNetworkRequestsManager(
            final NetworkRequestResult<GiniCaptureDocument> deletionRequestResult) {
        final CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>
                deletionResultFuture = new CompletableFuture<>();
        deletionResultFuture.complete(deletionRequestResult);

        final NetworkRequestsManager networkRequestsManager = mock(NetworkRequestsManager.class);
        when(networkRequestsManager.delete(any(GiniCaptureDocument.class))).thenReturn(
                deletionResultFuture);
        return networkRequestsManager;
    }

    @Test
    public void should_deleteEveryPage_ofMultiPageDocument() throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);
        final List<GiniCaptureDocument> documents = new ArrayList<>();
        documents.add(mock(GiniCaptureDocument.class));
        documents.add(mock(GiniCaptureDocument.class));
        documents.add(mock(GiniCaptureDocument.class));
        when(multiPageDocument.getDocuments()).thenReturn(documents);

        //noinspection unchecked
        final NetworkRequestResult<GiniCaptureDocument> deletionRequestResult = mock(
                NetworkRequestResult.class);

        final NetworkRequestsManager networkRequestsManager = createtNetworkRequestsManager(
                deletionRequestResult);

        createGiniCapture(networkRequestsManager);

        // When
        mAnalysisInteractor.deleteMultiPageDocument(multiPageDocument);

        // Then
        for (final GiniCaptureDocument document : documents) {
            verify(networkRequestsManager).cancel(document);
            verify(networkRequestsManager).delete(document);
        }
    }

    @Test
    public void should_deleteEverypPage_ofMultiPageDocument_whenDeletion_ofMultiPageDocumentFailes()
            throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureMultiPageDocument<GiniCaptureDocument, GiniCaptureDocumentError>
                multiPageDocument = mock(GiniCaptureMultiPageDocument.class);
        final List<GiniCaptureDocument> documents = new ArrayList<>();
        documents.add(mock(GiniCaptureDocument.class));
        documents.add(mock(GiniCaptureDocument.class));
        documents.add(mock(GiniCaptureDocument.class));
        when(multiPageDocument.getDocuments()).thenReturn(documents);

        final NetworkRequestsManager networkRequestsManager =
                createtNetworkRequestsManagerWithDeletionException(new RuntimeException());

        createGiniCapture(networkRequestsManager);

        // When
        mAnalysisInteractor.deleteMultiPageDocument(multiPageDocument);

        // Then
        verify(networkRequestsManager).cancel(multiPageDocument);
        verify(networkRequestsManager).delete(multiPageDocument);

        for (final GiniCaptureDocument document : documents) {
            verify(networkRequestsManager).cancel(document);
            verify(networkRequestsManager).delete(document);
        }
    }

    private NetworkRequestsManager createtNetworkRequestsManagerWithDeletionException(
            @NonNull final RuntimeException deletionException) {
        final CompletableFuture<NetworkRequestResult<GiniCaptureDocument>>
                deletionResultFuture = new CompletableFuture<>();
        deletionResultFuture.completeExceptionally(deletionException);

        final NetworkRequestsManager networkRequestsManager = mock(NetworkRequestsManager.class);
        when(networkRequestsManager.delete(any(GiniCaptureMultiPageDocument.class))).thenReturn(
                deletionResultFuture);
        return networkRequestsManager;
    }

    @Test
    public void should_notDeleteDocument_whenGiniCapture_isNotAvailable() throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureDocument document = mock(GiniCaptureDocument.class);

        // When
        final  CompletableFuture<NetworkRequestResult<GiniCaptureDocument>> future = mAnalysisInteractor.deleteDocument(
                document);

        // Then
        assertThat(future.get()).isNull();
    }

    @Test
    public void should_notDeleteDocument_whenNetworkRequestsManager_isNotAvailable() throws Exception {
        // Given
        //noinspection unchecked
        final GiniCaptureDocument document = mock(GiniCaptureDocument.class);

        createGiniCapture(null);

        // When
        final  CompletableFuture<NetworkRequestResult<GiniCaptureDocument>> future = mAnalysisInteractor.deleteDocument(
                document);

        // Then
        assertThat(future.get()).isNull();
    }

    @Test
    public void should_deleteDocument() throws Exception {
        // Given
        final GiniCaptureDocument document = mock(GiniCaptureDocument.class);

        //noinspection unchecked
        final NetworkRequestResult<GiniCaptureDocument> deletionRequestResult = mock(
                NetworkRequestResult.class);

        final NetworkRequestsManager networkRequestsManager = createtNetworkRequestsManager(
                deletionRequestResult);

        createGiniCapture(networkRequestsManager);

        // When
        mAnalysisInteractor.deleteDocument(document);

        // Then
        verify(networkRequestsManager).cancel(document);
        verify(networkRequestsManager).delete(document);
    }
}