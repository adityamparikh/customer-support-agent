package dev.langchain4j.carrental.utils;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Offline stand-in for the OpenAI chat model. Each call hands the request to a script that decides the reply, and
 * every request is recorded so a test can assert on exactly what the agent sent to the model: the system message,
 * the remembered conversation and the tool specifications.
 */
public class ScriptedChatModel implements ChatModel {

    private final List<ChatRequest> requests = new ArrayList<>();
    private Function<ChatRequest, AiMessage> script = request -> AiMessage.from("No script set");

    public void script(Function<ChatRequest, AiMessage> script) {
        this.script = script;
    }

    public List<ChatRequest> requests() {
        return List.copyOf(requests);
    }

    public void reset() {
        requests.clear();
    }

    @Override
    public ChatResponse doChat(ChatRequest request) {
        requests.add(request);
        AiMessage reply = script.apply(request);
        return ChatResponse.builder()
                .aiMessage(reply)
                .tokenUsage(new TokenUsage(0, 0))
                .finishReason(reply.hasToolExecutionRequests() ? FinishReason.TOOL_EXECUTION : FinishReason.STOP)
                .build();
    }

    public static ChatMessage lastMessage(ChatRequest request) {
        return request.messages().getLast();
    }

    public static boolean lastUserMessageContains(ChatRequest request, String text) {
        return lastMessage(request) instanceof UserMessage user
                && user.singleText().contains(text);
    }

    public static boolean lastToolResultIs(ChatRequest request, String toolName) {
        return lastMessage(request) instanceof ToolExecutionResultMessage result
                && result.toolName().equals(toolName);
    }

    public static AiMessage callTool(String name, String argumentsJson) {
        return AiMessage.from(ToolExecutionRequest.builder()
                .id(name + "-call")
                .name(name)
                .arguments(argumentsJson)
                .build());
    }
}
