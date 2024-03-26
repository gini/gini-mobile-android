UI Customization Guide
======================

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

Overview of UI customization options
------------------------------------

Styles
~~~~~~

We leverage the power of Material Design to configure a theme for the SDK with a global color palette and typography
that is applied on all the screens. By using global styles for the various widgets, we enable you to customize them in a
single place. They are then consistently applied on all screens.

Theme
+++++

The theme style is based on Material Design v3 and is named ``GiniHealthTheme``. To override the theme in your
application, use ``Root.GiniHealthTheme`` as the parent:

.. code-block:: xml

    <style name="GiniHealthTheme" parent="Root.GiniHealthTheme">
      (...)
    </style>

Widgets
+++++++

The style of buttons and other widgets is based on Material Design v3. To override them in your application, use the
root style as the parent, for example:

.. code-block:: xml

    <style name="GiniHealthTheme.Widget.Button.OutlinedButton" parent="Root.GiniHealthTheme.Widget.Button.OutlinedButton">
      (...)
    </style>

.. note::

    If you wish to change the button height please override ``android:minHeight``. We are using ``android:minHeight`` to
    support text resizing for accessibility.

    You may also force the height to a certain dimension by overriding both
    ``android:minHeight`` and ``android:height``, although we don't recommend this as it will prevent the button from increasing
    height to fit larger font sizes.

.. warning::

    Material Design buttons include insets which will decrease the visible height of the buttons. You can override
    ``android:insetTop`` and ``android:insetBottom`` to remove or adjust the vertical insets.

Colors
~~~~~~

We provide a global color palette which you are free to override. The custom colors are then applied to all screens.
Customization of colors is done via overriding of color resources. For example to override the color ``ghs_accent_01`` add
the following snippet to one of your resources XML file (e.g, ``colors.xml``):

.. code-block:: xml

    <color name="ghs_accent_01">#424242</color>

.. note::

    If you overridde the ``GiniHealthTheme``, the theme colors you set there override the color palette customization.

Find the names of the color resources in the color palette (you can also view it in Figma `here
<https://www.figma.com/file/AJTss4k0M6R2OxH0VQepdP/Android-Gini-Health-SDK-3.0.0-UI-Customisation?type=design&node-id=8502%3A357&mode=design&t=A1pTQWjJWSBUR6Zi-1>`_): 

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FAJTss4k0M6R2OxH0VQepdP%2FAndroid-Gini-Health-SDK-3.0.0-UI-Customisation%3Ftype%3Ddesign%26node-id%3D8502%253A357%26mode%3Ddesign%26t%3DA1pTQWjJWSBUR6Zi-1"
    allowfullscreen></iframe>

|

Images
~~~~~~

Customizing of images is done via overriding of drawable resources. Find the drawable resource names in the
screen-by-screen UI customisation sections below. We mostly use vector drawables.
Due to the limitations of vector drawables, some images had to be added as PNGs.

.. note::

    If you use vector drawables, add them to the ``drawable-anydpi`` and ``drawable-night-anydpi`` folders so that they also
    override any density specific PNGs.

If you want to override specific SDK images:

1. Add your own images to your app's ``res/drawable-*`` folders using the image names from the UI customization guide.
   It is important to name the images you wish to override exactly as shown in the UI customization guide, otherwise
   overriding won't work.
2. If you use vector drawables, add them to the ``drawable-anydpi`` (``drawable-night-anydpi`` for dark mode) folders so
   that they also override any density specific images.

Typography
~~~~~~~~~~~

We provide global typography based on text appearance styles from Material Design v2. To override them in your
application, use the root style as the parent, for example:

.. code-block:: xml

    <style name="GiniHealthTheme.Typography.Body1" parent="Root.GiniHealthTheme.Typography.Body1">
       (...)
    </style>

.. note::

    If you override the ``GiniHealthTheme``, the text appearances you set there override the typography customization. The
    same applies to the override of widget styles where you set a custom text appearance. 

