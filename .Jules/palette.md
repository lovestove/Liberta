## 2024-05-12 - Jetpack Compose Toggleable Rows
**Learning:** Using `Modifier.toggleable(value = ..., role = Role.Switch)` on a parent `Row` instead of a clickable modifier is the idiomatic approach. It automatically merges the row's semantics with the inner `Switch` (which must have `onCheckedChange = null`), providing screen readers with the correct combined state and interaction target.
**Action:** Always prefer `Modifier.toggleable` over separate `clickable` rows and `Switch` components to ensure a larger hit area with correct unified accessibility semantics.
