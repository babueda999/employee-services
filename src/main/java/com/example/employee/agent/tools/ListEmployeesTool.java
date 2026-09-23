package com.example.employee.agent.tools;

import com.example.employee.agent.EmployeeTools;

public class ListEmployeesTool {

    /**
     * Name used by the AI model when selecting this tool.
     */
    public static final String NAME = "list_employees";

    /**
     * Description provided to the AI model.
     */
    public static final String DESCRIPTION =
            "List all employees in the employee management system. " +
            "Use this tool when the user asks to see, list, " +
            "or retrieve all employees.";

    /**
     * This tool does not require any arguments.
     */
    public record Arguments() {
    }

    /**
     * Execute the tool.
     *
     * EmployeeTools performs the actual employee operation.
     */
    public static String execute(
            EmployeeTools employeeTools) {

        try {

            return employeeTools.listEmployees();

        } catch (Exception e) {

            return """
                    {
                      "success": false,
                      "message": "Unable to retrieve employees"
                    }
                    """;
        }
    }
}