Preview our typography and find the names of the style resources (you can also view it in Figma `here
<https://www.figma.com/file/AJTss4k0M6R2OxH0VQepdP/Android-Gini-Health-SDK-3.0.0-UI-Customisation?type=design&node-id=8503%3A491&mode=design&t=zZkiuvx3neNm8Tmv-1>`_):

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FAJTss4k0M6R2OxH0VQepdP%2FAndroid-Gini-Health-SDK-3.0.0-UI-Customisation%3Ftype%3Ddesign%26node-id%3D8503%253A491%26mode%3Ddesign%26t%3DzZkiuvx3neNm8Tmv-1"
    allowfullscreen></iframe>

|

Text
~~~~

Text customization is done via overriding of string resources.

Dark mode
~~~~~~~~~~

To customize resources for dark mode, add them to resource folders containing the -night resource qualifier.

UI customization options
------------------------

Payment Component
~~~~~~~~~~~~~~~~~

You can also view the UI customisation guide in Figma `here
<https://www.figma.com/file/AJTss4k0M6R2OxH0VQepdP/Android-Gini-Health-SDK-4.0.0-UI-Customisation?type=design&node-id=8663%3A1324&mode=design&t=prjCS8vTcjIzTtvT-1>`_.

.. note::

    To copy text from Figma you need to have a Figma account. If you don't have one, you can create one for free.

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FAJTss4k0M6R2OxH0VQepdP%2FAndroid-Gini-Health-SDK-4.0.0-UI-Customisation%3Ftype%3Ddesign%26node-id%3D8663%253A1324%26mode%3Ddesign%26t%3DprjCS8vTcjIzTtvT-1"
    allowfullscreen></iframe>

|

Bank Selection Bottom Sheet
~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can also view the UI customisation guide in Figma `here
<https://www.figma.com/file/AJTss4k0M6R2OxH0VQepdP/Android-Gini-Health-SDK-4.0.0-UI-Customisation?type=design&node-id=8794%3A1437&mode=design&t=osss80Nvdttp8opj-1>`_.

.. note::

    To copy text from Figma you need to have a Figma account. If you don't have one, you can create one for free.

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FAJTss4k0M6R2OxH0VQepdP%2FAndroid-Gini-Health-SDK-4.0.0-UI-Customisation%3Ftype%3Ddesign%26node-id%3D8794%253A1437%26mode%3Ddesign%26t%3Dosss80Nvdttp8opj-1"
    allowfullscreen></iframe>

|

Payment Feature Info Screen
~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can also view the UI customisation guide in Figma `here
<https://www.figma.com/file/AJTss4k0M6R2OxH0VQepdP/Android-Gini-Health-SDK-4.0.0-UI-Customisation?type=design&node-id=8865%3A1784&mode=design&t=K7YqsQfoIezyUS7U-1>`_.

.. note::

    To copy text from Figma you need to have a Figma account. If you don't have one, you can create one for free.

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FAJTss4k0M6R2OxH0VQepdP%2FAndroid-Gini-Health-SDK-4.0.0-UI-Customisation%3Ftype%3Ddesign%26node-id%3D8865%253A1784%26mode%3Ddesign%26t%3DK7YqsQfoIezyUS7U-1"
    allowfullscreen></iframe>

Payment Review Screen
~~~~~~~~~~~~~~~~~~~~~~~~~~~

You can also view the UI customisation guide in Figma `here
<https://www.figma.com/file/AJTss4k0M6R2OxH0VQepdP/Android-Gini-Health-SDK-4.0.0-UI-Customisation?type=design&node-id=8856%3A2344&mode=design&t=qTedJeTuklI9gPIm-1>`_.

.. note::

    To copy text from Figma you need to have a Figma account. If you don't have one, you can create one for free.

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FAJTss4k0M6R2OxH0VQepdP%2FAndroid-Gini-Health-SDK-4.0.0-UI-Customisation%3Ftype%3Ddesign%26node-id%3D8856%253A2344%26mode%3Ddesign%26t%3DqTedJeTuklI9gPIm-1"
    allowfullscreen></iframe>

