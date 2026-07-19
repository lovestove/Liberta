## 2026-07-19 - Toggleable modifier on Rows with Switch
**Learning:** In Jetpack Compose, using `Modifier.clickable` on a Row containing a Switch can lead to double-firing events and unmerged semantics. Screen readers might announce the row and the switch separately.
**Action:** Use `Modifier.toggleable(value = ..., role = Role.Switch, onValueChange = ...)` on the parent Row instead of `Modifier.clickable`, and set the internal Switch component's `onCheckedChange` to `null`. This correctly merges semantics and prevents double-firing events.
