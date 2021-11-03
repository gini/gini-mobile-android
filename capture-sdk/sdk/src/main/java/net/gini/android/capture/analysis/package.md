# Package net.gini.android.capture.analysis

Contains the Activity and Fragments used for the Analysis Screen.

## Screen API

The preferred way of adding network calls to the Gini Capture
SDK is by creating a [net.gini.android.capture.GiniCapture] instance with a [net.gini.android.capture.network.GiniCaptureNetworkService] and
a [net.gini.android.capture.network.GiniCaptureNetworkApi] implementation.

## Component API

To use the Component API you have to include the [net.gini.android.capture.analysis.AnalysisFragmentCompat] 
in an Activity in your app (a dedicated Activity is recommended). To receive events from the Fragments 
your Activity must implement the [net.gini.android.capture.analysis.AnalysisFragmentListener] interface.
