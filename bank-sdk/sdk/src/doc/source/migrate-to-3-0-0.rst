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

The capture feature is provided by the :root_html_path_capture_sdk:`Gini Capture SDK <index.html>`. In the following we
will often refer to resources provided by the Gini Capture SDK.

Migrate from Component API
--------------------------

The Component API allowed more UI customization options at the cost of a more difficult integration and maintenance. It
was based on fragments, and you had to manage navigation between them and also update the navigation whenever we introduced
breaking changes.

Maintaining the Component API along with the simpler Screen API required an increasing amount of effort as we added new
features. We decided therefore to unify both APIs and introduce the ability to inject fully custom UI elements.

The major benefit of the Component API was the ability to use a custom navigation bar (toolbar or action bar). Via
UI injection that is still possible with the new public API.

The following steps will help you migrate to the new public API:

* Configure the SDK the same way as before by using ``GiniBank``.
* If you used a custom navigation bar, then you can use the new ability to inject fully custom UI elements. For this you
  need to implement the ``NavigationBarTopAdapter`` interface and pass it to
  ``GiniBank.setCaptureConfiguration(CaptureConfiguration(navigationBarTopAdapter = ))``. The ``NavigationBarTopAdapter`` interface declares the
  contract your view has to fulfill and allows the SDK to ask for your view instance when needed.
* Use the ``CaptureFlowContract`` and ``GiniBank.startCaptureFlow()`` to launch the SDK instead of ``CameraFragmentCompat``.
* Handle the result of the ``CaptureFlowContract`` by implementing an ``ActivityResultCallback<CaptureResult>`` to
  receive the extracted information (or other results like errors or cancellation).
* Remove all code related to interacting with the SDK's fragments. From now on the entry point is
  ``GiniBank.startCaptureFlow()`` and customization happens through ``GiniBank`` and via overriding of resources.
* Use the new UI customization options and follow the :ref:`screen-by-screen UI customization section<Migrate to the new
  UI>` to adapt the look of the new UI.

Migrate from Screen API
-----------------------

The new public API is based on the Screen API, so you only need to use the new UI customization options and follow the
:ref:`screen-by-screen UI customization section<Migrate to the new UI>` to adapt the look of the new UI.

Migrate Cleanup Step and Feedback Sending
-----------------------------------------

We simplified the feedback sending logic. When you clean up the Gini Bank SDK you only need to pass the values the
user has used (and potentially corrected) to ``GiniBank.releaseCapture()``. All values except the one for the amount are
passed in as strings. Amount needs to be passed in as ``BigDecimal`` and its currency as an ``Enum`` value.

You don't have to call any additional methods to send the extraction feedback.

Default Networking Implementation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You don't need to interact with the ``GiniCaptureDefaultNetworkApi`` anymore. The ``GiniBank.releaseCapture()`` method
will take care of sending the feedback.

Custom Networking Implementation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You only need to implement the ``GiniCaptureNetworkService`` from now on. We removed the ``GiniCaptureNetworkApi`` and
moved the ``sendFeedback()`` method to the ``GiniCaptureNetworkService``. 

``GiniCaptureNetworkService.sendFeedback()`` will be called when you pass the values the user has used (and potentially
corrected) to ``GiniBank.releaseCapture()``.

Overview of New UI Customization Options
----------------------------------------

To simplify UI customization we introduced global customization options. There is no need to customize each screen
separately anymore.

Styles
~~~~~~

We leverage the power of Material Design to configure a theme for the SDK with a global color palette and typography
that is applied on all the screens. 

Using global styles for the various widgets, we enable you to customize them in a single place. They are then
consistently applied on all screens.

Theme
+++++

The theme style is based on Material Design v2 and is named ``GiniCaptureTheme``. To override the theme in your
application use ``Root.GiniCaptureTheme`` as the parent:

.. code-block:: xml

    <style name="GiniCaptureTheme" parent="Root.GiniCaptureTheme">
      (...)
    </style>

Widgets
+++++++

The style of buttons and other widgets is based on Material Design v3. To override them in your application use the
root style as the parent, for example:

.. code-block:: xml

    <style name="GiniCaptureTheme.Widget.Button.OutlinedButton" parent="Root.GiniCaptureTheme.Widget.Button.OutlinedButton">
      (...)
    </style>

Colors
~~~~~~

We introduced a global color palette which you are free to override. The custom colors will then be applied on all screens.

You can find the names of the color resources in the color palette `here <customization-guide.html#colors>`_.

.. note::

    If you have overridden the ``GiniCaptureTheme`` then the theme colors you have set there will override the color
    palette customization.

