package com.example.employee.agent.tools;

import com.example.employee.agent.EmployeeTools;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DeleteEmployeeTool {

    /**
     * Name that the AI model will use when selecting this tool.
     */
    public static final String NAME = "delete_employee";

    /**
     * Description shown to the AI model.
     */
    public static final String DESCRIPTION =
            "Permanently delete an employee by employee ID. This action cannot be undone. " +
            "Only use this when the user has clearly and explicitly asked to delete or " +
            "remove a specific employee.";

    /**
     * Input schema sent to the AI model.
     */
    public record Arguments(

            @JsonProperty("employeeId")
            @JsonPropertyDescription(
                    "The unique ID of the employee to delete"
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

            return employeeTools.deleteEmployee(
                    request.employeeId()
            );

        } catch (Exception e) {

            return """
                    {
                      "success": false,
                      "message": "Invalid employee delete request"
                    }
                    """;
        }
    }
}
