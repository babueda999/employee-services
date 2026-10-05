package com.example.employee.confirmation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A CRITICAL-risk tool call (see {@code ToolGuardrail}) that has been
 * authorized but is held back pending a separate, explicit human
 * confirmation before it actually executes — see
 * {@code EmployeeSupervisorAgent#confirm}. The {@code id} doubles as the
 * single-use confirmation token handed back to the caller.
 */
@Entity
@Table(name = "pending_tool_confirmations")
public class PendingToolConfirmation {

    @Id
    private String id;

    @Column(nullable = false)
    private String conversationId;

    @Column(nullable = false)
    private String toolName;

    @Column(nullable = false, length = 2000)
    private String argumentsJson;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private Instant createdAt;

    public PendingToolConfirmation() {
    }

    public PendingToolConfirmation(
            String id,
            String conversationId,
            String toolName,
            String argumentsJson,
            String role,
            Instant createdAt) {

        this.id = id;
        this.conversationId = conversationId;
        this.toolName = toolName;
        this.argumentsJson = argumentsJson;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    public void setArgumentsJson(String argumentsJson) {
        this.argumentsJson = argumentsJson;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
