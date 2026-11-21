# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Every check, no API keys needed and no paid calls: Error Prone (-Werror), keyless tests, Spotless check
./mvnw install

# Fix formatting (palantir-java-format) before committing
./mvnw spotless:apply

# Compile only (Error Prone runs here too)
./mvnw compile test-compile

# Unit and wiring tests only (Surefire, *Tests.java)
./mvnw test

# PAID: live integration tests against OpenAI + Anthropic (Failsafe, *IT.java). Ask first, see below.
./mvnw verify -Plive-it

# PAID: a single live integration test method
./mvnw verify -Plive-it -Dit.test="CustomerSupportAgentIT#should_provide_booking_details_for_existing_booking"
```

`-Plive-it` fails in `validate` unless both keys are set and non-empty:
- `OPENAI_API_KEY`: the agent (GPT-5.4) and the embedding model. Also needed by `./mvnw spring-boot:run`.
- `ANTHROPIC_API_KEY`: the LLM-as-a-judge (Claude Haiku) in `CustomerSupportAgentIT`.

Copy `.env.example` to `.env` (gitignored) and load it with `set -a; . ./.env; set +a`.
Why the integration tests are opt-in: `docs/adr/0004-paid-tests-are-opt-in.md`.

## Layout

```
src/main/java/dev/langchain4j/carrental/
  CustomerServiceAgentApplication.java   Spring Boot entry point
  CustomerSupportAgent.java              @AiService interface + system message (the agent's rules)
  CustomerSupportAgentController.java    GET /chat?sessionId=&userMessage=
  booking/                               Booking record, BookingService (in-memory sample data),
                                         BookingTools (@Tool methods the model calls), BookingNotFoundException
  config/                                ChatMemoryConfig, RagConfig (ingestion), ObservabilityConfig
  customer/                              Customer record
src/main/resources/
  application.properties                 model names, RAG thresholds, logging, actuator exposure
  miles-of-smiles-terms-of-use.txt       the only RAG source
src/test/java/dev/langchain4j/carrental/
  CustomerSupportAgentWiringTests.java   keyless: full Spring context, OpenAI models replaced offline
  CustomerSupportAgentIT.java            PAID: live agent + judge, runs only with -Plive-it
  booking/                               BookingServiceTests, BookingToolsTests (keyless)
  utils/                                 JudgeModelAssertions, ResultAssert, ScriptedChatModel,
                                         BagOfWordsEmbeddingModel
docs/adr/      why the system is built this way (index in docs/adr/README.md)
docs/specs/    specs with acceptance criteria; start new ones from docs/specs/TEMPLATE.md
docs/plans/    state of multi-session work (convention in docs/plans/README.md)
.claude/commands/  /review, /live-it, /spec
.github/       CI workflow (ci.yml), Dependabot, PR template
generated-requests.http   IntelliJ HTTP client script for the multi-turn /chat cancellation flow (PAID when run)
rag-comparison.md         Easy vs Naive vs Advanced RAG in LangChain4j; this repo uses Naive (ADR 0001)
AINE-SETTINGS-TODO.md     GitHub settings the owner still has to apply
```

`target/` is build output, never edit it. `.run/` (IntelliJ run configs) and `.env` hold API keys and must never be committed; `.run/` was added to `.gitignore` after a near-leak (commit 4b27148).

## Architecture

This is a **LangChain4j + Spring Boot** customer support agent for a fictional car rental company called "Miles of Smiles." The agent (named Roger) handles booking inquiries, cancellations, and policy questions via a REST API.

### Core flow

`CustomerSupportAgentController` (REST `GET /chat`) → `CustomerSupportAgent` (LangChain4j `@AiService`) → OpenAI GPT-5.4

The `@AiService` uses **explicit wiring** (`AiServiceWiringMode.EXPLICIT`) to avoid bean conflicts when multiple `ChatModel` beans exist (e.g., judge model in tests). It wires four collaborators by bean name: `openAiChatModel`, `chatMemoryProvider`, `contentRetriever`, `bookingTools` (ADR 0002). A new tool class must be added to `tools` by name.

### Tool calling

`BookingTools` exposes two `@Tool` methods (`getBookingDetails`, `cancelBooking`) that delegate to `BookingService`. The `@Tool` and `@P` descriptions are sent to the model, so they are behaviour, like the system message: rule 1 requires first name, last name and booking number before any tool call; rule 2 requires a `getBookingDetails` lookup, then explicit confirmation, before `cancelBooking`. The cancellation flow and its tests: `docs/specs/0002-booking-cancellation-flow.md`.

`BookingService` is in-memory sample data: one booking, `MS-777` for John Doe, starting 23 days after startup. A wrong name gives the same `BookingNotFoundException` as a wrong number, on purpose.

### RAG pipeline

`RagConfig` ingests `miles-of-smiles-terms-of-use.txt` on `ApplicationReadyEvent` (paid embedding calls on every start). Documents are split (100 tokens, 0 overlap), embedded with OpenAI `text-embedding-3-small`, and stored in an in-memory `EmbeddingStore`. The content retriever returns max 1 result with a 0.6 minimum score threshold (ADR 0001).

### Conversation memory

`ChatMemoryConfig` provides a `MessageWindowChatMemory` (20-message window) keyed by session/memory ID for multi-turn conversations.

## Testing approach

**Keyless tests** (`*Tests.java`, Surefire, run by `./mvnw install`): `CustomerSupportAgentWiringTests` loads the real Spring context with `@TestBean` replacing `openAiChatModel` with `ScriptedChatModel` (scripted replies, records every request) and the `EmbeddingModel` with `BagOfWordsEmbeddingModel`. They test how the app drives the model, not how the model behaves. A test that needs a live key must be a `*IT` with `@EnabledIfSystemProperty(named = "live-it", matches = "true")`, never a `*Tests`.

**Live integration tests** (`CustomerSupportAgentIT`, Failsafe, `-Plive-it` only) use **LLM-as-a-judge** with Anthropic Claude Haiku (ADR 0003):

- `JudgeModelAssertions` — sends response text + conditions to judge model, expects structured JSON assessment (SATISFIED/NOT_SATISFIED/NOT_SURE)
- `ResultAssert` — AssertJ extension for verifying tool executions (`onlyToolWasExecuted`, `noToolsWereExecuted`) and RAG sources (`retrievedSourcesContain`)

**Important:** Judge conditions must be evaluable from the response text alone — the judge does NOT see the original user message. Write conditions that describe what the text *says*, not what it *doesn't do* in response to an unseen question.

The `BookingService` is `@MockitoBean`-replaced in integration tests. Tests are not parallelizable due to shared mock state. Live results vary between runs: rerun one failing test before changing a prompt.

## Static analysis and formatting

Error Prone runs inside javac with `-Xlint:all` and `-Werror`: any warning fails the build, and the baseline is zero. Fix findings rather than suppressing them; if a suppression is unavoidable use `@SuppressWarnings("CheckName")` with a comment. Spotless (palantir-java-format) fails `./mvnw install` on unformatted code; run `./mvnw spotless:apply`. ADR 0005.

## Ask first

Do not do these without the user saying yes in this session:

- `git push` to any remote. `origin` is the public `github.com/adityamparikh/customer-support-agent`, and until `AINE-SETTINGS-TODO.md` is applied `main` is unprotected, so a push lands immediately.
- Rewriting history: `git push --force`, `git filter-branch`, `git rebase` or `git reset --hard` on commits that are already pushed, deleting branches or tags on the remote.
- Anything that spends API credit: `-Plive-it` (each full run is about 10 agent conversations plus judge calls), `./mvnw spring-boot:run` (embeds the terms of use at startup), calling `/chat`, running `generated-requests.http`, or dispatching the CI `live-it` job. A single targeted `-Dit.test=` run is the cheapest way to check a prompt change; say which test and why before running it.
- `gh` commands that change the repo or its settings (`gh api -X PUT/POST/PATCH/DELETE`, `gh repo edit`, `gh secret set`).
- Changing a decision recorded in `docs/adr/` (write a new ADR instead).

## Before you push

1. `./mvnw install` exits 0 (Error Prone, 16+ keyless tests with 0 skipped, Spotless).
2. If the system message, a `@Tool`/`@P` description or a RAG setting changed: the affected live test passed with `-Plive-it` (ask first), and the result is in the PR.
3. `git diff --cached --stat` shows no `.env`, `.run/`, `*.pem`, `*.key` or other credential.
4. `CLAUDE.md`, `README.md`, the relevant spec or ADR are updated if behaviour, commands or layout changed.
5. Changes reach `main` through a pull request; the CI `build` job must be green. `.github/pull_request_template.md` repeats this list.

## Observability

- Actuator exposes `health` and `metrics` on the app port: `curl localhost:8080/actuator/health`, `curl localhost:8080/actuator/metrics`.
- `ObservabilityConfig` registers LangChain4j's `ObservationChatModelListener` on the OpenAI chat model. It records `gen_ai.client.operation.duration` (per call, tagged with model and outcome) and `gen_ai.client.token.usage` (tagged `gen_ai.token.type` = input/output); read them at `/actuator/metrics/gen_ai.client.token.usage` after the first `/chat` call.
- Logs go to the console only. `logging.level.dev.langchain4j=DEBUG` with `log-requests`/`log-responses` prints every full prompt and response, including customer names and booking numbers.
- There is no deployment, no log shipping and no alerting.

## Work in progress

For anything that spans sessions, keep a plan in `docs/plans/` (Goal, Decisions, Done, Left, Open questions) and update it before stopping. New features start as a spec in `docs/specs/` from `TEMPLATE.md`; reference the spec in the commit message.
