package com.example.employee.agent;

import com.example.employee.agent.tools.AdjustSalaryTool;
import com.example.employee.agent.tools.DeleteEmployeeTool;
import com.example.employee.agent.tools.GetEmployeeTool;
import com.example.employee.agent.tools.ListEmployeesTool;
import com.example.employee.agent.tools.SearchEmployeeTool;
import com.example.employee.agent.tools.UpdateEmployeeTool;
import com.example.employee.guardrails.AuthorizationGuardrail;
import com.example.employee.guardrails.InputGuardrail;
import com.example.employee.guardrails.OutputGuardrail;
import com.example.employee.guardrails.ToolGuardrail;
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

    private static final String DEFAULT_ROLE = "USER";

    private final EmployeeTools employeeTools;
    private final ObjectMapper objectMapper;
    private final InputGuardrail inputGuardrail;
    private final OutputGuardrail outputGuardrail;
    private final ToolGuardrail toolGuardrail;
    private final AuthorizationGuardrail authorizationGuardrail;
    private volatile OpenAIClient openAIClient;

    public EmployeeAgent(
            EmployeeTools employeeTools,
            ObjectMapper objectMapper,
            InputGuardrail inputGuardrail,
            OutputGuardrail outputGuardrail,
            ToolGuardrail toolGuardrail,
            AuthorizationGuardrail authorizationGuardrail) {

        this.employeeTools = employeeTools;
        this.objectMapper = objectMapper;
        this.inputGuardrail = inputGuardrail;
        this.outputGuardrail = outputGuardrail;
        this.toolGuardrail = toolGuardrail;
        this.authorizationGuardrail = authorizationGuardrail;
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
        return process(userMessage, DEFAULT_ROLE);
    }

    /**
     * @param role caller's role (USER, MANAGER, or ADMIN); defaults to USER
     *             when null or blank. USER can only view, MANAGER can only
     *             update (including salary adjustments), and ADMIN can
     *             view, update, and delete — except salary adjustment,
     *             which stays MANAGER-exclusive. An unrecognized role fails
     *             fast up front, before any OpenAI call is made; the actual
     *             per-operation permission is then checked per tool — see
     *             {@link #authorizeForTool(String, String)}.
     */
    public String process(String userMessage, String role) {

        inputGuardrail.validate(userMessage);

        String effectiveRole = role == null || role.isBlank() ? DEFAULT_ROLE : role;

        authorizationGuardrail.validateRecognizedRole(effectiveRole);

        String instructions = """
                You are an Employee Management AI Agent.

                Your job is to help users with employee information.

                You have access to employee management tools, including tools
                that update and delete employee records.

                IMPORTANT RULES:
                1. Never invent employee information.
                2. When employee information is required, use the available tools.
                3. If an employee cannot be found, clearly say that the employee was not found.
                4. Give concise and useful answers.
                5. Do not expose internal implementation details.
                6. Only call update_employee or delete_employee when the user has
                   clearly and explicitly asked for that change. Never delete or
                   modify an employee as a side effect of an unrelated request.
                7. update_employee replaces the entire record — if you don't already
                   know the employee's current field values, call get_employee first.
                8. For a raise, pay cut, or percentage salary change, use
                   adjust_salary instead of update_employee — it computes the new
                   salary from the current value on the server, so you never need
                   to know or calculate the current salary yourself.
                """;

        ResponseCreateParams.Builder paramsBuilder =
                ResponseCreateParams.builder()
                        .model(ChatModel.GPT_5_2)
                        .instructions(instructions)
                        .input(userMessage)
                        .addTool(getEmployeeTool())
                        .addTool(listEmployeesTool())
                        .addTool(searchEmployeesTool())
                        .addTool(updateEmployeeTool())
                        .addTool(deleteEmployeeTool())
                        .addTool(adjustSalaryTool());

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
                            functionCall.arguments(),
                            effectiveRole
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
            return sanitizeOutput(extractText(response));
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
                        .addTool(updateEmployeeTool())
                        .addTool(deleteEmployeeTool())
                        .addTool(adjustSalaryTool())
                        .build();

        Response finalResponse =
                openAIClient().responses().create(
                        finalParams
                );

        return sanitizeOutput(extractText(finalResponse));
    }

    /**
     * Runs the agent's final reply through the output guardrail. A violation
     * (sensitive content, an empty/oversized response) fails safe with a
     * generic refusal rather than surfacing the raw response or a 500.
     */
    private String sanitizeOutput(String text) {

        try {
            return outputGuardrail.validate(text);
        } catch (RuntimeException ex) {
            return "I can't share that response. Please rephrase your question.";
        }
    }

    /**
     * Execute an AI-requested tool. Authorization is checked per tool, since
     * reads, updates, and deletes require different privilege levels — see
     * {@link #authorizeForTool(String, String)}.
     */
    private String executeTool(
            String functionName,
            String arguments,
            String role) {

        try {
            toolGuardrail.validateTool(functionName);
        } catch (SecurityException ex) {
            return """
                    {
                      "error": "Unknown tool"
                    }
                    """;
        }

        authorizeForTool(functionName, role);

        return switch (functionName) {

            case GetEmployeeTool.NAME ->
                    GetEmployeeTool.execute(arguments, employeeTools, objectMapper);

            case ListEmployeesTool.NAME ->
                    ListEmployeesTool.execute(employeeTools);

            case SearchEmployeeTool.NAME ->
                    SearchEmployeeTool.execute(arguments, employeeTools, objectMapper);

            case UpdateEmployeeTool.NAME ->
                    UpdateEmployeeTool.execute(arguments, employeeTools, objectMapper);

            case DeleteEmployeeTool.NAME ->
                    DeleteEmployeeTool.execute(arguments, employeeTools, objectMapper);

            case AdjustSalaryTool.NAME ->
                    AdjustSalaryTool.execute(arguments, employeeTools, objectMapper);

            default -> """
                    {
                      "error": "Unknown tool"
                    }
                    """;
        };
    }

    /**
     * Maps a tool to the AuthorizationGuardrail check matching its risk:
     * reads need USER/ADMIN, field updates need MANAGER/ADMIN, deletes need
     * ADMIN, and salary adjustments need MANAGER specifically (ADMIN is
     * deliberately excluded there — see
     * AuthorizationGuardrail.checkSalaryAdjustmentAccess). Throws
     * SecurityException (-> 403) when the role is insufficient.
     */
    private void authorizeForTool(String functionName, String role) {

        switch (functionName) {

            case UpdateEmployeeTool.NAME -> authorizationGuardrail.checkUpdateAccess(role);

            case AdjustSalaryTool.NAME -> authorizationGuardrail.checkSalaryAdjustmentAccess(role);

            case DeleteEmployeeTool.NAME -> authorizationGuardrail.checkDeleteAccess(role);

            default -> authorizationGuardrail.checkReadAccess(role);
        }
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
     * search_employees(name, department)
     *
     * Both parameters are listed as "required" with a nullable type, which
     * is how OpenAI's strict function-calling schema expresses an optional
     * argument — the model must always pass the key, but may pass null.
     */
    private FunctionTool searchEmployeesTool() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                                "properties",
                                JsonValue.from(Map.of(
                                        "name", Map.of(
                                                "type", List.of("string", "null"),
                                                "description", "Employee name or partial name to search for, or null if not filtering by name"
                                        ),
                                        "department", Map.of(
                                                "type", List.of("string", "null"),
                                                "description", "Department to filter by (e.g. Engineering, Sales), or null if not filtering by department"
                                        )
                                ))
                        )
                        .putAdditionalProperty(
                                "required",
                                JsonValue.from(List.of("name", "department"))
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

    /**
     * Tool definition exposed to OpenAI.
     *
     * The AI sees this as:
     *
     * update_employee(employeeId, firstName, lastName, email, department, salary)
     *
     * This is a full replace (matches PUT /api/employees/{id}), so every
     * field is required — none are nullable.
     */
    private FunctionTool updateEmployeeTool() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                                "properties",
                                JsonValue.from(Map.of(
                                        "employeeId", Map.of(
                                                "type", "integer",
                                                "description", "The numeric ID of the employee to update"
                                        ),
                                        "firstName", Map.of(
                                                "type", "string",
                                                "description", "The employee's first name"
                                        ),
                                        "lastName", Map.of(
                                                "type", "string",
                                                "description", "The employee's last name"
                                        ),
                                        "email", Map.of(
                                                "type", "string",
                                                "description", "The employee's email address"
                                        ),
                                        "department", Map.of(
                                                "type", "string",
                                                "description", "The employee's department (e.g. Engineering, Sales)"
                                        ),
                                        "salary", Map.of(
                                                "type", "number",
                                                "description", "The employee's salary"
                                        )
                                ))
                        )
                        .putAdditionalProperty(
                                "required",
                                JsonValue.from(List.of(
                                        "employeeId", "firstName", "lastName",
                                        "email", "department", "salary"
                                ))
                        )
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(UpdateEmployeeTool.NAME)
                .description(UpdateEmployeeTool.DESCRIPTION)
                .parameters(parameters)
                .strict(true)
                .build();
    }

    /**
     * Tool definition exposed to OpenAI.
     *
     * The AI sees this as:
     *
     * delete_employee(employeeId)
     */
    private FunctionTool deleteEmployeeTool() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                                "properties",
                                JsonValue.from(Map.of(
                                        "employeeId", Map.of(
                                                "type", "integer",
                                                "description", "The numeric ID of the employee to delete"
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
                .name(DeleteEmployeeTool.NAME)
                .description(DeleteEmployeeTool.DESCRIPTION)
                .parameters(parameters)
                .strict(true)
                .build();
    }

    /**
     * Tool definition exposed to OpenAI.
     *
     * The AI sees this as:
     *
     * adjust_salary(employeeId, amount, isPercentage)
     *
     * isPercentage is listed as "required" with a nullable type — the model
     * must always pass the key, but null is treated as a flat dollar amount.
     */
    private FunctionTool adjustSalaryTool() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                                "properties",
                                JsonValue.from(Map.of(
                                        "employeeId", Map.of(
                                                "type", "integer",
                                                "description", "The numeric ID of the employee whose salary is being adjusted"
                                        ),
                                        "amount", Map.of(
                                                "type", "number",
                                                "description", "The change to apply. Positive increases the salary, "
                                                        + "negative decreases it. A flat dollar amount unless "
                                                        + "isPercentage is true."
                                        ),
                                        "isPercentage", Map.of(
                                                "type", List.of("boolean", "null"),
                                                "description", "true if amount is a percentage of the current salary "
                                                        + "(e.g. 10 for +10%); false or null for a flat dollar amount"
                                        )
                                ))
                        )
                        .putAdditionalProperty(
                                "required",
                                JsonValue.from(List.of("employeeId", "amount", "isPercentage"))
                        )
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(AdjustSalaryTool.NAME)
                .description(AdjustSalaryTool.DESCRIPTION)
                .parameters(parameters)
                .strict(true)
                .build();
    }
}
