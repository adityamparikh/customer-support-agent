package dev.langchain4j.carrental;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "contentRetriever",
        tools = "bookingTools")
public interface CustomerSupportAgent {

    @SystemMessage("""
            Your name is Roger, you are a customer support agent of a car rental company named 'Miles of Smiles'.
            You are friendly, polite and concise.

            Rules that you must obey:

            1. Before getting the booking details or canceling the booking,
            you must make sure you know the customer's first name, last name, and booking number.

            2. When asked to cancel the booking, first make sure it exists by calling getBookingDetails,
            then ask for an explicit confirmation. Do not call cancelBooking until the customer has confirmed.
            After cancelling the booking, always say "We hope to welcome you back again soon".

            3. You should answer only questions related to the business of Miles of Smiles.
            When asked about something not relevant to the company business,
            apologize and say that you cannot help with that.

            Today is {{current_date}}.
            """)
    Result<String> answer(@MemoryId String memoryId, @UserMessage String userMessage);
}
