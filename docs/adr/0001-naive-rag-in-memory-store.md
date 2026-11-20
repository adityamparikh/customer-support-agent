# 0001. Naive RAG with OpenAI embeddings over an in-memory store

- Status: Accepted (recorded 2026-11-20 from the code; rationale reconstructed, confirm with the author)
- Code: `src/main/java/dev/langchain4j/carrental/config/RagConfig.java`, `src/main/resources/application.properties`

## Context

The agent has to answer policy questions ("When can I cancel my booking?") from one short
document, `src/main/resources/miles-of-smiles-terms-of-use.txt`: nine numbered sections, a few
hundred words. `rag-comparison.md` lays out the three LangChain4j options. Easy RAG bundles a
local ONNX embedding model and Apache Tika; Advanced RAG adds query transformation, routing and
re-ranking for multiple sources. Neither fits a single small text file with one retrieval source.

## Decision

Use LangChain4j "naive" RAG, assembled by hand:

- Embedding model: OpenAI `text-embedding-3-small` (`langchain4j.open-ai.embedding-model.*`),
  the same provider and key as the chat model, so the app needs one key.
- Store: the starter's `InMemoryEmbeddingStore`, filled on `ApplicationReadyEvent` by
  `RagConfig.ingestDocuments`.
- Splitting: `DocumentSplitters.recursive(100, 0, tokenizer)`, 100-token segments, no overlap,
  counted with the GPT-5 token estimator.
- Retrieval: the starter's `contentRetriever` bean with `langchain4j.rag.retrieval.max-results=1`
  and `min-score=0.6`, so the agent sees at most one segment, and none if nothing is close.

## Consequences

- Every application start re-embeds the whole document: a few paid embedding calls per start
  (including every `-Plive-it` test run), and nothing survives a restart.
- With one result per question, an answer can only draw on one 100-token segment. The test
  `CustomerSupportAgentIT#should_answer_cancellation_policy_question` relies on sections 4.1 and
  4.2 landing in the same segment; changing the chunk size or overlap can split them and break it.
- A question with no segment above 0.6 gets no context. Nothing then stops the model from
  answering a policy question from its own knowledge except the system message, whose rule 3 only
  keeps it on Miles of Smiles topics.
- Moving to a persistent store or Advanced RAG is a new ADR that supersedes this one.
