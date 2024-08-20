![Gini Merchant SDK for Android](./logo.png)

Gini Merchant SDK for Android
=============================

The Gini Merchant SDK for Android provides all the UI and functionality needed to use the Gini Pay Connect payment
method in Android apps. The payment information can be reviewed and paid using any available payment provider app (e.g.,
banking app).

The Gini Merchant API provides a secure channel for sharing payment related information between clients.

Documentation
-------------

* [Integration Guide](https://developer.gini.net/gini-mobile-android/merchant-sdk/sdk)

Example Apps
------------

### Merchant Example App

You can see a sample usage of the Gini Merchant SDK in the `:merchant-sdk:example-app` module. 

It requires Gini Merchant API credentials which are injected automatically if you create this file `merchant-sdk/example-app/local.properties` with the following properties:
```
clientId=*******
clientSecret=*******
```

### Bank Example App

An example bank app is available in the [Gini Bank SDK](https://github.com/gini/gini-mobile-android/tree/main/bank-sdk) called
[`example-app`](https://github.com/gini/gini-mobile-android/tree/main/bank-sdk/example-app).

License
-------

The Gini Merchant SDK for Android is available under a commercial license.
See the LICENSE file for more info.
