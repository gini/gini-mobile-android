package net.gini.android.authorization;


import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private final String mUserId;
    private final String mUsername;

    User(final String userId, final String username) {
        this.mUserId = userId;
        this.mUsername = username;
    }

    public String getUserId() {
        return mUserId;
    }

    public String getUsername() {
        return mUsername;
    }

    public static User fromApiResponse(JSONObject apiResponse) throws JSONException {
        return new User(apiResponse.getString("id"), apiResponse.getString("email"));
    }
}
