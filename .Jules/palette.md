
## 2024-05-24 - Accessible Switches in Jetpack Compose
**Learning:** In Jetpack Compose, wrapping a Switch inside a clickable Row without special handling creates poor accessibility. Screen readers don't announce it correctly as a switch, and it can result in double events.
**Action:** Always use `Modifier.toggleable(value = ..., role = Role.Switch, onValueChange = ...)` on the parent Row/container instead of `Modifier.clickable`. Also, set the internal Switch component's `onCheckedChange` to `null` to avoid double-firing events and properly merge semantics.
