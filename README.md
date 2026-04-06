# Miles of Smiles - Customer Support Agent

An AI-powered customer support agent for a car rental company, built with [LangChain4j](https://docs.langchain4j.dev/) and Spring Boot. Based on the [langchain4j-examples/customer-support-agent-example](https://github.com/langchain4j/langchain4j-examples/tree/main/customer-support-agent-example).

The agent (named **Roger**) can look up booking details, cancel reservations, and answer policy questions using RAG over the company's terms of use.

## Prerequisites

- Java 25+
- An [OpenAI API key](https://platform.openai.com/api-keys)

## Quick Start

```bash
export OPENAI_API_KEY=sk-...

./mvnw spring-boot:run
```

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

Integration tests use an **LLM-as-a-judge** pattern with Anthropic Claude Haiku to semantically evaluate the agent's responses.

```bash
export OPENAI_API_KEY=sk-...
export ANTHROPIC_API_KEY=sk-ant-...

./mvnw install
```

Tests are skipped automatically when API keys are not set.

## Tech Stack

- **Spring Boot 3.5** with Java 25
- **LangChain4j 1.12** — AI service, tool calling, RAG, chat memory
- **OpenAI GPT-5.4** — chat model
- **OpenAI text-embedding-3-small** — embedding model for RAG
- **Anthropic Claude Haiku 4.5** — LLM-as-a-judge in tests
