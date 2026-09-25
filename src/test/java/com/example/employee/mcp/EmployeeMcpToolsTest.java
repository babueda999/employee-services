package com.example.employee.mcp;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeMcpToolsTest {

    @Mock
    private EmployeeService employeeService;

    private EmployeeMcpTools employeeMcpTools;

    @BeforeEach
    void setUp() {
        employeeMcpTools = new EmployeeMcpTools(employeeService);
    }

    private EmployeeResponse sampleResponse(Long id) {
        return new EmployeeResponse(
                id, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);
    }

    @Test
    void getEmployee_delegatesToEmployeeService() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));

        EmployeeResponse result = employeeMcpTools.getEmployee(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void getEmployee_propagatesNotFoundException() {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        assertThatThrownBy(() -> employeeMcpTools.getEmployee(99L))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void listEmployees_delegatesToEmployeeService() {
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

        List<EmployeeResponse> result = employeeMcpTools.listEmployees();

        assertThat(result).hasSize(2);
    }

    @Test
    void listEmployees_returnsEmptyList_whenNoEmployees() {
        when(employeeService.getAllEmployees()).thenReturn(List.of());

        List<EmployeeResponse> result = employeeMcpTools.listEmployees();

        assertThat(result).isEmpty();
    }

    @Test
    void updateEmployee_delegatesToEmployeeServiceWithFullReplaceRequest() {
        ArgumentCaptor<EmployeeRequest> requestCaptor =
                ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(org.mockito.ArgumentMatchers.eq(1L), requestCaptor.capture()))
                .thenReturn(sampleResponse(1L));

        EmployeeResponse result = employeeMcpTools.updateEmployee(
                1L, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(requestCaptor.getValue().getFirstName()).isEqualTo("John");
        assertThat(requestCaptor.getValue().getLastName()).isEqualTo("Doe");
        assertThat(requestCaptor.getValue().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(requestCaptor.getValue().getDepartment()).isEqualTo("Engineering");
        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(75000.0);
    }

    @Test
    void updateEmployee_propagatesNotFoundException() {
        when(employeeService.updateEmployee(
                org.mockito.ArgumentMatchers.eq(99L),
                org.mockito.ArgumentMatchers.any()))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        assertThatThrownBy(() -> employeeMcpTools.updateEmployee(
                99L, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void adjustSalary_appliesFlatIncrease_andPreservesOtherFields() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));
        ArgumentCaptor<EmployeeRequest> requestCaptor =
                ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(org.mockito.ArgumentMatchers.eq(1L), requestCaptor.capture()))
                .thenReturn(sampleResponse(1L));

        employeeMcpTools.adjustSalary(1L, 5000.0, false);

        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(80000.0);
        assertThat(requestCaptor.getValue().getFirstName()).isEqualTo("John");
        assertThat(requestCaptor.getValue().getDepartment()).isEqualTo("Engineering");
    }

    @Test
    void adjustSalary_appliesPercentageDecrease() {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));
        ArgumentCaptor<EmployeeRequest> requestCaptor =
                ArgumentCaptor.forClass(EmployeeRequest.class);
        when(employeeService.updateEmployee(org.mockito.ArgumentMatchers.eq(1L), requestCaptor.capture()))
                .thenReturn(sampleResponse(1L));

        employeeMcpTools.adjustSalary(1L, -10.0, true);

        assertThat(requestCaptor.getValue().getSalary()).isEqualTo(67500.0);
    }

    @Test
    void adjustSalary_propagatesNotFoundException() {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        assertThatThrownBy(() -> employeeMcpTools.adjustSalary(99L, 1000.0, false))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void deleteEmployee_delegatesToEmployeeService() {
        employeeMcpTools.deleteEmployee(1L);

        verify(employeeService, times(1)).deleteEmployee(1L);
    }

    @Test
    void deleteEmployee_propagatesNotFoundException() {
        org.mockito.Mockito.doThrow(new EmployeeNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deleteEmployee(99L);

        assertThatThrownBy(() -> employeeMcpTools.deleteEmployee(99L))
                .isInstanceOf(EmployeeNotFoundException.class);
    }
}
