Customization
=============

Customization of is achieved through Android's resourcing system.
This means that all `resources of the library <https://github.com/gini/gini-health-sdk-android/tree/main/ginipaybusiness/src/main/res>`_
can be overridden by providing resources with the same name in the
application.

Material components are used so for in the ReviewFragment, so those
components will pick attributes of the hosting activity if it uses
a Material Theme.

Some attributes are set inside the library (styles that start with
``Root.``) to provide the basic UI, but you can hook into the other
styles to customize the screen further.

`See styles file <https://github.com/gini/gini-health-sdk-android/blob/main/ginipaybusiness/src/main/res/values/styles.xml>`_