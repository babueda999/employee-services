package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.GetEmployeeTool;
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
 * Single-tool agent for {@code get_employee}. Exposed to the supervisor
 * agent via {@link EmployeeSubAgent}, and directly to external MCP clients
 * via the {@link McpTool}-annotated method below.
 */
@Component
public class GetEmployeeAgent implements EmployeeSubAgent {

    private final EmployeeTools employeeTools;
    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;
    private final AuthorizationGuardrail authorizationGuardrail;

    public GetEmployeeAgent(
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
        return GetEmployeeTool.NAME;
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
                                                "description", "The numeric ID of the employee to look up"
                                        )
                                ))
                        )
                        .putAdditionalProperty(
                                "required",
                                JsonValue.from(List.of("employeeId"))
                        )
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(GetEmployeeTool.NAME)
                .description(GetEmployeeTool.DESCRIPTION)
                .parameters(parameters)
                .strict(true)
                .build();
    }

    @Override
    public void authorize(String role) {
        authorizationGuardrail.checkReadAccess(normalizeRole(role));
    }

    @Override
    public String handle(String arguments, String role) {

        authorize(role);

        return GetEmployeeTool.execute(arguments, employeeTools, objectMapper);
    }

    /**
     * MCP Tool: get one employee by ID.
     */
    @McpTool(
            name = GetEmployeeTool.NAME,
            description = GetEmployeeTool.DESCRIPTION
    )
    public EmployeeResponse getEmployee(
            @McpToolParam(
                    description = "The unique ID of the employee",
                    required = true
            )
            Long employeeId,

            @McpToolParam(
                    description = "Caller role: USER, MANAGER, or ADMIN; defaults to USER",
                    required = false
            )
            String role) {

        authorize(role);

        return employeeService.getEmployeeById(employeeId);
    }
}
