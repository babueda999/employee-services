package com.example.employee.usage;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Running total of OpenAI token usage across every call
 * {@code EmployeeSupervisorAgent} makes, for a simple "how much has this
 * demo cost so far" readout (e.g. the home page voice-over). In-memory only
 * — intentionally not persisted to H2, since it resets along with every
 * other piece of this app's state on restart, which is fine for a stat
 * whose only purpose is a rough running total during a session.
 */
@Component
public class TokenUsageTracker {

    /**
     * The only model this app's agent currently calls. Shown even before
     * any request has been made, since "what model is used" is a static
     * fact about the app, not something that depends on usage history.
     */
    private static final String DEFAULT_MODEL = "gpt-5.2";

    private final AtomicLong requestCount = new AtomicLong();
    private final AtomicLong inputTokens = new AtomicLong();
    private final AtomicLong outputTokens = new AtomicLong();
    private final AtomicLong totalTokens = new AtomicLong();
    private volatile String model = DEFAULT_MODEL;

    public void recordUsage(String model, long inputTokens, long outputTokens, long totalTokens) {

        this.model = model;
        requestCount.incrementAndGet();
        this.inputTokens.addAndGet(inputTokens);
        this.outputTokens.addAndGet(outputTokens);
        this.totalTokens.addAndGet(totalTokens);
    }

    public TokenUsageSnapshot snapshot() {

        return new TokenUsageSnapshot(
                model,
                requestCount.get(),
                inputTokens.get(),
                outputTokens.get(),
                totalTokens.get()
        );
    }
}
