package com.example.employee.memory;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ConversationMemoryServiceImpl implements ConversationMemoryService {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final ConversationMessageRepository conversationMessageRepository;

    public ConversationMemoryServiceImpl(ConversationMessageRepository conversationMessageRepository) {
        this.conversationMessageRepository = conversationMessageRepository;
    }

    @Override
    public List<ConversationMessage> getRecentHistory(String conversationId) {

        if (conversationId == null || conversationId.isBlank()) {
            return Collections.emptyList();
        }

        List<ConversationMessage> newestFirst = new ArrayList<>(
                conversationMessageRepository.findTop20ByConversationIdOrderByCreatedAtDesc(conversationId));

        Collections.reverse(newestFirst);

        return newestFirst;
    }

    @Override
    public void appendUserMessage(String conversationId, String content) {
        append(conversationId, ROLE_USER, content);
    }

    @Override
    public void appendAssistantMessage(String conversationId, String content) {
        append(conversationId, ROLE_ASSISTANT, content);
    }

    private void append(String conversationId, String role, String content) {

        conversationMessageRepository.save(
                new ConversationMessage(conversationId, role, content, Instant.now())
        );
    }
}
