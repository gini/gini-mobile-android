package net.gini.android.core.api.authorization;


import net.gini.android.core.api.authorization.apimodels.SessionToken;
import java.util.Date;


/**
 * The session is the value object for the session of a user.
 */
public class Session {
    final String mAccessToken;
    final Date mExpirationDate;

    public Session(final String accessToken, final Date expirationDate) {
        mAccessToken = accessToken;
        mExpirationDate = new Date(expirationDate.getTime());
    }

    /** The session's access token. */
    public String getAccessToken() {
        return mAccessToken;
    }

    /** The expiration date of the access token. */
    public Date getExpirationDate() {
        return mExpirationDate;
    }

    /**
     * Uses the current locale's time to check whether or not this session has already expired.
     *
     * @return Whether or not the session has already expired.
     */
    public boolean hasExpired() {
        Date now = new Date();
        return now.after(mExpirationDate);
    }

    public static Session fromAPIResponse(final SessionToken apiResponse) {
        final String accessToken = apiResponse.getAccessToken();
        final Date now = new Date();
        final long expirationTime = now.getTime() + apiResponse.getExpiresIn() * 1000;
        return new Session(accessToken, new Date(expirationTime));
    }
}
