package dev.langchain4j.carrental;

import dev.langchain4j.service.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that provides HTTP endpoints for interacting with the customer support agent.
 * This controller exposes chat functionality for the 'Miles of Smiles' car rental company,
 * delegating user interactions to an AI-powered customer support agent.
 *
 * The controller maintains conversational context through session identifiers, allowing
 * stateful multi-turn conversations with customers.
 */
@RestController
public class CustomerSupportAgentController {

    private final CustomerSupportAgent customerSupportAgent;

    public CustomerSupportAgentController(CustomerSupportAgent customerSupportAgent) {
        this.customerSupportAgent = customerSupportAgent;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String sessionId, @RequestParam String userMessage) {
        Result<String> result = customerSupportAgent.answer(sessionId, userMessage);
        return result.content();
    }
}
