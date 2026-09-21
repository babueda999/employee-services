
package com.example.employee.service;
import com.example.employee.entity.Employee;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import org.springframework.stereotype.Service;

@Service
public class EmployeeAgentService {

    private final EmployeeService employeeService;
    private volatile OpenAIClient openAIClient;

    public EmployeeAgentService(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * Built on first use rather than at construction time, so the
     * application context can start without an OpenAI credential
     * configured (it's only required once the agent is actually invoked).
     */
    private OpenAIClient openAIClient() {

        if (openAIClient == null) {
            synchronized (this) {
                if (openAIClient == null) {
                    openAIClient = OpenAIOkHttpClient.fromEnv();
                }
            }
        }

        return openAIClient;
    }

    /**
     * Main entry point for the Employee AI Agent.
     *
     * The user sends a natural-language request such as:
     *
     * "Find employee 101"
     *
     * The agent sends the request to OpenAI and returns the
     * generated response.
     */
    public String processRequest(String userMessage) {

        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "User message cannot be empty"
            );
        }

        String systemInstructions = """
                You are an Employee Management AI Agent.

                You help users with employee-related questions.

                Available employee operations include:
                - Finding an employee
                - Listing employees
                - Searching employees
                - Explaining employee information

                Always provide accurate and concise answers.

                Do not invent employee information.

                If employee information is not available,
                clearly tell the user that the information
                could not be found.
                """;

        ResponseCreateParams params =
                ResponseCreateParams.builder()
                        .model(ChatModel.GPT_5_2)
                        .instructions(systemInstructions)
                        .input(userMessage)
                        .build();

        Response response =
                openAIClient().responses().create(params);

        return response.output()
                .stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(outputText -> outputText.text())
                .reduce("", String::concat);
    }
}

