package com.example.employee.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentRequest {

    @NotBlank(message = "Message is required")
    private String message;

    /**
     * Caller's role for authorization (USER, MANAGER, or ADMIN). Optional;
     * defaults to USER when omitted. See AuthorizationGuardrail.
     */
    private String role;

    /**
     * Identifies a conversation so prior turns are replayed as context.
     * Optional; omit it to start a new conversation — the response's
     * {@code conversationId} is then the one to send back on the next turn.
     */
    private String conversationId;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
}
