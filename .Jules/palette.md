## 2024-05-24 - Accessible Switch Toggles in Compose
**Learning:** When using `Switch` components within a larger row (e.g., a setting item with text), relying on the `Switch` itself for interaction creates a small tap target and fragmented screen reader experience.
**Action:** Always apply `Modifier.toggleable` with `role = Role.Switch` to the parent container (like `Row`) to increase the touch target and provide unified semantics. Set the internal `Switch`'s `onCheckedChange` to `null` to avoid event duplication and semantic conflicts.
