package net.gini.android.capture.example.shared;

import android.util.Log;

import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.models.ExtractionsContainer;
import net.gini.android.core.api.models.SpecificExtraction;
import net.gini.android.capture.Document;
import net.gini.android.capture.internal.camera.api.UIExecutor;

import java.util.Map;

import bolts.Continuation;
import bolts.Task;

/**
 * Created by Alpar Szotyori on 29.11.2017.
 *
 * Copyright (c) 2017 Gini GmbH.
 */

public class DocumentAnalyzer {

    private final DocumentTaskManager mDocumentTaskManager;
    private final UIExecutor mUIExecutor = new UIExecutor();
    private boolean mCancelled = false;
    private net.gini.android.core.api.models.Document mGiniApiDocument;
    private Listener mListener;
    private Task<ExtractionsContainer> mResultTask;

    DocumentAnalyzer(final DocumentTaskManager documentTaskManager) {
        mDocumentTaskManager = documentTaskManager;
    }

    synchronized void analyze(final Document document) {
        mDocumentTaskManager
                .createDocument(document.getData(), null, null)
                .onSuccessTask(
                        new Continuation<net.gini.android.core.api.models.Document, Task<net.gini.android.core.api.models.Document>>() {
                            @Override
                            public Task<net.gini.android.core.api.models.Document> then(
                                    final Task<net.gini.android.core.api.models.Document> task)
                                    throws Exception {
                                final net.gini.android.core.api.models.Document giniDocument =
                                        task.getResult();
                                Log.d("gini-api", "Document created: " + giniDocument.getId());
                                if (isCancelled()) {
                                    Log.d("gini-api", "Analysis cancelled for document: "
                                            + giniDocument.getId());
                                    return Task.cancelled();
                                }
                                setGiniApiDocument(giniDocument);
                                Log.d("gini-api", "Polling document: " + giniDocument.getId());
                                return mDocumentTaskManager.pollDocument(getGiniApiDocument());
                            }
                        })
                .onSuccessTask(
                        new Continuation<net.gini.android.core.api.models.Document, Task<ExtractionsContainer>>() {
                            @Override
                            public Task<ExtractionsContainer> then(
                                    final Task<net.gini.android.core.api.models.Document> task)
                                    throws Exception {
                                final net.gini.android.core.api.models.Document giniDocument =
                                        task.getResult();
                                Log.d("gini-api", "Document polling done: " + giniDocument.getId());
                                if (isCancelled()) {
                                    Log.d("gini-api", "Analysis cancelled for document: "
                                            + giniDocument.getId());
                                    return Task.cancelled();
                                }
                                Log.d("gini-api", "Getting extractions for document: "
                                        + giniDocument.getId());
                                return mDocumentTaskManager.getAllExtractions(task.getResult());
                            }
                        })
                .continueWith(
                        new Continuation<ExtractionsContainer, Map<String, SpecificExtraction>>() {
                            @Override
                            public Map<String, SpecificExtraction> then(
                                    final Task<ExtractionsContainer> task)
                                    throws Exception {
                                if (isCancelled()) {
                                    Log.d("gini-api",
                                            "Analysis completed with cancellation for document: "
                                                    + getGiniApiDocument() != null
                                                    ? getGiniApiDocument().getId() : "null");
                                    return null;
                                }
                                Log.d("gini-api",
                                        String.format("Analysis completed with %s for document: %s",
                                                task.isFaulted() ? "fault" : "success",
                                                getGiniApiDocument() != null
                                                        ? getGiniApiDocument().getId() : "null"));
                                setResultTask(task);
                                publishResult();
                                return null;
                            }
                        });
    }

    private synchronized void publishResult() {
        mUIExecutor.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Task<ExtractionsContainer> resultTask = getResultTask();
                final Listener listener = getListener();
                if (resultTask == null || isCancelled() || listener == null) {
                    return;
                }
                Log.d("gini-api", "Publishing result");
                if (resultTask.isFaulted()) {
                    listener.onException(resultTask.getError());
                } else {
                    listener.onExtractionsReceived(resultTask.getResult().getSpecificExtractions());
                }
            }
        });
    }

    private synchronized Listener getListener() {
        return mListener;
    }

    synchronized void setListener(final Listener listener) {
        mListener = listener;
        publishResult();
    }

    private synchronized Task<ExtractionsContainer> getResultTask() {
        return mResultTask;
    }

    private synchronized void setResultTask(
            final Task<ExtractionsContainer> resultTask) {
        mResultTask = resultTask;
    }

    synchronized boolean isCancelled() {
        return mCancelled;
    }

    synchronized net.gini.android.core.api.models.Document getGiniApiDocument() {
        return mGiniApiDocument;
    }

    private synchronized void setGiniApiDocument(
            final net.gini.android.core.api.models.Document giniApiDocument) {
        mGiniApiDocument = giniApiDocument;
    }

    synchronized void cancel() {
        mCancelled = true;
        mListener = null;
    }

    synchronized boolean isCompleted() {
        return mResultTask != null;
    }

    public interface Listener {

        void onException(Exception exception);

        void onExtractionsReceived(Map<String, SpecificExtraction> extractions);
    }
}