Images
~~~~~~

Customizing images is done the same way as before via overriding of drawable resources. You can find the drawable
resource names in the :ref:`screen-by-screen UI customization section<Migrate to the new UI>`.

We replaced most drawables with vector drawables. Unfortunately due to the limitations of vector drawables some images
had to be added as PNGs.

If you use vector drawables please add them to the `drawable-anydpi` folder so that they also override any density specific PNGs.

Typography
~~~~~~~~~~

We introduced a global typography based on text appearance styles from Material Design v3. To override them in your application use the
root style as the parent, for example:

.. code-block:: xml

    <style name="GiniCaptureTheme.Typography.Body1" parent="Root.GiniCaptureTheme.Typography.Body1">
        (...)
    </style>

.. note::

  If you have overridden the ``GiniCaptureTheme`` then the text appearances you have set there will override the
  typography customization. The same applies to overridden widget styles where you have set a custom text appearance.

You can find all the typography style names `here <customization-guide.html#typography>`_.

Text
~~~~

Text customization is done the same way as before via string resources.

UI Elements
~~~~~~~~~~~

Certain elements of the UI can now be fully customized via UI injection. This allowed us to drop the Component API while
still allowing in-depth customization for certain parts of the UI.

UI injection utilizes view adapter interfaces which you can implement and pass to ``GiniBank`` when configuring the
SDK. These interfaces declare the contract the injected view has to fulfill and allow the SDK to ask for your view
instance when needed.

The most important injectable UI element is the top navigation bar. You may also show the navigation bar on the bottom
using your own custom view. You can find more details `here <customization-guide.html#custom-ui-elements>`_.

Dark mode
~~~~~~~~~

To customize resource for dark mode add them to resource folders containing the ``-night`` resource qualifier.

Migrate to the new UI
---------------------

Onboarding Screen
~~~~~~~~~~~~~~~~~

The new onboarding screen uses the global UI customization options. You can discard the old screen specific
customizations.

Images and text are onboarding page specific and need to be customized for each page.

`Here <customization-guide.html#onboarding-screen>`_ you can find the detailed description on how to customize this screen.

Breaking Changes
++++++++++++++++

Setting Custom Onboarding Pages
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The ``OnboardingPage`` class was changed to also allow setting a title for the page and inject a view for the
illustration.

You can use the ``ImageOnboardingIllustrationAdapter`` to display drawable resources.

If you are setting custom onboarding pages, then you have to create the ``OnboardingPage`` as shown in the example
below:

.. code-block:: java

    val page1 = OnboardingPage(
        R.string.your_title_page_1,
        R.string.your_message_page_1,
        ImageOnboardingIllustrationAdapter(R.drawable.your_illustration_page_1)
    )
    val page2 = OnboardingPage(
        R.string.your_title_page_2,
        R.string.your_message_page_2,
        ImageOnboardingIllustrationAdapter(R.drawable.your_illustration_page_2)
    )

    GiniBank.setCaptureConfiguration(
        CaptureConfiguration(
            onboardingPages = arrayListOf(page1, page2)
        )
    )

New Features
++++++++++++

Custom Illustration Views
^^^^^^^^^^^^^^^^^^^^^^^^^

By implementing the ``OnboardingIllustrationAdapter`` interface and passing it to either ``GiniBank`` or the
``OnboardingPage`` constructor you can inject any custom view for the illustration.

For example if you need to show animated illustrations you can use a `Lottie
<https://github.com/airbnb/lottie-android>`_ view in your ``OnboardingIllustrationAdapter`` implementation.

You can find more details `here <customization-guide.html#custom-illustration-views>`_.

Bottom Navigation Bar
^^^^^^^^^^^^^^^^^^^^^

You can show a bottom navigation bar by passing ``true`` to
``GiniBank.setCaptureConfiguration(CaptureConfiguration(bottomNavigationBarEnabled = ))``. There is a default
implementation, but you can also use your own by implementing the ``OnboardingNavigationBarBottomAdapter`` interface and
passing it to ``GiniBank``.

You can find more details `here <customization-guide.html#id1>`_.

Camera Screen
~~~~~

The new camera screen uses the global UI customization options. You can discard the old screen specific
customizations.

`Here <customization-guide.html#camera-screen>`_ you can find the detailed description on how to customize this screen.

New Features
++++++++++++

We implemented image cropping. Parts of the image that appears outside the white camera frame will be cut out from the final image.

Bottom Navigation Bar
^^^^^^^^^^^^^^^^^^^^^

