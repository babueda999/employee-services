package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.UpdateEmployeeTool;
import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.guardrails.AuthorizationGuardrail;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateEmployeeAgentTest {

    @Mock
    private EmployeeService employeeService;

    private UpdateEmployeeAgent updateEmployeeAgent;

    @BeforeEach
    void setUp() {
        EmployeeTools employeeTools = new EmployeeTools(employeeService, new ObjectMapper());
        updateEmployeeAgent = new UpdateEmployeeAgent(
                employeeTools, employeeService, new ObjectMapper(), new AuthorizationGuardrail());
    }

    private EmployeeResponse sampleResponse(Long id) {
        return new EmployeeResponse(
                id, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0, true);
    }

    @Test
    void name_returnsUpdateEmployeeToolName() {
        assertThat(updateEmployeeAgent.name()).isEqualTo(UpdateEmployeeTool.NAME);
    }

    @Test
    void authorize_doesNotThrow_whenRoleCanUpdate() {
        updateEmployeeAgent.authorize("MANAGER");
    }

    @Test
    void authorize_throwsSecurityException_whenRoleCannotUpdate() {
        assertThatThrownBy(() -> updateEmployeeAgent.authorize("USER"))
                .isInstanceOf(SecurityException.class);
    }

    // --- handle (supervisor-dispatched tool call) ---

    @Test
    void handle_returnsUpdatedEmployee_whenRoleCanUpdate() {
        when(employeeService.updateEmployee(eq(1L), any())).thenReturn(sampleResponse(1L));

        String result = updateEmployeeAgent.handle(
                "{\"employeeId\":1,\"firstName\":\"John\",\"lastName\":\"Doe\","
                        + "\"email\":\"john.doe@example.com\",\"department\":\"Engineering\",\"salary\":75000.0}",
                "MANAGER");

        assertThat(result).contains("\"id\":1");
    }

    @Test
    void handle_throwsSecurityException_whenRoleCannotUpdate() {
        assertThatThrownBy(() -> updateEmployeeAgent.handle(
                "{\"employeeId\":1,\"firstName\":\"John\",\"lastName\":\"Doe\","
                        + "\"email\":\"john.doe@example.com\",\"department\":\"Engineering\",\"salary\":75000.0}",
                "USER"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void handle_throwsSecurityException_whenRoleOmitted() {
        assertThatThrownBy(() -> updateEmployeeAgent.handle(
                "{\"employeeId\":1,\"firstName\":\"John\",\"lastName\":\"Doe\","
                        + "\"email\":\"john.doe@example.com\",\"department\":\"Engineering\",\"salary\":75000.0}",
                null))
                .isInstanceOf(SecurityException.class);
    }

    // --- updateEmployee (direct MCP tool) ---

    @Test
    void updateEmployee_delegatesToEmployeeServiceWithFullReplaceRequest_whenRoleCanUpdate() {
        ArgumentCaptor<EmployeeRequest> requestCaptor = ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(eq(1L), requestCaptor.capture()))
                .thenReturn(sampleResponse(1L));

        EmployeeResponse result = updateEmployeeAgent.updateEmployee(
                1L, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0, "ADMIN");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(requestCaptor.getValue().getFirstName()).isEqualTo("John");
        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(75000.0);
    }

    @Test
    void updateEmployee_throwsSecurityException_whenRoleCannotUpdate() {
        assertThatThrownBy(() -> updateEmployeeAgent.updateEmployee(
                1L, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0, "USER"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void updateEmployee_propagatesNotFoundException() {
        when(employeeService.updateEmployee(eq(99L), any()))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        assertThatThrownBy(() -> updateEmployeeAgent.updateEmployee(
                99L, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0, "MANAGER"))
                .isInstanceOf(EmployeeNotFoundException.class);
    }
}
