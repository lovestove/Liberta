## 2024-05-18 - Jetpack Compose Switch Accessibility
**Learning:** For Jetpack Compose UI accessibility, use `Modifier.toggleable` on the parent `Row` instead of `Modifier.clickable` for entire rows containing a switch. Set the internal `Switch` component`s `onCheckedChange` to `null` to avoid double-firing events and correctly merge semantics.
**Action:** Applied this pattern to `ToggleLine` in `LibertaUi.kt` to improve screen reader experience and interaction surface.
