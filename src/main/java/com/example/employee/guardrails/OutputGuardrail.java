package com.example.employee.guardrails;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutputGuardrail {

    private static final List<String> SENSITIVE_PATTERNS = List.of(
            "OPENAI_API_KEY",
            "AWS_SECRET_ACCESS_KEY",
            "AWS_ACCESS_KEY_ID",
            "password=",
            "passwd=",
            "secret=",
            "private_key",
            "BEGIN PRIVATE KEY"
    );

    private static final int MAX_RESPONSE_LENGTH = 10000;

    public String validate(String response) {

        if (response == null || response.isBlank()) {
            throw new IllegalStateException(
                    "Agent returned an empty response."
            );
        }

        if (response.length() > MAX_RESPONSE_LENGTH) {
            throw new IllegalStateException(
                    "Agent response exceeds the maximum allowed length."
            );
        }

        String normalizedResponse =
                response.toLowerCase();

        for (String pattern : SENSITIVE_PATTERNS) {

            if (normalizedResponse.contains(
                    pattern.toLowerCase()
            )) {

                throw new SecurityException(
                        "Agent response contains potentially sensitive information."
                );
            }
        }

        return response.trim();
    }

    public boolean isSafe(String response) {

        try {
            validate(response);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
