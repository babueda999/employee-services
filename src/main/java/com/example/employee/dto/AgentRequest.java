package com.example.employee.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentRequest {

    @NotBlank(message = "Message is required")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
