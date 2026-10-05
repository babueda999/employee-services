package com.example.employee.usage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenUsageTrackerTest {

    private TokenUsageTracker tokenUsageTracker;

    @BeforeEach
    void setUp() {
        tokenUsageTracker = new TokenUsageTracker();
    }

    @Test
    void snapshot_reportsTheConfiguredModel_beforeAnyUsageIsRecorded() {
        TokenUsageSnapshot snapshot = tokenUsageTracker.snapshot();

        assertThat(snapshot.model()).isEqualTo("gpt-5.2");
        assertThat(snapshot.requestCount()).isZero();
        assertThat(snapshot.inputTokens()).isZero();
        assertThat(snapshot.outputTokens()).isZero();
        assertThat(snapshot.totalTokens()).isZero();
    }

    @Test
    void recordUsage_accumulatesAcrossMultipleCalls() {
        tokenUsageTracker.recordUsage("gpt-5.2", 100, 20, 120);
        tokenUsageTracker.recordUsage("gpt-5.2", 50, 10, 60);

        TokenUsageSnapshot snapshot = tokenUsageTracker.snapshot();

        assertThat(snapshot.model()).isEqualTo("gpt-5.2");
        assertThat(snapshot.requestCount()).isEqualTo(2);
        assertThat(snapshot.inputTokens()).isEqualTo(150);
        assertThat(snapshot.outputTokens()).isEqualTo(30);
        assertThat(snapshot.totalTokens()).isEqualTo(180);
    }

    @Test
    void recordUsage_updatesTheReportedModel() {
        tokenUsageTracker.recordUsage("gpt-5.2-pro", 10, 5, 15);

        assertThat(tokenUsageTracker.snapshot().model()).isEqualTo("gpt-5.2-pro");
    }
}
