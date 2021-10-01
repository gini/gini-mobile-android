Authentication
==============

The entry point for the Gini Health SDK is ``GiniBusiness`` class which depends
on ``Gini`` (Gini Pay API lib) to interact with the backend.

The ``Gini`` class can be built either with client credentials (clientId and clientSecret)
or with a ``SessionManager`` if you have a token. For these two cases there are helper methods:
 - ``getGiniApi(context: Context, clientId: String, clientSecret: String, emailDomain: String)``
 - ``getGiniApi(context: Context, sessionManager: SessionManager)``

``SessionManager`` is an interface which you need to implement to send the token.

For more details about ``Gini`` see `Gini Pay API lib <https://github.com/gini/gini-pay-api-lib-android/>`_.