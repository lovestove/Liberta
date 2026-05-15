## 2024-10-24 - Toggleable modifier on Rows
**Learning:** In Jetpack Compose, use `Modifier.toggleable(value = ..., role = Role.Switch, onValueChange = ...)` on the parent `Row` instead of `Modifier.clickable` for entire rows containing a switch. Set the internal `Switch` component's `onCheckedChange` to `null` to avoid double-firing events and correctly merge semantics.
**Action:** When adding clickable rows that contain switches or checkboxes, apply `Modifier.toggleable` and nullify the internal control's change listener to ensure accessible screen reader experiences.
