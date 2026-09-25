package com.example.employee.agent.tools;

import com.example.employee.agent.EmployeeTools;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UpdateEmployeeTool {

    /**
     * Name that the AI model will use when selecting this tool.
     */
    public static final String NAME = "update_employee";

    /**
     * Description shown to the AI model.
     */
    public static final String DESCRIPTION =
            "Update an existing employee. This replaces the employee's full record, so " +
            "firstName, lastName, email, department, and salary must ALL be provided. " +
            "If you don't already know the employee's current values, call get_employee " +
            "first to retrieve them, then resend the unchanged fields together with the " +
            "ones being changed.";

    /**
     * Input schema sent to the AI model.
     */
    public record Arguments(

            @JsonProperty("employeeId")
            @JsonPropertyDescription(
                    "The unique ID of the employee to update"
            )
            Long employeeId,

            @JsonProperty("firstName")
            @JsonPropertyDescription("The employee's first name")
            String firstName,

            @JsonProperty("lastName")
            @JsonPropertyDescription("The employee's last name")
            String lastName,

            @JsonProperty("email")
            @JsonPropertyDescription("The employee's email address")
            String email,

            @JsonProperty("department")
            @JsonPropertyDescription("The employee's department (e.g. Engineering, Sales)")
            String department,

            @JsonProperty("salary")
            @JsonPropertyDescription("The employee's salary")
            Double salary

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

            return employeeTools.updateEmployee(
                    request.employeeId(),
                    request.firstName(),
                    request.lastName(),
                    request.email(),
                    request.department(),
                    request.salary()
            );

        } catch (Exception e) {

            return """
                    {
                      "success": false,
                      "message": "Invalid employee update request"
                    }
                    """;
        }
    }
}
