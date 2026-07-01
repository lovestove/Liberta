## 2024-07-01 - Improve Switch accessibility
**Learning:** Using `Modifier.toggleable` on a parent `Row` instead of attaching `onCheckedChange` directly to a `Switch` improves accessibility by creating a larger touch target and appropriately merging semantics for screen readers without double-firing events.
**Action:** Always prefer applying `toggleable` to the container Row with `Role.Switch` and setting the inner Switch `onCheckedChange` to null.
