package net.gini.android.authorization;


import org.json.JSONException;
import org.json.JSONObject;

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

    // TODO: exception encapsulation instead of simply throwing JSONException
    public static Session fromAPIResponse(final JSONObject apiResponse) throws JSONException {
        final String accessToken = apiResponse.getString("access_token");
        final Date now = new Date();
        final long expirationTime = now.getTime() + apiResponse.getInt("expires_in") * 1000;
        return new Session(accessToken, new Date(expirationTime));
    }
}
