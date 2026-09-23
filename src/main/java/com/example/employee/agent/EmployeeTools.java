package com.example.employee.agent;

import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmployeeTools {

    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;

    public EmployeeTools(
            EmployeeService employeeService,
            ObjectMapper objectMapper) {

        this.employeeService = employeeService;
        this.objectMapper = objectMapper;
    }

    /**
     * Get one employee by ID.
     */
    public String getEmployee(Long employeeId) {

        try {

            if (employeeId == null) {
                return """
                        {
                          "success": false,
                          "message": "Employee ID is required"
                        }
                        """;
            }

            EmployeeResponse employee = employeeService.getEmployeeById(employeeId);

            return objectMapper.writeValueAsString(
                    employee
            );

        } catch (EmployeeNotFoundException e) {

            return """
                    {
                      "success": false,
                      "message": "Employee not found"
                    }
                    """;

        } catch (Exception e) {

            return """
                    {
                      "success": false,
                      "message": "Unable to retrieve employee"
                    }
                    """;
        }
    }

    /**
     * Get all employees.
     */
    public String listEmployees() {

        try {

            List<EmployeeResponse> employees = employeeService.getAllEmployees();

            return objectMapper.writeValueAsString(
                    employees
            );

        } catch (JsonProcessingException e) {

            return """
                    {
                      "success": false,
                      "message": "Unable to retrieve employees"
                    }
                    """;
        }
    }

    /**
     * Search employees by name.
     */
    public String searchEmployees(String name) {

        try {

            if (name == null || name.isBlank()) {
                return """
                        {
                          "success": false,
                          "message": "Search name is required"
                        }
                        """;
            }

            String searchTerm = name.trim().toLowerCase();

            List<EmployeeResponse> matches = employeeService.getAllEmployees()
                    .stream()
                    .filter(employee ->
                            employee.getFirstName().toLowerCase().contains(searchTerm)
                                    || employee.getLastName().toLowerCase().contains(searchTerm))
                    .toList();

            return objectMapper.writeValueAsString(
                    matches
            );

        } catch (JsonProcessingException e) {

            return """
                    {
                      "success": false,
                      "message": "Unable to search employees"
                    }
                    """;
        }
    }
}