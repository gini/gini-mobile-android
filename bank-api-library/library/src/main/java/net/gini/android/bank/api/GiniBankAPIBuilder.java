package net.gini.android.bank.api;

import android.content.Context;

import androidx.annotation.NonNull;

import net.gini.android.bank.api.models.ExtractionsContainer;
import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.authorization.SessionManager;
import net.gini.android.core.api.internal.GiniCoreAPIBuilder;

public class GiniBankAPIBuilder extends GiniCoreAPIBuilder<BankApiDocumentTaskManager, BankApiDocumentManager,GiniBankAPI, BankApiCommunicator, ExtractionsContainer> {

    private final GiniApiType bankApiType = new GiniBankApiType(1);

    /**
     * Constructor to initialize a new builder instance where anonymous Gini users are used. <b>This requires access to
     * the Gini User Center API. Access to the User Center API is restricted to selected clients only.</b>
     *
     * @param context      Your application's Context instance (Android).
     * @param clientId     Your application's client ID for the Gini API.
     * @param clientSecret Your application's client secret for the Gini API.
     * @param emailDomain  The email domain which is used for created Gini users.
     */
    public GiniBankAPIBuilder(@NonNull final Context context, @NonNull final String clientId,
                                @NonNull final String clientSecret, @NonNull final String emailDomain) {
        super(context, clientId, clientSecret, emailDomain);
    }

    /**
     * Constructor to initialize a new builder instance. The created Gini instance will use the given
     * {@link SessionManager} for session management.
     *
     * @param context        Your application's Context instance (Android).
     * @param sessionManager The SessionManager to use.
     */
    public GiniBankAPIBuilder(@NonNull final Context context, @NonNull final SessionManager sessionManager) {
        super(context, sessionManager);
    }

    @NonNull
    @Override
    public GiniApiType getGiniApiType() {
        return bankApiType;
    }

    /**
     * Builds the GiniBankAPI instance with the configuration settings of the builder instance.
     *
     * @return The fully configured GiniBankAPI instance.
     */
    @Override
    public GiniBankAPI build() {
        return new GiniBankAPI(getDocumentTaskManager(), getCredentialsStore());
    }

    @Override
    protected BankApiCommunicator createApiCommunicator() {
        return new BankApiCommunicator(getApiBaseUrl(), getGiniApiType(), getRequestQueue(), getRetryPolicyFactory());
    }

    @Override
    protected BankApiDocumentTaskManager createDocumentTaskManager() {
        return new BankApiDocumentTaskManager(getApiCommunicator(), getSessionManager(), getGiniApiType(), getMoshi());
    }

}

