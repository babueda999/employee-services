package com.example.employee.guardrails;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * USER may only view records. MANAGER may update them (including salary
 * adjustments) but not view or delete. ADMIN may view, update, and delete —
 * except salary adjustment, which stays MANAGER-exclusive by design.
 */
class AuthorizationGuardrailTest {

    private final AuthorizationGuardrail authorizationGuardrail = new AuthorizationGuardrail();

    // --- validateRecognizedRole ---

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER", "ADMIN", "user", "manager", "admin"})
    void validateRecognizedRole_allowsAnyRecognizedRole(String role) {
        assertThatCode(() -> authorizationGuardrail.validateRecognizedRole(role))
                .doesNotThrowAnyException();
    }

    @Test
    void validateRecognizedRole_throwsSecurityException_whenRoleUnrecognized() {
        assertThatThrownBy(() -> authorizationGuardrail.validateRecognizedRole("GUEST"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Unrecognized role: GUEST");
    }

    @Test
    void validateRecognizedRole_throwsSecurityException_whenRoleIsBlank() {
        assertThatThrownBy(() -> authorizationGuardrail.validateRecognizedRole("  "))
                .isInstanceOf(SecurityException.class)
                .hasMessage("User role is required.");
    }

    // --- checkReadAccess ---

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    void checkReadAccess_allowsUserAndAdmin(String role) {
        assertThatCode(() -> authorizationGuardrail.checkReadAccess(role))
                .doesNotThrowAnyException();
    }

    @Test
    void checkReadAccess_throwsSecurityException_whenRoleIsManager() {
        assertThatThrownBy(() -> authorizationGuardrail.checkReadAccess("MANAGER"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("User does not have permission to read employee information.");
    }

    @Test
    void checkReadAccess_throwsSecurityException_whenRoleIsBlank() {
        assertThatThrownBy(() -> authorizationGuardrail.checkReadAccess("  "))
                .isInstanceOf(SecurityException.class)
                .hasMessage("User role is required.");
    }

    // --- checkCreateAccess ---

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void checkCreateAccess_allowsManagerAndAdmin(String role) {
        assertThatCode(() -> authorizationGuardrail.checkCreateAccess(role))
                .doesNotThrowAnyException();
    }

    @Test
    void checkCreateAccess_throwsSecurityException_whenRoleIsUser() {
        assertThatThrownBy(() -> authorizationGuardrail.checkCreateAccess("USER"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("User does not have permission to create employees.");
    }

    // --- checkUpdateAccess ---

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", "ADMIN"})
    void checkUpdateAccess_allowsManagerAndAdmin(String role) {
        assertThatCode(() -> authorizationGuardrail.checkUpdateAccess(role))
                .doesNotThrowAnyException();
    }

    @Test
    void checkUpdateAccess_throwsSecurityException_whenRoleIsUser() {
        assertThatThrownBy(() -> authorizationGuardrail.checkUpdateAccess("USER"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("User does not have permission to update employees.");
    }

    // --- checkDeleteAccess ---

    @Test
    void checkDeleteAccess_allowsAdmin() {
        assertThatCode(() -> authorizationGuardrail.checkDeleteAccess("ADMIN"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER"})
    void checkDeleteAccess_throwsSecurityException_whenRoleIsNotAdmin(String role) {
        assertThatThrownBy(() -> authorizationGuardrail.checkDeleteAccess(role))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Only administrators can delete employees.");
    }

    // --- checkSalaryAdjustmentAccess ---

    @Test
    void checkSalaryAdjustmentAccess_allowsManager() {
        assertThatCode(() -> authorizationGuardrail.checkSalaryAdjustmentAccess("MANAGER"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "ADMIN"})
    void checkSalaryAdjustmentAccess_throwsSecurityException_whenRoleIsNotManager(String role) {
        assertThatThrownBy(() -> authorizationGuardrail.checkSalaryAdjustmentAccess(role))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Only managers can adjust employee salaries.");
    }
}
