package net.gini.android.health.api;

import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.internal.GiniCoreAPI;
import net.gini.android.core.api.models.ExtractionsContainer;

public class GiniHealthAPI extends GiniCoreAPI<HealthApiDocumentTaskManager, HealthApiDocumentManager, HealthApiCommunicator, ExtractionsContainer> {

    protected GiniHealthAPI(HealthApiDocumentTaskManager documentTaskManager, CredentialsStore credentialsStore) {
        super(documentTaskManager, credentialsStore);
    }

    @Override
    public HealthApiDocumentManager getDocumentManager() {
        return new HealthApiDocumentManager(getDocumentTaskManager());
    }
}
