package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.DeleteEmployeeTool;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.guardrails.AuthorizationGuardrail;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteEmployeeAgentTest {

    @Mock
    private EmployeeService employeeService;

    private DeleteEmployeeAgent deleteEmployeeAgent;

    @BeforeEach
    void setUp() {
        EmployeeTools employeeTools = new EmployeeTools(employeeService, new ObjectMapper());
        deleteEmployeeAgent = new DeleteEmployeeAgent(
                employeeTools, employeeService, new ObjectMapper(), new AuthorizationGuardrail());
    }

    @Test
    void name_returnsDeleteEmployeeToolName() {
        assertThat(deleteEmployeeAgent.name()).isEqualTo(DeleteEmployeeTool.NAME);
    }

    @Test
    void authorize_doesNotThrow_whenRoleCanDelete() {
        deleteEmployeeAgent.authorize("ADMIN");
    }

    @Test
    void authorize_throwsSecurityException_whenRoleCannotDelete() {
        assertThatThrownBy(() -> deleteEmployeeAgent.authorize("MANAGER"))
                .isInstanceOf(SecurityException.class);
    }

    // --- handle (supervisor-dispatched tool call) ---

    @Test
    void handle_deletesEmployee_whenRoleCanDelete() {
        String result = deleteEmployeeAgent.handle("{\"employeeId\":1}", "ADMIN");

        assertThat(result).contains("\"success\": true");
        verify(employeeService, times(1)).deleteEmployee(1L);
    }

    @Test
    void handle_throwsSecurityException_whenRoleCannotDelete() {
        assertThatThrownBy(() -> deleteEmployeeAgent.handle("{\"employeeId\":1}", "MANAGER"))
                .isInstanceOf(SecurityException.class);

        verify(employeeService, never()).deleteEmployee(1L);
    }

    // --- deleteEmployee (direct MCP tool) ---

    @Test
    void deleteEmployee_delegatesToEmployeeService_whenRoleCanDelete() {
        deleteEmployeeAgent.deleteEmployee(1L, "ADMIN");

        verify(employeeService, times(1)).deleteEmployee(1L);
    }

    @Test
    void deleteEmployee_throwsSecurityException_whenRoleOmitted() {
        assertThatThrownBy(() -> deleteEmployeeAgent.deleteEmployee(1L, null))
                .isInstanceOf(SecurityException.class);

        verify(employeeService, never()).deleteEmployee(1L);
    }

    @Test
    void deleteEmployee_propagatesNotFoundException() {
        Mockito.doThrow(new EmployeeNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deleteEmployee(99L);

        assertThatThrownBy(() -> deleteEmployeeAgent.deleteEmployee(99L, "ADMIN"))
                .isInstanceOf(EmployeeNotFoundException.class);
    }
}
