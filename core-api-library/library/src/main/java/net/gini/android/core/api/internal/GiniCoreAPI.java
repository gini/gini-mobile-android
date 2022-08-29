package net.gini.android.core.api.internal;

import net.gini.android.core.api.ApiCommunicator;
import net.gini.android.core.api.DocumentManager;
import net.gini.android.core.api.DocumentRepository;
import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.models.ExtractionsContainer;

public abstract class GiniCoreAPI<DM extends DocumentManager<DR, E>, DR extends DocumentRepository, E extends ExtractionsContainer> {
    private final DM mDocumentManager;
    private final CredentialsStore mCredentialsStore;

    protected GiniCoreAPI(final DM documentManager, final CredentialsStore credentialsStore) {
        mDocumentManager = documentManager;
        mCredentialsStore = credentialsStore;
    }

    /**
     * Get the instance of the DocumentTaskManager. The DocumentTaskManager provides high level methods to handle
     * document related tasks easily.
     */
    public DM getDocumentkManager() {
        return mDocumentManager;
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
