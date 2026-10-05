package com.example.employee.agent;

import com.example.employee.agent.subagents.EmployeeSubAgent;
import com.example.employee.confirmation.PendingToolConfirmation;
import com.example.employee.confirmation.ToolConfirmationService;
import com.example.employee.guardrails.AuthorizationGuardrail;
import com.example.employee.guardrails.InputGuardrail;
import com.example.employee.guardrails.OutputGuardrail;
import com.example.employee.guardrails.ToolGuardrail;
import com.example.employee.memory.ConversationMemoryService;
import com.example.employee.usage.TokenUsageTracker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeSupervisorAgentTest {

    @Mock
    private ConversationMemoryService conversationMemoryService;

    @Mock
    private ToolConfirmationService toolConfirmationService;

    @Mock
    private EmployeeSubAgent deleteEmployeeSubAgent;

    private EmployeeSupervisorAgent employeeSupervisorAgent;

    @BeforeEach
    void setUp() {
        employeeSupervisorAgent = new EmployeeSupervisorAgent(
                List.of(),
                conversationMemoryService,
                toolConfirmationService,
                new TokenUsageTracker(),
                new ObjectMapper(),
                new InputGuardrail(),
                new OutputGuardrail(),
                new ToolGuardrail(),
                new AuthorizationGuardrail());
    }

    // --- process: validation performed before any OpenAI call ---
    //
    // Only this part is unit-testable here: the OpenAI client is built
    // lazily from environment credentials on first use, so exercising an
    // actual tool-calling turn requires a real credential and is out of
    // scope for these tests.

    @Test
    void process_throwsIllegalArgumentException_whenMessageIsNull() {
        assertThatThrownBy(() -> employeeSupervisorAgent.process(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Employee agent request cannot be empty.");
    }

    @Test
    void process_throwsIllegalArgumentException_whenMessageIsBlank() {
        assertThatThrownBy(() -> employeeSupervisorAgent.process("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Employee agent request cannot be empty.");
    }

    @Test
    void process_throwsSecurityException_whenRoleIsUnrecognized() {
        assertThatThrownBy(() -> employeeSupervisorAgent.process("List all employees", "GUEST", null))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Unrecognized role: GUEST");
    }

    // --- confirm: no OpenAI call involved, fully testable ---

    private PendingToolConfirmation pendingDelete() {
        return new PendingToolConfirmation(
                "token-1", "conv-1", "delete_employee", "{\"employeeId\":1}", "ADMIN", Instant.now());
    }

    @Test
    void confirm_doesNotExecute_whenDeniedByManager() {
        when(toolConfirmationService.resolve("token-1")).thenReturn(pendingDelete());

        AgentReply reply = employeeSupervisorAgent.confirm("token-1", false, "MANAGER");

        assertThat(reply.text()).isEqualTo("Okay, I won't go ahead with that.");
        assertThat(reply.conversationId()).isEqualTo("conv-1");
        assertThat(reply.confirmationRequired()).isFalse();
        assertThat(reply.toolsUsed()).containsExactly("delete_employee");
        verify(conversationMemoryService).appendAssistantMessage("conv-1", reply.text());
    }

    @Test
    void confirm_throwsSecurityException_whenApproverIsNotManager() {
        assertThatThrownBy(() -> employeeSupervisorAgent.confirm("token-1", true, "ADMIN"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Only managers can approve or deny a pending confirmation.");

        verify(toolConfirmationService, org.mockito.Mockito.never()).resolve(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void confirm_throwsSecurityException_whenApproverRoleOmitted() {
        assertThatThrownBy(() -> employeeSupervisorAgent.confirm("token-1", true, null))
                .isInstanceOf(SecurityException.class)
                .hasMessage("Only managers can approve or deny a pending confirmation.");

        verify(toolConfirmationService, org.mockito.Mockito.never()).resolve(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void confirm_executesAndSummarizesResult_whenApprovedByManager() {
        when(toolConfirmationService.resolve("token-1")).thenReturn(pendingDelete());

        EmployeeSupervisorAgent agentWithDeleteSubAgent = new EmployeeSupervisorAgent(
                List.of(namedSubAgent(deleteEmployeeSubAgent, "delete_employee")),
                conversationMemoryService,
                toolConfirmationService,
                new TokenUsageTracker(),
                new ObjectMapper(),
                new InputGuardrail(),
                new OutputGuardrail(),
                new ToolGuardrail(),
                new AuthorizationGuardrail());

        when(deleteEmployeeSubAgent.handle("{\"employeeId\":1}", "ADMIN"))
                .thenReturn("{\"success\": true, \"message\": \"Employee deleted\"}");

        AgentReply reply = agentWithDeleteSubAgent.confirm("token-1", true, "MANAGER");

        assertThat(reply.text()).isEqualTo("Employee deleted");
        assertThat(reply.conversationId()).isEqualTo("conv-1");
        assertThat(reply.toolsUsed()).containsExactly("delete_employee");
        verify(conversationMemoryService).appendAssistantMessage("conv-1", "Employee deleted");
    }

    private EmployeeSubAgent namedSubAgent(EmployeeSubAgent mock, String name) {
        when(mock.name()).thenReturn(name);
        return mock;
    }
}
