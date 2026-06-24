## 2024-06-25 - Improved Switch Row Accessibility
**Learning:** Using `Modifier.clickable` or just placing a `Switch` in a `Row` limits the touch target to the small switch and fragments semantics. Applying `Modifier.toggleable` to the parent `Row` with `Role.Switch` unifies the element for screen readers and vastly improves the touch area.
**Action:** Always apply `toggleable` to the parent container of a `Switch` and set the `Switch`'s `onCheckedChange` to `null`.
