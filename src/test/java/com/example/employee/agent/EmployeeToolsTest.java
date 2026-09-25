package com.example.employee.agent;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    // --- updateEmployee ---

    @Test
    void updateEmployee_returnsSerializedEmployee_whenSuccessful() {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class)))
                .thenReturn(sampleResponse(1L));

        String result = employeeTools.updateEmployee(
                1L, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);

        assertThat(result).contains("\"id\":1", "john.doe@example.com");
    }

    @Test
    void updateEmployee_returnsFailureJson_whenIdIsNull() {
        String result = employeeTools.updateEmployee(
                null, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);

        assertThat(result).contains("\"success\": false", "Employee ID is required");
        verify(employeeService, never()).updateEmployee(any(), any());
    }

    @Test
    void updateEmployee_returnsNotFoundJson_whenEmployeeDoesNotExist() {
        when(employeeService.updateEmployee(eq(99L), any(EmployeeRequest.class)))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        String result = employeeTools.updateEmployee(
                99L, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);

        assertThat(result).contains("\"success\": false", "Employee not found");
    }

    @Test
    void updateEmployee_returnsDuplicateJson_whenEmailBelongsToAnotherEmployee() {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class)))
                .thenThrow(new DuplicateEmployeeException(
                        "Email already used by another employee: taken@example.com"));

        String result = employeeTools.updateEmployee(
                1L, "John", "Doe", "taken@example.com", "Engineering", 75000.0);

        assertThat(result).contains("\"success\": false", "Email already used by another employee");
    }

    @Test
    void updateEmployee_returnsFailureJson_whenServiceThrowsUnexpectedException() {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class)))
                .thenThrow(new RuntimeException("database connection lost"));

        String result = employeeTools.updateEmployee(
                1L, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);

        assertThat(result).contains("\"success\": false", "Unable to update employee");
    }

    // --- adjustSalary ---

    @Test
    void adjustSalary_increasesByFlatAmount_whenIsPercentageFalse() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));
        ArgumentCaptor<EmployeeRequest> requestCaptor = ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(eq(1L), requestCaptor.capture()))
                .thenReturn(sampleResponse(1L));

        employeeTools.adjustSalary(1L, 5000.0, false);

        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(80000.0);
        assertThat(requestCaptor.getValue().getFirstName()).isEqualTo("John");
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void adjustSalary_decreasesByFlatAmount_whenAmountIsNegative() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));
        ArgumentCaptor<EmployeeRequest> requestCaptor = ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(eq(1L), requestCaptor.capture()))
                .thenReturn(sampleResponse(1L));

        employeeTools.adjustSalary(1L, -5000.0, false);

        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(70000.0);
    }

    @Test
    void adjustSalary_appliesPercentageIncrease_whenIsPercentageTrue() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));
        ArgumentCaptor<EmployeeRequest> requestCaptor = ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(eq(1L), requestCaptor.capture()))
                .thenReturn(sampleResponse(1L));

        employeeTools.adjustSalary(1L, 10.0, true);

        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(82500.0);
    }

    @Test
    void adjustSalary_treatsNullIsPercentage_asFlatAmount() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));
        ArgumentCaptor<EmployeeRequest> requestCaptor = ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(eq(1L), requestCaptor.capture()))
                .thenReturn(sampleResponse(1L));

        employeeTools.adjustSalary(1L, 1000.0, null);

        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(76000.0);
    }

    @Test
    void adjustSalary_returnsFailureJson_whenIdIsNull() {
        String result = employeeTools.adjustSalary(null, 1000.0, false);

        assertThat(result).contains("\"success\": false", "Employee ID is required");
        verify(employeeService, never()).updateEmployee(any(), any());
    }

    @Test
    void adjustSalary_returnsFailureJson_whenAmountIsNull() {
        String result = employeeTools.adjustSalary(1L, null, false);

        assertThat(result).contains("\"success\": false", "Amount is required");
        verify(employeeService, never()).updateEmployee(any(), any());
    }

    @Test
    void adjustSalary_returnsNotFoundJson_whenEmployeeDoesNotExist() {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        String result = employeeTools.adjustSalary(99L, 1000.0, false);

        assertThat(result).contains("\"success\": false", "Employee not found");
    }

    @Test
    void adjustSalary_returnsFailureJson_whenResultingSalaryNotPositive() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));

        String result = employeeTools.adjustSalary(1L, -100.0, true);

        assertThat(result).contains("\"success\": false", "Resulting salary must be greater than zero");
        verify(employeeService, never()).updateEmployee(any(), any());
    }

    // --- deleteEmployee ---

    @Test
    void deleteEmployee_returnsSuccessJson_whenSuccessful() {
        String result = employeeTools.deleteEmployee(1L);

        assertThat(result).contains("\"success\": true", "Employee deleted");
        verify(employeeService, times(1)).deleteEmployee(1L);
    }

    @Test
    void deleteEmployee_returnsFailureJson_whenIdIsNull() {
        String result = employeeTools.deleteEmployee(null);

        assertThat(result).contains("\"success\": false", "Employee ID is required");
        verify(employeeService, never()).deleteEmployee(any());
    }

    @Test
    void deleteEmployee_returnsNotFoundJson_whenEmployeeDoesNotExist() {
        org.mockito.Mockito.doThrow(new EmployeeNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deleteEmployee(99L);

        String result = employeeTools.deleteEmployee(99L);

        assertThat(result).contains("\"success\": false", "Employee not found");
    }

    @Test
    void deleteEmployee_returnsFailureJson_whenServiceThrowsUnexpectedException() {
        org.mockito.Mockito.doThrow(new RuntimeException("database connection lost"))
                .when(employeeService).deleteEmployee(1L);

        String result = employeeTools.deleteEmployee(1L);

        assertThat(result).contains("\"success\": false", "Unable to delete employee");
    }
}
