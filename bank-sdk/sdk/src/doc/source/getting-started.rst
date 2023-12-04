Getting started
===============

Requirements
------------

* Android 5.0+ (API Level 21+)
* Gini Capture SDK's :root_html_path_capture_sdk:`requirements <getting-started.html#requirements>`.

The Gini Bank SDK uses our `Gini Capture SDK <https://github.com/gini/gini-mobile-android/tree/main/capture-sdk>`_ to capture
invoices with the camera or by importing them from the device or other apps.

Installation
------------

Add the Gini Bank SDK to your app's dependencies:

build.gradle:

.. code-block:: groovy

    dependencies {
        implementation 'net.gini.android:gini-bank-sdk:3.8.1'
    }

After syncing Gradle you can start integrating the SDK.
