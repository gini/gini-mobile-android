package net.gini.android.health.api;

import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.internal.GiniCoreAPI;

public class GiniHealthAPI extends GiniCoreAPI {

    protected GiniHealthAPI(final DocumentTaskManager documentTaskManager, final CredentialsStore credentialsStore) {
        super(documentTaskManager, credentialsStore);
    }
}
