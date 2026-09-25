package com.example.employee.agent.tools;

import com.example.employee.agent.EmployeeTools;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SearchEmployeeTool {

    /**
     * Name used by the AI model when selecting this tool.
     */
    public static final String NAME = "search_employees";

    /**
     * Description provided to the AI model.
     */
    public static final String DESCRIPTION =
            "Search employees by name and/or department. " +
            "Use this tool when the user wants to find employees " +
            "matching a name, partial name, or department (e.g. Engineering, Sales). " +
            "At least one of name or department must be provided.";

    /**
     * Arguments supplied by the AI model.
     *
     * Example:
     *
     * {
     *   "name": "John",
     *   "department": null
     * }
     */
    public record Arguments(

            @JsonProperty("name")
            @JsonPropertyDescription(
                    "Employee name or partial name to search for, or null if not filtering by name"
            )
            String name,

            @JsonProperty("department")
            @JsonPropertyDescription(
                    "Department to filter by (e.g. Engineering, Sales), or null if not filtering by department"
            )
            String department

    ) {
    }

    /**
     * Execute the search employee tool.
     */
    public static String execute(
            String arguments,
            EmployeeTools employeeTools,
            ObjectMapper objectMapper) {

        try {

            Arguments request =
                    objectMapper.readValue(
                            arguments,
                            Arguments.class
                    );

            boolean hasName = request.name() != null && !request.name().isBlank();
            boolean hasDepartment = request.department() != null && !request.department().isBlank();

            if (!hasName && !hasDepartment) {

                return """
                        {
                          "success": false,
                          "message": "Employee name or department is required"
                        }
                        """;
            }

            return employeeTools.searchEmployees(
                    request.name(),
                    request.department()
            );

        } catch (Exception e) {

            return """
                    {
                      "success": false,
                      "message": "Unable to search employees"
                    }
                    """;
        }
    }
}