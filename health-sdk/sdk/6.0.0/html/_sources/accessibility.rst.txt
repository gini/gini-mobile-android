Accessibility Guide
====================

Gini is committed to building inclusive, accessible technology. Our Health SDK already largely followed the [Web Content Accessibility Guidelines (WCAG) 2.2 AAA](https://www.w3.org/TR/WCAG22/) and [Android’s accessibility best practices](https://developer.android.com/guide/topics/ui/accessibility) to support compliance with the ``Accessibility Strengthening Act (Barrierefreiheitsstärkungsgesetz - BFSG)``, taking effect in Germany on June 28, 2025.
We improved the Gini Health SDK to ensure that:
all users - including those with visual, motor, or cognitive impairments - can fully access mobile banking services.
our Health SDK is now fully compliant with (WCAG) 2.2 AAA
We’ve designed the SDK to work seamlessly with Android accessibility features, so users who rely on these tools can fully interact with your app.

* ``Key Accessibility areas currently supported``:

* ``TalkBack``: Enables users to navigate and interact with the interface without needing to see the screen.
* ``Contrast``: Text and UI elements meet contrast ratio requirements to ensure good readability.
* ``Orientation``: The SDK adapts seamlessly to both orientations - enhancing usability and delivering a consistent experience across all device layouts.
* ``Dynamic Texts``: The SDK supports system text size adjustments, so users can scale content based on their preferences.
* ``Display Customization``: We support Android system settings such as:
  - Bold Text
  - Increase Contrast
  - Reduce Transparency
  - Smart Invert
  - Differentiate Without Color
  - On/Off Labels
  - Button Shapes
  - Dark Mode
  - Reduce Motion
* ``External Keyboard Support``: Users can navigate and interact with the SDK using a hardware keyboard - improving accessibility for users with motor impairments.
* ``System Settings Compatibility``: Any accessibility preferences set at the Android level (like color filters or zoom) are respected by the SDK and automatically applied.

* ``Note on Integration``: If the Gini Health SDK is used as part of a larger view (e.g., embedded within your own `Fragment` or `ViewGroup`), please ensure that accessibility behaviors such as setting the initial focus are handled appropriately within your implementation.