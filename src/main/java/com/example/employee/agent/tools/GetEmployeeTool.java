package com.example.employee.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.employee.agent.EmployeeTools;

public class GetEmployeeTool {

    /**
     * Name that the AI model will use when selecting this tool.
     */
    public static final String NAME = "get_employee";

    /**
     * Description shown to the AI model.
     */
    public static final String DESCRIPTION =
            "Get an employee by employee ID. " +
            "Use this tool when the user asks for information " +
            "about a specific employee.";

    /**
     * Input schema sent to the AI model.
     */
    public record Arguments(

            @JsonProperty("employeeId")
            @JsonPropertyDescription(
                    "The unique ID of the employee to retrieve"
            )
            Long employeeId

    ) {
    }

    /**
     * Execute the tool.
     *
     * EmployeeTools contains the actual business operation.
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

            if (request.employeeId() == null) {

                return """
                        {
                          "success": false,
                          "message": "Employee ID is required"
                        }
                        """;
            }

            return employeeTools.getEmployee(
                    request.employeeId()
            );

        } catch (Exception e) {

            return """
                    {
                      "success": false,
                      "message": "Invalid employee request"
                    }
                    """;
        }
    }
}