You can show a bottom navigation bar by passing ``true`` to ``GiniBank.setCaptureConfiguration(CaptureConfiguration(bottomNavigationBarEnabled = ))``. There is a default implementation, but you can also use
your own by implementing the ``CameraNavigationBarBottomAdapter`` interface and passing it to ``GiniBank``.

You can find more details `here <customization-guide.html#id3>`_.

Custom Loading Indicator View
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There is a default implementation of indicator which indicates that image is in the cropping process, but you can show your own activity indicator
by implementing the ``CustomLoadingIndicatorAdapter`` interface and passing it to ``GiniBank``.

You can find more details `here <customization-guide.html#custom-loading-indicator>`_.

Breaking Changes
++++++++++++++++

We removed the tooltip popups that were shown on first launch.

QR Code Scanner
~~~~~

The new UI for the QR code scanner uses the global UI customization options. You can discard the old screen specific
customizations.

In the `camera screen customisation guide <customization-guide.html#camera-screen>`_ you can find the detailed description on how to customize it.

Breaking Changes
++++++++++++++++

QR code scanning UI and functionality have changed. Scanning and processing happens automatically now.

New Features
++++++++++++

The SDK can be launched to only scan QR codes. To enable this feature simply pass ``true`` to ``GiniBank.setCaptureConfiguration(CaptureConfiguration(onlyQRCodeScanningEnabled = ))``.

Review Screen
~~~~~

The new review screen uses the global UI customization options. You can discard the old screen specific
customizations.

`Here <customization-guide.html#review-screen>`_ you can find the detailed description on how to customize this screen.

New Features
++++++++++++

Custom "Process" Button Loading Indicator
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There is a default implementation of loading indicator on the "Process" button that indicates document upload is in progress, but you can show your own indicator
by implementing the ``CustomLoadingIndicatorAdapter`` interface and passing it to ``GiniBank``.

You can find more details `here <customization-guide.html#custom-process-button-loading-indicator>`_.

Bottom Navigation Bar
^^^^^^^^^^^^^^^^^^^^^

You can show a bottom navigation bar by passing ``true`` to
``GiniBank.setCaptureConfiguration(CaptureConfiguration(bottomNavigationBarEnabled = ))``. There is a default
implementation, but you can also use your own by implementing the ``ReviewNavigationBarBottomAdapter`` interface and
passing it to ``GiniCapture``.

You can find more details `here <customization-guide.html#id5>`_.

Breaking Changes
++++++++++++++++

Re-ordering and rotation of the images are not supported anymore. The Gini API can automatically correct rotation during processing.
If processing of images fails, then the user is redirected to the error screen.

Help Screen
~~~~~

The new help screen uses the global UI customization options. You can discard the old screen specific
customizations.

`Here <customization-guide.html#help-screen>`_ you can find the detailed description on how to customize this screen.

New Features
++++++++++++

Bottom Navigation Bar
^^^^^^^^^^^^^^^^^^^^^

You can show a bottom navigation bar by passing ``true`` to
``GiniBank.setCaptureConfiguration(CaptureConfiguration(bottomNavigationBarEnabled = ))``. There is a default implementation, but you can also use
your own by implementing the ``HelpNavigationBarBottomAdapter`` interface and passing it to ``GiniBank``.

You can find more details `here <customization-guide.html#id9>`_.

Analysis Screen
~~~~~~~~

The new analysis screen uses the global UI customization options. You can discard the old screen specific
customizations.

`Here <customization-guide.html#analysis-screen>`_ you can find the detailed description on how to customize this screen.

Breaking Changes
++++++++++++++++

The new analysis screen does not show the page count of PDF files and preview image for photo documents.

New Features
++++++++++++

Custom Loading Indicator View
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

There is a default implementation for indicating that document analysis is in progress, but you can show your own activity indicator
by implementing the ``CustomLoadingIndicatorAdapter`` interface and passing it to ``GiniBank``.

You can find more details `here <customization-guide.html#id7>`_.

Error Screen
~~~~~

The new analysis screen uses the global UI customization options.

`Here <customization-guide.html#error-screen>`_ you can find the detailed description on how to customize this screen.

Breaking Changes
++++++++++++++++

Showing errors during usage of the SDK was changed from snackbar to a whole new screen.

New Features
++++++++++++

New UI
^^^^^^

The new error screen gives options to retake photos or enter details manually and displays errors with more detailed description.

Bottom Navigation Bar
^^^^^^^^^^^^^^^^^^^^^

