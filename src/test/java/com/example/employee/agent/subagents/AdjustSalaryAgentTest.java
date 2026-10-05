package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.AdjustSalaryTool;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdjustSalaryAgentTest {

    @Mock
    private EmployeeService employeeService;

    private AdjustSalaryAgent adjustSalaryAgent;

    @BeforeEach
    void setUp() {
        EmployeeTools employeeTools = new EmployeeTools(employeeService, new ObjectMapper());
        adjustSalaryAgent = new AdjustSalaryAgent(
                employeeTools, employeeService, new ObjectMapper(), new AuthorizationGuardrail());
    }

    private EmployeeResponse sampleResponse(Long id) {
        return new EmployeeResponse(
                id, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0, true);
    }

    @Test
    void name_returnsAdjustSalaryToolName() {
        assertThat(adjustSalaryAgent.name()).isEqualTo(AdjustSalaryTool.NAME);
    }

    @Test
    void authorize_doesNotThrow_whenRoleIsManager() {
        adjustSalaryAgent.authorize("MANAGER");
    }

    @Test
    void authorize_throwsSecurityException_whenRoleIsAdmin() {
        assertThatThrownBy(() -> adjustSalaryAgent.authorize("ADMIN"))
                .isInstanceOf(SecurityException.class);
    }

    // --- handle (supervisor-dispatched tool call) ---

    @Test
    void handle_appliesFlatIncrease_whenRoleIsManager() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));
        ArgumentCaptor<EmployeeRequest> requestCaptor = ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(eq(1L), requestCaptor.capture())).thenReturn(sampleResponse(1L));

        adjustSalaryAgent.handle("{\"employeeId\":1,\"amount\":5000.0,\"isPercentage\":false}", "MANAGER");

        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(80000.0);
    }

    @Test
    void handle_throwsSecurityException_whenRoleIsAdmin() {
        assertThatThrownBy(() -> adjustSalaryAgent.handle(
                "{\"employeeId\":1,\"amount\":5000.0,\"isPercentage\":false}", "ADMIN"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void handle_throwsSecurityException_whenRoleIsUser() {
        assertThatThrownBy(() -> adjustSalaryAgent.handle(
                "{\"employeeId\":1,\"amount\":5000.0,\"isPercentage\":false}", "USER"))
                .isInstanceOf(SecurityException.class);
    }

    // --- adjustSalary (direct MCP tool) ---

    @Test
    void adjustSalary_appliesPercentageDecrease_whenRoleIsManager() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));
        ArgumentCaptor<EmployeeRequest> requestCaptor = ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(eq(1L), requestCaptor.capture())).thenReturn(sampleResponse(1L));

        adjustSalaryAgent.adjustSalary(1L, -10.0, true, "MANAGER");

        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(67500.0);
    }

    @Test
    void adjustSalary_throwsSecurityException_whenRoleOmitted() {
        assertThatThrownBy(() -> adjustSalaryAgent.adjustSalary(1L, 5000.0, false, null))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void adjustSalary_propagatesNotFoundException() {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        assertThatThrownBy(() -> adjustSalaryAgent.adjustSalary(99L, 1000.0, false, "MANAGER"))
                .isInstanceOf(EmployeeNotFoundException.class);
    }
}
