package com.example.employee.agent;

import java.util.List;

/**
 * Result of a supervisor turn (either {@code process} or {@code confirm}).
 * Carries the conversation ID forward so the caller can continue the same
 * conversation, surfaces a pending human-in-the-loop confirmation
 * structurally rather than leaving a UI to parse it out of {@code text},
 * and names which {@code EmployeeSubAgent}(s) actually handled the turn —
 * e.g. {@code ["search_employees", "update_employee"]} — so a UI can show
 * the caller which agent did the work instead of just the prose reply.
 */
public record AgentReply(
        String text,
        String conversationId,
        boolean confirmationRequired,
        String confirmationToken,
        List<String> toolsUsed) {

    public static AgentReply of(String text, String conversationId) {
        return new AgentReply(text, conversationId, false, null, List.of());
    }
}
