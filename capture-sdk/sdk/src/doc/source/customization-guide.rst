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

You can find the names of the colors `here <https://github.com/gini/gini-mobile-android/blob/main/capture-sdk/sdk/src/main/res/values/colors.xml>`_.

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

You can find all the typography styles `here <https://github.com/gini/gini-mobile-android/blob/main/capture-sdk/sdk/src/main/res/values/typography.xml>`_.

You can preview our typography here:

.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D40%253A492%26t%3DNoWz8V7m9GX9SNwS-1"
    allowfullscreen></iframe>

Text
~~~~

Text customization is done via overriding of string resources.

You can find all the string resources `here <https://github.com/gini/gini-mobile-android/blob/main/capture-sdk/sdk/src/main/res/values/strings.xml>`_.

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

.. _camera:

Camera Screen
----
.. raw:: html

    <iframe style="border: 1px solid rgba(0, 0, 0, 0.1);" width="600" height="450"
    src="https://www.figma.com/embed?embed_host=share&url=https%3A%2F%2Fwww.figma.com%2Ffile%2FMcDZrQPr6IgkzCQtN3lqAe%2FAndroid-Gini-Capture-SDK-3.0.0-UI-Customisation%3Fnode-id%3D92%253A3712%26t%3Dc3jMrBwHYOfKgDHC-1"
    allowfullscreen></iframe>

.. _review:

Review Screen
----

.. raw:: html

    <img src="_static/customization/Review Screen.png" usemap="#review-map" width="324" height="576">

    <map id="review-map" name="review-map">
        <area shape="rect" alt="" title="Action Bar" coords="189,26,220,54" href="customization-guide.html#review-1" target="" />
        <area shape="rect" alt="" title="Next Button" coords="241,408,272,438" href="customization-guide.html#review-2" target="" />
        <area shape="rect" alt="" title="Rotate Button" coords="244,352,275,385" href="customization-guide.html#review-3" target="" />
        <area shape="rect" alt="" title="Advice" coords="231,490,264,520" href="customization-guide.html#review-4" target="" />
        <area shape="rect" alt="" title="Background" coords="2,288,29,319" href="customization-guide.html#review-5" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

.. _review-1:

1. Action Bar
^^^^

All Action Bar customizations except the title are global to all Activities.

- **Title**

  Via the string resource named ``gc_title_review``.

- **Title Color**

  Via the color resource named ``gc_action_bar_title``.

- **Back Button Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_action_bar_back``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Background Color**

  Via the color resource named ``gc_action_bar``.

- **Status Bar Background Color**

  Via the color resource named ``gc_status_bar``.

  If you use a light background color, then you should set the ``gc_light_status_bar`` boolean
  resource to ``true``. This will cause the status bar contents to be drawn with a dark color.

:ref:`Back to screenshot. <review>`

.. _review-2:

2. Next Button
^^^^

- **Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_review_fab_next.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Color**

  Via the color resources named ``gc_review_fab`` and ``gc_review_fab_pressed``.

:ref:`Back to screenshot. <review>`

.. _review-3:

3. Rotate Button
^^^^

- **Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_review_button_rotate.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Color**

  Via the color resources named ``gc_review_fab_mini`` and ``gc_review_fab_mini_pressed``.

:ref:`Back to screenshot. <review>`

.. _review-4:

4. Advice
^^^^

- **Text**

  Via the string resource named ``gc_review_bottom_panel_text``.

- **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Review.BottomPanel.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Review.BottomPanel.TextStyle``).

  - **Font**

  Via overriding the style named ``GiniCaptureTheme.Review.BottomPanel.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Review.BottomPanel.TextStyle``) and setting an
  item named ``gcCustomFont`` with the path to the font file in your assets folder.

- **Background Color**

  Via the color resource named ``gc_review_bottom_panel_background``.

:ref:`Back to screenshot. <review>`

.. _review-5:

5. Background
^^^^

- **Color**

  Via the color resource named ``gc_background``. **Note**: this color resource is global to all Activities.

:ref:`Back to screenshot. <review>`

.. _analysis:

Analysis Screen
----

.. raw:: html

    <img src="_static/customization/Analysis Screen.png" usemap="#analysis-map-1" width="324" height="576">

    <map id="analysis-map-1" name="analysis-map-1">
        <area shape="rect" alt="" title="Action Bar" coords="189,24,222,55" href="customization-guide.html#analysis-1" target="" />
        <area shape="rect" alt="" title="Activity Indicator" coords="105,283,132,310" href="customization-guide.html#analysis-2" target="" />
        <area shape="rect" alt="" title="Error Snackbar" coords="190,500,219,530" href="customization-guide.html#analysis-4" target="" />
        <area shape="rect" alt="" title="Background" title" coords="74,61,105,93" href="customization-guide.html#analysis-5" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

