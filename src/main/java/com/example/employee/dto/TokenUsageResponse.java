package com.example.employee.dto;

public class TokenUsageResponse {

    private String model;
    private long requestCount;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;

    public TokenUsageResponse() {
    }

    public TokenUsageResponse(
            String model,
            long requestCount,
            long inputTokens,
            long outputTokens,
            long totalTokens) {

        this.model = model;
        this.requestCount = requestCount;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(long requestCount) {
        this.requestCount = requestCount;
    }

    public long getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(long inputTokens) {
        this.inputTokens = inputTokens;
    }

    public long getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(long outputTokens) {
        this.outputTokens = outputTokens;
    }

    public long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(long totalTokens) {
        this.totalTokens = totalTokens;
    }
}
