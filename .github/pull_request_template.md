## What and why

<!-- What changed, and the spec (docs/specs/), ADR (docs/adr/) or issue it comes from. -->

## Checks

- [ ] `./mvnw install` passes locally (Error Prone, keyless tests, Spotless); CI `build` is green.
- [ ] Changed the system message, a `@Tool`/`@P` description or the RAG settings? Ran
      `./mvnw verify -Plive-it` (or the affected `-Dit.test=...`) and noted the result here.
- [ ] No `.env`, `.run/`, key or other credential staged (`git diff --cached --stat`).
- [ ] `CLAUDE.md`, `README.md`, specs or ADRs updated if behaviour, commands or layout changed.
