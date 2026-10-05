package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.ListEmployeesTool;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.guardrails.AuthorizationGuardrail;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListEmployeesAgentTest {

    @Mock
    private EmployeeService employeeService;

    private ListEmployeesAgent listEmployeesAgent;

    @BeforeEach
    void setUp() {
        EmployeeTools employeeTools = new EmployeeTools(employeeService, new ObjectMapper());
        listEmployeesAgent = new ListEmployeesAgent(
                employeeTools, employeeService, new AuthorizationGuardrail());
    }

    private EmployeeResponse sampleResponse(Long id) {
        return new EmployeeResponse(
                id, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0, true);
    }

    @Test
    void name_returnsListEmployeesToolName() {
        assertThat(listEmployeesAgent.name()).isEqualTo(ListEmployeesTool.NAME);
    }

    @Test
    void authorize_throwsSecurityException_whenRoleCannotRead() {
        assertThatThrownBy(() -> listEmployeesAgent.authorize("MANAGER"))
                .isInstanceOf(SecurityException.class);
    }

    // --- handle (supervisor-dispatched tool call) ---

    @Test
    void handle_returnsSerializedEmployees_whenRoleCanRead() {
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

        String result = listEmployeesAgent.handle("{}", "USER");

        assertThat(result).contains("\"id\":1", "\"id\":2");
    }

    @Test
    void handle_throwsSecurityException_whenRoleCannotRead() {
        assertThatThrownBy(() -> listEmployeesAgent.handle("{}", "MANAGER"))
                .isInstanceOf(SecurityException.class);
    }

    // --- listEmployees (direct MCP tool) ---

    @Test
    void listEmployees_delegatesToEmployeeService_whenRoleCanRead() {
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse(1L)));

        List<EmployeeResponse> result = listEmployeesAgent.listEmployees("ADMIN");

        assertThat(result).hasSize(1);
    }

    @Test
    void listEmployees_defaultsToUserRole_whenRoleOmitted() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());

        List<EmployeeResponse> result = listEmployeesAgent.listEmployees(null);

        assertThat(result).isEmpty();
    }

    @Test
    void listEmployees_throwsSecurityException_whenRoleCannotRead() {
        assertThatThrownBy(() -> listEmployeesAgent.listEmployees("MANAGER"))
                .isInstanceOf(SecurityException.class);
    }
}
