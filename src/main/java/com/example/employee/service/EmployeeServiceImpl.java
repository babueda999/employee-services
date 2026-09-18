package com.example.employee.service;


import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmployeeServiceImpl
        implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            EmployeeMapper employeeMapper) {

        this.employeeRepository = employeeRepository;
        this.employeeMapper = employeeMapper;
    }


    @Override
    public EmployeeResponse createEmployee(
            EmployeeRequest request) {

        // Check duplicate email
        if (employeeRepository.existsByEmail(
                request.getEmail())) {

            throw new DuplicateEmployeeException(
                    "Employee already exists with email: "
                            + request.getEmail()
            );
        }

        // DTO -> Entity
        Employee employee =
                employeeMapper.toEntity(request);

        // Save
        Employee savedEmployee =
                employeeRepository.save(employee);

        // Entity -> Response DTO
        return employeeMapper.toResponse(savedEmployee);
    }


    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployeeById(Long id) {

        Employee employee =
                employeeRepository.findById(id)
                        .orElseThrow(() ->
                                new EmployeeNotFoundException(
                                        "Employee not found with id: "
                                                + id
                                )
                        );

        return employeeMapper.toResponse(employee);
    }


    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {

        return employeeRepository.findAll()
                .stream()
                .map(employeeMapper::toResponse)
                .toList();
    }


    @Override
    public EmployeeResponse updateEmployee(
            Long id,
            EmployeeRequest request) {

        Employee employee =
                employeeRepository.findById(id)
                        .orElseThrow(() ->
                                new EmployeeNotFoundException(
                                        "Employee not found with id: "
                                                + id
                                )
                        );

        // Check if email belongs to another employee
        employeeRepository.findByEmail(
                        request.getEmail())
                .ifPresent(existingEmployee -> {

                    if (!existingEmployee.getId()
                            .equals(id)) {

                        throw new DuplicateEmployeeException(
                                "Email already used by another employee: "
                                        + request.getEmail()
                        );
                    }
                });

        // Update existing entity
        employeeMapper.updateEntity(
                employee,
                request
        );

        Employee updatedEmployee =
                employeeRepository.save(employee);

        return employeeMapper.toResponse(
                updatedEmployee
        );
    }


    @Override
    public void deleteEmployee(Long id) {

        if (!employeeRepository.existsById(id)) {

            throw new EmployeeNotFoundException(
                    "Employee not found with id: " + id
            );
        }

        employeeRepository.deleteById(id);
    }
}