.. raw:: html

    <img src="_static/customization/Analysis Screen PDF.png" usemap="#analysis-map-2" width="324" height="576">

    <map id="analysis-map-2" name="analysis-map-2">
        <area shape="rect" alt="" title="PDF Info Panel" coords="60,78,90,106" href="customization-guide.html#analysis-3" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>


.. _analysis-1:

1. Action Bar
^^^^

All Action Bar customizations except the title are global to all Activities.

- **Back Button Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_action_bar_back``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Background Color**

  Via the color resource named ``gc_action_bar``.

- **Status Bar Background Color**

  Via the color resource named ``gc_status_bar``.

  If you use a light background color, then you should set the ``gc_light_status_bar`` boolean
  resource to ``true``. This will cause the status bar contents to be drawn with a dark color.

:ref:`Back to screenshots. <analysis>`

.. _analysis-2:

2. Activity Indicator
^^^^

- **Color**

  Via the color resource named ``gc_analysis_activity_indicator``.

- **Message**

  - **Text**
  
    Via the string resource named ``gc_analysis_activity_indicator_message``.

  - **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Analysis.AnalysingMessage.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Analysis.AnalysingMessage.TextStyle``).

  - **Font**

    Via overriding the style named ``GiniCaptureTheme.Analysis.AnalysingMessage.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Analysis.AnalysingMessage.TextStyle``) and setting an
    item named ``gcCustomFont`` with the path to the font file in your assets folder.

:ref:`Back to screenshots. <analysis>`

.. _analysis-3:

3. PDF Info Panel
^^^^

- **Background Color**

  Via the color resource named ``gc_analysis_pdf_info_background``.

- **Filename**

  - **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Analysis.PdfFilename.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Analysis.PdfFilename.TextStyle``).

  - **Font**

    Via overriding the style named ``GiniCaptureTheme.Analysis.PdfFilename.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Analysis.PdfFilename.TextStyle``) and setting an
    item named ``gcCustomFont`` with the path to the font file in your assets folder.

- **Page Count**

  - **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Analysis.PdfPageCount.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Analysis.PdfPageCount.TextStyle``).

  - **Font**

    Via overriding the style named ``GiniCaptureTheme.Analysis.PdfPageCount.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Analysis.PdfPageCount.TextStyle``) and setting an
    item named ``gcCustomFont`` with the path to the font file in your assets folder.

  :ref:`Back to screenshots. <analysis>`

.. _analysis-4:

4. Error Snackbar
^^^^

- **Message**

  - **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Snackbar.Error.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Snackbar.Error.TextStyle``).

  - **Font**

    Via overriding the style named ``GiniCaptureTheme.Snackbar.Error.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Snackbar.Error.TextStyle``) and setting an
    item named ``gcCustomFont`` with the path to the font file in your assets folder.

- **Button**

  - **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Snackbar.Error.Button.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Snackbar.Error.Button.TextStyle``).

  - **Font**

    Via overriding the style named ``GiniCaptureTheme.Snackbar.Error.Button.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Snackbar.Error.Button.TextStyle``) and setting an
    item named ``gcCustomFont`` with the path to the font file in your assets folder.

  - **Retry Button Text**

    Via the string resource named ``gc_document_analysis_error_retry``.

- **Background Color**

  Via the color resource named ``gc_snackbar_error_background``.

:ref:`Back to screenshots. <analysis>`

.. _analysis-5:

5. Background
^^^^

- **Color**

  Via the color resource named ``gc_background``. **Note**: this color resource is global to all Activities.

:ref:`Back to screenshots. <analysis>`

.. _multi-page-review:

Multi-Page Review Screen
----

.. raw:: html

    <img src="_static/customization/Multi-Page Review.png" usemap="#multi-page-review-map-1" width="324" height="576">

    <map id="multi-page-review-map-1" name="multi-page-review-map-1">
        <area shape="rect" alt="" title="Action Bar" coords="189,23,220,54" href="customization-guide.html#multi-page-review-1" target="" />
        <area shape="rect" alt="" title="Page Indicators" coords="174,284,207,316" href="customization-guide.html#multi-page-review-2" target="" />
        <area shape="rect" alt="" title="Next Button" coords="273,259,302,288" href="customization-guide.html#multi-page-review-3" target="" />
        <area shape="rect" alt="" title="Thumbnails Panel" coords="296,341,323,371" href="customization-guide.html#multi-page-review-4" target="" />
        <area shape="rect" alt="" title="Add Pages Card" coords="213,345,243,376" href="customization-guide.html#multi-page-review-6" target="" />
        <area shape="rect" alt="" title="Reorder Pages Tip" coords="2,478,28,508" href="customization-guide.html#multi-page-review-7" target="" />
        <area shape="rect" alt="" title="Bottom Toolbar" coords="150,502,177,532" href="customization-guide.html#multi-page-review-8" target="" />
        <area shape="rect" alt="" title="Image Error" coords="178,67,212,97" href="customization-guide.html#multi-page-review-9" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

