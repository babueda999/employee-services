package com.example.employee.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Only the validation performed before any OpenAI call is unit-testable here:
 * the client is built lazily from environment credentials on first use, so
 * exercising a real request requires an OpenAI credential and is out of scope
 * for these tests.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeAgentServiceTest {

    @Mock
    private EmployeeService employeeService;

    private EmployeeAgentService employeeAgentService;

    @BeforeEach
    void setUp() {
        employeeAgentService = new EmployeeAgentService(employeeService);
    }

    @Test
    void processRequest_throwsIllegalArgumentException_whenMessageIsNull() {
        assertThatThrownBy(() -> employeeAgentService.processRequest(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User message cannot be empty");
    }

    @Test
    void processRequest_throwsIllegalArgumentException_whenMessageIsBlank() {
        assertThatThrownBy(() -> employeeAgentService.processRequest("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User message cannot be empty");
    }
}
