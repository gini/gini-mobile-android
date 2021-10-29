# Module sdk

## Gini Capture SDK for Android

The Gini Capture SDK for Android provides Activities and Fragments for capturing, reviewing and analyzing photos, images and pdfs of
invoices and remittance slips.

By integrating this library in your application you can allow your users to easily take a picture of a document, review it and - by
implementing the necessary callbacks - upload the document to the Gini API for analysis.

Communication with the Gini API is not part of the Gini Capture SDK to allow clients the freedom of using a networking implementation of
choosing. The quickest way to add networking is to use the Default Network Library. You may also use the Gini Bank API Library for Android
or implement communication with the Gini Bank API yourself.

The Gini Capture SDK for Android can be integrated in two ways, either by using the Screen API or the Component API. The Screen API
provides Activities for easy integration that can be customized in a limited way. The screen and configuration design is based on our
long-lasting experience with integration in customer apps. In the Component API we provide Fragments for advanced integration with more
freedom for customization. We strongly recommend keeping in mind our UI/UX guidelines, however.

Customization of the Views is provided mostly via overriding of app resources: dimensions, strings, colors, texts, etc. Onboarding can also
be customized to show your own pages, each consisting of an image and a short text.

Due to in-memory image handling applications using the Gini Capture SDK must enable large heap.

### Tablet Support

The Gini Capture SDK can be used on tablets too. We have adapted some UI elements to offer a better experience to tablet users and
removed the camera flash requirement for tablets since flash is not present on all tablets. For more information please consult our guide
[Supporting Tablets](http://developer.gini.net/gini-mobile-android/capture-sdk/sdk/html/updating-to-2-4-0.html#tablet-support).

### Requirements

* Screen API: Android 4.4+ (API Level 19+)
* Component API: Android 4.4+ (API Level 19+)

#### Phone Hardware

* Back-facing camera with auto-focus and flash
* Minimum 8MP camera resolution.
* Minimum 512MB RAM.

#### Tablet Hardware

* Back-facing camera with auto-focus.
* Minimum 8MP camera resolution.
* Minimum 512MB RAM.

### Screen API

The Screen API provides a configuration singleton and a main Activity with which to start the Gini Capture SDK. 

In order to support the widest variety of Android versions while keeping the look and feel consistent, we use the AndroidX libraries
and provide only Activities subclassing the AppCompatActivity.

### Component API

The Component API provides Fragments which you can include in your own layouts. This allows you more freedom to customize the Gini
Capture SDK, without being restricted to AppCompatActivities and the Gini Capture SDK Theme.
