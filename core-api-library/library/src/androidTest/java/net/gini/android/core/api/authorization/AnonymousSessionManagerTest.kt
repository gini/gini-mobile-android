package net.gini.android.core.api.authorization

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import io.mockk.*
import kotlinx.coroutines.test.runTest
import net.gini.android.core.api.Resource
import net.gini.android.core.api.authorization.apimodels.SessionToken
import net.gini.android.core.api.authorization.apimodels.UserRequestModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@MediumTest
@RunWith(AndroidJUnit4::class)
class AnonymousSessionManagerTest {

    private var mAnonymousSessionSessionManager: AnonymousSessionManager? = null
    private var mUserRepository: UserRepository? = null
    private var mCredentialsStore: CredentialsStore? = null
    private var mEmailDomain: String? = null

    @Before
    fun setUp() {
        // https://code.google.com/p/dexmaker/issues/detail?id=2
        System.setProperty("dexmaker.dexcache", ApplicationProvider.getApplicationContext<Context>().cacheDir.path)
        mUserRepository = mockk()
        mCredentialsStore = mockk()
        mEmailDomain = "example.com"
        mAnonymousSessionSessionManager = AnonymousSessionManager(mUserRepository!!, mCredentialsStore!!, mEmailDomain!!)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testGetSessionShouldResolveToSession() = runTest {
        val userCredentials = UserCredentials(email("foobar"), "1234")
        every { mCredentialsStore?.userCredentials } returns (userCredentials)
        coEvery {mUserRepository?.loginUser(any()) } returns Resource.Success(
            SessionToken(accessToken = UUID.randomUUID().toString(), tokenType = "bearer", expiresIn = 30000)
        )

        val sessionToken = mAnonymousSessionSessionManager?.getSession()
        assertNotNull(sessionToken?.data)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testLoginUserShouldResolveToSession() = runTest {
        val userCredentials = UserCredentials(email("foobar"), "1234")
        every { mCredentialsStore?.userCredentials } returns (userCredentials)
        coEvery {mUserRepository?.loginUser(UserRequestModel(userCredentials.username, userCredentials.password)) } returns Resource.Success(
            SessionToken(accessToken = UUID.randomUUID().toString(), tokenType = "bearer", expiresIn = 30000)
        )

        val sessionToken = mAnonymousSessionSessionManager?.loginUser()
        assertNotNull(sessionToken?.data)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testThatNewUserCredentialsAreStored() = runTest {
        every { mCredentialsStore?.userCredentials } returns (null)
        coEvery { mUserRepository?.createUser(any()) } returns Resource.Success(Unit)

        every { mCredentialsStore?.storeUserCredentials(any()) } returns (true)

        mAnonymousSessionSessionManager?.getSession()
        verify { mCredentialsStore?.storeUserCredentials(any()) }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testThatStoredUserCredentialsAreUsed() = runTest {
        val userCredentials = UserCredentials(email("foobar"), "1234")
        every { mCredentialsStore!!.userCredentials } returns (userCredentials)

        val sessionToken = SessionToken(accessToken = "1234-5678-9012", tokenType = "bearer", expiresIn = 30000)
        coEvery {mUserRepository?.loginUser(UserRequestModel(userCredentials.username, userCredentials.password)) } returns Resource.Success(
            sessionToken
        )

        val session = mAnonymousSessionSessionManager?.getSession()?.data
        assertSame(sessionToken, session)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testThatUserSessionsAreReused() = runTest {
        every { mCredentialsStore!!.userCredentials } returns (UserCredentials(email("foobar"), "1234"))
        coEvery { mUserRepository?.loginUser(ofType(UserRequestModel::class)) } returnsMany listOf(Resource.Success(
            SessionToken(accessToken = UUID.randomUUID().toString(), tokenType = "bearer", expiresIn = 10)
        ), Resource.Success(
            SessionToken(accessToken = UUID.randomUUID().toString(), tokenType = "bearer", expiresIn = 0)
        ))

        val firstSession = mAnonymousSessionSessionManager?.getSession()?.data
        val secondSession = mAnonymousSessionSessionManager?.getSession()?.data
        assertSame(firstSession, secondSession)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testThatUserSessionsAreNotReusedWhenTimedOut() = runTest {
        every { mCredentialsStore!!.userCredentials } returns (UserCredentials(email("foobar"), "1234"))
        coEvery {mUserRepository?.loginUser(ofType(UserRequestModel::class)) } returnsMany listOf(Resource.Success(
            SessionToken(accessToken = UUID.randomUUID().toString(), tokenType = "bearer", expiresIn = -10)
        ), Resource.Success(
            SessionToken(accessToken = UUID.randomUUID().toString(), tokenType = "bearer", expiresIn = 0)
        ))

        val firstSession = mAnonymousSessionSessionManager?.getSession()?.data
        assertTrue(firstSession?.hasExpired() ?: false)

        val secondSession = mAnonymousSessionSessionManager?.getSession()?.data

        assertNotSame(firstSession, secondSession)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testThatCreatedUserNamesAreEmailAddresses() = runTest {
        every { mCredentialsStore!!.userCredentials } returns (null)
        coEvery { mUserRepository?.createUser(ofType(UserRequestModel::class)) } returns Resource.Success(Unit)

        every { mCredentialsStore?.storeUserCredentials(any()) } returns (true)
        mAnonymousSessionSessionManager?.getSession()

        val userRequestModelSlot = slot<UserRequestModel>()
        coVerify { mUserRepository?.createUser(capture(userRequestModelSlot)) }

        assertTrue(userRequestModelSlot.captured.email?.endsWith("@$mEmailDomain") ?: false)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testThatExistingUserIsDeletedAndNewUserIsCreatedIfExistingIsInvalid() = runTest {
        every { mCredentialsStore!!.userCredentials } returns (UserCredentials(email("foobar"), "1234"))
        val invalidGrantErrorJson = "{\"error\": \"invalid_grant\"}"
        coEvery {mUserRepository?.loginUser(any()) } returns Resource.Error(responseStatusCode = 400, responseBody = invalidGrantErrorJson)
//        null, null, null, 400, null, invalidGrantErrorJson

        coEvery { mUserRepository?.createUser(ofType(UserRequestModel::class)) } returns Resource.Success(Unit)

        every { mCredentialsStore?.deleteUserCredentials() } returns (true)
        every { mCredentialsStore?.storeUserCredentials(any()) } returns (true)
        val session = mAnonymousSessionSessionManager?.getSession()?.data

        verify { mCredentialsStore?.deleteUserCredentials() }
        coVerify { mUserRepository?.createUser(ofType(UserRequestModel::class)) }
    }


    @Test
    @Throws(InterruptedException::class)
    fun testThatExistingUserIsDeletedAndNewUserIsCreatedIfExistingIsUnauthorized() = runTest {
        every { mCredentialsStore!!.userCredentials } returns (UserCredentials(email("foobar"), "1234"))
        val invalidGrantErrorJson = "{\"error\": \"Speak, friend, and enter.\"}"
        coEvery {mUserRepository?.loginUser(any()) } returns Resource.Error(responseStatusCode = 401, responseBody = invalidGrantErrorJson)

        coEvery { mUserRepository?.createUser(ofType(UserRequestModel::class)) } returns Resource.Success(Unit)
        every { mCredentialsStore?.deleteUserCredentials() } returns (true)
        every { mCredentialsStore?.storeUserCredentials(any()) } returns (true)

       mAnonymousSessionSessionManager?.getSession()

        verify { mCredentialsStore?.deleteUserCredentials() }
        coVerify { mUserRepository?.createUser(ofType(UserRequestModel::class)) }
    }

    @Test
    @Throws(InterruptedException::class)
    fun testThatCreateUserErrorIsReturnedWhenNewUserIsCreatedIfExistingIsInvalid() = runTest {
        every { mCredentialsStore!!.userCredentials } returns (null)
        val invalidGrantErrorJson = "{\"error\": \"invalid_grant\"}"
        coEvery {mUserRepository?.createUser(any()) } returns Resource.Error(
            responseStatusCode = 503, responseHeaders = mapOf("Some-Header" to listOf("10")), responseBody = invalidGrantErrorJson
        )

        val session = mAnonymousSessionSessionManager?.getSession()

        assertTrue("Task should have faulted", session is Resource.Error)
        assertEquals(503, session?.responseStatusCode)
        val headerValue = session?.responseHeaders?.getValue("Some-Header")

        assertNotNull("Task error should contain response header 'Some-Header'", headerValue)
        assertEquals("10", headerValue?.get(0) ?: 0)
    }

    @Test
    fun testHasUserCredentialsEmailDomainReturnsTrueIfUsernameEmailDomainIsSameAsEmailDomain() {
        val userCredentials = UserCredentials("1234@example.com", "1234")
        assertTrue(mAnonymousSessionSessionManager!!.hasUserCredentialsEmailDomain("example.com", userCredentials))
    }

    @Test
    fun testHasUserCredentialsEmailDomainReturnsFalseIfUsernameEmailDomainIsNotSameAsEmailDomain() {
        val userCredentials = UserCredentials("1234@example.com", "1234")
        assertFalse(mAnonymousSessionSessionManager!!.hasUserCredentialsEmailDomain("beispiel.com", userCredentials))
    }

    @Test
    fun testHasUserCredentialsEmailDomainReturnsFalseIfUsernameEmailDomainContainsEmailDomain() {
        val userCredentials = UserCredentials("1234@exampledomain.com", "1234")
        assertFalse(mAnonymousSessionSessionManager!!.hasUserCredentialsEmailDomain("domain.com", userCredentials))
    }

    @Test
    fun testThatLoginUserUpdatesEmailDomainIfChanged() = runTest{
        val newEmailDomain = "beispiel.com"
        val oldEmailDomain = "example.com"
        mAnonymousSessionSessionManager = AnonymousSessionManager(mUserRepository!!, mCredentialsStore!!, newEmailDomain)

        every { mCredentialsStore!!.userCredentials } returns (UserCredentials("1234@$oldEmailDomain", "5678"))
        coEvery {mUserRepository?.updateEmail(any(), any(), any()) } returns Resource.Success(Unit)
        coEvery {mUserRepository?.loginUser(ofType(UserRequestModel::class)) } returnsMany listOf(Resource.Success(
            SessionToken(accessToken = "1234-5678-9012", tokenType = "bearer", expiresIn = 30000)
        ), Resource.Success(
            SessionToken(accessToken = "1234-5678-9012", tokenType = "bearer", expiresIn = 30000)
        ))

        every { mCredentialsStore?.deleteUserCredentials() } returns (true)
        every { mCredentialsStore?.storeUserCredentials(any()) } returns (true)

        val session = mAnonymousSessionSessionManager?.loginUser()

        assertTrue(session is Resource.Success)
        verify { mCredentialsStore?.deleteUserCredentials() }

        val userCredentialsSlot = slot<UserCredentials>()
        verify { mCredentialsStore?.storeUserCredentials(capture(userCredentialsSlot)) }

        val newUserCredentials = userCredentialsSlot.captured
        assertEquals(newEmailDomain, extractEmailDomain(newUserCredentials.username))
        assertEquals("5678", newUserCredentials.password)
    }

    private fun email(name: String): String {
        return "$name@$mEmailDomain"
    }

    private fun extractEmailDomain(email: String): String? {
        val components = email.split("@").toTypedArray()
        return if (components.size > 1) {
            components[1]
        } else ""
    }

}