# Module default-network

## Gini Capture SDK - Default Network Library for Android

The Default Network Library for Android provides a default implementation of
the network related tasks required by the Gini Capture SDK.

Adding this library along with the Gini Capture SDK to your application is the quickest way to
integrate invoice scanning.

In order for the Gini Capture SDK to use the default implementations pass the instances of
`GiniCaptureNetworkService` and `GiniCaptureNetworkApi` to the `GiniCapture.Builder` when
creating a new `GiniCapture`.