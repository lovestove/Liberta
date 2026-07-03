## 2024-07-03 - Toggleable Rows for Switches
**Learning:** In Jetpack Compose, putting a Switch inside a Row can cause poor accessibility and usability if the Row isn't toggleable. Screen readers won't merge semantics, and tapping the text won't toggle the switch.
**Action:** Use `Modifier.toggleable(value = ..., role = Role.Switch, onValueChange = ...)` on the parent `Row` and set the internal `Switch`'s `onCheckedChange` to `null` to avoid double-firing events and correctly merge semantics.
