package com.example.employee.agent;

import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeToolsTest {

    @Mock
    private EmployeeService employeeService;

    private EmployeeTools employeeTools;

    @BeforeEach
    void setUp() {
        employeeTools = new EmployeeTools(employeeService, new ObjectMapper());
    }

    private EmployeeResponse sampleResponse(Long id) {
        return new EmployeeResponse(
                id, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);
    }

    // --- getEmployee ---

    @Test
    void getEmployee_returnsSerializedEmployee_whenFound() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));

        String result = employeeTools.getEmployee(1L);

        assertThat(result).contains("\"id\":1", "john.doe@example.com");
    }

    @Test
    void getEmployee_returnsFailureJson_whenIdIsNull() {
        String result = employeeTools.getEmployee(null);

        assertThat(result).contains("\"success\": false", "Employee ID is required");
    }

    @Test
    void getEmployee_returnsNotFoundJson_whenEmployeeDoesNotExist() {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        String result = employeeTools.getEmployee(99L);

        assertThat(result).contains("\"success\": false", "Employee not found");
    }

    @Test
    void getEmployee_returnsFailureJson_whenServiceThrowsUnexpectedException() {
        when(employeeService.getEmployeeById(1L))
                .thenThrow(new RuntimeException("database connection lost"));

        String result = employeeTools.getEmployee(1L);

        assertThat(result).contains("\"success\": false", "Unable to retrieve employee");
    }

    // --- listEmployees ---

    @Test
    void listEmployees_returnsSerializedList() {
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

        String result = employeeTools.listEmployees();

        assertThat(result).contains("\"id\":1", "\"id\":2");
    }

    @Test
    void listEmployees_returnsSerializedEmptyList_whenNoEmployees() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());

        String result = employeeTools.listEmployees();

        assertThat(result).isEqualTo("[]");
    }

    // --- searchEmployees ---

    @Test
    void searchEmployees_returnsFailureJson_whenNameAndDepartmentBlank() {
        String result = employeeTools.searchEmployees(" ", " ");

        assertThat(result).contains("\"success\": false", "Search name or department is required");
    }

    @Test
    void searchEmployees_returnsOnlyMatchingEmployees_whenNameProvided() {
        EmployeeResponse john = sampleResponse(1L);
        EmployeeResponse jane = new EmployeeResponse(
                2L, "Jane", "Smith", "jane.smith@example.com", "Sales", 65000.0);
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(john, jane));

        String result = employeeTools.searchEmployees("doe", null);

        assertThat(result).contains("\"id\":1").doesNotContain("\"id\":2");
    }

    @Test
    void searchEmployees_returnsEmptyList_whenNoNameMatches() {
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse(1L)));

        String result = employeeTools.searchEmployees("nonexistent", null);

        assertThat(result).isEqualTo("[]");
    }

    @Test
    void searchEmployees_returnsOnlyMatchingEmployees_whenDepartmentProvided() {
        EmployeeResponse john = sampleResponse(1L);
        EmployeeResponse jane = new EmployeeResponse(
                2L, "Jane", "Smith", "jane.smith@example.com", "Sales", 65000.0);
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(john, jane));

        String result = employeeTools.searchEmployees(null, "Engineering");

        assertThat(result).contains("\"id\":1").doesNotContain("\"id\":2");
    }

    @Test
    void searchEmployees_returnsMatchingDepartment_caseInsensitive() {
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse(1L)));

        String result = employeeTools.searchEmployees(null, "engineering");

        assertThat(result).contains("\"id\":1");
    }

    @Test
    void searchEmployees_appliesBothFilters_whenNameAndDepartmentProvided() {
        EmployeeResponse john = sampleResponse(1L);
        EmployeeResponse johnSales = new EmployeeResponse(
                2L, "John", "Appleseed", "john.appleseed@example.com", "Sales", 65000.0);
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(john, johnSales));

        String result = employeeTools.searchEmployees("john", "Engineering");

        assertThat(result).contains("\"id\":1").doesNotContain("\"id\":2");
    }

    @Test
    void searchEmployees_returnsEmptyList_whenNoDepartmentMatches() {
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse(1L)));

        String result = employeeTools.searchEmployees(null, "Nonexistent");

        assertThat(result).isEqualTo("[]");
    }
}
