---
name: android-accessibility
description: Expert checklist and prompts for auditing and fixing Android accessibility issues, especially in Jetpack Compose.
---

# Android Accessibility Checklist

## Instructions

Analyze the provided component or screen for the following accessibility aspects.

### 1. Content Descriptions
*   **Check**: Do `Image` and `Icon` composables have a meaningful `contentDescription`?
*   **Decorative**: If an image is purely decorative, use `contentDescription = null`.
*   **Actionable**: If an element is clickable, the description should describe the *action* (e.g., "Play music"), not the icon (e.g., "Triangle").

### 2. Touch Target Size
*   **Standard**: Minimum **48x48dp** for all interactive elements.
*   **Fix**: Use `MinTouchTargetSize` or wrap in `Box` with appropriate padding if the visual icon is smaller.

### 3. Color Contrast
*   **Standard**: WCAG AA requires **4.5:1** for normal text and **3.0:1** for large text/icons.
*   **Tool**: Verify colors against backgrounds using contrast logic.

### 4. Focus & Semantics
*   **Focus Order**: Ensure keyboard/screen-reader focus moves logically (e.g., Top-Start to Bottom-End).
*   **Grouping**: Use `Modifier.semantics(mergeDescendants = true)` for complex items (like a row with text and icon) so they are announced as a single item.
*   **State descriptions**: Use `stateDescription` to describe custom states (e.g., "Selected", "Checked") if standard semantics aren't enough.

### 5. Headings
*   **Traversal**: Mark title texts with `Modifier.semantics { heading() }` to allow screen reader users to jump between sections.

### 6. TalkBack Support
*   **Live Regions**: Use `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` on dynamic content (e.g., status messages, counters) so TalkBack announces changes automatically without losing focus.
*   **Custom Actions**: Use `Modifier.semantics { customActions = listOf(CustomAccessibilityAction("Delete item") { ... }) }` to expose swipe or long-press actions to TalkBack users via its actions menu.
*   **Click Labels**: Use `Modifier.semantics { onClick(label = "Add to favourites") { ... } }` to give TalkBack a meaningful verb instead of the generic "double-tap to activate".
*   **Invisible Elements**: Use `Modifier.semantics { invisibleToUser() }` to hide purely decorative or redundant elements from the TalkBack focus ring.
*   **Traversal Order**: Use `isTraversalGroup = true` and `traversalIndex` inside `Modifier.semantics { }` to control the order in which TalkBack reads items in complex layouts (e.g., overlapping composables, bottom sheets).
*   **Role Announcement**: Assign an explicit `Role` (e.g., `Role.Button`, `Role.Switch`, `Role.Tab`) via `Modifier.semantics { role = Role.Button }` so TalkBack announces the correct element type after the label.
*   **Testing with TalkBack**: Enable TalkBack on a device/emulator and navigate the screen using swipe gestures. Verify every interactive element is reachable, its announcement is meaningful, and state changes (loading, error, success) are announced.

## Example Prompts for Agent Usage
*   "Analyze the content description of this Image. Is it appropriate?"
*   "Check if the touch target size of this button is at least 48dp."
*   "Does this custom toggle button report its 'Checked' state to TalkBack?"
*   "Add a live region to this error message so TalkBack announces it automatically."
*   "This icon button only says 'double-tap to activate' in TalkBack — add a click label to fix it."
*   "Ensure TalkBack traversal order on this bottom sheet follows a logical top-to-bottom flow."
*   "Add a custom TalkBack action for the swipe-to-dismiss gesture on this list item."
