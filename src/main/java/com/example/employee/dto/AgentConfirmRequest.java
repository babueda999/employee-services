package com.example.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AgentConfirmRequest {

    @NotBlank(message = "Confirmation token is required")
    private String confirmationToken;

    @NotNull(message = "Approve is required")
    private Boolean approve;

    /**
     * Role of the caller approving/denying this confirmation — separate
     * from whatever role originally requested the action. Required to be
     * MANAGER (see AuthorizationGuardrail.checkConfirmationApprovalAccess);
     * optional in the request only in the sense that omitting it defaults
     * to USER, which is then rejected.
     */
    private String role;

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public Boolean getApprove() {
        return approve;
    }

    public void setApprove(Boolean approve) {
        this.approve = approve;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
