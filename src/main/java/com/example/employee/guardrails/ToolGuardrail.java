package com.example.employee.guardrails;

import com.example.employee.agent.tools.AdjustSalaryTool;
import com.example.employee.agent.tools.DeleteEmployeeTool;
import com.example.employee.agent.tools.GetEmployeeTool;
import com.example.employee.agent.tools.ListEmployeesTool;
import com.example.employee.agent.tools.SearchEmployeeTool;
import com.example.employee.agent.tools.UpdateEmployeeTool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class ToolGuardrail {

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    private static final Map<String, RiskLevel> TOOL_RISK_LEVELS = Map.of(
            GetEmployeeTool.NAME, RiskLevel.LOW,
            ListEmployeesTool.NAME, RiskLevel.LOW,
            SearchEmployeeTool.NAME, RiskLevel.LOW,
            "create_employee", RiskLevel.MEDIUM,
            UpdateEmployeeTool.NAME, RiskLevel.HIGH,
            AdjustSalaryTool.NAME, RiskLevel.HIGH,
            DeleteEmployeeTool.NAME, RiskLevel.CRITICAL
    );

    public RiskLevel validateTool(String toolName) {

        if (toolName == null || toolName.isBlank()) {
            throw new SecurityException(
                    "Tool name cannot be empty."
            );
        }

        RiskLevel riskLevel = TOOL_RISK_LEVELS.get(toolName);

        if (riskLevel == null) {
            throw new SecurityException(
                    "Tool is not registered: " + toolName
            );
        }

        return riskLevel;
    }

    public boolean requiresConfirmation(String toolName) {

        RiskLevel riskLevel = validateTool(toolName);

        return riskLevel == RiskLevel.CRITICAL;
    }

    public void authorizeTool(
            String toolName,
            String role
    ) {

        RiskLevel riskLevel = validateTool(toolName);

        String normalizedRole = role.toUpperCase();

        switch (riskLevel) {

            case LOW -> {
                if (!Set.of(
                        "USER",
                        "MANAGER",
                        "ADMIN"
                ).contains(normalizedRole)) {

                    throw new SecurityException(
                            "User is not authorized to execute tool: "
                                    + toolName
                    );
                }
            }

            case MEDIUM, HIGH -> {
                if (!Set.of(
                        "MANAGER",
                        "ADMIN"
                ).contains(normalizedRole)) {

                    throw new SecurityException(
                            "User is not authorized to execute tool: "
                                    + toolName
                    );
                }
            }

            case CRITICAL -> {
                if (!"ADMIN".equals(normalizedRole)) {

                    throw new SecurityException(
                            "Only ADMIN users can execute critical tool: "
                                    + toolName
                    );
                }
            }
        }
    }

    public RiskLevel getRiskLevel(String toolName) {
        return validateTool(toolName);
    }
}
