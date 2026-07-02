## 2024-05-24 - Accessible Switch Rows
**Learning:** Using `Modifier.clickable` on a `Row` containing a `Switch` creates duplicate announcements for screen readers and splits the focus area.
**Action:** Use `Modifier.toggleable(value = ..., role = Role.Switch, onValueChange = ...)` on the parent `Row` instead. Set the internal `Switch` component's `onCheckedChange` to `null` to avoid double-firing events and correctly merge semantics.