.. raw:: html

    <img src="_static/customization/Multi-Page Review Upload Indicators.png" usemap="#multi-page-review-map-2" width="324" height="576">

    <map id="multi-page-review-map-2" name="multi-page-review-map-2">
        <area shape="rect" alt="" title="Thumbnail Card" coords="12,345,41,375" href="customization-guide.html#multi-page-review-5" target="" />
        <area shape="rect" alt="" title="Badge" coords="131,440,152,463" href="customization-guide.html#multi-page-review-5-1" target="" />
        <area shape="rect" alt="" title="Drag Indicator Bumps" coords="276,435,299,457" href="customization-guide.html#multi-page-review-5-2" target="" />
        <area shape="rect" alt="" title="Highlight Strip" coords="10,464,31,488" href="customization-guide.html#multi-page-review-5-3" target="" />
        <area shape="rect" alt="" title="Activity Indicator" coords="263,367,285,390" href="customization-guide.html#multi-page-review-5-4" target="" />
        <area shape="rect" alt="" title="Upload Success Icon" coords="59,369,84,393" href="customization-guide.html#multi-page-review-5-5" target="" />
        <area shape="rect" alt="" title="Upload Failure Icon" coords="161,371,182,394" href="customization-guide.html#multi-page-review-5-6" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

.. raw:: html

    <img src="_static/customization/Multi-Page Review Delete Last Page.png" usemap="#multi-page-review-map-3" width="324" height="576">

    <map id="multi-page-review-map-3" name="multi-page-review-map-3">
        <area shape="rect" alt="" title="Imported Image Delete Last Page Dialog" coords="146,213,176,249" href="customization-guide.html#multi-page-review-10" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

.. _multi-page-review-1:

1. Action Bar
^^^^

All Action Bar customizations except the title are global to all Activities.

- **Title**

  Via the string resource named ``gc_title_multi_page_review``.

- **Title Color**

  Via the color resource named ``gc_action_bar_title``.

- **Back Button Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_action_bar_back``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Background Color**

  Via the color resource named ``gc_action_bar``.

- **Status Bar Background Color**

  Via the color resource named ``gc_status_bar``.

  If you use a light background color, then you should set the ``gc_light_status_bar`` boolean
  resource to ``true``. This will cause the status bar contents to be drawn with a dark color.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-2:

2. Page Indicators
^^^^

- **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Review.MultiPage.PageIndicator.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Review.MultiPage.PageIndicator.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Review.MultiPage.PageIndicator.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Review.MultiPage.PageIndicator.TextStyle``) and setting an
  item named ``gcCustomFont`` with the path to the font file in your assets folder.

- **Background Color**

  Via the color resource named ``gc_multi_page_review_page_indicator_background``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-3:

3. Next Button
^^^^

- **Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_review_fab_checkmark.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Color**

  Via the color resources named ``gc_review_fab`` and ``gc_review_fab_pressed``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-4:

4. Thumbnails Panel
^^^^

- **Background Color**

  Via the color resource named ``gc_multi_page_review_thumbnails_panel_background``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-5:

5. Thumbnail Card
^^^^

- **Background Color**

  Via the color resource named ``gc_multi_page_review_thumbnail_card_background``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-5-1:

5.1 Badge
~~~~

- **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Review.MultiPage.ThumbnailBadge.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Review.MultiPage.ThumbnailBadge.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Review.MultiPage.ThumbnailBadge.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Review.MultiPage.ThumbnailBadge.TextStyle``) and setting an
  item named ``gcCustomFont`` with the path to the font file in your assets folder.

- **Background Border Color**

  Via the color resource named ``gc_multi_page_thumbnail_badge_background_border``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-5-2:

5.2 Drag Indicator Bumps
~~~~~

- **Icon**

 Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_bumps_icon.png``.
 Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-5-3:

5.3 Highlight Strip
~~~~

- **Color**

  Via the color resource named ``gc_multi_page_thumbnail_highlight_strip``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-5-4:

5.4 Activity Indicator
~~~~

- **Color**

 Via the color resource named ``gc_analysis_activity_indicator``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-5-5:

