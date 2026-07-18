## 2026-07-18 - Toggleable Rows for Switches
**Learning:** Making only the Switch component toggleable creates a small touch target. By using Modifier.toggleable on the parent Row and setting the Switch's onCheckedChange to null, the entire row becomes interactive and semantics are merged correctly.
**Action:** Use Modifier.toggleable on the parent container instead of just the Switch.
