Migrate to 3.0.0
================

..
  Audience: Android dev who has integrated 1.0.0
  Purpose: Describe what is new in 3.0.0 and how to migrate from 2.0.0 to 3.0.0
  Content type: Procedural - How-To

  Headers:
  h1 =====
  h2 -----
  h3 ~~~~~
  h4 +++++
  h5 ^^^^^

In version 3.0.0 we modernised our UI and added support for light and dark modes. In addition we simplified how the UI
is customised. We also unified the public API of the SDK and introduced an easier way to customise certain parts of the
UI.

Migrate the Invoice Capture Feature
-----------------------------------

The capture feature uses our `Gini Capture SDK <https://github.com/gini/gini-mobile-android/tree/main/capture-sdk>`_ and
its `migration guide <https://developer.gini.net/gini-mobile-android/capture-sdk/sdk/html/migrate-to-2-0-0.html>`_
describes what has changed.

Please be aware that for the Gini Bank SDK you need to use the ``CaptureConfiguration`` instead of the Gini Capture SDK's
``GiniCapture.Builder``. The configuration names are the same so you can easily map them to the
``CaptureConfiguration``. This applies also to the new configuration options added in 3.0.0.


Migrate the Return Assistant
-----------------------------------

Onboarding
~~~~~~~~~~

The new onboarding screen uses the global UI customization options. You can discard the old screen specific
customizations.

`Here <customization-guide.html#onboarding-screen>`_ you can find the detailed description on how to customize this screen.

Breaking Changes
++++++++++++++++

Old UI is replaced with new UI.

New Features
++++++++++++

Bottom navigation bar
^^^^^^^^^^^^^^^^^^^^^

You can show a bottom navigation bar by passing true to ``GiniCapture`` ``setBottomNavigationBarEnabled``. There is a default implementation, but you can also use
your own by implementing the ``DigitalInvoiceOnboardingNavigationBarBottomAdapter`` interface and passing it to ``GiniBank``.
