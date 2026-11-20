# 0001. Live integration tests are opt-in

- Status: Implemented
- Date: 2026-11-20
- Related: [ADR 0004](../adr/0004-paid-tests-are-opt-in.md)

## Goal

`./mvnw install` must never spend OpenAI or Anthropic credit, and a green default build must
mean that real tests ran. The ten `CustomerSupportAgentIT` tests stay available, behind an explicit opt-in.

## Acceptance criteria

1. `env -u OPENAI_API_KEY -u ANTHROPIC_API_KEY ./mvnw install` exits 0. Surefire reports at least
   16 tests run, 0 skipped; Failsafe logs `Tests are skipped.` (default build)
2. With both keys exported, `./mvnw install` still makes no call to `api.openai.com` or
   `api.anthropic.com`: Failsafe is skipped, and `CustomerSupportAgentWiringTests` replaces the
   `openAiChatModel` and `EmbeddingModel` beans with `@TestBean` offline stand-ins and sets
   `OPENAI_API_KEY=unused-offline-test-key` as a test property, which wins over the environment.
3. `./mvnw verify -Plive-it` with `OPENAI_API_KEY` or `ANTHROPIC_API_KEY` unset or empty fails in
   the `validate` phase with `-Plive-it needs OPENAI_API_KEY (agent and embeddings).` or
   `-Plive-it needs ANTHROPIC_API_KEY (LLM-as-a-judge).`, before anything compiles.
4. `./mvnw verify -Plive-it` with both keys runs all ten `CustomerSupportAgentIT` tests through
   Failsafe. (`-Plive-it` only; spends credit)
5. `CustomerSupportAgentIT` run without the `live-it=true` system property (an IDE run, or
   `./mvnw test -Dtest=CustomerSupportAgentIT`) is skipped by `@EnabledIfSystemProperty`.
6. The keyless tests assert behaviour of real code: booking lookup, ownership and cancellation
   rules (`BookingServiceTests`), the tool specification the model receives (`BookingToolsTests`),
   and the agent's tool calls, memory and RAG ingestion through the real Spring context
   (`CustomerSupportAgentWiringTests`).

## Failure cases

- A key is exported but empty: the enforcer rule uses `<regex>.+</regex>`, so criterion 3 applies.
- `-DskipITs=false` without the profile: Failsafe runs, but `CustomerSupportAgentIT` is skipped
  by criterion 5, so nothing is spent.
- A future test class that needs a live key and is named `*Tests`: it would run in the default
  build. Paid tests must be `*IT` and carry `@EnabledIfSystemProperty(named = "live-it", matches = "true")`;
  `CLAUDE.md` states the rule.

## Non-goals

- A spend cap. Limits are set on the OpenAI and Anthropic projects that own the keys, not in this repo.
- Approval for the CI live job. That needs a GitHub environment with a required reviewer (`AINE-SETTINGS-TODO.md`).
- Making the integration tests deterministic. They grade a real model and can vary between runs.

## Rollback

Revert the commit that introduced the `live-it` profile. A revert brings back the old behaviour,
where `./mvnw install` spends whenever the keys are exported, and removes the keyless tests.
For a one-off live run nothing needs reverting: use `-Plive-it`.
