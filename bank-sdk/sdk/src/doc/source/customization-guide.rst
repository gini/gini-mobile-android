Customization Guide
===================

..
  Headers:
  h1 =====
  h2 -----
  h3 ~~~~~
  h4 +++++
  h5 ^^^^^

.. contents::
   :depth: 1
   :local:

UI customization is provided mostly via overriding of app resources: theme, styles, dimensions, strings,
colors, texts, etc.

We provide global customization options which are applied on all screens consistently. Screen specific customizations
are only needed for images and texts.

.. note::

    Please note that capture related screens are provided by the :root_html_path_capture_sdk:`Gini Capture SDK
    <index.html>`. In the following we will often refer to resources provided by the Gini Capture SDK.

Overview of UI Customization Options
------------------------------------

Styles
~~~~~

We leverage the power of Material Design to configure a theme for the SDK with a global color palette and typography
that is applied on all the screens. 

Using global styles for the various widgets we enable you to customize them in a single place. They are then
consistently applied on all screens.

Theme
+++++

The theme style is based on Material Design v3 and is named ``GiniCaptureTheme``. To override the theme in your
application use ``Root.GiniCaptureTheme`` as the parent:

.. code-block:: xml

    <style name="GiniCaptureTheme" parent="Root.GiniCaptureTheme">
      (...)
    </style>

Widgets
+++++++

The style of buttons and other widgets is based on Material Design v2. To override them in your application use the
root style as the parent, for example:

.. code-block:: xml

    <style name="GiniCaptureTheme.Widget.Button.OutlinedButton" parent="Root.GiniCaptureTheme.Widget.Button.OutlinedButton">
      (...)
    </style>

Colors
~~~~~~

We are providing a global color palette which you are free to override. The custom colors will be then applied on all screens.

You can find the names of the color resources in the color palette below.

.. note::

    If you have overridden the ``GiniCaptureTheme`` then the theme colors you have set there will override the color
    palette customization.

You can view our color palette here:

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D40%253A491%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Images
~~~~~~

Customizing of images is done via overriding of drawable resources. You can find the drawable
resource names in the :ref:`screen-by-screen UI customization section<screen-customization>`.

We are using mostly vector drawables. Unfortunately due to the limitations of vector drawables some images had to be
added as PNGs.

If you use vector drawables please add them to the `drawable-anydpi` folder so that they also override any density
specific PNGs.

Typography
~~~~~~~~~~

We provide a global typography based on text appearance styles from Material Design v2. To override them in your
application use the root style as the parent, for example:

.. code-block:: xml

    <style name="GiniCaptureTheme.Typography.Body1" parent="Root.GiniCaptureTheme.Typography.Body1">
        (...)
    </style>

.. note::

  If you have overriden the ``GiniCaptureTheme`` then the text appearances you have set there will override the
  typography customization. Same applies to overriden widget styles where you have set a custom text appearance.

You can preview our typography along with their style resource names below:

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D40%253A492%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Text
~~~~

Text customization is done via overriding of string resources.

Custom UI Elements
~~~~~~~~~~~~~~~~~~

Certain elements of the UI can be fully customized via UI injection. It utilizes view adapter interfaces which you
can implement and pass to ``GiniBank`` when configuring the SDK. These interfaces declare the contract the injected
view has to fulfill and allow the SDK to ask for your view instance when needed.

Top Navigation Bar
++++++++++++++++++

To inject your own navigation bar view implement the ``NavigationBarTopAdapter`` and pass it to
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(navigationBarTopAdapter = ))``. Your view will then be displayed
on all screens as the top navigation bar.

Bottom Navigation Bar
+++++++++++++++++++++

You can opt to show a bottom navigation bar. To enable it pass ``true`` to
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(bottomNavigationBarEnabled = ))``.

.. note::

    The top navigation bar will still be used, but its functionality will be limited to showing the screen's title and
    an optional close button. Please inject a custom top navigation bar if your design requires it even if you have
    enabled the bottom navigation bar.

Each screen has a slightly different bottom navigation bar because they contain screen specific call-to-action buttons.

To inject your own views implement each screen's view adapter interface (e.g., ``OnboardingNavigationBarBottomAdapter``)
and pass it to ``GiniBank`` (e.g., ``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(onboardingNavigationBarBottomAdapter = ))``). Your
view will then be displayed on the relevant screen.

Dark mode
~~~~~~~~~

To customize resources for dark mode add them to resource folders containing the ``-night`` resource qualifier.

.. _screen-customization:

Onboarding Screen
----

UI Customization
~~~~~~~~~~~~~~~~

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D40%253A584%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Bottom Navigation Bar
~~~~~~~~~~~~~~~~~~~~~

You can inject your own view for the bottom navigation bar, if you set
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(bottomNavigationBarEnabled = ))`` to ``true`` and pass a custom
``OnboardingNavigationBarBottomAdapter`` implementation to ``GiniBank``:

.. code-block:: java

    let customOnboardingNavigationBarBottomAdapter:OnboardingNavigationBarBottomAdapter = CustomOnboardingNavigationBarBottomAdapter();

    GiniBank.setCaptureConfiguration(context,
        CaptureConfiguration(
            onboardingNavigationBarBottomAdapter = customOnboardingNavigationBarBottomAdapter
        )
    )

Custom Onboarding Pages
~~~~~~~~~~~~~~~~~~~~~~~

If you wish to show different onboarding pages then pass a list of ``OnboardingPage`` objects to
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(onboardingPages = ))``.

Custom Illustration Views
~~~~~~~~~~~~~~~~~~~~~~~~~

You can inject your own views for the illustrations. For example if you need to animate the illustrations on the
onboarding pages implement the ``OnboardingIllustrationAdapter`` interface to inject a view that can animate images
(e.g., `Lottie <https://github.com/airbnb/lottie-android>`_) and pass it to the relevant onboarding illustration adapter
setters (e.g., ``onboardingAlignCornersIllustrationAdapter``) when configuring ``GiniBank``. The
:root_dokka_path:`reference documentation <sdk/net.gini.android.bank.sdk.capture/-capture-configuration/index.html>` of
``CaptureConfiguration`` lists all the setters.

Camera Screen
----

UI Customization
~~~~~~~~~~~~~~~~

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D92%253A3712%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Bottom Navigation Bar
~~~~~~~~~~~~~~~~~~~~~

You can inject your own view for the bottom navigation bar, if you set
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(bottomNavigationBarEnabled = ))`` to ``true`` and pass a custom
``CameraNavigationBarBottomAdapter`` implementation to ``GiniBank``:

.. code-block:: java

    let customCameraNavigationBarBottomAdapter:CameraNavigationBarBottomAdapter = CustomCameraNavigationBarBottomAdapter();

    GiniBank.setCaptureConfiguration(context,
        CaptureConfiguration(
            cameraNavigationBarBottomAdapter = customCameraNavigationBarBottomAdapter
        )
    )

Custom Loading Indicator
~~~~~~~~~~~~~~~~~~~~~~~~

There is a default loading indicator which shows that image is being processed. You can show your own activity indicator
by implementing the ``CustomLoadingIndicatorAdapter`` interface and passing it to ``GiniBank``:

.. code-block:: java

    let myCustomLoadingIndicatorAdapter:CustomLoadingIndicatorAdapter = MyCustomLoadingIndicatorAdapter();

    GiniBank.setCaptureConfiguration(context,
        CaptureConfiguration(
            customLoadingIndicatorAdapter = myCustomLoadingIndicatorAdapter
        )
    )

Review Screen
----

UI Customization
~~~~~~~~~~~~~~~~

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D143%253A4156%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Bottom Navigation Bar
~~~~~~~~~~~~~~~~~~~~~

You can inject your own view for the bottom navigation bar, if you set
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(bottomNavigationBarEnabled = ))`` to ``true`` and pass a custom
``ReviewNavigationBarBottomAdapter`` implementation to ``GiniBank``:

.. code-block:: java

    let customReviewNavigationBarBottomAdapter:ReviewNavigationBarBottomAdapter = CustomReviewNavigationBarBottomAdapter();

    GiniBank.setCaptureConfiguration(context,
        CaptureConfiguration(
            reviewNavigationBarBottomAdapter = customReviewNavigationBarBottomAdapter
        )
    )

Custom "Process" Button Loading Indicator 
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There is a default loading indicator on the "Process" button which shows that the upload is in progress. You can show
your own activity indicator by implementing the ``OnButtonLoadingIndicatorAdapter`` interface and passing it to
``GiniBank``:

.. code-block:: java

    let customOnButtonLoadingIndicatorAdapter:OnButtonLoadingIndicatorAdapter = CustomOnButtonLoadingIndicatorAdapter();

    GiniBank.setCaptureConfiguration(context,
        CaptureConfiguration(
            onButtonLoadingIndicatorAdapter = customOnButtonLoadingIndicatorAdapter
        )
    )

Analysis Screen
----

UI Customization
~~~~~~~~~~~~~~~~

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D7%253A18496%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

.. note::

    This screen does not show a bottom navigation bar even if the value passed to ``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(bottomNavigationBarEnabled = ))`` is ``true``.

Custom Loading Indicator
~~~~~~~~~~~~~~~~~~~~~~~~

You can show a customized activity indicator on this screen. You can pass your custom ``CustomLoadingIndicatorAdapter`` implementation to
``GiniBank`` :

.. code-block:: java

    let myCustomOnButtonLoadingIndicatorAdapter:CustomLoadingIndicatorAdapter = MyCustomLoadingIndicatorAdapter();

    GiniBank.setCaptureConfiguration(context,
        CaptureConfiguration(
            customLoadingIndicatorAdapter = myCustomOnButtonLoadingIndicatorAdapter
        )
    )

Help Screen
----