You can show a bottom navigation bar by passing ``true`` to
``GiniBank.setCaptureConfiguration(CaptureConfiguration(bottomNavigationBarEnabled = ))``. There is a default implementation, but you can also use
your own by implementing the ``ErrorNavigationBarBottomAdapter`` interface and passing it to ``GiniBank``.

You can find more details `here <customization-guide.html#id13>`_.

Enter Details Manually Button
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Users can now click an "Enter manually" button on the error screen which will exit the SDK with ``CaptureResult.EnterManually``.

You can find more details `here <integration.html#capturing-documents>`_.

No Results Screen
~~~~~~~~~~

The new no results screen uses the global UI customization options. You can discard the old screen specific
customizations.

`Here <customization-guide.html#no-results-screen>`_ you can find the detailed description on how to customize this screen.

New Features
++++++++++++

New UI
^^^^^^

The new no results screen gives options to enter document details manually.

Bottom Navigation Bar
^^^^^^^^^^^^^^^^^^^^^

You can show a bottom navigation bar by passing ``true`` to
``GiniBank.setCaptureConfiguration(CaptureConfiguration(bottomNavigationBarEnabled = ))``. There is a default implementation, but you can also use
your own by implementing the ``NoResultsNavigationBarBottomAdapter`` interface and passing it to ``GiniBank``.

You can find more details `here <customization-guide.html#id11>`_.

Enter Details Manually Button
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Users can now click an "Enter manually" button on the no results screen which will exit the SDK with ``CaptureResult.EnterManually``.

You can find more details `here <integration.html#capturing-documents>`_.


.. @Adnan, @Lenci: Please uncomment everything below when adding return assistant migration info. 
.. To not confuse clients I temporarily commented it out until we have added all infos.

.. (TODO) Migrate the Return Assistant
.. ----------------------------

.. TODO: Review this section after all migration info has been added.

.. Onboarding
.. ~~~~~~~~~~

.. The new onboarding screen uses the global UI customization options. You can discard the old screen specific
.. customizations.

.. `Here <customization-guide.html#onboarding-screen>`_ you can find the detailed description on how to customize this screen.

.. Breaking Changes
.. ++++++++++++++++

.. Old UI is replaced with new UI.

.. New Features
.. ++++++++++++

.. Custom illustration view
.. ^^^^^^^^^^^^^^^^^^^^^^^^^

.. By implementing the ``OnboardingIllustrationAdapter`` interface and passing it to ``GiniBank.digitalInvoiceOnboardingIllustrationAdapter`` you can inject any custom view for the illustration.

.. For example if you need to show animated illustrations you can use a `Lottie
.. <https://github.com/airbnb/lottie-android>`_ view in your ``OnboardingIllustrationAdapter`` implementation.

.. You can find more details `here <customization-guide.html>`_.

.. Bottom navigation bar
.. ^^^^^^^^^^^^^^^^^^^^^

.. You can show a bottom navigation bar by passing true to ``GiniCapture`` ``setBottomNavigationBarEnabled``. There is a default implementation, but you can also use
.. your own by implementing the ``DigitalInvoiceOnboardingNavigationBarBottomAdapter`` interface and passing it to ``GiniBank``.

.. Digital Invoice Help
.. ~~~~~~~~~~~~~~~~~~~~

.. The new help screen for digital invoice uses the global UI customization options.

.. Features
.. ++++++++

.. Bottom navigation bar
.. ^^^^^^^^^^^^^^^^^^^^^

.. You can show a bottom navigation bar by passing true to ``GiniCapture`` ``setBottomNavigationBarEnabled``. There is a default implementation, but you can also use
.. your own by implementing the ``DigitalInvoiceHelpNavigationBarBottomAdapter`` interface and passing it to ``GiniBank``.

.. You can find more details `here <capture-features.html#digital-invoice-help-screen-customization>`_.

.. Digital Invoice Screen
.. ~~~~~~~~~~~~~~~~~~~~

.. The new digital invoice screen for digital invoice uses the global UI customization options.

.. Changes
.. +++++++

.. We removed the ability for users to manually add additional line items.

.. Features
.. ++++++++

.. Bottom navigation bar
.. ^^^^^^^^^^^^^^^^^^^^^

.. You can show an invoice bottom navigation bar by passing true to ``GiniCapture`` ``setBottomNavigationBarEnabled``. There is a default implementation, but you can also use
.. your own by implementing the ``DigitalInvoiceNavigationBarBottomAdapter`` interface and passing it to ``GiniBank``.

.. You can find more details `here <capture-features.html#digital-invoice-screen-customization>`_.
