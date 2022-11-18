Updating to 2.0.0
=================

..
  Audience: Android dev who has integrated 1.0.0
  Purpose: Describe what is new in 2.0.0 and how to migrate from 1.0.0 to 2.0.0
  Content type: Procedural - How-To

  Headers:
  h1 =====
  h2 -----
  h3 ~~~~~
  h4 +++++
  h5 ^^^^^

In version 2.0.0 we updated the default networking implementation to use Gini Bank API Library version 2.0.0 which
removed dependency on Bolts and Volley. Bolts was replaced with kotlin coroutines which is part of the kotlin standard
library. Volley was replaced with Retrofit2, a more popular and elegant networking library built upon okhttp3.

Default Networking Implementation
---------------------------------

The breaking changes affect only the default networking implementation which is distributed via the
``net.gini.android:gini-capture-sdk-default-network`` maven package.

GiniCaptureDefaultNetworkService.Builder
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ``GiniCaptureDefaultNetworkService.Builder`` was ported to kotlin and the following methods were changed, removed or added: 

- ``setCache()`` now takes an ``okhttp3.Cache`` instead of ``com.android.volley.Cache``. Please consult the `okhttp3
  documentation <https://square.github.io/okhttp/features/caching/>`_ on how to customize caching.
- ``setConnectionBackOffMultiplier()`` was removed as it's not available in Retrofit2 and okhttp3.
- ``setMaxNumberOfRetries()`` was removed as it's not available in Retrofit2 and okhttp3.
- ``setDebuggingEnabled()`` was added to allow enabling debugging mode which will log all requests and responses. Make
  sure to disable debugging for release builds!

SessionManager
~~~~~~~~~~~~~~

The ``SessionManager`` interface was changed to declare the ``getSession()`` method as a suspend function with a return
type of ``Resource<Session>``. You can find more details about the ``Resource`` class in the Gini Bank API Library's
`documentation <https://developer.gini.net/gini-mobile-android/bank-api-library/library/>`_.
