![Gini Health SDK for Android](./logo.png)

Gini Health SDK for Android
===========================

// TODO: update documentation to align with the style of capture sdk (https://github.com/gini/gini-capture-sdk-android/blob/main/README.md)
// and bank sdk (https://github.com/gini/gini-pay-bank-sdk-android/blob/main/README.md)

Installation
------------

To install add our Maven repo to the root build.gradle file and add it as a dependency to your app
module's build.gradle.

build.gradle:

```
repositories {
    maven {
        url 'https://repo.gini.net/nexus/content/repositories/open
    }
}
```

app/build.gradle:

```
dependencies {
    implementation 'net.gini:gini-pay-business-sdk:1.0.5'
}
```

Example Apps
---

### Health

The health example app is in the `:health-sdk:example-app` module.
It needs `health-sdk/example-app/local.properties` with credentials:
```
clientId=*******
clientSecret=*******
```

### Bank

In order to pass the Requirements a bank app needs to be installed on the device.

An example bank app is available in the Gini Pay Bank SDK's
[repository](https://github.com/gini/gini-pay-bank-sdk-android) called
[`appcreenapi`](https://github.com/gini/gini-pay-bank-sdk-android/tree/main/appscreenapi).

## License

Gini Health SDK is available under a commercial license.
See the LICENSE file for more info.
