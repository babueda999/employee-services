package com.example.employee.agent.tools;

import com.example.employee.agent.EmployeeTools;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AdjustSalaryTool {

    /**
     * Name that the AI model will use when selecting this tool.
     */
    public static final String NAME = "adjust_salary";

    /**
     * Description shown to the AI model.
     */
    public static final String DESCRIPTION =
            "Adjust an employee's salary by a relative amount (a raise, cut, or " +
            "percentage change) instead of setting an absolute value. The current " +
            "salary is looked up and the arithmetic is performed on the server, so " +
            "you do not need to know the employee's current salary or compute the " +
            "new value yourself — just provide the change. Use this for requests " +
            "like 'give employee 5 a 10% raise' or 'increase employee 3's salary by " +
            "$5000'. For a raise/increase, amount is positive; for a cut/decrease, " +
            "amount is negative.";

    /**
     * Input schema sent to the AI model.
     */
    public record Arguments(

            @JsonProperty("employeeId")
            @JsonPropertyDescription(
                    "The unique ID of the employee whose salary is being adjusted"
            )
            Long employeeId,

            @JsonProperty("amount")
            @JsonPropertyDescription(
                    "The change to apply. Positive increases the salary, negative " +
                    "decreases it. Interpreted as a flat dollar amount unless " +
                    "isPercentage is true, in which case it's a percentage of the " +
                    "current salary (e.g. 10 for +10%, -15 for -15%)."
            )
            Double amount,

            @JsonProperty("isPercentage")
            @JsonPropertyDescription(
                    "true if amount is a percentage of the current salary; false if " +
                    "amount is a flat dollar amount"
            )
            Boolean isPercentage

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

            return employeeTools.adjustSalary(
                    request.employeeId(),
                    request.amount(),
                    request.isPercentage()
            );

        } catch (Exception e) {

            return """
                    {
                      "success": false,
                      "message": "Invalid salary adjustment request"
                    }
                    """;
        }
    }
}