5.5 Upload Success Icon
~~~~~

- **Background Color**

  Via the color resource named ``gc_multi_page_thumbnail_upload_success_icon_background``.

- **Tick Color**

  Via the color resource named ``gc_multi_page_thumbnail_upload_success_icon_foreground``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-5-6:

5.6 Upload Failure Icon
~~~~

- **Background Color**

  Via the color resource named ``gc_multi_page_thumbnail_upload_failure_icon_background``.

- **Cross Color**

  Via the color resource named ``gc_multi_page_thumbnail_upload_failure_icon_foreground``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-6:

6. Add Pages Card
^^^^

- **Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_multi_page_add_page_icon.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Subtitle**

  - **Text**

    Via the string resource named ``gc_multi_page_review_add_pages_subtitle``.

  - **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Review.MultiPage.AddPagesSubtitle.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Review.MultiPage.AddPagesSubtitle.TextStyle``).

  - **Font**

    Via overriding the style named ``GiniCaptureTheme.Review.MultiPage.AddPagesSubtitle.TextStyle``
    (with parent style ``Root.GiniCaptureTheme.Review.MultiPage.AddPagesSubtitle.TextStyle``) and
    setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

  :ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-7:

7. Reorder Pages Tip
^^^^

- **Text**

  Via the string resource named ``gc_multi_page_review_reorder_pages_tip``.

- **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Review.MultiPage.ReorderPagesTip.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Review.MultiPage.ReorderPagesTip.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Review.MultiPage.ReorderPagesTip.TextStyle``
  (with parent style ``Root.GiniCaptureTheme.Review.MultiPage.ReorderPagesTip.TextStyle``) and
  setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-8:

8. Bottom Toolbar
^^^^

- **Rotate Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_rotate_icon.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Delete Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_delete_icon.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-9:

9. Image Error
^^^^

- **Background Color**

  Via the color resource named ``gc_snackbar_error_background``.

- **Message**

  - **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Snackbar.Error.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Snackbar.Error.TextStyle``).

  - **Font**

    Via overriding the style named ``GiniCaptureTheme.Snackbar.Error.TextStyle``
    (with parent style ``Root.GiniCaptureTheme.Snackbar.Error.TextStyle``) and
    setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

- **Button**

  - **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Snackbar.Error.Button.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Snackbar.Error.Button.TextStyle``).

  - **Font**

    Via overriding the style named ``GiniCaptureTheme.Snackbar.Error.Button.TextStyle``
    (with parent style ``Root.GiniCaptureTheme.Snackbar.Error.Button.TextStyle``) and
    setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

  - **Retry Text (Analysis)**
  
    Via the string resource named ``gc_document_analysis_error_retry``.

  - **Delete Text (Imported Image)**

    Via the string resource named ``gc_multi_page_review_delete_invalid_document``.

:ref:`Back to screenshots. <multi-page-review>`

.. _multi-page-review-10:

10. Imported Image Delete Last Page Dialog
^^^^

- **Message**

  Via the string resource named ``gc_multi_page_review_file_import_delete_last_page_dialog_message``.

- **Positive Button Title**

  Via the string resource named ``gc_multi_page_review_file_import_delete_last_page_dialog_positive_button``.

- **Negative Button Title**

  Via the string resource named ``gc_multi_page_review_file_import_delete_last_page_dialog_negative_button``.

- **Button Color**

  Via the color resource named ``gc_accent``.

:ref:`Back to screenshots. <multi-page-review>`

.. _help-screen:

Help Screen
----

.. raw:: html

    <img src="_static/customization/Help Screen.png" usemap="#help-screen-map" width="324" height="576">

    <map id="help-screen-map" name="help-screen-map">
        <area shape="rect" alt="" title="Action Bar" coords="97,23,135,56" href="customization-guide.html#help-screen-1" target="" />
        <area shape="rect" alt="" title="Background" coords="136,346,168,379" href="customization-guide.html#help-screen-2" target="" />
        <area shape="rect" alt="" title="Help List Item" coords="217,74,246,104" href="customization-guide.html#help-screen-3" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

.. _help-screen-1:

1. Action Bar
^^^^

All Action Bar customizations except the title are global to all Activities.

- **Title**

  Via the string resource named ``gc_title_help``.

- **Title Color**

  Via the color resource named ``gc_action_bar_title``.

- **Back Button Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_action_bar_back``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Background Color**

  Via the color resource named ``gc_action_bar``.

- **Status Bar Background Color**

  Via the color resource named ``gc_status_bar``.

  If you use a light background color, then you should set the ``gc_light_status_bar`` boolean
  resource to ``true``. This will cause the status bar contents to be drawn with a dark color.

