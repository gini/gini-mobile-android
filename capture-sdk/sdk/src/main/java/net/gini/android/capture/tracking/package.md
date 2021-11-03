# Package net.gini.android.capture.tracking

Contains the interface, enums and classes used for tracking various events which occur during the usage of the Gini Capture SDK.

Implement the [net.gini.android.capture.tracking.EventTracker] interface and pass it to the
[net.gini.android.capture.GiniCapture.Builder.setEventTracker(net.gini.android.capture.tracking.EventTracker)] when creating a new GiniCapture
instance.

Check each screens event enum to see which events are triggered.

