package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.SearchEmployeeTool;
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
class SearchEmployeeAgentTest {

    @Mock
    private EmployeeService employeeService;

    private SearchEmployeeAgent searchEmployeeAgent;

    @BeforeEach
    void setUp() {
        EmployeeTools employeeTools = new EmployeeTools(employeeService, new ObjectMapper());
        searchEmployeeAgent = new SearchEmployeeAgent(
                employeeTools, employeeService, new ObjectMapper(), new AuthorizationGuardrail());
    }

    private EmployeeResponse sampleResponse(Long id, String firstName, String department) {
        return new EmployeeResponse(
                id, firstName, "Doe", firstName.toLowerCase() + ".doe@example.com", department, 75000.0, true);
    }

    @Test
    void name_returnsSearchEmployeeToolName() {
        assertThat(searchEmployeeAgent.name()).isEqualTo(SearchEmployeeTool.NAME);
    }

    @Test
    void authorize_throwsSecurityException_whenRoleCannotRead() {
        assertThatThrownBy(() -> searchEmployeeAgent.authorize("MANAGER"))
                .isInstanceOf(SecurityException.class);
    }

    // --- handle (supervisor-dispatched tool call) ---

    @Test
    void handle_returnsMatches_whenRoleCanRead() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                sampleResponse(1L, "John", "Engineering"),
                sampleResponse(2L, "Jane", "Sales")));

        String result = searchEmployeeAgent.handle("{\"name\":\"John\",\"department\":null}", "USER");

        assertThat(result).contains("\"id\":1").doesNotContain("\"id\":2");
    }

    @Test
    void handle_throwsSecurityException_whenRoleCannotRead() {
        assertThatThrownBy(() ->
                searchEmployeeAgent.handle("{\"name\":\"John\",\"department\":null}", "MANAGER"))
                .isInstanceOf(SecurityException.class);
    }

    // --- searchEmployees (direct MCP tool) ---

    @Test
    void searchEmployees_filtersByNameAndDepartment_whenRoleCanRead() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                sampleResponse(1L, "John", "Engineering"),
                sampleResponse(2L, "Jane", "Sales")));

        List<EmployeeResponse> result = searchEmployeeAgent.searchEmployees("john", null, "ADMIN");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void searchEmployees_defaultsToUserRole_whenRoleOmitted() {
        when(employeeService.getAllEmployees()).thenReturn(List.of(
                sampleResponse(1L, "John", "Engineering")));

        List<EmployeeResponse> result = searchEmployeeAgent.searchEmployees(null, "Engineering", null);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchEmployees_throwsIllegalArgumentException_whenNeitherNameNorDepartmentProvided() {
        assertThatThrownBy(() -> searchEmployeeAgent.searchEmployees(null, null, "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Employee name or department is required");
    }

    @Test
    void searchEmployees_throwsSecurityException_whenRoleCannotRead() {
        assertThatThrownBy(() -> searchEmployeeAgent.searchEmployees("John", null, "MANAGER"))
                .isInstanceOf(SecurityException.class);
    }
}