:ref:`Back to screenshot. <help-screen>`

.. _help-screen-2:

2. Background 
^^^^

- **Color**

  Via the color resource named ``gc_help_activity_background``.

:ref:`Back to screenshot. <help-screen>`

.. _help-screen-3:

3. Help List Item
^^^^

- **Background Color**

  Via the color resource name ``gc_help_item_background``.
  
- **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Help.Item.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Help.Item.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Help.Item.TextStyle``
  (with parent style ``Root.GiniCaptureTheme.Help.Item.TextStyle``) and
  setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

:ref:`Back to screenshot. <help-screen>`

.. _photo-tips:

Photo Tips Screen
----

.. raw:: html

    <img src="_static/customization/Photo Tips Screen.png" usemap="#photo-tips-map" width="324" height="576">

    <map id="photo-tips-map" name="photo-tips-map">
        <area shape="rect" alt="" title="Action Bar" coords="173,25,203,56" href="customization-guide.html#photo-tips-1" target="" />
        <area shape="rect" alt="" title="Background" coords="275,251,306,281" href="customization-guide.html#photo-tips-2" target="" />
        <area shape="rect" alt="" title="Header" coords="277,71,308,103" href="customization-guide.html#photo-tips-3" target="" />
        <area shape="rect" alt="" title="Tip" coords="227,138,257,171" href="customization-guide.html#photo-tips-4" target="" />
        <area shape="rect" alt="" title="Good Lighting" coords="5,124,29,145" href="customization-guide.html#photo-tips-4-1" target="" />
        <area shape="rect" alt="" title="Document Should be Flat" coords="4,198,27,220" href="customization-guide.html#photo-tips-4-2" target="" />
        <area shape="rect" alt="" title="Device Parallel to Document" coords="2,269,26,292" href="customization-guide.html#photo-tips-4-3" target="" />
        <area shape="rect" alt="" title="Document Aligned with Corner Guides" coords="5,344,28,367" href="customization-guide.html#photo-tips-4-4" target="" />
        <area shape="rect" alt="" title="Document with Multiple Pages" coords="5,420,29,441" href="customization-guide.html#photo-tips-4-5" target="" />
        <area shape="rect" alt="" title="Back To Camera Button" coords="81,489,116,520" href="customization-guide.html#photo-tips-5" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

    <map id="imgmap201874183930" name="imgmap201874183930">
    <area shape="rect" alt="" title="" coords="275,251,306,281" href="" target="" />
    <area shape="rect" alt="" title="" coords="5,420,29,441" href="" target="" />
    <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) --></map>

.. _photo-tips-1:

1. Action Bar
^^^^

All Action Bar customizations except the title are global to all Activities.

- **Title**

  Via the string resource named ``gc_title_photo_tips``.

- **Title Color**

  Via the color resource named ``gc_action_bar_title``.

- **Back Button Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_action_bar_back``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Background Color**

  Via the color resource named ``gc_action_bar``.

- **Status Bar Background Color**

  Via the color resource named ``gc_status_bar``.

  If you use a light background color, then you should set the ``gc_light_status_bar`` boolean
  resource to ``true``. This will cause the status bar contents to be drawn with a dark color.

:ref:`Back to screenshot. <photo-tips>`

.. _photo-tips-2:

2. Background
^^^^

- **Color**

  Via the color resource named ``gc_photo_tips_activity_background``.

:ref:`Back to screenshot. <photo-tips>`

.. _photo-tips-3:

3. Header
^^^^

- **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Help.PhotoTips.Header.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Help.PhotoTips.Header.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Help.PhotoTips.Header.TextStyle``
  (with parent style ``Root.GiniCaptureTheme.Help.PhotoTips.Header.TextStyle``) and
  setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

:ref:`Back to screenshot. <photo-tips>`

.. _photo-tips-4:

4. Tip
^^^^

