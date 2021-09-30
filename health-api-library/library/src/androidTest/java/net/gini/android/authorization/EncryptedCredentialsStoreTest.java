package net.gini.android.authorization;

import static android.content.Context.MODE_PRIVATE;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.gini.android.authorization.crypto.GiniCryptoHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by Alpar Szotyori on 08.10.2018.
 *
 * Copyright (c) 2018 Gini GmbH.
 */
@RunWith(AndroidJUnit4.class)
public class EncryptedCredentialsStoreTest {

    private SharedPreferences mSharedPreferences;
    private EncryptedCredentialsStore mCredentialsStore;

    @Before
    public void setUp() throws Exception {
        mSharedPreferences = getApplicationContext().getSharedPreferences("GiniTests", MODE_PRIVATE);
        mSharedPreferences.edit().clear().commit();

        mCredentialsStore = new EncryptedCredentialsStore(mSharedPreferences, getApplicationContext());
    }

    @After
    public void tearDown() throws Exception {
        mSharedPreferences.edit().clear().commit();
    }

    @Test
    public void testEncryptsExistingPlaintextCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentialsWithoutEncryption(userCredentials);
        // When
        mCredentialsStore.encryptExistingPlaintextCredentials();
        // Then
        final UserCredentials encryptedUserCredentials =
                mCredentialsStore.getEncryptedUserCredentials();
        assertTrue(!userCredentials.getUsername().equals(encryptedUserCredentials.getUsername()));
        assertTrue(!userCredentials.getPassword().equals(encryptedUserCredentials.getPassword()));
    }

    @Test
    public void testDecryptsEncryptedExistingPlaintextCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentialsWithoutEncryption(userCredentials);
        // When
        mCredentialsStore.encryptExistingPlaintextCredentials();
        // Then
        final UserCredentials decryptedUserCredentials = mCredentialsStore.getUserCredentials();
        assertEquals(userCredentials.getUsername(), decryptedUserCredentials.getUsername());
        assertEquals(userCredentials.getPassword(), decryptedUserCredentials.getPassword());
    }

    @Test
    public void testDoesNotEncryptAlreadyEncryptedCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        final UserCredentials encryptedUserCredentialsBefore =
                mCredentialsStore.getEncryptedUserCredentials();
        // When
        mCredentialsStore.encryptExistingPlaintextCredentials();
        // Then
        final UserCredentials encryptedUserCredentialsAfter =
                mCredentialsStore.getEncryptedUserCredentials();
        assertEquals(encryptedUserCredentialsBefore.getUsername(),
                encryptedUserCredentialsAfter.getUsername());
        assertEquals(encryptedUserCredentialsBefore.getPassword(),
                encryptedUserCredentialsAfter.getPassword());
    }

    @Test
    public void testEncryptsCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // Then
        final UserCredentials encryptedUserCredentials =
                mCredentialsStore.getEncryptedUserCredentials();
        assertTrue(!userCredentials.getUsername().equals(encryptedUserCredentials.getUsername()));
        assertTrue(!userCredentials.getPassword().equals(encryptedUserCredentials.getPassword()));
    }

    @Test
    public void testDecryptsCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // When
        final UserCredentials decryptedUserCredentials = mCredentialsStore.getUserCredentials();
        // Then
        assertEquals(userCredentials.getUsername(), decryptedUserCredentials.getUsername());
        assertEquals(userCredentials.getPassword(), decryptedUserCredentials.getPassword());
    }

    @Test
    public void testDeleteCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // When
        mCredentialsStore.deleteUserCredentials();
        // Then
        assertNull(mCredentialsStore.getUserCredentials());
    }

    @Test
    public void testSetsEncryptionVersion() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // Then
        final int encryptionVersion = mCredentialsStore.getEncryptionVersion();
        assertEquals(encryptionVersion, EncryptedCredentialsStore.ENCRYPTION_VERSION);
    }

    @Test
    public void testEncryptionIsDifferentForSameCredentials() {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        final UserCredentials encryptedCredentials1 = mCredentialsStore.getEncryptedUserCredentials();
        // When
        mCredentialsStore.storeUserCredentials(userCredentials);
        final UserCredentials encryptedCredentials2 = mCredentialsStore.getEncryptedUserCredentials();
        // Then
        assertTrue(!encryptedCredentials1.getUsername().equals(encryptedCredentials2.getUsername()));
        assertTrue(!encryptedCredentials1.getPassword().equals(encryptedCredentials2.getPassword()));
    }

    @Test
    public void testReturnsNullCredentialsIfTheEncryptionKeyChanged() throws Exception {
        // Given
        final UserCredentials userCredentials = new UserCredentials("testuser@gini.net",
                "12345678");
        mCredentialsStore.storeUserCredentials(userCredentials);
        // When
        GiniCryptoHelper.deleteSecretKey(mCredentialsStore.getGiniCrypto());
        // Then
        final UserCredentials encryptedUserCredentials = mCredentialsStore.getUserCredentials();
        assertNull(encryptedUserCredentials);
    }
}