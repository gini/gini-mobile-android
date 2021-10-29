Getting started
===============

Requirements
------------

In addition to the minimum Android version we also have hardware requirements to ensure the best possible analysis
results:

* Android 5.0+ (API Level 21+)

* Phone Hardware

   * Back-facing camera with auto-focus and flash.
   * Minimum 8MP camera resolution.
   * Minimum 512MB RAM.
* Tablet Hardware

   * Back-facing camera with auto-focus.
   * Minimum 8MP camera resolution.
   * Minimum 512MB RAM.

Installation
------------

The Gini Capture SDK is available in our maven repository which you need to add to your ``build.gradle`` first:

.. code:: groovy

    repositories {
        maven {
            url 'https://repo.gini.net/nexus/content/repositories/open'
        }
    }

Now you can add the Gini Capture SDK to your app's dependencies:

.. code:: groovy

    dependencies {
        implementation 'net.gini:gini-capture-sdk:1.4.1'
    }

After syncing Gradle you can start integrating the Gini Capture SDK.