package net.gini.android;

import net.gini.android.authorization.CredentialsStore;

public class Gini {
    private final DocumentTaskManager mDocumentTaskManager;
    private final CredentialsStore mCredentialsStore;

    protected Gini(final DocumentTaskManager documentTaskManager, final CredentialsStore credentialsStore) {
        mDocumentTaskManager = documentTaskManager;
        mCredentialsStore = credentialsStore;
    }

    /**
     * Get the instance of the DocumentTaskManager. The DocumentTaskManager provides high level methods to handle
     * document related tasks easily.
     */
    public DocumentTaskManager getDocumentTaskManager() {
        return mDocumentTaskManager;
    }

    /**
     * Get the instance of the DocumentManager. The DocumentTaskManager provides high level methods to handle
     * document related tasks easily.
     *
     * Provides same functionality as {@link DocumentTaskManager} as suspend functions instead of {@link bolts.Task}
     */
    public DocumentManager getDocumentManager() {
        return new DocumentManager(mDocumentTaskManager);
    }

    /**
     * Get the instance of the CredentialsStore implementation which is used to store user information. Handy to get
     * information on the "anonymous" user.
     */
    public CredentialsStore getCredentialsStore() {
        return mCredentialsStore;
    }
}
