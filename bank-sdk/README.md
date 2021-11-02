![Gini Bank SDK for Android](logo.png)

Gini Bank SDK for Android
=========================

The Gini Bank SDK for Android provides all the UI and functionality needed to use the Gini Bank API in your app to
extract payment information from invoices and to resolve payment requests originating from other apps.

The Gini Bank API provides an information extraction service for analyzing invoices. Specifically it extracts information
such as the document sender or the payment relevant information (amount to pay, IBAN, etc.). In addition it also
provides a secure channel for sharing payment related information between clients.

Documentation
-------------
* [Integration Guide](https://developer.gini.net/gini-mobile-android/bank-sdk/sdk/html/)
* [Reference Docs](http://developer.gini.net/gini-mobile-android/bank-sdk/sdk/dokka/index.html)

Examples
--------

## Invoice Capture

We are providing example apps for the Screen API and the Component API. These apps demonstrate how
to integrate the Gini Pay Bank SDK's invoice capture feature:

You can find the example apps in the following modules:

- `screen-api-example-app` for the Screen API
- `component-api-example-app` for the Component API

## Payment Requests

You can find an example on how to handle payment requests in the `screen-api-example-app` module's `PayActivity`. 

For testing the payment flow integration you can use the example app from the [Health
SDK](https://github.com/gini/gini-mobile-android/tree/main/health-sdk#example-apps) to create payment requests.
 
## License

Gini Bank SDK is available under a commercial license.
See the LICENSE file for more info.
