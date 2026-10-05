package com.example.employee.memory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    /**
     * Most recent {@code limit} messages for a conversation, newest first —
     * callers that want chronological order (replaying history to the
     * model) must reverse the result themselves. Bounding by a limit keeps
     * replayed context — and OpenAI token cost — from growing unbounded as
     * a conversation gets long.
     */
    List<ConversationMessage> findTop20ByConversationIdOrderByCreatedAtDesc(String conversationId);
}
