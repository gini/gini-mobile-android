Authentication
==============

The entry point for the Gini Merchant SDK is the ``GiniMerchant`` class.

The ``GiniMerchant`` class can be built either with client credentials (clientId and clientSecret)
or with a ``SessionManager`` if you have a token:

- ``GiniMerchant(context: Context, clientId: String, clientSecret: String, emailDomain: String)``
- ``GiniMerchant(context: Context, sessionManager: SessionManager)``

``SessionManager`` is an interface which you need to implement to send the token.