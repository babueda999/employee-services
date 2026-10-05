package com.example.employee.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryServiceImplTest {

    @Mock
    private ConversationMessageRepository conversationMessageRepository;

    private ConversationMemoryServiceImpl conversationMemoryService;

    @BeforeEach
    void setUp() {
        conversationMemoryService = new ConversationMemoryServiceImpl(conversationMessageRepository);
    }

    @Test
    void getRecentHistory_returnsEmptyList_whenConversationIdIsNull() {
        assertThat(conversationMemoryService.getRecentHistory(null)).isEmpty();
    }

    @Test
    void getRecentHistory_returnsEmptyList_whenConversationIdIsBlank() {
        assertThat(conversationMemoryService.getRecentHistory("   ")).isEmpty();
    }

    @Test
    void getRecentHistory_reversesRepositoryResult_toChronologicalOrder() {
        ConversationMessage newest = new ConversationMessage("conv-1", "assistant", "second", Instant.now());
        ConversationMessage oldest = new ConversationMessage("conv-1", "user", "first", Instant.now().minusSeconds(60));

        when(conversationMessageRepository.findTop20ByConversationIdOrderByCreatedAtDesc("conv-1"))
                .thenReturn(List.of(newest, oldest));

        List<ConversationMessage> history = conversationMemoryService.getRecentHistory("conv-1");

        assertThat(history).containsExactly(oldest, newest);
    }

    @Test
    void appendUserMessage_savesMessageWithUserRole() {
        ArgumentCaptor<ConversationMessage> captor = ArgumentCaptor.forClass(ConversationMessage.class);

        conversationMemoryService.appendUserMessage("conv-1", "Find employee 1");

        verify(conversationMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getConversationId()).isEqualTo("conv-1");
        assertThat(captor.getValue().getRole()).isEqualTo("user");
        assertThat(captor.getValue().getContent()).isEqualTo("Find employee 1");
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void appendAssistantMessage_savesMessageWithAssistantRole() {
        ArgumentCaptor<ConversationMessage> captor = ArgumentCaptor.forClass(ConversationMessage.class);

        conversationMemoryService.appendAssistantMessage("conv-1", "Employee 1 is John Doe.");

        verify(conversationMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("assistant");
        assertThat(captor.getValue().getContent()).isEqualTo("Employee 1 is John Doe.");
    }
}
