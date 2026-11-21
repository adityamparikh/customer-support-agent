package dev.langchain4j.carrental.utils;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline stand-in for the OpenAI embedding model: hashes each word into a fixed-size vector. Texts that share words
 * score higher than texts that do not, which is enough to test ingestion and retrieval wiring without a paid call.
 */
public class BagOfWordsEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSION = 256;
    private static final Pattern WORD = Pattern.compile("\\p{Alnum}+");

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        return Response.from(
                segments.stream().map(segment -> embedText(segment.text())).toList());
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    private static Embedding embedText(String text) {
        float[] vector = new float[DIMENSION];
        Matcher words = WORD.matcher(text.toLowerCase(Locale.ROOT));
        while (words.find()) {
            vector[Math.floorMod(words.group().hashCode(), DIMENSION)] += 1;
        }
        Embedding embedding = Embedding.from(vector);
        embedding.normalize();
        return embedding;
    }
}
