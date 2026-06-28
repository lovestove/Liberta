## 2024-05-18 - Jetpack Compose Switch Accessibility
**Learning:** In Jetpack Compose, clicking the row containing a Switch doesn't trigger it if the click area is only the Switch itself. Using `Modifier.toggleable` on the parent `Row` with `Role.Switch` improves accessibility by making the whole row interactable. The internal `Switch` should have `onCheckedChange = null` to prevent double firing.
**Action:** Always wrap `Switch` components in a `Row` with `Modifier.toggleable` for better UX and a11y.
