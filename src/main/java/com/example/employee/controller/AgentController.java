package com.example.employee.controller;

import com.example.employee.agent.AgentReply;
import com.example.employee.agent.EmployeeSupervisorAgent;
import com.example.employee.dto.AgentConfirmRequest;
import com.example.employee.dto.AgentRequest;
import com.example.employee.dto.AgentResponse;
import com.example.employee.dto.TokenUsageResponse;
import com.example.employee.usage.TokenUsageSnapshot;
import com.example.employee.usage.TokenUsageTracker;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final EmployeeSupervisorAgent employeeSupervisorAgent;
    private final TokenUsageTracker tokenUsageTracker;

    public AgentController(
            EmployeeSupervisorAgent employeeSupervisorAgent,
            TokenUsageTracker tokenUsageTracker) {

        this.employeeSupervisorAgent = employeeSupervisorAgent;
        this.tokenUsageTracker = tokenUsageTracker;
    }


    // ASK
    @PostMapping
    public ResponseEntity<AgentResponse> ask(
            @Valid @RequestBody AgentRequest request) {

        AgentReply reply = employeeSupervisorAgent.process(
                request.getMessage(), request.getRole(), request.getConversationId());

        return ResponseEntity.ok(toResponse(reply));
    }

    // CONFIRM
    @PostMapping("/confirm")
    public ResponseEntity<AgentResponse> confirm(
            @Valid @RequestBody AgentConfirmRequest request) {

        AgentReply reply = employeeSupervisorAgent.confirm(
                request.getConfirmationToken(), request.getApprove(), request.getRole());

        return ResponseEntity.ok(toResponse(reply));
    }

    // USAGE
    @GetMapping("/usage")
    public ResponseEntity<TokenUsageResponse> usage() {

        TokenUsageSnapshot snapshot = tokenUsageTracker.snapshot();

        return ResponseEntity.ok(new TokenUsageResponse(
                snapshot.model(),
                snapshot.requestCount(),
                snapshot.inputTokens(),
                snapshot.outputTokens(),
                snapshot.totalTokens()
        ));
    }

    private AgentResponse toResponse(AgentReply reply) {
        return new AgentResponse(
                reply.text(),
                reply.conversationId(),
                reply.confirmationRequired(),
                reply.confirmationToken(),
                reply.toolsUsed()
        );
    }
}
