
## 2026-05-11 - Clickable Row Accessibility
**Learning:** Making entire rows clickable in Jetpack Compose instead of just the switch improves UX. For rows containing a switch, `Modifier.toggleable` is the idiomatic approach as it automatically handles correct accessibility semantics and merges row and switch states.
**Action:** Always add `.toggleable(value = checked, role = Role.Switch, onValueChange = onChecked)` to the row container for toggleable settings instead of relying solely on the Switch component or using `.clickable`.
