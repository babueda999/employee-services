package com.example.employee.mcp;

import com.example.employee.agent.tools.GetEmployeeTool;
import com.example.employee.agent.tools.ListEmployeesTool;
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
}

