## 2024-05-24 - Accessible Switch Rows
**Learning:** Using `Modifier.clickable` on a row that contains a `Switch` causes redundant announcements, and leaves the switch's hit target too small if users try to tap it directly.
**Action:** Use `Modifier.toggleable(value = ..., role = Role.Switch, onValueChange = ...)` on the parent `Row` and set the internal `Switch`'s `onCheckedChange` to `null`. This expands the touch target to the entire row and correctly merges semantics for screen readers.
