package com.example.employee.repository;

import com.example.employee.entity.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee sampleEmployee(String email) {
        return new Employee("John", "Doe", email, "Engineering", 75000.0);
    }

    @Test
    void existsByEmail_returnsTrue_whenEmailSaved() {
        employeeRepository.save(sampleEmployee("john.doe@example.com"));

        assertThat(employeeRepository.existsByEmail("john.doe@example.com")).isTrue();
    }

    @Test
    void existsByEmail_returnsFalse_whenEmailNotSaved() {
        assertThat(employeeRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void findByEmail_returnsEmployee_whenExists() {
        Employee saved = employeeRepository.save(sampleEmployee("jane.doe@example.com"));

        Optional<Employee> found = employeeRepository.findByEmail("jane.doe@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    void findByEmail_returnsEmpty_whenNotExists() {
        assertThat(employeeRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void save_throwsException_whenEmailAlreadyUsed() {
        employeeRepository.saveAndFlush(sampleEmployee("dup@example.com"));

        assertThatThrownBy(() ->
                employeeRepository.saveAndFlush(sampleEmployee("dup@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findAll_returnsAllSavedEmployees() {
        employeeRepository.save(sampleEmployee("one@example.com"));
        employeeRepository.save(sampleEmployee("two@example.com"));

        assertThat(employeeRepository.findAll()).hasSize(2);
    }
}
