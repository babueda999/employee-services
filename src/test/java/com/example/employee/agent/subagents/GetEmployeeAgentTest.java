package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.GetEmployeeTool;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.guardrails.AuthorizationGuardrail;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetEmployeeAgentTest {

    @Mock
    private EmployeeService employeeService;

    private GetEmployeeAgent getEmployeeAgent;

    @BeforeEach
    void setUp() {
        EmployeeTools employeeTools = new EmployeeTools(employeeService, new ObjectMapper());
        getEmployeeAgent = new GetEmployeeAgent(
                employeeTools, employeeService, new ObjectMapper(), new AuthorizationGuardrail());
    }

    private EmployeeResponse sampleResponse(Long id) {
        return new EmployeeResponse(
                id, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0, true);
    }

    @Test
    void name_returnsGetEmployeeToolName() {
        assertThat(getEmployeeAgent.name()).isEqualTo(GetEmployeeTool.NAME);
    }

    @Test
    void authorize_doesNotThrow_whenRoleCanRead() {
        getEmployeeAgent.authorize("USER");
    }

    @Test
    void authorize_throwsSecurityException_whenRoleCannotRead() {
        assertThatThrownBy(() -> getEmployeeAgent.authorize("MANAGER"))
                .isInstanceOf(SecurityException.class);
    }

    // --- handle (supervisor-dispatched tool call) ---

    @Test
    void handle_returnsSerializedEmployee_whenRoleCanRead() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));

        String result = getEmployeeAgent.handle("{\"employeeId\":1}", "USER");

        assertThat(result).contains("\"id\":1", "john.doe@example.com");
    }

    @Test
    void handle_defaultsToUserRole_whenRoleIsNull() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));

        String result = getEmployeeAgent.handle("{\"employeeId\":1}", null);

        assertThat(result).contains("\"id\":1");
    }

    @Test
    void handle_throwsSecurityException_whenRoleCannotRead() {
        assertThatThrownBy(() -> getEmployeeAgent.handle("{\"employeeId\":1}", "MANAGER"))
                .isInstanceOf(SecurityException.class);
    }

    // --- getEmployee (direct MCP tool) ---

    @Test
    void getEmployee_delegatesToEmployeeService_whenRoleCanRead() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));

        EmployeeResponse result = getEmployeeAgent.getEmployee(1L, "ADMIN");

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getEmployee_defaultsToUserRole_whenRoleOmitted() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));

        EmployeeResponse result = getEmployeeAgent.getEmployee(1L, null);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getEmployee_throwsSecurityException_whenRoleCannotRead() {
        assertThatThrownBy(() -> getEmployeeAgent.getEmployee(1L, "MANAGER"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void getEmployee_propagatesNotFoundException() {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        assertThatThrownBy(() -> getEmployeeAgent.getEmployee(99L, "USER"))
                .isInstanceOf(EmployeeNotFoundException.class);
    }
}
