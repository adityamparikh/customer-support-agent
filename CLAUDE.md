# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build and run all tests (unit + integration)
./mvnw install

# Compile only (no tests)
./mvnw compile test-compile

# Run unit tests only (Surefire)
./mvnw test

# Run integration tests only (Failsafe)
./mvnw failsafe:integration-test failsafe:verify

# Run a single integration test method
./mvnw failsafe:integration-test -Dit.test="CustomerSupportAgentIT#should_provide_booking_details_for_existing_booking"
```

**Required environment variables for tests:**
- `OPENAI_API_KEY` — used by the agent (GPT-5.4) and embedding model
- `ANTHROPIC_API_KEY` — used by the LLM-as-a-judge (Claude Haiku) in integration tests

Tests are annotated with `@EnabledIfEnvironmentVariable` and skip gracefully when keys are absent.

## Architecture

This is a **LangChain4j + Spring Boot** customer support agent for a fictional car rental company called "Miles of Smiles." The agent (named Roger) handles booking inquiries, cancellations, and policy questions via a REST API.

### Core flow

`CustomerSupportAgentController` (REST `GET /chat`) → `CustomerSupportAgent` (LangChain4j `@AiService`) → OpenAI GPT-5.4

The `@AiService` uses **explicit wiring** (`AiServiceWiringMode.EXPLICIT`) to avoid bean conflicts when multiple `ChatModel` beans exist (e.g., judge model in tests). It wires four collaborators by bean name: `openAiChatModel`, `chatMemoryProvider`, `contentRetriever`, `bookingTools`.

### Tool calling

`BookingTools` exposes two `@Tool` methods (`getBookingDetails`, `cancelBooking`) that delegate to `BookingService`. The system prompt enforces rules: verify customer identity before tool calls, require explicit confirmation before cancellation.

### RAG pipeline

`RagConfig` ingests `miles-of-smiles-terms-of-use.txt` on `ApplicationReadyEvent`. Documents are split (100 tokens, 0 overlap), embedded with OpenAI `text-embedding-3-small`, and stored in an in-memory `EmbeddingStore`. The content retriever returns max 1 result with a 0.6 minimum score threshold.

### Conversation memory

`ChatMemoryConfig` provides a `MessageWindowChatMemory` (20-message window) keyed by session/memory ID for multi-turn conversations.

## Testing approach

Integration tests (`CustomerSupportAgentIT`) use **LLM-as-a-judge** with Anthropic Claude Haiku to semantically evaluate agent responses. Key utilities:

- `JudgeModelAssertions` — sends response text + conditions to judge model, expects structured JSON assessment (SATISFIED/NOT_SATISFIED/NOT_SURE)
- `ResultAssert` — AssertJ extension for verifying tool executions (`onlyToolWasExecuted`, `noToolsWereExecuted`) and RAG sources (`retrievedSourcesContain`)

**Important:** Judge conditions must be evaluable from the response text alone — the judge does NOT see the original user message. Write conditions that describe what the text *says*, not what it *doesn't do* in response to an unseen question.

The `BookingService` is `@MockitoBean`-replaced in integration tests. Tests are not parallelizable due to shared mock state.

Unit tests (`*Tests.java`) run via Surefire; integration tests (`*IT.java`) run via Failsafe.
