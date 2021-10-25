package net.gini.android.health.api;

import android.content.Context;

import net.gini.android.core.api.GiniApiType;
import net.gini.android.core.api.authorization.SessionManager;
import net.gini.android.core.api.internal.GiniCoreAPI;
import net.gini.android.core.api.internal.GiniCoreAPIBuilder;
import androidx.annotation.NonNull;

public class GiniHealthAPIBuilder extends GiniCoreAPIBuilder {

    /**
     * Constructor to initialize a new builder instance where anonymous Gini users are used. <b>This requires access to
     * the Gini User Center API. Access to the User Center API is restricted to selected clients only.</b>
     *
     * @param context      Your application's Context instance (Android).
     * @param clientId     Your application's client ID for the Gini API.
     * @param clientSecret Your application's client secret for the Gini API.
     * @param emailDomain  The email domain which is used for created Gini users.
     */
    public GiniHealthAPIBuilder(@NonNull final Context context, @NonNull final String clientId,
                                @NonNull final String clientSecret, @NonNull final String emailDomain) {
        super(context, clientId, clientSecret, emailDomain);
        setGiniApiType(GiniApiType.HEALTH);
    }

    /**
     * Constructor to initialize a new builder instance. The created Gini instance will use the given
     * {@link SessionManager} for session management.
     *
     * @param context        Your application's Context instance (Android).
     * @param sessionManager The SessionManager to use.
     */
    public GiniHealthAPIBuilder(@NonNull final Context context, @NonNull final SessionManager sessionManager) {
        super(context, sessionManager);
        setGiniApiType(GiniApiType.HEALTH);
    }

    /**
     * Builds the GiniHealthAPI instance with the configuration settings of the builder instance.
     *
     * @return The fully configured Gini instance.
     */
    public GiniHealthAPI build() {
        return new GiniHealthAPI(getDocumentTaskManager(), getCredentialsStore());
    }

}

