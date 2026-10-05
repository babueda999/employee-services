package com.example.employee.agent.subagents;

import com.openai.models.responses.FunctionTool;

/**
 * A single-tool agent: owns one employee operation end-to-end — its OpenAI
 * function schema, its role-based authorization, and its execution.
 * {@code EmployeeSupervisorAgent} discovers every bean implementing this
 * interface and delegates to whichever one matches the tool name OpenAI
 * decides to call, rather than switching on tool name itself.
 */
public interface EmployeeSubAgent {

    /**
     * Tool name used by OpenAI function-calling and by the supervisor to
     * route a tool call to this sub-agent. Matches the corresponding
     * {@code agent.tools.*Tool.NAME} constant.
     */
    String name();

    /**
     * OpenAI function-calling schema for this tool.
     */
    FunctionTool definition();

    /**
     * Authorizes {@code role} for this specific operation on its own,
     * without executing anything. Throws {@link SecurityException} when the
     * role lacks permission. Split out from {@link #handle} so a caller
     * (the supervisor, gating a CRITICAL-risk tool behind a human
     * confirmation) can check permission up front without running the
     * operation itself.
     */
    void authorize(String role);

    /**
     * Authorizes {@code role} for this specific operation, then executes it
     * with the given (raw JSON) arguments. Throws {@link SecurityException}
     * when the role lacks permission for this operation.
     */
    String handle(String arguments, String role);

    /**
     * Null/blank-safe role default, shared by every sub-agent and by the
     * supervisor's own top-level role handling.
     */
    default String normalizeRole(String role) {
        return role == null || role.isBlank() ? "USER" : role;
    }
}