- **Text Style**

    Via overriding the style named ``GiniCaptureTheme.Help.PhotoTips.Tip.TextStyle`` (with
    parent style ``Root.GiniCaptureTheme.Help.PhotoTips.Tip.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Help.PhotoTips.Tip.TextStyle``
  (with parent style ``Root.GiniCaptureTheme.Help.PhotoTips.Tip.TextStyle``) and
  setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

:ref:`Back to screenshot. <photo-tips>`

.. _photo-tips-4-1:

4.1 Good Lighting
~~~~~

- **Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_photo_tip_lighting.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

:ref:`Back to screenshot. <photo-tips>`

.. _photo-tips-4-2:

4.2 Document Should be Flat
~~~~~

- **Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_photo_tip_flat.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

:ref:`Back to screenshot. <photo-tips>`

.. _photo-tips-4-3:

4.3 Device Parallel to Document
~~~~

- **Icon**

  Via images for mdpi, hdpi, xhdpi,xxhdpi, xxxhdpi named ``gc_photo_tip_parallel.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

:ref:`Back to screenshot. <photo-tips>`

.. _photo-tips-4-4:

4.4 Document Aligned with Corner Guides
~~~~~

- **Icon**

  Via images for mdpi, hdpi, xhdpi,xxhdpi, xxxhdpi named ``gc_photo_tip_align.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

:ref:`Back to screenshot. <photo-tips>`

.. _photo-tips-4-5:

4.5 Document with Multiple Pages
~~~~~

- **Icon**

  Via images for mdpi, hdpi, xhdpi,xxhdpi, xxxhdpi named ``gc_photo_tip_multipage.png``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

:ref:`Back to screenshot. <photo-tips>`

.. _photo-tips-5:

5. Back To Camera Button
^^^^

- **Button Style**

  Via overriding the style named ``GiniCaptureTheme.Button`` (with parent style ``Root.GiniCaptureTheme.Button``).

- **Background Color**

  Via the color resource named ``gc_photo_tips_button``.

- **Text Color**

  Via the color resource named ``gc_photo_tips_button_text``.

:ref:`Back to screenshot. <photo-tips>`

.. _supported-formats:

Supported Formats Screen
----

.. raw:: html

    <img src="_static/customization/Supported Formats Screen.png" usemap="#supported-formats-map" width="324" height="576">

    <map id="supported-formats-map" name="supported-formats-map">
        <area shape="rect" alt="" title="Action Bar" coords="215,24,246,54" href="customization-guide.html#supported-formats-1" target="" />
        <area shape="rect" alt="" title="Background" coords="144,483,178,518" href="customization-guide.html#supported-formats-2" target="" />
        <area shape="rect" alt="" title="Header" coords="239,74,269,106" href="customization-guide.html#supported-formats-3" target="" />
        <area shape="rect" alt="" title="Format Info List Item" coords="278,128,307,160" href="customization-guide.html#supported-formats-4" target="" />
        <area shape="rect" alt="" title="Supported Format Icon" coords="3,117,26,138" href="customization-guide.html#supported-formats-4-1" target="" />
        <area shape="rect" alt="" title="Unsupported Format Icon" coords="2,343,27,365" href="customization-guide.html#supported-formats-4-2" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

.. _supported-formats-1:

1. Action Bar
^^^^

All Action Bar customizations except the title are global to all Activities.

- **Title**

  Via the string resource named ``gc_title_supported_formats``.

- **Title Color**

  Via the color resource named ``gc_action_bar_title``.

- **Back Button Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_action_bar_back``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Background Color**

  Via the color resource named ``gc_action_bar``.

- **Status Bar Background Color**

  Via the color resource named ``gc_status_bar``.

  If you use a light background color, then you should set the ``gc_light_status_bar`` boolean
  resource to ``true``. This will cause the status bar contents to be drawn with a dark color.

:ref:`Back to screenshot. <supported-formats>`

.. _supported-formats-2:

2. Background
^^^^

- **Color**

  Via the color resource named ``gc_supported_formats_activity_background``.

:ref:`Back to screenshot. <supported-formats>`

.. _supported-formats-3:

3. Header
^^^^

- **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Help.SupportedFormats.Item.Header.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Help.SupportedFormats.Item.Header.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Help.SupportedFormats.Item.Header.TextStyle``
  (with parent style ``Root.GiniCaptureTheme.Help.SupportedFormats.Item.Header.TextStyle``) and
  setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

:ref:`Back to screenshot. <supported-formats>`

.. _supported-formats-4:

4. Format Info List Item
^^^^

- **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Help.SupportedFormats.Item.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Help.SupportedFormats.Item.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Help.SupportedFormats.Item.TextStyle``
  (with parent style ``Root.GiniCaptureTheme.Help.SupportedFormats.Item.TextStyle``) and
  setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

- **Background Color**

  Via overriding the style named ``gc_supported_formats_item_background``.

:ref:`Back to screenshot. <supported-formats>`

.. _supported-formats-4-1:

4.1 Supported Format Icon
~~~~

- **Background Color**

  Via the color resource named ``gc_supported_formats_item_supported_icon_background``.

- **Tick Color**

  Via the color resource named ``gc_supported_formats_item_supported_icon_foreground``.

:ref:`Back to screenshot. <supported-formats>`

.. _supported-formats-4-2:

4.2 Unsupported Format Icon
~~~~

- **Background Color**

  Via the color resource named ``gc_supported_formats_item_unsupported_icon_background``.

- **Cross Color**

  Via the color resource named ``gc_supported_formats_item_unsupported_icon_foreground``.

:ref:`Back to screenshot. <supported-formats>`

.. _file-import:

File Import Screen
----

.. raw:: html

    <img src="_static/customization/File Import Screen.png" usemap="#file-import-map" width="324" height="576">

    <map id="file-import-map" name="file-import-map">
        <area shape="rect" alt="" title="Action Bar" coords="288,22,317,54" href="customization-guide.html#file-import-1" target="" />
        <area shape="rect" alt="" title="Background" coords="283,157,313,190" href="customization-guide.html#file-import-2" target="" />
        <area shape="rect" alt="" title="Header" coords="284,82,315,117" href="customization-guide.html#file-import-3" target="" />
        <area shape="rect" alt="" title="Separator Line" coords="147,143,181,178" href="customization-guide.html#file-import-4" target="" />
        <area shape="rect" alt="" title="Section" coords="259,218,292,254" href="customization-guide.html#file-import-5" target="" />
        <area shape="rect" alt="" title="Section Number" coords="38,163,62,187" href="customization-guide.html#file-import-5-1" target="" />
        <area shape="rect" alt="" title="Section Title" coords="188,209,214,235" href="customization-guide.html#file-import-5-2" target="" />
        <area shape="rect" alt="" title="Section Body" coords="13,235,33,256" href="customization-guide.html#file-import-5-3" target="" />
        <area shape="rect" alt="" title="Section Illustration" coords="83,368,110,395" href="customization-guide.html#file-import-5-4" target="" />
        <area shape="rect" alt="" title="Sections" coords="274,380,303,412" href="customization-guide.html#file-import-6" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

.. _file-import-1:

1. Action Bar
^^^^

All Action Bar customizations except the title are global to all Activities.

- **Title**

  Via the string resource named ``gc_title_file_import``.

- **Title Color**

  Via the color resource named ``gc_action_bar_title``.

- **Back Button Icon**

  Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named ``gc_action_bar_back``.
  Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

- **Background Color**

  Via the color resource named ``gc_action_bar``.

- **Status Bar Background Color**

  Via the color resource named ``gc_status_bar``.

  If you use a light background color, then you should set the ``gc_light_status_bar`` boolean
  resource to ``true``. This will cause the status bar contents to be drawn with a dark color.

:ref:`Back to screenshot. <file-import>`

.. _file-import-2:

2. Background
^^^^

- **Color**

  Via the color resource named ``gc_file_import_activity_background``.

:ref:`Back to screenshot. <file-import>`

.. _file-import-3:

3. Header
^^^^

- **Text**

  Via overriding the string resource named ``gc_file_import_header``.

- **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Help.FileImport.Header.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Help.FileImport.Header.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Help.FileImport.Header.TextStyle``
  (with parent style ``Root.GiniCaptureTheme.Help.FileImport.Header.TextStyle``) and
  setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

:ref:`Back to screenshot. <file-import>`

.. _file-import-4:

4. Separator Line
^^^^

- **Color**

  Via the color resource named ``gc_file_import_separator``.

:ref:`Back to screenshot. <file-import>`

.. _file-import-5:

5. Section
^^^^

.. _file-import-5-1:

5.1 Number
~~~~

- **Background Color**

  Via the color resource named ``gc_file_import_section_number_background``.

- **Text Color**

  Via the color resource named ``gc_file_import_section_number``.

:ref:`Back to screenshot. <file-import>`

.. _file-import-5-2:

5.2 Title
~~~~

- **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Help.FileImport.Section.Title.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Help.FileImport.Section.Title.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Help.FileImport.Section.Title.TextStyle``
  (with parent style ``Root.GiniCaptureTheme.Help.FileImport.Section.Title.TextStyle``) and
  setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

:ref:`Back to screenshot. <file-import>`

.. _file-import-5-3:

5.3 Body
~~~~

- **Text Style**

  Via overriding the style named ``GiniCaptureTheme.Help.FileImport.Section.Body.TextStyle`` (with
  parent style ``Root.GiniCaptureTheme.Help.FileImport.Section.Body.TextStyle``).

- **Font**

  Via overriding the style named ``GiniCaptureTheme.Help.FileImport.Section.Body.TextStyle``
  (with parent style ``Root.GiniCaptureTheme.Help.FileImport.Section.Body.TextStyle``) and
  setting an item named ``gcCustomFont`` with the path to the font file in your assets folder.

:ref:`Back to screenshot. <file-import>`

.. _file-import-5-4:

5.4 Illustration
~~~~~

- Image

  Via image resources as specified in the section illustrations :ref:`below <file-import-6>`.

:ref:`Back to screenshot. <file-import>`

.. _file-import-6:

6. Sections
^^^^

- **Section 1**

  - **Title**

    Via overriding the string resource named ``gc_file_import_section_1_title``.

  - **Body**

    Via overriding the string resource named ``gc_file_import_section_1_body``.
    
  - **Illustration**

    Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named
    ``gc_file_import_section_1_illustration``.
    Or via a vector drawable added to the ``drawable-anydpi`` resource folder.
    
    **Note**: For creating your custom illustration you may use `this template
    <https://github.com/gini/gini-vision-lib-assets/blob/master/Gini-Vision-Lib-Design-Elements/Illustrations/PDF/android_pdf_open_with_illustration_1.pdf>`_
    from the `Gini Vision Library UI Assets
    <https://github.com/gini/gini-vision-lib-assets>`_ repository. 

- **Section 2**

  - **Title**

    Via overriding the string resource named ``gc_file_import_section_2_title``.

  - **Body**

    Via overriding the string resource named ``gc_file_import_section_2_body``.
    
  - **Illustration**

    Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named
    ``gc_file_import_section_2_illustration``.
    Or via a vector drawable added to the ``drawable-anydpi`` resource folder.
    
    **Note**: For creating your custom illustration you may use `this template
    <https://github.com/gini/gini-vision-lib-assets/blob/master/Gini-Vision-Lib-Design-Elements/Illustrations/PDF/android_pdf_open_with_illustration_2.pdf>`_
    from the `Gini Vision Library UI Assets
    <https://github.com/gini/gini-vision-lib-assets>`_ repository. 

.. _file-import-6-3:

- **Section 3**

  - **Title**

    Via overriding the string resource named ``gc_file_import_section_3_title``.

  - **Body**

    Via overriding the string resource named ``gc_file_import_section_3_body`` and ``gc_file_import_section_3_body_2``.
    
  - **Illustration**

    Via images for mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi named
    ``gc_file_import_section_3_illustration``.
    Or via a vector drawable added to the ``drawable-anydpi`` resource folder.

    **Note**: For creating your custom illustration you may use `this template
    <https://github.com/gini/gini-vision-lib-assets/blob/master/Gini-Vision-Lib-Design-Elements/Illustrations/PDF/android_pdf_open_with_illustration_3.pdf>`_
    from the `Gini Vision Library UI Assets
    <https://github.com/gini/gini-vision-lib-assets>`_ repository. 

  - **Clear app defaults section**

    - **Title**

    Via overriding the string resource named ``gc_file_import_section_3_clear_app_defaults_title``.

    - **Body**

    Via overriding the string resource named ``gc_file_import_section_3_clear_app_defaults_body``.

:ref:`Back to screenshot. <file-import>`


Clear Defaults Dialog
----

.. raw:: html

    <img src="_static/customization/Clear Defaults Dialog.png" usemap="#clear-defaults-map" width="324" height="576">

    <map id="clear-defaults-map" name="clear-defaults-map">
        <area shape="rect" alt="" title="Message" coords="236,139,260,166" href="customization-guide.html#clear-defaults-1" target="" />
        <area shape="rect" alt="" title="File Type" coords="265,223,299,257" href="customization-guide.html#clear-defaults-1-1" target="" />
        <area shape="rect" alt="" title="Positive Button Title" coords="73,329,106,362" href="customization-guide.html#clear-defaults-2" target="" />
        <area shape="rect" alt="" title="Negative Button Title" coords="74,369,105,400" href="customization-guide.html#clear-defaults-3" target="" />
        <!-- Created by Online Image Map Editor (http://www.maschek.hu/imagemap/index) -->
    </map>

.. _clear-defaults-1:

1. Message
^^^^

Via the string resource named ``gc_file_import_default_app_dialog_message``.

.. _clear-defaults-1-1:

1.1 File Type
~~~~

- **PDF**

  Via the string resources named ``gc_file_import_default_app_dialog_pdf_file_type``.

- **Image**

  Via the string resources named ``gc_file_import_default_app_dialog_image_file_type``.

- **Document (Other)**

  Via the string resources named ``gc_file_import_default_app_dialog_document_file_type``.

.. _clear-defaults-2:

2. Positive Button Title
~~~~

Via the string resources named ``gc_file_import_default_app_dialog_positive_button``.

.. _clear-defaults-3:

3. Negative Button Title
~~~~

Via the string resources named ``gc_file_import_default_app_dialog_negative_button``.

:ref:`Back to screenshot. <file-import>`