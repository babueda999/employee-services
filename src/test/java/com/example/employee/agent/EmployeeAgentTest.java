package com.example.employee.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class EmployeeAgentTest {

    @Mock
    private EmployeeTools employeeTools;

    private EmployeeAgent employeeAgent;

    @BeforeEach
    void setUp() {
        employeeAgent = new EmployeeAgent(employeeTools, new ObjectMapper());
    }

    @Test
    void process_throwsIllegalArgumentException_whenMessageIsNull() {
        assertThatThrownBy(() -> employeeAgent.process(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User message cannot be empty");
    }

    @Test
    void process_throwsIllegalArgumentException_whenMessageIsBlank() {
        assertThatThrownBy(() -> employeeAgent.process("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User message cannot be empty");
    }
}
