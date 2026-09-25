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
}
