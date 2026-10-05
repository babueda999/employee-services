package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.ListEmployeesTool;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.guardrails.AuthorizationGuardrail;
import com.example.employee.service.EmployeeService;
import com.openai.core.JsonValue;
import com.openai.models.responses.FunctionTool;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Single-tool agent for {@code list_employees}. Exposed to the supervisor
 * agent via {@link EmployeeSubAgent}, and directly to external MCP clients
 * via the {@link McpTool}-annotated method below.
 */
@Component
public class ListEmployeesAgent implements EmployeeSubAgent {

    private final EmployeeTools employeeTools;
    private final EmployeeService employeeService;
    private final AuthorizationGuardrail authorizationGuardrail;

    public ListEmployeesAgent(
            EmployeeTools employeeTools,
            EmployeeService employeeService,
            AuthorizationGuardrail authorizationGuardrail) {

        this.employeeTools = employeeTools;
        this.employeeService = employeeService;
        this.authorizationGuardrail = authorizationGuardrail;
    }

    @Override
    public String name() {
        return ListEmployeesTool.NAME;
    }

    @Override
    public FunctionTool definition() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty("properties", JsonValue.from(Map.of()))
                        .putAdditionalProperty("required", JsonValue.from(List.of()))
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(ListEmployeesTool.NAME)
                .description(ListEmployeesTool.DESCRIPTION)
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

        return ListEmployeesTool.execute(employeeTools);
    }

    /**
     * MCP Tool: list all employees.
     */
    @McpTool(
            name = ListEmployeesTool.NAME,
            description = ListEmployeesTool.DESCRIPTION
    )
    public List<EmployeeResponse> listEmployees(
            @McpToolParam(
                    description = "Caller role: USER, MANAGER, or ADMIN; defaults to USER",
                    required = false
            )
            String role) {

        authorize(role);

        return employeeService.getAllEmployees();
    }
}
