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
            "Search employees by name. " +
            "Use this tool when the user wants to find employees " +
            "matching a name or partial name.";

    /**
     * Arguments supplied by the AI model.
     *
     * Example:
     *
     * {
     *   "name": "John"
     * }
     */
    public record Arguments(

            @JsonProperty("name")
            @JsonPropertyDescription(
                    "Employee name or partial name to search for"
            )
            String name

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

            if (request.name() == null ||
                    request.name().isBlank()) {

                return """
                        {
                          "success": false,
                          "message": "Employee name is required"
                        }
                        """;
            }

            return employeeTools.searchEmployees(
                    request.name()
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