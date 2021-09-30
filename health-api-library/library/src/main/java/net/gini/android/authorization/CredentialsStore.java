package net.gini.android.authorization;


public interface CredentialsStore {

    /**
     * Store the given user credentials.
     *
     * Warning: This method overwrites existing user credentials.
     *
     * @param userCredentials   The user's credentials.
     * @return                  Whether the storing of the credentials was successful.
     */
    boolean storeUserCredentials(UserCredentials userCredentials);

    /**
     * Returns the stored user credentials.
     *
     * Warning: If there are no stored user credentials, this method returns null.
     *
     * @return                  The stored user credentials.
     */
    UserCredentials getUserCredentials();

    /**
     * Deletes the stored user credentials.
     *
     * @return                  Whether the deleting of the credentials was successful.
     */
    boolean deleteUserCredentials();
}
