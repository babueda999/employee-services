package com.example.employee.usage;

/**
 * Point-in-time total of OpenAI token usage accumulated by
 * {@code EmployeeSupervisorAgent} since this JVM started (in-memory only,
 * like the rest of this app's H2-backed state — a restart resets it).
 */
public record TokenUsageSnapshot(
        String model,
        long requestCount,
        long inputTokens,
        long outputTokens,
        long totalTokens) {
}
