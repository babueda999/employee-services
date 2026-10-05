package com.example.employee.dto;

import java.util.List;

public class AgentResponse {

    private String reply;

    /**
     * Echoes back the conversation this reply belongs to — pass it as
     * {@code conversationId} on the next {@code AgentRequest} to continue
     * the same conversation.
     */
    private String conversationId;

    /**
     * True when the reply describes an irreversible action (currently just
     * {@code delete_employee}) that was authorized but held back pending a
     * separate human confirmation — see {@code confirmationToken}.
     */
    private boolean confirmationRequired;

    /**
     * Single-use token to pass to {@code POST /api/agent/confirm} to approve
     * or deny the pending action. Null unless {@code confirmationRequired}.
     */
    private String confirmationToken;

    /**
     * Tool names of the {@code EmployeeSubAgent}(s) that actually handled
     * this turn, in the order they ran — e.g. {@code ["search_employees",
     * "update_employee"]}. Empty when the model answered without calling
     * any tool.
     */
    private List<String> toolsUsed;

    public AgentResponse() {
    }

    public AgentResponse(String reply) {
        this.reply = reply;
    }

    public AgentResponse(
            String reply,
            String conversationId,
            boolean confirmationRequired,
            String confirmationToken,
            List<String> toolsUsed) {

        this.reply = reply;
        this.conversationId = conversationId;
        this.confirmationRequired = confirmationRequired;
        this.confirmationToken = confirmationToken;
        this.toolsUsed = toolsUsed;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public List<String> getToolsUsed() {
        return toolsUsed;
    }

    public void setToolsUsed(List<String> toolsUsed) {
        this.toolsUsed = toolsUsed;
    }
}
