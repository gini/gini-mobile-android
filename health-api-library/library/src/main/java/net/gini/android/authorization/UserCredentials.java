package net.gini.android.authorization;


/**
 * Value object for storing user credentials.
 */
public class UserCredentials {

    private final String mUsername;
    private final String mPassword;

    public UserCredentials(final String username, final String password) {
        mUsername = username;
        mPassword = password;
    }

    public String getUsername() {
        return mUsername;
    }

    public String getPassword() {
        return mPassword;
    }
}
