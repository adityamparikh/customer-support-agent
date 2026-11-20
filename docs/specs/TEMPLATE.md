# NNNN. <Feature or change, in a few words>

- Status: Draft | Accepted | Implemented | Abandoned
- Date: YYYY-MM-DD
- Related: ADRs, issues, plans in `docs/plans/`

## Goal

What changes for the user or the developer, in two or three sentences, and why now.

## Acceptance criteria

Each one checkable by a command or a named test, with the expected result. Write
"`./mvnw install` exits 0 and Surefire reports 0 skipped", not "the build works". Say for each
whether the default keyless build checks it or only `-Plive-it` does.

1. ...

## Failure cases

What happens when a step fails: a tool throws, a key is missing, the model ignores the rule,
an input is malformed. Each case gets the expected behaviour and the test that pins it.

## Non-goals

What this change deliberately does not do, so an agent knows where to stop.

## Rollback

How to undo it: which commit to revert, which setting to flip, and anything a revert does not undo.

## Open questions

Decisions that still need a human. Remove the section when it is empty.
