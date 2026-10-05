package com.example.employee.confirmation;

import com.example.employee.exception.PendingConfirmationNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ToolConfirmationServiceImpl implements ToolConfirmationService {

    /**
     * How long a caller has to confirm before the token is treated as
     * expired. Short enough that a stale confirmation from an abandoned
     * conversation can't be replayed much later.
     */
    private static final Duration CONFIRMATION_TTL = Duration.ofMinutes(5);

    private final PendingToolConfirmationRepository pendingToolConfirmationRepository;

    public ToolConfirmationServiceImpl(PendingToolConfirmationRepository pendingToolConfirmationRepository) {
        this.pendingToolConfirmationRepository = pendingToolConfirmationRepository;
    }

    @Override
    public String createPending(
            String conversationId,
            String toolName,
            String argumentsJson,
            String role) {

        String token = UUID.randomUUID().toString();

        pendingToolConfirmationRepository.save(
                new PendingToolConfirmation(
                        token, conversationId, toolName, argumentsJson, role, Instant.now())
        );

        return token;
    }

    @Override
    public PendingToolConfirmation resolve(String token) {

        Optional<PendingToolConfirmation> pending =
                token == null
                        ? Optional.empty()
                        : pendingToolConfirmationRepository.findById(token);

        if (pending.isEmpty()) {
            throw new PendingConfirmationNotFoundException(
                    "No pending confirmation found for that token. It may have already been "
                            + "resolved, or it may have expired."
            );
        }

        PendingToolConfirmation confirmation = pending.get();

        // Single-use: remove it whether it turns out to be expired or not.
        pendingToolConfirmationRepository.deleteById(token);

        if (Duration.between(confirmation.getCreatedAt(), Instant.now()).compareTo(CONFIRMATION_TTL) > 0) {
            throw new PendingConfirmationNotFoundException(
                    "No pending confirmation found for that token. It may have already been "
                            + "resolved, or it may have expired."
            );
        }

        return confirmation;
    }
}
