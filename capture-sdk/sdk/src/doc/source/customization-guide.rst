Customization Guide
====

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

The theme style is based on Material Design v2 and is named ``GiniCaptureTheme``. To override the theme in your
application use ``Root.GiniCaptureTheme`` as the parent:

.. code-block:: xml

    <style name="GiniCaptureTheme" parent="Root.GiniCaptureTheme">
      (...)
    </style>

You can view the theme `here <https://github.com/gini/gini-mobile-android/blob/main/capture-sdk/sdk/src/main/res/values/styles.xml>`_

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
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D40%253A491%26t%3DNoWz8V7m9GX9SNwS-1"
    allowfullscreen></iframe>

Images
~~~~~~

Customizing of images is done via overriding of drawable resources. You can find the drawable
resource names in the :ref:`screen-by-screen UI customization section<Migrate to the new UI>`.

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
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D40%253A492%26t%3DNoWz8V7m9GX9SNwS-1"
    allowfullscreen></iframe>

Text
~~~~

Text customization is done via overriding of string resources.

UI Elements
~~~~~~~~~~~

Certain elements of the UI can be fully customized via UI injection.

UI injection utilizes view adapter interfaces which you can implement and pass to ``GiniCapture`` when configuring the
SDK. These interfaces declare the contract the injected view has to fulfill and allow the SDK to ask for your view
instance when needed.

The most important injectable UI element is the top navigation bar. You may also show the navigation bar on the bottom
using your own custom view. You can find more details `here <features.html#custom-ui-elements>`_.

Dark mode
~~~~~~~~~

To customize resources for dark mode add them to resource folders containing the ``-night`` resource qualifier.

Onboarding Screen
----

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D40%253A584%26t%3DNoWz8V7m9GX9SNwS-1"
    allowfullscreen></iframe>

Camera Screen
----
.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D92%253A3712%26t%3Dc3jMrBwHYOfKgDHC-1"
    allowfullscreen></iframe>

Review Screen
----

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D143%253A4156%26t%3DbxRb1PoNfoS2K8LX-1"
    allowfullscreen></iframe>

Analysis Screen
----

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D7%253A18496%26t%3DRrYhEBagMqQ9uksD-1"
    allowfullscreen></iframe>

Help Screen
----

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D9%253A4645%26t%3DHtNbZnDsRjA5FeBu-1"
    allowfullscreen></iframe>

No Results Screen
----

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D10%253A2540%26t%3DRrYhEBagMqQ9uksD-1"
    allowfullscreen></iframe>

Error Screen
----

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D9%253A5075%26t%3DQkcPe6W16KvhSI1a-1"
    allowfullscreen></iframe>