UI Customization
~~~~~~~~~~~~~~~~

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D9%253A4645%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Bottom Navigation Bar
~~~~~~~~~~~~~~~~~~~~~

You can inject your own view for the bottom navigation bar, if you set
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(bottomNavigationBarEnabled = ))`` to ``true`` and pass a custom
``HelpNavigationBarBottomAdapter`` implementation to ``GiniBank``:

.. code-block:: java

    let customHelpNavigationBarBottomAdapter:HelpNavigationBarBottomAdapter = CustomHelpNavigationBarBottomAdapter();

    GiniBank.setCaptureConfiguration(context,
        CaptureConfiguration(
            helpNavigationBarBottomAdapter = customHelpNavigationBarBottomAdapter
        )
    )

Custom Help Screens
~~~~~~~~~~~~~~~~~~~

You can show your own help screens. They will be appended to the list on the main help screen.

You can pass the title and activity for each screen to ``GiniBank`` using a list of ``HelpItem.Custom`` objects:

.. code-block:: java

    val customHelpItems: MutableList<HelpItem.Custom> = ArrayList()

    customHelpItems.add(
        HelpItem.Custom(
            R.string.custom_help_screen_title,
            Intent(this, CustomHelpActivity::class.java)
        )
    )
    
    GiniBank.setCaptureConfiguration(context,
        CaptureConfiguration(
            customHelpItems = customHelpItems
        )
    )

No Results Screen
-----------------

UI Customization
~~~~~~~~~~~~~~~~

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D10%253A2540%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Error Screen
------------

UI Customization
~~~~~~~~~~~~~~~~

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D9%253A5075%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Return Assistant
----------------

Digital Invoice Onboarding Screen
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

UI Customization
++++++++++++++++

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D3243%253A6523%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Bottom Navigation Bar
+++++++++++++++++++++

You can inject your own view for the bottom navigation bar, if you set
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(bottomNavigationBarEnabled = ))`` to ``true`` and pass a custom
``DigitalInvoiceOnboardingNavigationBarBottomAdapter`` implementation to ``GiniBank``:

.. code-block:: java

    let customBottomNavigationBar: DigitalInvoiceOnboardingNavigationBarBottomAdapter = CustomDigitalInvoiceOnboardingNavigationBarBottomAdapter();

    GiniBank.digitalInvoiceOnboardingNavigationBarBottomAdapter = customBottomNavigationBar

Custom Illustration View
++++++++++++++++++++++++

You can inject your own view for the illustrations. For example if you need to animate the illustration on the
onboarding page implement the ``OnboardingIllustrationAdapter`` interface to inject a view that can animate images
(e.g., `Lottie <https://github.com/airbnb/lottie-android>`_) and pass it to ``GiniBank``:

.. code-block:: java

    let customIllustration: OnboardingIllustrationAdapter = CustomDigitalInvoiceOnboardingIllustrationAdapter();

    GiniBank.digitalInvoiceOnboardingIllustrationAdapter = customIllustration

Digital Invoice Help Screen
~~~~~~~~~~~~~~~~~~~~~~~~~~~

UI Customization
++++++++++++++++

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D3513%253A5595%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Bottom Navigation Bar
+++++++++++++++++++++

You can inject your own view for the bottom navigation bar, if you set
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(bottomNavigationBarEnabled = ))`` to ``true`` and pass a custom
``DigitalInvoiceOnboardingNavigationBarBottomAdapter`` implementation to ``GiniBank``:

.. code-block:: java

    let customBottomNavigationBar: DigitalInvoiceHelpNavigationBarBottomAdapter = CustomDigitalInvoiceHelpNavigationBarBottomAdapter();

    GiniBank.digitalInvoiceHelpNavigationBarBottomAdapter = customBottomNavigationBar


Digital Invoice Overview Screen
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

UI Customization
++++++++++++++++

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D3688%253A6085%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>

Bottom Navigation Bar
+++++++++++++++++++++

You can inject your own view for the bottom navigation bar, if you set
``GiniBank.setCaptureConfiguration(context, CaptureConfiguration(bottomNavigationBarEnabled = ))`` to ``true`` and pass a custom
``DigitalInvoiceNavigationBarBottomAdapter`` implementation to ``GiniBank``:

.. code-block:: java

    let customBottomNavigationBar: DigitalInvoiceNavigationBarBottomAdapter = CustomDigitalInvoiceNavigationBarBottomAdapter();

    GiniBank.digitalInvoiceNavigationBarBottomAdapter = customBottomNavigationBar


Digital Invoice Edit Article Screen
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

UI Customization
++++++++++++++++

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FLTPEOdGgitjNX7Aw9qlOaj%2FAndroid-Gini-Bank-SDK-3.2.1-UI-Customisation%3Ftype%3Ddesign%26node-id%3D3688%253A7453%26mode%3Ddesign%26t%3DcUyUl8MB4jMlPMM0-1"
    allowfullscreen></iframe>
