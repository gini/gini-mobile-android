Getting started
===============

Installation
-------------

Add our sdk to your dependencies:

build.gradle:

.. code-block:: groovy

    dependencies {
        implementation 'net.gini.android:gini-health-sdk:5.5.0'
    }

Gini Pay Deep Link For Your App
-------------------------------

In order for banking apps to be able to return the user to your app after the payment has been resolved you can
register one of your activities to respond to a deep link scheme known by the Gini Health API.

You should already have a scheme and host from us. Please contact us in case you don't have them.

The following is an example for the deep link ``ginipay-business://payment-requester``:

.. code-block:: xml

    <activity android:name=".YourActivity">
        <intent-filter>
            <action android:name="android.intent.action.VIEW" />

            <category android:name="android.intent.category.DEFAULT" />
            
            <data
                android:host="payment-requester" 
                android:scheme="ginipay-business" />
        </intent-filter>
    </activity>

Gini Health API Client Credentials
-------------------------------

You should have received Gini Health API client credentials from us. Please get in touch with us in case you don't have them.

Continue to `Authentication <authentication.html>`_ to see how to use the client credentials to initialize the Gini Health SDK.