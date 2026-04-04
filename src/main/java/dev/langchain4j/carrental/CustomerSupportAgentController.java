package dev.langchain4j.carrental;

import dev.langchain4j.service.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerSupportAgentController {

    private final CustomerSupportAgent customerSupportAgent;

    public CustomerSupportAgentController(CustomerSupportAgent customerSupportAgent) {
        this.customerSupportAgent = customerSupportAgent;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String sessionId,
                       @RequestParam String userMessage) {
        Result<String> result = customerSupportAgent.answer(sessionId, userMessage);
        return result.content();
    }
}