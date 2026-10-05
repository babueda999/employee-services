package com.example.employee.confirmation;

import com.example.employee.exception.PendingConfirmationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolConfirmationServiceImplTest {

    @Mock
    private PendingToolConfirmationRepository pendingToolConfirmationRepository;

    private ToolConfirmationServiceImpl toolConfirmationService;

    @BeforeEach
    void setUp() {
        toolConfirmationService = new ToolConfirmationServiceImpl(pendingToolConfirmationRepository);
    }

    @Test
    void createPending_savesAndReturnsAUniqueToken() {
        ArgumentCaptor<PendingToolConfirmation> captor = ArgumentCaptor.forClass(PendingToolConfirmation.class);

        String token = toolConfirmationService.createPending(
                "conv-1", "delete_employee", "{\"employeeId\":1}", "ADMIN");

        assertThat(token).isNotBlank();
        verify(pendingToolConfirmationRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(token);
        assertThat(captor.getValue().getConversationId()).isEqualTo("conv-1");
        assertThat(captor.getValue().getToolName()).isEqualTo("delete_employee");
        assertThat(captor.getValue().getArgumentsJson()).isEqualTo("{\"employeeId\":1}");
        assertThat(captor.getValue().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void resolve_returnsAndConsumesPending_whenTokenIsFreshAndKnown() {
        PendingToolConfirmation pending = new PendingToolConfirmation(
                "token-1", "conv-1", "delete_employee", "{\"employeeId\":1}", "ADMIN", Instant.now());

        when(pendingToolConfirmationRepository.findById("token-1")).thenReturn(Optional.of(pending));

        PendingToolConfirmation resolved = toolConfirmationService.resolve("token-1");

        assertThat(resolved).isEqualTo(pending);
        verify(pendingToolConfirmationRepository).deleteById("token-1");
    }

    @Test
    void resolve_throws_whenTokenUnknown() {
        when(pendingToolConfirmationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> toolConfirmationService.resolve("missing"))
                .isInstanceOf(PendingConfirmationNotFoundException.class);

        verify(pendingToolConfirmationRepository, never()).deleteById(any());
    }

    @Test
    void resolve_throwsAndStillConsumesToken_whenExpired() {
        PendingToolConfirmation expired = new PendingToolConfirmation(
                "token-1", "conv-1", "delete_employee", "{\"employeeId\":1}", "ADMIN",
                Instant.now().minusSeconds(600));

        when(pendingToolConfirmationRepository.findById("token-1")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> toolConfirmationService.resolve("token-1"))
                .isInstanceOf(PendingConfirmationNotFoundException.class);

        verify(pendingToolConfirmationRepository).deleteById("token-1");
    }

    @Test
    void resolve_throws_whenTokenIsNull() {
        assertThatThrownBy(() -> toolConfirmationService.resolve(null))
                .isInstanceOf(PendingConfirmationNotFoundException.class);
    }
}
