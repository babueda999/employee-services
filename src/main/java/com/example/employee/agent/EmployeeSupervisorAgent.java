package com.example.employee.agent;

import com.example.employee.agent.subagents.EmployeeSubAgent;
import com.example.employee.confirmation.PendingToolConfirmation;
import com.example.employee.confirmation.ToolConfirmationService;
import com.example.employee.guardrails.AuthorizationGuardrail;
import com.example.employee.guardrails.InputGuardrail;
import com.example.employee.guardrails.OutputGuardrail;
import com.example.employee.guardrails.ToolGuardrail;
import com.example.employee.memory.ConversationMemoryService;
import com.example.employee.memory.ConversationMessage;
import com.example.employee.usage.TokenUsageTracker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates the employee sub-agents. Each employee operation
 * (get/list/search/update/delete/adjustSalary) lives in its own
 * {@link EmployeeSubAgent} bean, discovered here and offered to OpenAI as
 * one combined toolset. When the model calls a tool, this class routes the
 * call to the one sub-agent that owns it — that sub-agent is responsible
 * for its own authorization and execution.
 *
 * <p>Two things happen above that per-tool dispatch:
 * <ul>
 *   <li><b>Multi-step tool use.</b> {@link #process} loops — executing tool
 *   calls and sending results back to OpenAI — until the model stops asking
 *   for tools or a safety cap is hit, so a request that needs several tools
 *   in sequence (e.g. search, then adjust each match's salary) actually
 *   works instead of being silently cut off after one round.
 *   <li><b>Conversational memory.</b> Each turn is persisted via
 *   {@link ConversationMemoryService} and replayed as context on the next
 *   call for the same {@code conversationId}, so a follow-up like
 *   "give him a 10% raise instead" can resolve "him" from the prior turn.
 * </ul>
 *
 * <p>A CRITICAL-risk tool (currently just {@code delete_employee} — see
 * {@link ToolGuardrail}) is never executed directly out of this loop: it's
 * authorized, then held as a {@link PendingToolConfirmation} and only run
 * once a human approves it via {@link #confirm}.
 */
@Service
public class EmployeeSupervisorAgent {

    private static final String DEFAULT_ROLE = "USER";

    /**
     * Hard cap on how many times this loop will send tool results back to
     * OpenAI and ask it to continue, per {@link #process} call. Without a
     * cap, a model stuck in a call/re-call pattern would run (and bill)
     * indefinitely.
     */
    private static final int MAX_TOOL_ITERATIONS = 5;

    private static final String UNKNOWN_TOOL_RESULT = """
            {
              "error": "Unknown tool"
            }
            """;

    /**
     * Single source of truth for the model name reported alongside token
     * usage — derived from the same {@link ChatModel} constant used in the
     * actual API calls below, so it can never drift out of sync.
     */
    private static final String MODEL_NAME = ChatModel.GPT_5_2.asString();

    private final Map<String, EmployeeSubAgent> subAgentsByName;
    private final ConversationMemoryService conversationMemoryService;
    private final ToolConfirmationService toolConfirmationService;
    private final TokenUsageTracker tokenUsageTracker;
    private final ObjectMapper objectMapper;
    private final InputGuardrail inputGuardrail;
    private final OutputGuardrail outputGuardrail;
    private final ToolGuardrail toolGuardrail;
    private final AuthorizationGuardrail authorizationGuardrail;
    private volatile OpenAIClient openAIClient;

    public EmployeeSupervisorAgent(
            List<EmployeeSubAgent> subAgents,
            ConversationMemoryService conversationMemoryService,
            ToolConfirmationService toolConfirmationService,
            TokenUsageTracker tokenUsageTracker,
            ObjectMapper objectMapper,
            InputGuardrail inputGuardrail,
            OutputGuardrail outputGuardrail,
            ToolGuardrail toolGuardrail,
            AuthorizationGuardrail authorizationGuardrail) {

        this.subAgentsByName = subAgents.stream()
                .collect(Collectors.toMap(EmployeeSubAgent::name, Function.identity()));
        this.conversationMemoryService = conversationMemoryService;
        this.toolConfirmationService = toolConfirmationService;
        this.tokenUsageTracker = tokenUsageTracker;
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
     * Creates a response and records its token usage (when OpenAI returns
     * one — some responses omit it) against {@link #tokenUsageTracker},
     * so every OpenAI call made anywhere in {@link #process} is counted,
     * not just the first one per turn.
     */
    private Response createResponse(ResponseCreateParams params) {

        Response response = openAIClient().responses().create(params);

        response.usage().ifPresent(usage ->
                tokenUsageTracker.recordUsage(
                        MODEL_NAME,
                        usage.inputTokens(),
                        usage.outputTokens(),
                        usage.totalTokens()
                )
        );

        return response;
    }

    public AgentReply process(String userMessage) {
        return process(userMessage, DEFAULT_ROLE, null);
    }

    /**
     * @param role           caller's role (USER, MANAGER, or ADMIN); defaults
     *                       to USER when null or blank. An unrecognized role
     *                       fails fast up front, before any OpenAI call is
     *                       made; the actual per-operation permission is
     *                       then checked by whichever sub-agent owns the
     *                       tool the model calls.
     * @param conversationId continues an existing conversation (its prior
     *                       turns are replayed as context) when non-blank;
     *                       otherwise a new conversation is started and its
     *                       ID is returned on {@link AgentReply#conversationId()}.
     */
    public AgentReply process(String userMessage, String role, String conversationId) {

        inputGuardrail.validate(userMessage);

        String effectiveRole = role == null || role.isBlank() ? DEFAULT_ROLE : role;

        authorizationGuardrail.validateRecognizedRole(effectiveRole);

        String effectiveConversationId =
                conversationId == null || conversationId.isBlank()
                        ? UUID.randomUUID().toString()
                        : conversationId;

        List<ResponseInputItem> inputItems = buildInputItems(effectiveConversationId, userMessage);

        conversationMemoryService.appendUserMessage(effectiveConversationId, userMessage);

        ResponseCreateParams.Builder paramsBuilder =
                ResponseCreateParams.builder()
                        .model(ChatModel.GPT_5_2)
                        .instructions(INSTRUCTIONS)
                        .input(ResponseCreateParams.Input.ofResponse(inputItems));

        subAgentsByName.values().forEach(subAgent -> paramsBuilder.addTool(subAgent.definition()));

        Response response = createResponse(paramsBuilder.build());

        String pendingConfirmationToken = null;
        int iterations = 0;
        LinkedHashSet<String> toolsUsed = new LinkedHashSet<>();

        while (true) {

            List<ResponseFunctionToolCall> functionCalls =
                    response.output().stream()
                            .filter(ResponseOutputItem::isFunctionCall)
                            .map(ResponseOutputItem::asFunctionCall)
                            .toList();

            if (functionCalls.isEmpty()) {
                break;
            }

            iterations++;

            if (iterations > MAX_TOOL_ITERATIONS) {
                String cappedReply =
                        "That request needs more steps than I can safely take at once. "
                                + "Try breaking it into smaller requests.";
                conversationMemoryService.appendAssistantMessage(effectiveConversationId, cappedReply);
                return new AgentReply(
                        cappedReply, effectiveConversationId, false, null, List.copyOf(toolsUsed));
            }

            List<ResponseInputItem> toolResults = new ArrayList<>();
            boolean confirmationRequestedThisRound = false;

            for (ResponseFunctionToolCall functionCall : functionCalls) {

                if (subAgentsByName.containsKey(functionCall.name())) {
                    toolsUsed.add(functionCall.name());
                }

                ToolExecutionResult result =
                        executeTool(
                                functionCall.name(),
                                functionCall.arguments(),
                                effectiveRole,
                                effectiveConversationId
                        );

                toolResults.add(
                        ResponseInputItem.ofFunctionCallOutput(
                                ResponseInputItem.FunctionCallOutput.builder()
                                        .callId(functionCall.callId())
                                        .output(result.outputForModel())
                                        .build()
                        )
                );

                if (result.confirmationToken() != null) {
                    pendingConfirmationToken = result.confirmationToken();
                    confirmationRequestedThisRound = true;
                }
            }

            ResponseCreateParams.Builder nextParamsBuilder =
                    ResponseCreateParams.builder()
                            .model(ChatModel.GPT_5_2)
                            .instructions(INSTRUCTIONS)
                            .previousResponseId(response.id())
                            .input(ResponseCreateParams.Input.ofResponse(toolResults));

            subAgentsByName.values().forEach(subAgent -> nextParamsBuilder.addTool(subAgent.definition()));

            response = createResponse(nextParamsBuilder.build());

            if (confirmationRequestedThisRound) {
                // A critical action is gated on a human — stop chaining
                // further tool calls and surface the model's explanation
                // of what it's waiting on.
                break;
            }
        }

        String finalText = sanitizeOutput(extractText(response));

        conversationMemoryService.appendAssistantMessage(effectiveConversationId, finalText);

        return new AgentReply(
                finalText,
                effectiveConversationId,
                pendingConfirmationToken != null,
                pendingConfirmationToken,
                List.copyOf(toolsUsed)
        );
    }

    /**
     * Approves or denies a tool call that was previously held back for
     * human confirmation (see {@link #executeTool}). Runs the real
     * operation directly — this is the human's decision, not the model's,
     * so there's no OpenAI call involved.
     *
     * @param approverRole role of the caller approving/denying — two-person
     *                     control: the sub-agent already required the
     *                     <i>requester</i> to have the right role to ask for
     *                     this action (e.g. ADMIN for delete); the
     *                     <i>approver</i> here must separately be a MANAGER,
     *                     regardless of who requested it. Defaults to USER
     *                     (and is therefore rejected) when null or blank.
     */
    public AgentReply confirm(String confirmationToken, boolean approve, String approverRole) {

        String effectiveApproverRole =
                approverRole == null || approverRole.isBlank() ? DEFAULT_ROLE : approverRole;

        authorizationGuardrail.checkConfirmationApprovalAccess(effectiveApproverRole);

        PendingToolConfirmation pending = toolConfirmationService.resolve(confirmationToken);

        String resultText;

        if (!approve) {
            resultText = "Okay, I won't go ahead with that.";
        } else {
            EmployeeSubAgent subAgent = subAgentsByName.get(pending.getToolName());
            String outcomeJson = subAgent.handle(pending.getArgumentsJson(), pending.getRole());
            resultText = summarize(outcomeJson);
        }

        conversationMemoryService.appendAssistantMessage(pending.getConversationId(), resultText);

        return new AgentReply(
                resultText, pending.getConversationId(), false, null, List.of(pending.getToolName()));
    }

    /**
     * Turns a tool's JSON result into a short, deterministic sentence. No
     * OpenAI call involved — the confirm step should not depend on a model
     * to report back accurately on a destructive action.
     */
    private String summarize(String outcomeJson) {

        try {
            JsonNode node = objectMapper.readTree(outcomeJson);

            if (node.hasNonNull("message")) {
                return node.get("message").asText();
            }

            return "Done.";

        } catch (Exception ex) {
            return "Done.";
        }
    }

    /**
     * Builds the OpenAI input for this turn: the conversation's recent
     * history (oldest first) followed by the new user message, so a
     * follow-up like "give him a raise instead" can resolve context from
     * earlier turns of the same conversation.
     */
    private List<ResponseInputItem> buildInputItems(String conversationId, String userMessage) {

        List<ResponseInputItem> items = new ArrayList<>();

        for (ConversationMessage message : conversationMemoryService.getRecentHistory(conversationId)) {

            EasyInputMessage.Role role =
                    "assistant".equals(message.getRole())
                            ? EasyInputMessage.Role.ASSISTANT
                            : EasyInputMessage.Role.USER;

            items.add(
                    ResponseInputItem.ofEasyInputMessage(
                            EasyInputMessage.builder()
                                    .role(role)
                                    .content(message.getContent())
                                    .build()
                    )
            );
        }

        items.add(
                ResponseInputItem.ofEasyInputMessage(
                        EasyInputMessage.builder()
                                .role(EasyInputMessage.Role.USER)
                                .content(userMessage)
                                .build()
                )
        );

        return items;
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
     * Result of dispatching one OpenAI-requested tool call: the JSON to
     * hand back to the model, and — only for a CRITICAL-risk tool that was
     * authorized but held back — the confirmation token the caller now
     * needs to approve or deny it via {@link #confirm}.
     */
    private record ToolExecutionResult(String outputForModel, String confirmationToken) {

        static ToolExecutionResult plain(String outputForModel) {
            return new ToolExecutionResult(outputForModel, null);
        }
    }

    /**
     * Executes an AI-requested tool by delegating to the sub-agent
     * registered under that name. Authorization is enforced by that
     * sub-agent itself, since each operation requires a different
     * privilege level. A CRITICAL-risk tool (see {@link ToolGuardrail}) is
     * authorized here but not executed — it's parked as a pending
     * confirmation instead, see {@link #confirm}.
     */
    private ToolExecutionResult executeTool(
            String functionName,
            String arguments,
            String role,
            String conversationId) {

        try {
            toolGuardrail.validateTool(functionName);
        } catch (SecurityException ex) {
            return ToolExecutionResult.plain(UNKNOWN_TOOL_RESULT);
        }

        EmployeeSubAgent subAgent = subAgentsByName.get(functionName);

        if (subAgent == null) {
            return ToolExecutionResult.plain(UNKNOWN_TOOL_RESULT);
        }

        if (toolGuardrail.requiresConfirmation(functionName)) {

            // Authorize now so an unauthorized caller gets a 403 up front,
            // rather than a confirmation prompt they could never approve.
            subAgent.authorize(role);

            String token = toolConfirmationService.createPending(conversationId, functionName, arguments, role);

            String output = """
                    {
                      "success": false,
                      "confirmationRequired": true,
                      "confirmationToken": "%s",
                      "message": "This action is irreversible, so it needs explicit human confirmation before it runs. Tell the user to confirm by calling POST /api/agent/confirm with this confirmationToken."
                    }
                    """.formatted(token);

            return new ToolExecutionResult(output, token);
        }

        return ToolExecutionResult.plain(subAgent.handle(arguments, role));
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

    private static final String INSTRUCTIONS = """
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
            9. delete_employee always requires a separate human confirmation
               step before it actually runs. When its result says
               confirmationRequired, tell the user plainly what will be
               deleted, then tell them exactly how to confirm: call
               POST /api/agent/confirm with the confirmationToken value from
               the tool result (quote the actual token) and approve: true —
               replying here with "yes" or "confirm" does nothing. Never say
               the deletion already happened.
            """;
}
