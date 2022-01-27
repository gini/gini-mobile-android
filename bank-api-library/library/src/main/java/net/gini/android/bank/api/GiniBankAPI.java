package net.gini.android.bank.api;

import net.gini.android.core.api.DocumentTaskManager;
import net.gini.android.core.api.authorization.CredentialsStore;
import net.gini.android.core.api.internal.GiniCoreAPI;

public class GiniBankAPI extends GiniCoreAPI<BankApiDocumentTaskManager, BankApiDocumentManager> {

    protected GiniBankAPI(final BankApiDocumentTaskManager documentTaskManager, final CredentialsStore credentialsStore) {
        super(documentTaskManager, credentialsStore);
    }

    @Override
    public BankApiDocumentManager getDocumentManager() {
        return new BankApiDocumentManager(getDocumentTaskManager());
    }
}
