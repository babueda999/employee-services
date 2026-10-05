package com.example.employee.memory;

import java.util.List;

/**
 * Persists and replays per-conversation agent history, so a conversation
 * started in one HTTP request can be continued in the next one by passing
 * back the same {@code conversationId}.
 */
public interface ConversationMemoryService {

    /**
     * The most recent turns for a conversation, oldest first, bounded so
     * replayed context doesn't grow unbounded. Empty for an unknown or new
     * conversation ID — never null.
     */
    List<ConversationMessage> getRecentHistory(String conversationId);

    void appendUserMessage(String conversationId, String content);

    void appendAssistantMessage(String conversationId, String content);
}
