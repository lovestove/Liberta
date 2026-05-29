## 2026-05-29 - [Make switch rows toggleable]
**Learning:** Entire rows containing a Switch should be explicitly marked with `toggleable` and `role = Role.Switch` to ensure correct semantics and interaction.
**Action:** Next time, use `toggleable` on the parent layout and set the inner Switch `onCheckedChange` to `null` instead of using `clickable`.
