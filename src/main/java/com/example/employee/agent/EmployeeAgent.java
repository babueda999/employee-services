package com.example.employee.agent;

import com.example.employee.agent.tools.GetEmployeeTool;
import com.example.employee.agent.tools.ListEmployeesTool;
import com.example.employee.agent.tools.SearchEmployeeTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.models.ChatModel;
import com.openai.models.responses.FunctionTool;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeAgent {

    private final EmployeeTools employeeTools;
    private final ObjectMapper objectMapper;
    private volatile OpenAIClient openAIClient;

    public EmployeeAgent(
            EmployeeTools employeeTools,
            ObjectMapper objectMapper) {

        this.employeeTools = employeeTools;
        this.objectMapper = objectMapper;
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
     * Example:
     *
     * "Find employee 101"
     *
     * The model can decide to call one of the employee tools.
     */
    public String process(String userMessage) {

        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "User message cannot be empty"
            );
        }

        String instructions = """
                You are an Employee Management AI Agent.

                Your job is to help users with employee information.

                You have access to employee management tools.

                IMPORTANT RULES:
                1. Never invent employee information.
                2. When employee information is required, use the available tools.
                3. If an employee cannot be found, clearly say that the employee was not found.
                4. Give concise and useful answers.
                5. Do not expose internal implementation details.
                """;

        ResponseCreateParams.Builder paramsBuilder =
                ResponseCreateParams.builder()
                        .model(ChatModel.GPT_5_2)
                        .instructions(instructions)
                        .input(userMessage)
                        .addTool(getEmployeeTool())
                        .addTool(listEmployeesTool())
                        .addTool(searchEmployeesTool());

        Response response =
                openAIClient().responses().create(
                        paramsBuilder.build()
                );

        /*
         * Process tool calls.
         */
        List<ResponseInputItem> toolResults =
                new ArrayList<>();

        for (ResponseOutputItem outputItem : response.output()) {

            if (!outputItem.isFunctionCall()) {
                continue;
            }

            ResponseFunctionToolCall functionCall =
                    outputItem.asFunctionCall();

            /*
             * Execute the requested employee tool.
             */
            String toolResult =
                    executeTool(
                            functionCall.name(),
                            functionCall.arguments()
                    );

            /*
             * Return the result of our Java function back to the AI
             * model. The function_call item itself doesn't need to be
             * resent here — previousResponseId(response.id()) already
             * carries it as part of that response's state; resending it
             * causes OpenAI to reject the request with a "Duplicate item
             * found" error.
             */
            toolResults.add(
                    ResponseInputItem.ofFunctionCallOutput(
                            ResponseInputItem.FunctionCallOutput.builder()
                                    .callId(functionCall.callId())
                                    .output(toolResult)
                                    .build()
                    )
            );
        }

        /*
         * If no tool was requested, return the
         * normal AI response.
         */
        if (toolResults.isEmpty()) {
            return extractText(response);
        }

        /*
         * Send the tool results back to OpenAI.
         *
         * The model now has the actual employee data
         * and can formulate the final answer.
         */
        ResponseCreateParams finalParams =
                ResponseCreateParams.builder()
                        .model(ChatModel.GPT_5_2)
                        .instructions(instructions)
                        .previousResponseId(response.id())
                        .input(
                                ResponseCreateParams.Input.ofResponse(
                                        toolResults
                                )
                        )
                        .addTool(getEmployeeTool())
                        .addTool(listEmployeesTool())
                        .addTool(searchEmployeesTool())
                        .build();

        Response finalResponse =
                openAIClient().responses().create(
                        finalParams
                );

        return extractText(finalResponse);
    }

    /**
     * Execute an AI-requested tool.
     */
    private String executeTool(
            String functionName,
            String arguments) {

        return switch (functionName) {

            case GetEmployeeTool.NAME ->
                    GetEmployeeTool.execute(arguments, employeeTools, objectMapper);

            case ListEmployeesTool.NAME ->
                    ListEmployeesTool.execute(employeeTools);

            case SearchEmployeeTool.NAME ->
                    SearchEmployeeTool.execute(arguments, employeeTools, objectMapper);

            default -> """
                    {
                      "error": "Unknown tool"
                    }
                    """;
        };
    }

    /**
     * Extract text from the OpenAI response.
     *
     * With reasoning models, a response can occasionally come back with no
     * message/output_text item at all (e.g. the model spent its output on
     * a reasoning item without producing a final answer), which would
     * otherwise silently surface as a blank reply to the user.
     */
    private String extractText(Response response) {

        String text = response.output()
                .stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(outputText -> outputText.text() == null ? "" : outputText.text())
                .reduce("", (left, right) -> left + right);

        if (text.isBlank()) {
            return "I couldn't generate a response to that. Please try rephrasing your question.";
        }

        return text;
    }

    /**
     * Tool definition exposed to OpenAI.
     *
     * The AI sees this as:
     *
     * get_employee(employeeId)
     */
    private FunctionTool getEmployeeTool() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                                "properties",
                                JsonValue.from(Map.of(
                                        "employeeId", Map.of(
                                                "type", "integer",
                                                "description", "The numeric ID of the employee to look up"
                                        )
                                ))
                        )
                        .putAdditionalProperty(
                                "required",
                                JsonValue.from(List.of("employeeId"))
                        )
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(GetEmployeeTool.NAME)
                .description(GetEmployeeTool.DESCRIPTION)
                .parameters(parameters)
                .strict(true)
                .build();
    }

    /**
     * Tool definition exposed to OpenAI.
     *
     * The AI sees this as:
     *
     * list_employees()
     */
    private FunctionTool listEmployeesTool() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty("properties", JsonValue.from(Map.of()))
                        .putAdditionalProperty("required", JsonValue.from(List.of()))
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(ListEmployeesTool.NAME)
                .description(ListEmployeesTool.DESCRIPTION)
                .parameters(parameters)
                .strict(true)
                .build();
    }

    /**
     * Tool definition exposed to OpenAI.
     *
     * The AI sees this as:
     *
     * search_employees(name)
     */
    private FunctionTool searchEmployeesTool() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                                "properties",
                                JsonValue.from(Map.of(
                                        "name", Map.of(
                                                "type", "string",
                                                "description", "Employee name or partial name to search for"
                                        )
                                ))
                        )
                        .putAdditionalProperty(
                                "required",
                                JsonValue.from(List.of("name"))
                        )
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(SearchEmployeeTool.NAME)
                .description(SearchEmployeeTool.DESCRIPTION)
                .parameters(parameters)
                .strict(true)
                .build();
    }
}
