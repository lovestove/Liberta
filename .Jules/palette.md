## 2024-06-11 - Jetpack Compose Nested Interactions
**Learning:** When making a parent container `clickable` or `toggleable` for a larger touch target in Compose, if a child component (like an `IconButton`) retains its own `onClick` handler, removing its `contentDescription` to reduce screen-reader noise will cause TalkBack to announce it as an "Unlabelled button."
**Action:** Do not remove `contentDescription` from inner interactive components unless you also remove their `onClick` to make them purely decorative or merge semantics properly via `clearAndSetSemantics`.
