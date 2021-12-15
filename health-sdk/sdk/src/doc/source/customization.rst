Customization
=============

Customization is achieved through Android's resourcing system.
This means that all `resources of the library <https://github.com/gini/gini-mobile-android/tree/main/health-sdk/sdk/src/main/res>`_
can be overridden by providing resources with the same name in the
application.

When overriding the Health SDK's
`styles <https://github.com/gini/gini-mobile-android/blob/main/health-sdk/sdk/src/main/res/values/styles.xml>`_
make sure to keep their name and parent style the same. You can see examples
of overridden styles in the
`example app <https://github.com/gini/gini-mobile-android/blob/main/health-sdk/example-app/src/main/res/values/styles.xml>`_.

Material components are used in the ReviewFragment and those components will pick attributes of the hosting activity
if it uses a material theme.

