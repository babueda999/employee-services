package com.example.employee.mcp;

import com.example.employee.agent.tools.AdjustSalaryTool;
import com.example.employee.agent.tools.DeleteEmployeeTool;
import com.example.employee.agent.tools.GetEmployeeTool;
import com.example.employee.agent.tools.ListEmployeesTool;
import com.example.employee.agent.tools.UpdateEmployeeTool;
import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.service.EmployeeService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmployeeMcpTools {

    private final EmployeeService employeeService;

    public EmployeeMcpTools(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * MCP Tool:
     * Get one employee by ID.
     */
    @McpTool(
            name = GetEmployeeTool.NAME,
            description = "Get an employee by employee ID"
    )
    public EmployeeResponse getEmployee(
            @McpToolParam(
                    description = "The unique ID of the employee",
                    required = true
            )
            Long employeeId) {

        return employeeService.getEmployeeById(employeeId);
    }

    /**
     * MCP Tool:
     * Get all employees.
     */
    @McpTool(
            name = ListEmployeesTool.NAME,
            description = "List all employees"
    )
    public List<EmployeeResponse> listEmployees() {

        return employeeService.getAllEmployees();
    }

    /**
     * MCP Tool:
     * Update an existing employee. This is a full replace, matching
     * PUT /api/employees/{id} — every field is required.
     */
    @McpTool(
            name = UpdateEmployeeTool.NAME,
            description = UpdateEmployeeTool.DESCRIPTION
    )
    public EmployeeResponse updateEmployee(
            @McpToolParam(
                    description = "The unique ID of the employee to update",
                    required = true
            )
            Long employeeId,

            @McpToolParam(
                    description = "The employee's first name",
                    required = true
            )
            String firstName,

            @McpToolParam(
                    description = "The employee's last name",
                    required = true
            )
            String lastName,

            @McpToolParam(
                    description = "The employee's email address",
                    required = true
            )
            String email,

            @McpToolParam(
                    description = "The employee's department (e.g. Engineering, Sales)",
                    required = true
            )
            String department,

            @McpToolParam(
                    description = "The employee's salary",
                    required = true
            )
            Double salary) {

        EmployeeRequest request = new EmployeeRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        request.setDepartment(department);
        request.setSalary(salary);

        return employeeService.updateEmployee(employeeId, request);
    }

    /**
     * MCP Tool:
     * Adjust an employee's salary by a relative amount (flat or percentage),
     * computed server-side against the current salary.
     */
    @McpTool(
            name = AdjustSalaryTool.NAME,
            description = AdjustSalaryTool.DESCRIPTION
    )
    public EmployeeResponse adjustSalary(
            @McpToolParam(
                    description = "The unique ID of the employee whose salary is being adjusted",
                    required = true
            )
            Long employeeId,

            @McpToolParam(
                    description = "The change to apply. Positive increases the salary, negative "
                            + "decreases it. A flat dollar amount unless isPercentage is true.",
                    required = true
            )
            Double amount,

            @McpToolParam(
                    description = "true if amount is a percentage of the current salary "
                            + "(e.g. 10 for +10%); false or omitted for a flat dollar amount",
                    required = false
            )
            Boolean isPercentage) {

        EmployeeResponse current = employeeService.getEmployeeById(employeeId);

        double newSalary = Boolean.TRUE.equals(isPercentage)
                ? current.getSalary() * (1 + amount / 100.0)
                : current.getSalary() + amount;

        EmployeeRequest request = new EmployeeRequest();
        request.setFirstName(current.getFirstName());
        request.setLastName(current.getLastName());
        request.setEmail(current.getEmail());
        request.setDepartment(current.getDepartment());
        request.setSalary(newSalary);

        return employeeService.updateEmployee(employeeId, request);
    }

    /**
     * MCP Tool:
     * Delete an employee by ID.
     */
    @McpTool(
            name = DeleteEmployeeTool.NAME,
            description = DeleteEmployeeTool.DESCRIPTION
    )
    public void deleteEmployee(
            @McpToolParam(
                    description = "The unique ID of the employee to delete",
                    required = true
            )
            Long employeeId) {

        employeeService.deleteEmployee(employeeId);
    }
}

