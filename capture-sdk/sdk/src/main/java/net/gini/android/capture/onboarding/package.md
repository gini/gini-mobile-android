# Package net.gini.android.capture.onboarding

Contains the Activity and Fragments used for the Onboarding Screen.

## Screen API

The [net.gini.android.capture.onboarding.OnboardingActivity] is launched directly by the [net.gini.android.capture.camera.CameraActivity]. It
can be configured by overriding Gini Capture SDK app resources and/or by supplying the [net.gini.android.capture.camera.CameraActivity]
with an [java.util.ArrayList] of [net.gini.android.capture.onboarding.OnboardingPage] objects in the
[net.gini.android.capture.camera.CameraActivity.EXTRA_IN_ONBOARDING_PAGES] extra.

## Component API

To use the Component API you have to include the [net.gini.android.capture.onboarding.OnboardingFragmentCompat] 
in an Activity in your app (a dedicated Activity is recommended). To receive events from the Fragments 
your Activity must implement the [net.gini.android.capture.onboarding.OnboardingFragmentListener] interface.

The Onboarding Screen was designed as a semi-transparent overlay for the camera preview. Your Activity should be configured to be
transparent and the Camera Fragment should not stop the camera when the onboarding is shown.


