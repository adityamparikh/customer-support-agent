# AINE readiness fixes

## Goal

Close the gaps the 2026-11-20 AINE readiness audit found (22 passed, 21 failed, 3 N/A), on the
branch `aine-readiness`, without changing GitHub settings or spending API credit.

## Decisions

- Paid integration tests are opt-in with `-Plive-it` (ADR 0004, spec 0001).
- Error Prone with `-Werror`, palantir-java-format through Spotless (ADR 0005).
- The `should_cancel_booking` failure is treated as an under-specified tool contract and fixed in
  the system message and `@Tool` descriptions, not in the test (spec 0002).
- GitHub settings are written as commands in `AINE-SETTINGS-TODO.md` for the owner to run.

## Done

- `.gitignore` covers `.env*`, `*.pem`, `*.key`; `.env.example` added.
- ADRs 0001 to 0005, spec template, specs 0001 and 0002.
- Live-IT opt-in profile; keyless unit and wiring tests (16 tests, 0 skipped).
- Error Prone and Spotless in the build.
- GitHub Actions CI on pull requests and pushes to `main`; manual live-IT job.
- `CLAUDE.md`: layout, ask-first list, before-you-push checklist, observability; `.claude/commands/`.

## Left

- Owner: run the commands in `AINE-SETTINGS-TODO.md` (branch protection with the `build` check
  required, `live-api` environment with a reviewer, repository secrets).
- Owner: push the branch and open a pull request, so CI runs for the first time.
- Run `./mvnw verify -Plive-it -Dit.test="CustomerSupportAgentIT#should_cancel_booking"` a few
  times and record the result in spec 0002.

## Open questions

- Enforce terms-of-use sections 4.1 and 4.2 in `BookingService.cancelBooking`? (spec 0002)
- Set spend limits on the OpenAI and Anthropic projects behind the keys used here.
