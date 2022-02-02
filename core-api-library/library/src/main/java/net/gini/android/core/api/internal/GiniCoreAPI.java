package net.gini.android.core.api.internal;

import net.gini.android.core.api.ApiCommunicator;
import net.gini.android.core.api.DocumentManager;
import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.models.ExtractionsContainer;

public abstract class GiniCoreAPI<DTM extends DocumentTaskManager<A, E>, DM extends DocumentManager<A, DTM, E>, A extends ApiCommunicator, E extends ExtractionsContainer> {
    private final DTM mDocumentTaskManager;
    private final CredentialsStore mCredentialsStore;

    protected GiniCoreAPI(final DTM documentTaskManager, final CredentialsStore credentialsStore) {
        mDocumentTaskManager = documentTaskManager;
        mCredentialsStore = credentialsStore;
    }

    /**
     * Get the instance of the DocumentTaskManager. The DocumentTaskManager provides high level methods to handle
     * document related tasks easily.
     */
    public DTM getDocumentTaskManager() {
        return mDocumentTaskManager;
    }

    /**
     * Get the instance of the DocumentManager. The DocumentTaskManager provides high level methods to handle
     * document related tasks easily.
     *
     * Provides same functionality as {@link DocumentTaskManager} as suspend functions instead of {@link bolts.Task}
     */
    public abstract DM getDocumentManager();

    /**
     * Get the instance of the CredentialsStore implementation which is used to store user information. Handy to get
     * information on the "anonymous" user.
     */
    public CredentialsStore getCredentialsStore() {
        return mCredentialsStore;
    }
}
