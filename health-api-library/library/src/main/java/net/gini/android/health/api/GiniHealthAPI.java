package net.gini.android.health.api;

import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.internal.GiniCoreAPI;

public class GiniHealthAPI extends GiniCoreAPI<HealthApiDocumentTaskManager, HealthApiDocumentManager> {

    protected GiniHealthAPI(HealthApiDocumentTaskManager documentTaskManager, CredentialsStore credentialsStore) {
        super(documentTaskManager, credentialsStore);
    }

    @Override
    public HealthApiDocumentManager getDocumentManager() {
        return new HealthApiDocumentManager(getDocumentTaskManager());
    }
}
