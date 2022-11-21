Migrate to 2.0.0
================

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

In version 2.0.0 we updated the Gini Health API Library to version 2.0.0 which removed dependency on Bolts and Volley.
Bolts was replaced with kotlin coroutines which is part of the kotlin standard library. Volley was replaced with
Retrofit2, a more popular and elegant networking library built upon okhttp3.

The breaking changes affect only the Gini Health API Library. If you are interacting with it directly, then please consult
the Gini Health API Library's `migration guide <https://developer.gini.net/gini-mobile-android/health-api-library/library/>`_
for details.
