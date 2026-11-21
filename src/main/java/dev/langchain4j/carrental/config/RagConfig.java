package dev.langchain4j.carrental.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.ResolvableType;

@Configuration
public class RagConfig {

    @Bean
    TokenCountEstimator tokenCountEstimator() {
        return new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_5);
    }

    @EventListener(ApplicationReadyEvent.class)
    void ingestDocuments(ApplicationReadyEvent event) throws IOException {
        var context = event.getApplicationContext();
        var embeddingModel = context.getBean(EmbeddingModel.class);
        var tokenizer = context.getBean(TokenCountEstimator.class);
        EmbeddingStore<TextSegment> embeddingStore = context.<EmbeddingStore<TextSegment>>getBeanProvider(
                        ResolvableType.forClassWithGenerics(EmbeddingStore.class, TextSegment.class))
                .getObject();
        var termsOfUse = context.getResource("classpath:miles-of-smiles-terms-of-use.txt");

        // Read as a stream, not a File, so ingestion also works when the app runs from the packaged jar.
        Document document;
        try (InputStream content = termsOfUse.getInputStream()) {
            document = new TextDocumentParser().parse(content);
        }

        EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(100, 0, tokenizer))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build()
                .ingest(document);
    }
}
