package com.example.employee.agent.subagents;

import com.example.employee.agent.EmployeeTools;
import com.example.employee.agent.tools.SearchEmployeeTool;
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
 * Single-tool agent for {@code search_employees}. Exposed to the supervisor
 * agent via {@link EmployeeSubAgent}, and directly to external MCP clients
 * via the {@link McpTool}-annotated method below.
 */
@Component
public class SearchEmployeeAgent implements EmployeeSubAgent {

    private final EmployeeTools employeeTools;
    private final EmployeeService employeeService;
    private final ObjectMapper objectMapper;
    private final AuthorizationGuardrail authorizationGuardrail;

    public SearchEmployeeAgent(
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
        return SearchEmployeeTool.NAME;
    }

    @Override
    public FunctionTool definition() {

        FunctionTool.Parameters parameters =
                FunctionTool.Parameters.builder()
                        .putAdditionalProperty("type", JsonValue.from("object"))
                        .putAdditionalProperty(
                                "properties",
                                JsonValue.from(Map.of(
                                        "name", Map.of(
                                                "type", List.of("string", "null"),
                                                "description", "Employee name or partial name to search for, or null if not filtering by name"
                                        ),
                                        "department", Map.of(
                                                "type", List.of("string", "null"),
                                                "description", "Department to filter by (e.g. Engineering, Sales), or null if not filtering by department"
                                        )
                                ))
                        )
                        .putAdditionalProperty(
                                "required",
                                JsonValue.from(List.of("name", "department"))
                        )
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build();

        return FunctionTool.builder()
                .name(SearchEmployeeTool.NAME)
                .description(SearchEmployeeTool.DESCRIPTION)
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

        return SearchEmployeeTool.execute(arguments, employeeTools, objectMapper);
    }

    /**
     * MCP Tool: search employees by name and/or department. At least one of
     * the two must be provided; when both are given, an employee must match
     * both.
     */
    @McpTool(
            name = SearchEmployeeTool.NAME,
            description = SearchEmployeeTool.DESCRIPTION
    )
    public List<EmployeeResponse> searchEmployees(
            @McpToolParam(
                    description = "Employee name or partial name to search for",
                    required = false
            )
            String name,

            @McpToolParam(
                    description = "Department to filter by (e.g. Engineering, Sales)",
                    required = false
            )
            String department,

            @McpToolParam(
                    description = "Caller role: USER, MANAGER, or ADMIN; defaults to USER",
                    required = false
            )
            String role) {

        authorize(role);

        boolean hasName = name != null && !name.isBlank();
        boolean hasDepartment = department != null && !department.isBlank();

        if (!hasName && !hasDepartment) {
            throw new IllegalArgumentException("Employee name or department is required");
        }

        String nameTerm = hasName ? name.trim().toLowerCase() : null;
        String departmentTerm = hasDepartment ? department.trim() : null;

        return employeeService.getAllEmployees()
                .stream()
                .filter(employee ->
                        !hasName
                                || employee.getFirstName().toLowerCase().contains(nameTerm)
                                || employee.getLastName().toLowerCase().contains(nameTerm))
                .filter(employee ->
                        !hasDepartment
                                || employee.getDepartment().equalsIgnoreCase(departmentTerm))
                .toList();
    }
}
