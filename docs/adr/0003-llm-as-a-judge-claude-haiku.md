# 0003. Integration tests are graded by Claude Haiku as LLM-as-a-judge

- Status: Accepted (recorded 2026-11-20 from the code and `CLAUDE.md`; rationale reconstructed)
- Code: `src/test/java/dev/langchain4j/carrental/utils/JudgeModelAssertions.java`,
  `src/test/java/dev/langchain4j/carrental/CustomerSupportAgentIT.java`

## Context

The agent's replies are free text from a non-deterministic model. String checks work for a few
facts ("contains Roger", "does not contain Berlin") but not for "asks the user to confirm the
cancellation" or "does not reveal the booking dates".

## Decision

- Structural facts are asserted directly: which tools ran (`ResultAssert.onlyToolWasExecuted`,
  `noToolsWereExecuted`), which RAG segments were retrieved (`retrievedSourcesContain`),
  token bounds, and the mocked `BookingService` interactions.
- Semantic conditions go to a judge: `AnthropicChatModel` `claude-haiku-4-5`, a different
  provider from the agent under test. `JudgeModelAssertions` sends the response text and numbered
  conditions and requires a JSON answer matching a schema (`SATISFIED`, `NOT_SATISFIED`,
  `NOT_SURE` per condition). Anything other than `SATISFIED` fails the test with the judge's reasoning.
- The judge sees only the response text, never the user's message. Conditions must be
  answerable from the text alone.

## Consequences

- The integration tests need a second paid key (`ANTHROPIC_API_KEY`), and each judged
  assertion is one more paid call. ADR 0004 keeps them out of the default build.
- Judge verdicts can vary between runs. A single red run is evidence, not proof: rerun the one
  test before changing the prompt.
- A condition phrased against the unseen question ("does not answer the user's question about X")
  makes the judge guess. `CLAUDE.md` carries this rule.
