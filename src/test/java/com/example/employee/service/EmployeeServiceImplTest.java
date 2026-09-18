package com.example.employee.service;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private EmployeeMapper employeeMapper;

    private EmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository, employeeMapper);
    }

    private EmployeeRequest sampleRequest() {
        EmployeeRequest request = new EmployeeRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setDepartment("Engineering");
        request.setSalary(75000.0);
        return request;
    }

    private Employee sampleEmployee(Long id) {
        Employee employee = new Employee(
                "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);
        employee.setId(id);
        return employee;
    }

    private EmployeeResponse sampleResponse(Long id) {
        return new EmployeeResponse(
                id, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);
    }

    // --- createEmployee ---

    @Test
    void createEmployee_savesAndReturnsResponse_whenEmailNotUsed() {
        EmployeeRequest request = sampleRequest();
        Employee entity = sampleEmployee(null);
        Employee saved = sampleEmployee(1L);
        EmployeeResponse response = sampleResponse(1L);

        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(employeeMapper.toEntity(request)).thenReturn(entity);
        when(employeeRepository.save(entity)).thenReturn(saved);
        when(employeeMapper.toResponse(saved)).thenReturn(response);

        EmployeeResponse result = employeeService.createEmployee(request);

        assertThat(result).isEqualTo(response);
        verify(employeeRepository).save(entity);
    }

    @Test
    void createEmployee_throwsDuplicateEmployeeException_whenEmailExists() {
        EmployeeRequest request = sampleRequest();
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateEmployeeException.class)
                .hasMessageContaining(request.getEmail());

        verify(employeeRepository, never()).save(any());
    }

    // --- getEmployeeById ---

    @Test
    void getEmployeeById_returnsResponse_whenFound() {
        Employee entity = sampleEmployee(1L);
        EmployeeResponse response = sampleResponse(1L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(entity));
        when(employeeMapper.toResponse(entity)).thenReturn(response);

        EmployeeResponse result = employeeService.getEmployeeById(1L);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getEmployeeById_throwsNotFoundException_whenMissing() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployeeById(99L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");
    }

    // --- getAllEmployees ---

    @Test
    void getAllEmployees_returnsMappedList() {
        Employee e1 = sampleEmployee(1L);
        Employee e2 = sampleEmployee(2L);
        EmployeeResponse r1 = sampleResponse(1L);
        EmployeeResponse r2 = sampleResponse(2L);

        when(employeeRepository.findAll()).thenReturn(List.of(e1, e2));
        when(employeeMapper.toResponse(e1)).thenReturn(r1);
        when(employeeMapper.toResponse(e2)).thenReturn(r2);

        List<EmployeeResponse> result = employeeService.getAllEmployees();

        assertThat(result).containsExactly(r1, r2);
    }

    @Test
    void getAllEmployees_returnsEmptyList_whenNoEmployees() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        assertThat(employeeService.getAllEmployees()).isEmpty();
    }

    // --- updateEmployee ---

    @Test
    void updateEmployee_updatesAndReturnsResponse_whenEmailBelongsToSameEmployee() {
        EmployeeRequest request = sampleRequest();
        Employee existing = sampleEmployee(1L);
        Employee updated = sampleEmployee(1L);
        EmployeeResponse response = sampleResponse(1L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existing));
        when(employeeRepository.save(existing)).thenReturn(updated);
        when(employeeMapper.toResponse(updated)).thenReturn(response);

        EmployeeResponse result = employeeService.updateEmployee(1L, request);

        assertThat(result).isEqualTo(response);
        verify(employeeMapper).updateEntity(existing, request);
    }

    @Test
    void updateEmployee_updatesAndReturnsResponse_whenEmailNotUsedByAnyone() {
        EmployeeRequest request = sampleRequest();
        Employee existing = sampleEmployee(1L);
        Employee updated = sampleEmployee(1L);
        EmployeeResponse response = sampleResponse(1L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(employeeRepository.save(existing)).thenReturn(updated);
        when(employeeMapper.toResponse(updated)).thenReturn(response);

        EmployeeResponse result = employeeService.updateEmployee(1L, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void updateEmployee_throwsDuplicateEmployeeException_whenEmailBelongsToAnotherEmployee() {
        EmployeeRequest request = sampleRequest();
        Employee existing = sampleEmployee(1L);
        Employee otherEmployeeWithSameEmail = sampleEmployee(2L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.findByEmail(request.getEmail()))
                .thenReturn(Optional.of(otherEmployeeWithSameEmail));

        assertThatThrownBy(() -> employeeService.updateEmployee(1L, request))
                .isInstanceOf(DuplicateEmployeeException.class)
                .hasMessageContaining(request.getEmail());

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateEmployee_throwsNotFoundException_whenEmployeeMissing() {
        EmployeeRequest request = sampleRequest();
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(99L, request))
                .isInstanceOf(EmployeeNotFoundException.class);

        verify(employeeRepository, never()).save(any());
    }

    // --- deleteEmployee ---

    @Test
    void deleteEmployee_deletesWhenExists() {
        when(employeeRepository.existsById(1L)).thenReturn(true);

        employeeService.deleteEmployee(1L);

        verify(employeeRepository, times(1)).deleteById(eq(1L));
    }

    @Test
    void deleteEmployee_throwsNotFoundException_whenMissing() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");

        verify(employeeRepository, never()).deleteById(any());
    }
}
