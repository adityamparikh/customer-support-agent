# 0004. Tests that call paid APIs only run with `-Plive-it`

- Status: Accepted 2026-11-20
- Spec: [docs/specs/0001-live-integration-tests-opt-in.md](../specs/0001-live-integration-tests-opt-in.md)
- Code: `pom.xml` (`skipITs`, profile `live-it`), `CustomerSupportAgentIT`, `CustomerSupportAgentWiringTests`

## Context

`./mvnw install`, the documented "build and run all tests" command, ran the ten integration
tests whenever `OPENAI_API_KEY` and `ANTHROPIC_API_KEY` were exported: GPT-5.4, embeddings and
Claude Haiku calls on every build, with no prompt and no cap. Without the keys every test was
skipped and the build still reported success, so a green build proved nothing.

## Decision

- `skipITs` defaults to `true`. The `live-it` profile sets it to `false`, sets the `live-it=true`
  system property for Failsafe, and fails in `validate` unless both keys are set and non-empty
  (maven-enforcer `requireEnvironmentVariable`).
- `CustomerSupportAgentIT` also requires `-Dlive-it=true`, so running it from an IDE or with
  `-Dtest=` does not spend by accident.
- The default build gets keyless tests that exercise real code: `BookingServiceTests`,
  `BookingToolsTests`, and `CustomerSupportAgentWiringTests`, which loads the full Spring context
  with both OpenAI models replaced by offline stand-ins (`ScriptedChatModel`, `BagOfWordsEmbeddingModel`).

## Consequences

- `./mvnw install` makes no paid calls, with or without keys in the shell.
- The default build no longer checks how the real model behaves. A change to the system message,
  the `@Tool`/`@P` descriptions or the RAG settings needs `./mvnw verify -Plive-it` (or the single
  targeted test), and a human decides when to spend on it (`CLAUDE.md`, "Ask first").
- CI runs the keyless build on every pull request; the live job is manual and is meant to sit
  behind a GitHub environment with a required reviewer (`AINE-SETTINGS-TODO.md`).
