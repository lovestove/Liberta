## 2024-05-18 - Jetpack Compose Switch Accessibility
**Learning:** When using Jetpack Compose, clicking on a row containing a switch doesn't automatically toggle the switch or announce correctly to screen readers if implemented as a standard `clickable` modifier.
**Action:** Use `Modifier.toggleable(value = ..., role = Role.Switch, onValueChange = ...)` on the parent `Row` instead of `Modifier.clickable`. Set the internal `Switch` component's `onCheckedChange` to `null` to avoid double-firing events and correctly merge semantics.
