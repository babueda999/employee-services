package com.example.employee.mapper;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeMapperTest {

    private final EmployeeMapper mapper = new EmployeeMapper();

    private EmployeeRequest sampleRequest() {
        EmployeeRequest request = new EmployeeRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setDepartment("Engineering");
        request.setSalary(75000.0);
        return request;
    }

    @Test
    void toEntity_mapsAllFields() {
        Employee entity = mapper.toEntity(sampleRequest());

        assertThat(entity.getId()).isNull();
        assertThat(entity.getFirstName()).isEqualTo("John");
        assertThat(entity.getLastName()).isEqualTo("Doe");
        assertThat(entity.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(entity.getDepartment()).isEqualTo("Engineering");
        assertThat(entity.getSalary()).isEqualTo(75000.0);
    }

    @Test
    void toResponse_mapsAllFieldsIncludingId() {
        Employee entity = new Employee(
                "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);
        entity.setId(42L);

        EmployeeResponse response = mapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(response.getDepartment()).isEqualTo("Engineering");
        assertThat(response.getSalary()).isEqualTo(75000.0);
    }

    @Test
    void updateEntity_overwritesExistingFieldsInPlace() {
        Employee entity = new Employee(
                "Old", "Name", "old@example.com", "Sales", 50000.0);
        entity.setId(7L);

        EmployeeRequest request = sampleRequest();
        mapper.updateEntity(entity, request);

        assertThat(entity.getId()).isEqualTo(7L);
        assertThat(entity.getFirstName()).isEqualTo("John");
        assertThat(entity.getLastName()).isEqualTo("Doe");
        assertThat(entity.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(entity.getDepartment()).isEqualTo("Engineering");
        assertThat(entity.getSalary()).isEqualTo(75000.0);
    }
}
