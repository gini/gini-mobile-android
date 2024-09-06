Google Play Data Safety Guide
=============================

..
  Headers:
  h1 =====
  h2 -----
  h3 ~~~~~
  h4 +++++
  h5 ^^^^^

Since `July 20, 2022 <https://support.google.com/googleplay/android-developer/answer/10787469?hl=en>`_, Google requires
all apps published via the Google Play Store to declare how their app collects and handles user data. This document
provides information about the Gini Bank API Library's data use to help you fill out or update your app's `Data safety
form <https://developer.android.com/privacy-and-security/declare-data-use>`_ for the Google Play Store.

Data collection and security
----------------------------

For the question **“Does your app collect or share any of the required user data types?”** please answer **Yes**. Gini
Bank API Library collects information about purchases and transactions.

For the questions **“Is all of the user data collected by your app encrypted in transit?“** please answer **Yes.** Gini
Bank API Library transmits data only via https and in addition you can also use certificate pinning.

The answer to the question **“Do you provide a way for users to request that their data is deleted?“** depends on
whether you provide your users a way to request data deletion. If you do, then you can contact Gini support or use the
Gini Pay API to delete user data.

Data types
----------

.. list-table::
   :header-rows: 1

   * - Category
     - Used by Gini Bank API Library
   * - Location
     - 🔴 Gini Bank API Library doesn't collect approximate or precise location data.
   * - Personal info
     - 🔴 Gini Bank API Library doesn't collect personal user data.
   * - Financial info
     - 🟢 Gini Bank API Library collects **purchase history** (purchases and transactions).
   * - Health and fitness
     - 🔴 Gini Bank API Library doesn't collect health or fitness data.
   * - Messages
     - 🔴 Gini Bank API Library doesn't collect data about emails, SMS/MMS messages, or other in-app messages from users.
   * - Photos and videos
     - 🟢 Gini Bank API Library collects **photos** which you can upload for the purpose of data extraction.
   * - Audio files
     - 🔴 Audio files
   * - Files and docs
     - 🟢 Gini Bank API Library collects **PDF files** which you can upload for the purpose of data extraction. 
   * - Calendar
     - 🔴 Gini Bank API Library doesn't collect calendar events from users.
   * - Contacts
     - 🔴 Gini Bank API Library doesn't collect contacts from users.
   * - App activity
     - 🔴 Gini Bank API Library doesn't collect app activity.
   * - Web browsing
     - 🔴 Gini Bank API Library doesn't collect web browsing history from users.
   * - App info and performance
     - 🔴 Gini Bank API Library doesn't collect crash logs or diagnostics.
   * - Device or other IDs
     - 🟢 Gini Bank API Library collects **anonymous user IDs** which are generated random IDs and unique to app installations.

Data usage and handling
-----------------------

The following sections provide information for filling out the “Data usage and handling” forms. For ease of use the
terms and phrases which are used in the forms are highlighted in bold letters.

Financial info - Purchase history
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The Gini Bank API Library **collects** data related to purchases for a limited duration. This duration is 28 days unless
a different period has been agreed upon during contract negotiations. This means that data is **not processed
ephemerally**. In addition **users can't choose whether this data is collected**.

The **purpose** for collecting purchase history is to provide SDK functionality (**app functionality**) and for
**analytics**.

This data is only used internally and **is not shared with third-parties**.

Photos and videos
~~~~~~~~~~~~~~~~~

The Gini Bank API Library **collects photos** for a limited duration. This duration is 28 days unless a different period
has been agreed upon during contract negotiations. This means that data is **not processed ephemerally**. In addition
**users can't choose whether this data is collected**.

The **purpose** for collecting photos is to provide SDK functionality (**app functionality**) and for **analytics**.

This data is only used internally and **is not shared with third-parties**.

Files and docs
~~~~~~~~~~~~~~

The Gini Bank API Library **collects PDF files** for a limited duration. This duration is 28 days unless a different
period has been agreed upon during contract negotiations. This means that data is **not processed ephemerally**. In
addition **users can't choose whether this data is collected**.

The **purpose** for collecting PDF files is to provide SDK functionality (**app functionality**) and for **analytics**.

This data is only used internally and **is not shared with third-parties**.

Device or other IDs
~~~~~~~~~~~~~~~~~~~

The Gini Bank API Library **collects** anonymous user IDs which are generated random IDs and unique to app
installations. This data is **not processed ephemerally** and **users can't choose whether this data is collected**.

The reason for collecting anonymous user IDs is to provide SDK functionality (**app functionality**) and for **analytics** to monitor service quality.

This data is only used internally and **is not shared with third-parties**.