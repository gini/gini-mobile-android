Authentication
==============

The entry point for the Gini Merchant SDK is ``GiniMerchant`` class which depends
on ``GiniHealthAPI`` from the Gini Health API Library to interact with the Gini Health API.

The ``GiniHealthAPI`` class can be built either with client credentials (clientId and clientSecret)
or with a ``SessionManager`` if you have a token. For these two cases there are helper methods:

- ``getGiniApi(context: Context, clientId: String, clientSecret: String, emailDomain: String)``
- ``getGiniApi(context: Context, sessionManager: SessionManager)``

``SessionManager`` is an interface which you need to implement to send the token.

For more details about ``GiniHealthAPI`` see the `Gini Health API Library
<https://github.com/gini/gini-mobile-android/tree/main/health-api-library/>`_.