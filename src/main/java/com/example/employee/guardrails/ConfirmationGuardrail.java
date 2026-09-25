package com.example.employee.guardrails;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConfirmationGuardrail {

    private final Map<String, ConfirmationRequest> pendingConfirmations =
            new ConcurrentHashMap<>();

    public String createConfirmation(
            String userId,
            String toolName,
            String description
    ) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "User ID is required."
            );
        }

        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException(
                    "Tool name is required."
            );
        }

        String token = UUID.randomUUID().toString();

        ConfirmationRequest request =
                new ConfirmationRequest(
                        userId,
                        toolName,
                        description,
                        System.currentTimeMillis()
                );

        pendingConfirmations.put(token, request);

        return token;
    }

    public boolean confirm(
            String userId,
            String token
    ) {

        ConfirmationRequest request =
                pendingConfirmations.get(token);

        if (request == null) {
            return false;
        }

        if (!request.userId().equals(userId)) {
            throw new SecurityException(
                    "Confirmation does not belong to this user."
            );
        }

        pendingConfirmations.remove(token);

        return true;
    }

    public boolean isPending(String token) {
        return token != null &&
                pendingConfirmations.containsKey(token);
    }

    public void cancel(String token) {

        if (token != null) {
            pendingConfirmations.remove(token);
        }
    }

    public record ConfirmationRequest(
            String userId,
            String toolName,
            String description,
            long createdAt
    ) {
    }
}
