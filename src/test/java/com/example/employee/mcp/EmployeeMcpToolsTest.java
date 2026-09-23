package com.example.employee.mcp;

import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
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
}
