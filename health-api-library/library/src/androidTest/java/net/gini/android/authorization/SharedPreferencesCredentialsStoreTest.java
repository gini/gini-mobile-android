package net.gini.android.authorization;

import static android.content.Context.MODE_PRIVATE;
import static android.content.SharedPreferences.Editor;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.SharedPreferences;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SharedPreferencesCredentialsStoreTest {

    private SharedPreferencesCredentialsStore mCredentialsStore;
    private SharedPreferences mSharedPreferences;

    @Before
    public void setUp() {
        mSharedPreferences = getApplicationContext().getSharedPreferences("GiniTests", MODE_PRIVATE);
        // Clear preferences from previous tests
        final Editor preferencesEditor = mSharedPreferences.edit();
        preferencesEditor.clear();
        preferencesEditor.commit();

        mCredentialsStore = new SharedPreferencesCredentialsStore(mSharedPreferences);
    }

    @Test
    public void testConstructionThrowsNullPointerExceptionForNullArgument() {
        try {
            new SharedPreferencesCredentialsStore(null);
            fail("NullPointerException not raised");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    public void testStoreCredentialsStoresCredentials() {
        final UserCredentials userCredentials = new UserCredentials("foo@example.com", "1234");

        assertTrue(mCredentialsStore.storeUserCredentials(userCredentials));

        assertEquals("foo@example.com",
                mSharedPreferences.getString(SharedPreferencesCredentialsStore.USERNAME_KEY, null));
        assertEquals("1234",
                mSharedPreferences.getString(SharedPreferencesCredentialsStore.PASSWORD_KEY, null));
    }

    @Test
    public void testGetCredentialsReturnsNullIfNoCredentialsAreStored() {
        assertNull(mCredentialsStore.getUserCredentials());
    }

    @Test
    public void testGetCredentialsReturnsUserCredentials() {
        final UserCredentials storedUserCredentials = new UserCredentials("foo@example.com", "1234");
        mCredentialsStore.storeUserCredentials(storedUserCredentials);

        final UserCredentials userCredentials = mCredentialsStore.getUserCredentials();
        assertEquals("foo@example.com", userCredentials.getUsername());
        assertEquals("1234", userCredentials.getPassword());
    }

    @Test
    public void testDeleteCredentials() {
        mCredentialsStore.storeUserCredentials(new UserCredentials("foo@example.com", "1234"));

        assertTrue(mCredentialsStore.deleteUserCredentials());

        assertNull(mCredentialsStore.getUserCredentials());
    }
}
