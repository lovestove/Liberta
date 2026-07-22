
## 2024-05-18 - Jetpack Compose Switch Rows Accessibility
**Learning:** In Jetpack Compose lists, placing a `Switch` within a `Row` alongside text causes accessibility and usability issues. The click target is too small, and info icons with `contentDescription` duplicate the visible text for screen readers.
**Action:** Always apply `Modifier.toggleable(..., role = Role.Switch)` to the parent `Row` and set `onCheckedChange = null` on the inner `Switch`. Clear the `contentDescription` on decorative info icons if the descriptive text is visually present underneath.
