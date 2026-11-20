# 0002. `@AiService` uses explicit wiring by bean name

- Status: Accepted (recorded 2026-11-20 from the code and `CLAUDE.md`)
- Code: `src/main/java/dev/langchain4j/carrental/CustomerSupportAgent.java`

## Context

The LangChain4j Spring starter can wire an `@AiService` automatically, picking up the single
`ChatModel`, `ChatMemoryProvider`, `ContentRetriever` and every `@Tool` bean in the context.
Automatic wiring breaks as soon as there are two `ChatModel` beans. `CustomerSupportAgentIT`
adds a second one, the Anthropic `judgeModel`, and the keyless tests replace the OpenAI model
with a scripted one.

## Decision

`CustomerSupportAgent` uses `wiringMode = AiServiceWiringMode.EXPLICIT` and names its
collaborators: `chatModel = "openAiChatModel"`, `chatMemoryProvider = "chatMemoryProvider"`,
`contentRetriever = "contentRetriever"`, `tools = "bookingTools"`.

## Consequences

- Adding a `ChatModel` bean (a judge, a cheaper model for something else) cannot silently
  change which model the agent talks to.
- The bean names are a contract. Renaming `ChatMemoryConfig.chatMemoryProvider()` or
  `BookingTools`, or switching the OpenAI starter (whose bean is `openAiChatModel`), breaks the
  agent at startup. `CustomerSupportAgentWiringTests` loads the real context and fails first.
- A new tool class must be added to `tools = {...}` by name; annotating it `@Component` is not enough.
