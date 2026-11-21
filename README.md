# Miles of Smiles - Customer Support Agent

An AI-powered customer support agent for a car rental company, built with [LangChain4j](https://docs.langchain4j.dev/) and Spring Boot. Based on the [langchain4j-examples/customer-support-agent-example](https://github.com/langchain4j/langchain4j-examples/tree/main/customer-support-agent-example).

The agent (named **Roger**) can look up booking details, cancel reservations, and answer policy questions using RAG over the company's terms of use.

## Prerequisites

- Java 25+
- An [OpenAI API key](https://platform.openai.com/api-keys) to run the app
- An [Anthropic API key](https://console.anthropic.com/) only for the paid live integration tests

## Quick Start

```bash
cp .env.example .env        # fill in OPENAI_API_KEY; .env is gitignored
set -a; . ./.env; set +a

./mvnw spring-boot:run
```

Startup embeds the terms of use with the OpenAI embedding model, so every start makes a few paid calls.

The app starts on `http://localhost:8080`.

## Usage

Chat with the agent via the `/chat` endpoint:

```
GET /chat?sessionId=1&userMessage=Hi, I am John Doe. When does my booking MS-777 start?
```

The `sessionId` parameter maintains conversation context across requests, enabling multi-turn interactions like booking cancellation flows that require confirmation.

## What the Agent Can Do

- **Look up bookings** — retrieves start/end dates given a booking number and customer name
- **Cancel bookings** — asks for explicit confirmation before cancelling
- **Answer policy questions** — uses RAG to answer questions about cancellation policies, vehicle use restrictions, etc.
- **Stay on topic** — politely declines questions outside the scope of Miles of Smiles

## Running Tests

```bash
./mvnw install
```

This needs no API keys and makes no paid calls. It compiles with Error Prone (`-Werror`), runs the
keyless unit and wiring tests, and checks formatting (fix with `./mvnw spotless:apply`). The wiring
tests load the real Spring context with the OpenAI models replaced by offline stand-ins.

The live integration tests talk to the real agent and use an **LLM-as-a-judge** pattern with
Anthropic Claude Haiku to evaluate its replies. They spend OpenAI and Anthropic credit, so they only
run with the `live-it` profile, which fails fast if either key is missing:

```bash
set -a; . ./.env; set +a    # OPENAI_API_KEY and ANTHROPIC_API_KEY

./mvnw verify -Plive-it
./mvnw verify -Plive-it -Dit.test="CustomerSupportAgentIT#should_cancel_booking"
```

CI (`.github/workflows/ci.yml`) runs `./mvnw install` on every pull request; the live tests are a manual job.

## Project Docs

- `CLAUDE.md`: commands, layout, rules and the ask-first list for coding agents
- `docs/adr/`: why the system is built this way
- `docs/specs/`: specs with acceptance criteria (template in `docs/specs/TEMPLATE.md`)
- `docs/plans/`: state of work that spans sessions

## Tech Stack

- **Spring Boot 3.5** with Java 25
- **LangChain4j 1.12** — AI service, tool calling, RAG, chat memory
- **OpenAI GPT-5.4** — chat model
- **OpenAI text-embedding-3-small** — embedding model for RAG
- **Anthropic Claude Haiku 4.5** — LLM-as-a-judge in tests
