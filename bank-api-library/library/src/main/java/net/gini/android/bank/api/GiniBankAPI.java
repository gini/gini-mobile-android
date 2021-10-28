package net.gini.android.bank.api;

import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.internal.GiniCoreAPI;

public class GiniBankAPI extends GiniCoreAPI {

    protected GiniBankAPI(final DocumentTaskManager documentTaskManager, final CredentialsStore credentialsStore) {
        super(documentTaskManager, credentialsStore);
    }
}
