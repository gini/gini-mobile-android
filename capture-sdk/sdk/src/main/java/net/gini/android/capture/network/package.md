# Package net.gini.android.capture.network

Contains interfaces and classes for adding networking calls to the Gini Capture SDK in order to communicate with the Gini API.

The Gini Capture SDK uses the [net.gini.android.capture.network.GiniCaptureNetworkService] interface to request network calls when
required. By implementing the interface and passing it to the [net.gini.android.capture.GiniCapture.Builder.setGiniCaptureNetworkService()]
when creating the [net.gini.android.capture.GiniCapture] instance clients are free to use any networking implementation that fits their needs.

The [net.gini.android.capture.network.GiniCaptureNetworkApi] can be implemented and used to perform network calls manually outside of the Gini
Capture SDK (e.g. for sending feedback).

The easiest way to get started is to use the Gini Capture Network Library package which provides a default implementation of both interfaces.