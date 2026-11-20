# Architecture Decision Records

One file per decision, numbered in order: `NNNN-short-title.md`, with the sections
Status, Context, Decision, Consequences. A decision that replaces an earlier one gets a new
number and marks the old one `Superseded by NNNN`; old records are not rewritten.

ADRs 0001 to 0003 were recorded on 2026-11-20 from decisions already visible in the code.
Their rationale is reconstructed from the code, `rag-comparison.md` and `CLAUDE.md`, and says so.

| ADR | Decision | Status |
|---|---|---|
| [0001](0001-naive-rag-in-memory-store.md) | Naive RAG: OpenAI `text-embedding-3-small`, in-memory store, 1 result | Accepted |
| [0002](0002-explicit-aiservice-wiring.md) | `@AiService` with explicit wiring by bean name | Accepted |
| [0003](0003-llm-as-a-judge-claude-haiku.md) | Integration tests graded by Claude Haiku as LLM-as-a-judge | Accepted |
| [0004](0004-paid-tests-are-opt-in.md) | Tests that call paid APIs only run with `-Plive-it` | Accepted |
| [0005](0005-error-prone-and-palantir-format.md) | Error Prone with `-Werror`, palantir-java-format via Spotless | Accepted |
