package dev.langchain4j.carrental;

import static dev.langchain4j.carrental.utils.ResultAssert.assertThat;
import static dev.langchain4j.carrental.utils.ScriptedChatModel.callTool;
import static dev.langchain4j.carrental.utils.ScriptedChatModel.lastToolResultIs;
import static dev.langchain4j.carrental.utils.ScriptedChatModel.lastUserMessageContains;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.carrental.booking.BookingNotFoundException;
import dev.langchain4j.carrental.booking.BookingService;
import dev.langchain4j.carrental.utils.BagOfWordsEmbeddingModel;
import dev.langchain4j.carrental.utils.ScriptedChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.service.Result;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * Keyless tests of the real Spring wiring: the {@code @AiService} with its explicitly named beans, the booking tools,
 * chat memory and RAG ingestion. Both OpenAI models are replaced by offline stand-ins, so these tests make no network
 * calls even when API keys are exported. They check how the application drives the model, not how a real model
 * behaves; that is what the opt-in {@code CustomerSupportAgentIT} is for.
 */
@SpringBootTest(properties = "OPENAI_API_KEY=unused-offline-test-key")
class CustomerSupportAgentWiringTests {

    private static final String JOHN_DOE_MS_777 =
            "{\"bookingNumber\":\"MS-777\",\"customerName\":\"John\",\"customerSurname\":\"Doe\"}";

    @TestBean(name = "openAiChatModel", methodName = "scriptedChatModel")
    ChatModel chatModel;

    @TestBean(methodName = "bagOfWordsEmbeddingModel")
    EmbeddingModel embeddingModel;

    @Autowired
    CustomerSupportAgent agent;

    @Autowired
    BookingService bookingService;

    @Autowired
    EmbeddingStore<TextSegment> embeddingStore;

    private final String memoryId = UUID.randomUUID().toString();

    static ChatModel scriptedChatModel() {
        return new ScriptedChatModel();
    }

    static EmbeddingModel bagOfWordsEmbeddingModel() {
        return new BagOfWordsEmbeddingModel();
    }

    private ScriptedChatModel model() {
        return (ScriptedChatModel) chatModel;
    }

    @BeforeEach
    void resetModel() {
        model().reset();
    }

    @Test
    void agent_sends_the_system_rules_and_both_booking_tools_to_the_model() {
        model().script(request -> AiMessage.from("Hello, I am Roger."));

        agent.answer(memoryId, "Hi");

        ChatRequest request = model().requests().getFirst();
        assertThat(request.messages().getFirst()).isInstanceOf(SystemMessage.class);
        assertThat(((SystemMessage) request.messages().getFirst()).text())
                .contains("Your name is Roger")
                .contains("first make sure it exists by calling getBookingDetails")
                .contains("Do not call cancelBooking until the customer has confirmed");
        assertThat(request.toolSpecifications())
                .extracting(ToolSpecification::name)
                .containsExactlyInAnyOrder("getBookingDetails", "cancelBooking");
    }

    @Test
    void two_turn_cancellation_runs_the_real_tools_and_removes_the_booking() {
        model().script(request -> {
            if (lastUserMessageContains(request, "Cancel my booking MS-777")) {
                return callTool("getBookingDetails", JOHN_DOE_MS_777);
            }
            if (lastToolResultIs(request, "getBookingDetails")) {
                return AiMessage.from("I found booking MS-777. Do you want me to cancel it?");
            }
            if (lastUserMessageContains(request, "yes, cancel it")) {
                return callTool("cancelBooking", JOHN_DOE_MS_777);
            }
            if (lastToolResultIs(request, "cancelBooking")) {
                return AiMessage.from("Your booking is cancelled. We hope to welcome you back again soon.");
            }
            return AiMessage.from("Unexpected request");
        });

        Result<String> first = agent.answer(memoryId, "Cancel my booking MS-777. My name is John Doe.");

        assertThat(first).onlyToolWasExecuted("getBookingDetails");
        assertThat(first.toolExecutions().getFirst().result())
                .contains("MS-777")
                .contains("John");
        assertThat(first.content()).contains("Do you want me to cancel it?");
        assertThat(bookingService.getBookingDetails("MS-777", "John", "Doe")).isNotNull();

        Result<String> second = agent.answer(memoryId, "yes, cancel it");

        assertThat(second).onlyToolWasExecuted("cancelBooking");
        assertThat(second.content()).contains("We hope to welcome you back again soon");
        assertThatThrownBy(() -> bookingService.getBookingDetails("MS-777", "John", "Doe"))
                .isInstanceOf(BookingNotFoundException.class);

        List<ChatMessage> remembered = model().requests().get(2).messages(); // the request carrying "yes, cancel it"
        assertThat(remembered)
                .filteredOn(ToolExecutionResultMessage.class::isInstance)
                .extracting(message -> ((ToolExecutionResultMessage) message).toolName())
                .containsExactly("getBookingDetails");
    }

    @Test
    void lookup_of_a_booking_that_does_not_exist_reaches_the_model_as_a_tool_error() {
        model().script(request -> {
            if (lastUserMessageContains(request, "54321")) {
                return callTool(
                        "getBookingDetails",
                        "{\"bookingNumber\":\"54321\",\"customerName\":\"John\",\"customerSurname\":\"Doe\"}");
            }
            return AiMessage.from("I could not find that booking.");
        });

        Result<String> result = agent.answer(memoryId, "Hi, I am John Doe. When does my booking 54321 start?");

        assertThat(result).onlyToolWasExecuted("getBookingDetails");
        assertThat(result.toolExecutions().getFirst().result()).contains("Booking 54321 not found");
    }

    @Test
    void terms_of_use_are_ingested_into_the_embedding_store_at_startup() {
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore
                .search(EmbeddingSearchRequest.builder()
                        .queryEmbedding(embeddingModel
                                .embed("Reservations can be cancelled up to 7 days prior to the start of the booking")
                                .content())
                        .maxResults(1)
                        .build())
                .matches();

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().embedded().text())
                .contains("4.1 Reservations can be cancelled up to 7 days prior to the start of the booking period.");
    }
}
