package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.UpdateEmployeeTool;
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
 * Single-tool agent for {@code update_employee}. Exposed to the supervisor
 * agent via {@link EmployeeSubAgent}, and directly to external MCP clients
 * via the {@link McpTool}-annotated method below.
 */
@Component
public class UpdateEmployeeAgent implements EmployeeSubAgent {

    private final EmployeeTools employeeTools;
    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;
    private final AuthorizationGuardrail authorizationGuardrail;

    public UpdateEmployeeAgent(
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
        return UpdateEmployeeTool.NAME;
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
                                                "description", "The numeric ID of the employee to update"
                                        ),
                                        "firstName", Map.of(
                                                "type", "string",
                                                "description", "The employee's first name"
                                        ),
                                        "lastName", Map.of(
                                                "type", "string",
                                                "description", "The employee's last name"
                                        ),
                                        "email", Map.of(
                                                "type", "string",
                                                "description", "The employee's email address"
                                        ),
                                        "department", Map.of(
                                                "type", "string",
                                                "description", "The employee's department (e.g. Engineering, Sales)"
                                        ),
                                        "salary", Map.of(
                                                "type", "number",
                                                "description", "The employee's salary"
                                        )
                                ))
                        )
                        .putAdditionalProperty(
                                "required",
                                JsonValue.from(List.of(
                                        "employeeId", "firstName", "lastName",
                                        "email", "department", "salary"
                                ))
                        )
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(UpdateEmployeeTool.NAME)
                .description(UpdateEmployeeTool.DESCRIPTION)
                .parameters(parameters)
                .strict(true)
                .build();
    }

    @Override
    public void authorize(String role) {
        authorizationGuardrail.checkUpdateAccess(normalizeRole(role));
    }

    @Override
    public String handle(String arguments, String role) {

        authorize(role);

        return UpdateEmployeeTool.execute(arguments, employeeTools, objectMapper);
    }

    /**
     * MCP Tool: update an existing employee. This is a full replace,
     * matching PUT /api/employees/{id} — every field is required.
     */
    @McpTool(
            name = UpdateEmployeeTool.NAME,
            description = UpdateEmployeeTool.DESCRIPTION
    )
    public EmployeeResponse updateEmployee(
            @McpToolParam(description = "The unique ID of the employee to update", required = true)
            Long employeeId,

            @McpToolParam(description = "The employee's first name", required = true)
            String firstName,

            @McpToolParam(description = "The employee's last name", required = true)
            String lastName,

            @McpToolParam(description = "The employee's email address", required = true)
            String email,

            @McpToolParam(description = "The employee's department (e.g. Engineering, Sales)", required = true)
            String department,

            @McpToolParam(description = "The employee's salary", required = true)
            Double salary,

            @McpToolParam(
                    description = "Caller role: MANAGER or ADMIN; defaults to USER (which is not permitted to update)",
                    required = false
            )
            String role) {

        authorize(role);

        EmployeeRequest request = new EmployeeRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        request.setDepartment(department);
        request.setSalary(salary);

        return employeeService.updateEmployee(employeeId, request);
    }
}
