package com.example.employee.confirmation;

import com.example.employee.exception.PendingConfirmationNotFoundException;

/**
 * Holds CRITICAL-risk tool calls (see {@code ToolGuardrail}) that have been
 * authorized but are waiting on a separate, explicit human confirmation
 * before they execute.
 */
public interface ToolConfirmationService {

    /**
     * Records a pending confirmation and returns its single-use token.
     */
    String createPending(
            String conversationId,
            String toolName,
            String argumentsJson,
            String role);

    /**
     * Consumes (removes) the pending confirmation for {@code token}.
     *
     * @throws PendingConfirmationNotFoundException if the token is unknown,
     *         already resolved, or has expired
     */
    PendingToolConfirmation resolve(String token);
}
