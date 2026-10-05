package com.example.employee.memory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ConversationMessageRepositoryTest {

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    @Test
    void findTop20ByConversationIdOrderByCreatedAtDesc_returnsOnlyThatConversation_newestFirst() {
        Instant base = Instant.now();

        conversationMessageRepository.save(
                new ConversationMessage("conv-1", "user", "first", base));
        conversationMessageRepository.save(
                new ConversationMessage("conv-1", "assistant", "second", base.plusSeconds(1)));
        conversationMessageRepository.save(
                new ConversationMessage("conv-2", "user", "other conversation", base.plusSeconds(2)));

        List<ConversationMessage> result =
                conversationMessageRepository.findTop20ByConversationIdOrderByCreatedAtDesc("conv-1");

        assertThat(result).extracting(ConversationMessage::getContent)
                .containsExactly("second", "first");
    }

    @Test
    void findTop20ByConversationIdOrderByCreatedAtDesc_limitsToTwenty() {
        Instant base = Instant.now();

        for (int i = 0; i < 25; i++) {
            conversationMessageRepository.save(
                    new ConversationMessage("conv-1", "user", "message-" + i, base.plusSeconds(i)));
        }

        List<ConversationMessage> result =
                conversationMessageRepository.findTop20ByConversationIdOrderByCreatedAtDesc("conv-1");

        assertThat(result).hasSize(20);
        assertThat(result.get(0).getContent()).isEqualTo("message-24");
    }
}
