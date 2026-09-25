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
     * Search employees by name and/or department. At least one of the two
     * must be provided; when both are given, an employee must match both.
     */
    public String searchEmployees(String name, String department) {

        try {

            boolean hasName = name != null && !name.isBlank();
            boolean hasDepartment = department != null && !department.isBlank();

            if (!hasName && !hasDepartment) {
                return """
                        {
                          "success": false,
                          "message": "Search name or department is required"
                        }
                        """;
            }

            String nameTerm = hasName ? name.trim().toLowerCase() : null;
            String departmentTerm = hasDepartment ? department.trim() : null;

            List<EmployeeResponse> matches = employeeService.getAllEmployees()
                    .stream()
                    .filter(employee ->
                            !hasName
                                    || employee.getFirstName().toLowerCase().contains(nameTerm)
                                    || employee.getLastName().toLowerCase().contains(nameTerm))
                    .filter(employee ->
                            !hasDepartment
                                    || employee.getDepartment().equalsIgnoreCase(departmentTerm))
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