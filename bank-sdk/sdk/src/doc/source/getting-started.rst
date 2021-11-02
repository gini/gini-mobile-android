Getting started
===============

Requirements
------------

* Android 5.0+ (API Level 21+)
* Gini Capture SDK's `requirements <https://developer.gini.net/gini-capture-sdk-android/html/getting-started.html#requirements>`_.

The Gini Pay Bank SDK uses our `Gini Capture SDK <https://github.com/gini/gini-capture-sdk-android>`_ to capture
invoices with the camera or by importing them from the device or other apps.

Installation
------------

The Gini Pay Bank SDK is available in our maven repository which you need to add to your ``build.gradle`` first:

.. code:: groovy

    repositories {
        maven {
            url 'https://repo.gini.net/nexus/content/repositories/open'
        }
    }

Now you can add the Gini Pay Bank SDK to your app's dependencies:

.. code:: groovy

    dependencies {
        implementation 'net.gini:gini-pay-bank-sdk:1.3.1'
    }

After syncing Gradle you can start integrating the SDK.
