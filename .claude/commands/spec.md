---
description: Draft a new spec in docs/specs from the template
argument-hint: "<feature or change in a few words>"
---

Draft a spec for: $ARGUMENTS

1. Read `docs/specs/TEMPLATE.md`, the existing specs in `docs/specs/` and the ADRs in `docs/adr/`.
2. Read the code the change touches, so the spec describes the real system, not a guess.
3. Create `docs/specs/NNNN-short-name.md` with the next free number, Status `Draft`, today's date.
4. Every acceptance criterion names the command or test that checks it and whether the keyless
   build (`./mvnw install`) or only `-Plive-it` checks it. Cover failure cases, non-goals and rollback.
5. Put anything that needs a human decision under Open questions and ask the user about it.
   Do not start implementing until the user accepts the spec.
