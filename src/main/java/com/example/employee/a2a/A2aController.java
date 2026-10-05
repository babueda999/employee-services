package com.example.employee.a2a;

import com.example.employee.agent.AgentReply;
import com.example.employee.agent.EmployeeSupervisorAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Minimal hand-rolled A2A (Agent2Agent) JSON-RPC endpoint over EmployeeSupervisorAgent,
 * for the Python employee-rag-langgraph agent's tools/a2a_tools.py client to
 * delegate employee write requests to. No A2A SDK dependency: the official
 * Java SDK's reference server targets Quarkus, and this app is Spring Boot,
 * so pulling it in would fight the framework rather than fit it. Instead this
 * implements just the two calls that client actually makes -- captured
 * directly off the wire from the Python side's own (SDK-based) A2A server so
 * the two interoperate exactly: GET the agent card, POST a "SendMessage"
 * JSON-RPC envelope and get one back.
 */
@RestController
@RequestMapping("/a2a")
public class A2aController {

    private final EmployeeSupervisorAgent employeeSupervisorAgent;

    public A2aController(EmployeeSupervisorAgent employeeSupervisorAgent) {
        this.employeeSupervisorAgent = employeeSupervisorAgent;
    }

    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> agentCard() {
        return Map.of(
                "name", "employee-write-agent",
                "description",
                "Updates employee records and adjusts salaries, gated by caller role "
                        + "(MANAGER can update/adjust salary; ADMIN can update/delete; "
                        + "deletion is never exposed to MANAGER-level callers).",
                "version", "1.0.0",
                "supportedInterfaces", List.of(Map.of(
                        "url", "http://localhost:8080/a2a",
                        "protocolBinding", "JSONRPC",
                        "protocolVersion", "1.0.0"
                )),
                "capabilities", Map.of("streaming", false),
                "defaultInputModes", List.of("text/plain"),
                "defaultOutputModes", List.of("text/plain"),
                "skills", List.of(
                        Map.of(
                                "id", "update_employee",
                                "name", "Update employee",
                                "description", "Update an employee record's fields (MANAGER or ADMIN).",
                                "tags", List.of("employee", "write"),
                                "examples", List.of("Update employee 3's department to Sales")
                        ),
                        Map.of(
                                "id", "adjust_salary",
                                "name", "Adjust salary",
                                "description", "Give an employee a raise or pay cut, flat or percentage (MANAGER only).",
                                "tags", List.of("employee", "write"),
                                "examples", List.of("Give employee 7 a 10% raise")
                        )
                )
        );
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> handle(@RequestBody Map<String, Object> request) {

        Object id = request.get("id");
        String method = (String) request.get("method");

        if (!"SendMessage".equals(method)) {
            return ResponseEntity.ok(errorEnvelope(id, -32601, "Method not found: " + method));
        }

        Map<String, Object> params = (Map<String, Object>) request.get("params");
        Map<String, Object> message = params == null ? null : (Map<String, Object>) params.get("message");
        if (message == null) {
            return ResponseEntity.ok(errorEnvelope(id, -32602, "Missing params.message"));
        }

        String text = extractText(message);
        String role = extractAuthRole(message);
        String contextId = extractContextId(message);

        AgentReply reply;
        try {
            reply = employeeSupervisorAgent.process(text, role, contextId);
        } catch (RuntimeException ex) {
            // Never bubble a stack trace over the wire -- report the
            // guardrail/validation message as the agent's own reply, the
            // same way tools/employee_tools.py never raises on the Python
            // side.
            reply = AgentReply.of(
                    ex.getMessage(),
                    contextId == null ? UUID.randomUUID().toString() : contextId);
        }

        return ResponseEntity.ok(successEnvelope(id, reply));
    }

    private String extractText(Map<String, Object> message) {
        Object partsObj = message.get("parts");
        if (partsObj instanceof List<?> parts) {
            for (Object part : parts) {
                if (part instanceof Map<?, ?> partMap && partMap.get("text") instanceof String text) {
                    return text;
                }
            }
        }
        return "";
    }

    private String extractAuthRole(Map<String, Object> message) {
        Object metadataObj = message.get("metadata");
        if (metadataObj instanceof Map<?, ?> metadata && metadata.get("authRole") instanceof String role) {
            return role;
        }
        return null;
    }

    /**
     * Reused as the conversation ID for {@link EmployeeSupervisorAgent}, so
     * a Python-side caller that keeps sending the same {@code contextId}
     * gets conversational memory across turns for free.
     */
    private String extractContextId(Map<String, Object> message) {
        Object contextId = message.get("contextId");
        return contextId instanceof String id && !id.isBlank() ? id : null;
    }

    private Map<String, Object> successEnvelope(Object id, AgentReply reply) {
        Map<String, Object> responseMessage = new LinkedHashMap<>();
        responseMessage.put("messageId", UUID.randomUUID().toString());
        responseMessage.put("contextId", reply.conversationId());
        responseMessage.put("taskId", UUID.randomUUID().toString());
        responseMessage.put("role", "ROLE_AGENT");
        responseMessage.put("parts", List.of(Map.of("text", reply.text() == null ? "" : reply.text())));

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("jsonrpc", "2.0");
        envelope.put("id", id);
        envelope.put("result", Map.of("message", responseMessage));
        return envelope;
    }

    private Map<String, Object> errorEnvelope(Object id, int code, String message) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("jsonrpc", "2.0");
        envelope.put("id", id);
        envelope.put("error", Map.of("code", code, "message", message));
        return envelope;
    }
}
