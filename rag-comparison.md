# LangChain4j RAG Approaches Comparison

| Aspect | Easy RAG | Naive RAG | Advanced RAG |
|---|---|---|---|
| **Setup** | Minimal — static `ingest()` call, defaults for everything | Manual — you pick model, splitter, store | Full pipeline — query transformer, router, aggregator, re-ranker |
| **Embedding model** | Bundled local ONNX (`bge-small-en-v1.5`) by default, overridable via builder | Your choice (e.g. OpenAI `text-embedding-3-small`) | Your choice |
| **Document parsing** | Auto-detected via Apache Tika | You choose the parser | You choose the parser |
| **Splitting** | 300 tokens, 30 overlap by default, overridable via builder | Configurable | Configurable |
| **Retrieval sources** | Single vector store | Single vector store | Multiple (vector, web search, SQL, graph DB, etc.) |
| **Query transformation** | None | None | Compressing (follow-ups to standalone) and expanding (multiple query variants) |
| **Query routing** | None | None | LLM-based routing to the right retriever |
| **Re-ranking** | No | No | Yes (e.g. Cohere scoring model) |
| **Metadata filtering** | Depends on `EmbeddingStore` and `ContentRetriever` used | Depends on `EmbeddingStore` and `ContentRetriever` used | Dynamic per-query filters |
| **Multi-turn context** | No | No | Yes (via `CompressingQueryTransformer`) |
| **Best for** | Prototyping, learning | Standard use cases with known requirements | Production systems with complex retrieval needs |
