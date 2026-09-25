package com.example.employee.guardrails;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class AuthorizationGuardrail {

    /**
     * All roles the agent recognizes at all, regardless of what each is
     * permitted to do — used only to fail fast on a garbage role before
     * any per-operation check runs. See {@link #validateRecognizedRole}.
     */
    private static final Set<String> ALL_ROLES = Set.of(
            "USER",
            "MANAGER",
            "ADMIN"
    );

    /**
     * USER may only view records. MANAGER may update them (including salary
     * adjustments) but not view or delete. ADMIN can view, update, and
     * delete — the one role that isn't restricted to a single action —
     * except salary adjustment, which stays MANAGER-exclusive by explicit
     * design (see {@link #checkSalaryAdjustmentAccess}).
     */
    private static final Set<String> READ_ROLES = Set.of(
            "USER",
            "ADMIN"
    );

    private static final Set<String> CREATE_ROLES = Set.of(
            "MANAGER",
            "ADMIN"
    );

    private static final Set<String> UPDATE_ROLES = Set.of(
            "MANAGER",
            "ADMIN"
    );

    private static final Set<String> DELETE_ROLES = Set.of(
            "ADMIN"
    );

    private static final Set<String> SALARY_ADJUSTMENT_ROLES = Set.of(
            "MANAGER"
    );

    /**
     * Fails fast on a null, blank, or unrecognized role before any
     * per-operation check or OpenAI call runs. Does not by itself grant
     * any permission — every recognized role still needs the matching
     * check<Operation>Access call for the specific action it's attempting.
     */
    public void validateRecognizedRole(String role) {

        validateRole(role);

        if (!ALL_ROLES.contains(role.toUpperCase())) {
            throw new SecurityException(
                    "Unrecognized role: " + role
            );
        }
    }

    public void checkReadAccess(String role) {

        validateRole(role);

        if (!READ_ROLES.contains(role.toUpperCase())) {
            throw new SecurityException(
                    "User does not have permission to read employee information."
            );
        }
    }

    public void checkCreateAccess(String role) {

        validateRole(role);

        if (!CREATE_ROLES.contains(role.toUpperCase())) {
            throw new SecurityException(
                    "User does not have permission to create employees."
            );
        }
    }

    public void checkUpdateAccess(String role) {

        validateRole(role);

        if (!UPDATE_ROLES.contains(role.toUpperCase())) {
            throw new SecurityException(
                    "User does not have permission to update employees."
            );
        }
    }

    /**
     * Salary increments/adjustments are restricted to MANAGER specifically —
     * unlike general updates, ADMIN is deliberately not included here.
     */
    public void checkSalaryAdjustmentAccess(String role) {

        validateRole(role);

        if (!SALARY_ADJUSTMENT_ROLES.contains(role.toUpperCase())) {
            throw new SecurityException(
                    "Only managers can adjust employee salaries."
            );
        }
    }

    public void checkDeleteAccess(String role) {

        validateRole(role);

        if (!DELETE_ROLES.contains(role.toUpperCase())) {
            throw new SecurityException(
                    "Only administrators can delete employees."
            );
        }
    }

    private void validateRole(String role) {

        if (role == null || role.isBlank()) {
            throw new SecurityException(
                    "User role is required."
            );
        }
    }
}
