package com.example.employee.agent;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.DuplicateEmployeeException;
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

    /**
     * Update an existing employee. This is a full replace, matching
     * PUT /api/employees/{id} — every field must be supplied.
     */
    public String updateEmployee(
            Long employeeId,
            String firstName,
            String lastName,
            String email,
            String department,
            Double salary) {

        try {

            if (employeeId == null) {
                return """
                        {
                          "success": false,
                          "message": "Employee ID is required"
                        }
                        """;
            }

            EmployeeRequest request = new EmployeeRequest();
            request.setFirstName(firstName);
            request.setLastName(lastName);
            request.setEmail(email);
            request.setDepartment(department);
            request.setSalary(salary);

            EmployeeResponse updated =
                    employeeService.updateEmployee(employeeId, request);

            return objectMapper.writeValueAsString(updated);

        } catch (EmployeeNotFoundException e) {

            return """
                    {
                      "success": false,
                      "message": "Employee not found"
                    }
                    """;

        } catch (DuplicateEmployeeException e) {

            return """
                    {
                      "success": false,
                      "message": "Email already used by another employee"
                    }
                    """;

        } catch (Exception e) {

            return """
                    {
                      "success": false,
                      "message": "Unable to update employee"
                    }
                    """;
        }
    }

    /**
     * Adjust an employee's salary by a relative amount, computed server-side
     * against the employee's current salary — avoids relying on the model
     * to know the current value or to do the arithmetic itself.
     *
     * @param amount       the change to apply; positive increases, negative
     *                     decreases
     * @param isPercentage when true, {@code amount} is a percentage of the
     *                     current salary (e.g. 10 = +10%, -15 = -15%);
     *                     when false, {@code amount} is a flat dollar amount
     */
    public String adjustSalary(
            Long employeeId,
            Double amount,
            Boolean isPercentage) {

        try {

            if (employeeId == null) {
                return """
                        {
                          "success": false,
                          "message": "Employee ID is required"
                        }
                        """;
            }

            if (amount == null) {
                return """
                        {
                          "success": false,
                          "message": "Amount is required"
                        }
                        """;
            }

            EmployeeResponse current = employeeService.getEmployeeById(employeeId);

            double currentSalary = current.getSalary();
            double newSalary = Boolean.TRUE.equals(isPercentage)
                    ? currentSalary * (1 + amount / 100.0)
                    : currentSalary + amount;

            if (newSalary <= 0) {
                return """
                        {
                          "success": false,
                          "message": "Resulting salary must be greater than zero"
                        }
                        """;
            }

            EmployeeRequest request = new EmployeeRequest();
            request.setFirstName(current.getFirstName());
            request.setLastName(current.getLastName());
            request.setEmail(current.getEmail());
            request.setDepartment(current.getDepartment());
            request.setSalary(newSalary);

            EmployeeResponse updated =
                    employeeService.updateEmployee(employeeId, request);

            return objectMapper.writeValueAsString(updated);

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
                      "message": "Unable to adjust employee salary"
                    }
                    """;
        }
    }

    /**
     * Delete an employee by ID.
     */
    public String deleteEmployee(Long employeeId) {

        try {

            if (employeeId == null) {
                return """
                        {
                          "success": false,
                          "message": "Employee ID is required"
                        }
                        """;
            }

            employeeService.deleteEmployee(employeeId);

            return """
                    {
                      "success": true,
                      "message": "Employee deleted"
                    }
                    """;

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
                      "message": "Unable to delete employee"
                    }
                    """;
        }
    }
}