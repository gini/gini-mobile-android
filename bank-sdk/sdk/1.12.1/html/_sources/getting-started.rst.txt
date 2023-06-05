Getting started
===============

Requirements
------------

* Android 5.0+ (API Level 21+)
* Gini Capture SDK's `requirements <https://developer.gini.net/gini-mobile-android/capture-sdk/sdk/html/getting-started.html#requirements>`_.

The Gini Bank SDK uses our `Gini Capture SDK <https://github.com/gini/gini-mobile-android/tree/main/capture-sdk>`_ to capture
invoices with the camera or by importing them from the device or other apps.

Installation
------------

Add the Gini Bank SDK to your app's dependencies:

build.gradle:

.. code-block:: groovy

    dependencies {
        implementation 'net.gini.android:gini-bank-sdk:1.12.1'
    }

After syncing Gradle you can start integrating the SDK.
