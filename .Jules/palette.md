## 2024-05-18 - Switch accessibility in Jetpack Compose
**Learning:** Using `Modifier.toggleable` on a parent Row that contains a Switch and text allows the entire row to be clickable and screen-reader accessible as a single toggleable element. Disabling `onCheckedChange` inside the Switch itself prevents double-firing of events.
**Action:** Apply `Modifier.toggleable` with `Role.Switch` on parent rows for Switch components.
