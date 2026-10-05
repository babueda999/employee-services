package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.AdjustSalaryTool;
import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.guardrails.AuthorizationGuardrail;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.JsonValue;
import com.openai.models.responses.FunctionTool;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Single-tool agent for {@code adjust_salary}. Exposed to the supervisor
 * agent via {@link EmployeeSubAgent}, and directly to external MCP clients
 * via the {@link McpTool}-annotated method below.
 */
@Component
public class AdjustSalaryAgent implements EmployeeSubAgent {

    private final EmployeeTools employeeTools;
    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;
    private final AuthorizationGuardrail authorizationGuardrail;

    public AdjustSalaryAgent(
            EmployeeTools employeeTools,
            EmployeeService employeeService,
            ObjectMapper objectMapper,
            AuthorizationGuardrail authorizationGuardrail) {

        this.employeeTools = employeeTools;
        this.employeeService = employeeService;
        this.objectMapper = objectMapper;
        this.authorizationGuardrail = authorizationGuardrail;
    }

    @Override
    public String name() {
        return AdjustSalaryTool.NAME;
    }

    @Override
    public FunctionTool definition() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                                "properties",
                                JsonValue.from(Map.of(
                                        "employeeId", Map.of(
                                                "type", "integer",
                                                "description", "The numeric ID of the employee whose salary is being adjusted"
                                        ),
                                        "amount", Map.of(
                                                "type", "number",
                                                "description", "The change to apply. Positive increases the salary, "
                                                        + "negative decreases it. A flat dollar amount unless "
                                                        + "isPercentage is true."
                                        ),
                                        "isPercentage", Map.of(
                                                "type", List.of("boolean", "null"),
                                                "description", "true if amount is a percentage of the current salary "
                                                        + "(e.g. 10 for +10%); false or null for a flat dollar amount"
                                        )
                                ))
                        )
                        .putAdditionalProperty(
                                "required",
                                JsonValue.from(List.of("employeeId", "amount", "isPercentage"))
                        )
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(AdjustSalaryTool.NAME)
                .description(AdjustSalaryTool.DESCRIPTION)
                .parameters(parameters)
                .strict(true)
                .build();
    }

    @Override
    public void authorize(String role) {
        authorizationGuardrail.checkSalaryAdjustmentAccess(normalizeRole(role));
    }

    @Override
    public String handle(String arguments, String role) {

        authorize(role);

        return AdjustSalaryTool.execute(arguments, employeeTools, objectMapper);
    }

    /**
     * MCP Tool: adjust an employee's salary by a relative amount (flat or
     * percentage), computed server-side against the current salary.
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
            Boolean isPercentage,

            @McpToolParam(
                    description = "Caller role: MANAGER; defaults to USER (which is not permitted to adjust salary)",
                    required = false
            )
            String role) {

        authorize(role);

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
}
