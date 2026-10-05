package com.example.employee.controller;

import com.example.employee.agent.AgentReply;
import com.example.employee.agent.EmployeeSupervisorAgent;
import com.example.employee.dto.AgentConfirmRequest;
import com.example.employee.dto.AgentRequest;
import com.example.employee.usage.TokenUsageSnapshot;
import com.example.employee.usage.TokenUsageTracker;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentController.class)
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeSupervisorAgent employeeSupervisorAgent;

    @MockitoBean
    private TokenUsageTracker tokenUsageTracker;

    private AgentRequest requestWithMessage(String message) {
        AgentRequest request = new AgentRequest();
        request.setMessage(message);
        return request;
    }

    // --- POST /api/agent ---

    @Test
    void ask_returns200WithReplyAndConversationId_whenValid() throws Exception {
        when(employeeSupervisorAgent.process("Find employee 101", null, null))
                .thenReturn(new AgentReply(
                        "Employee 101 is John Doe.", "conv-1", false, null, List.of("get_employee")));

        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithMessage("Find employee 101"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Employee 101 is John Doe."))
                .andExpect(jsonPath("$.conversationId").value("conv-1"))
                .andExpect(jsonPath("$.confirmationRequired").value(false))
                .andExpect(jsonPath("$.toolsUsed[0]").value("get_employee"));
    }

    @Test
    void ask_returnsConfirmationFields_whenAgentRequestsConfirmation() throws Exception {
        when(employeeSupervisorAgent.process("Delete employee 5", "ADMIN", null))
                .thenReturn(new AgentReply(
                        "Deleting employee 5 needs your confirmation.", "conv-2", true, "token-abc",
                        List.of("delete_employee")));

        AgentRequest request = requestWithMessage("Delete employee 5");
        request.setRole("ADMIN");

        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmationRequired").value(true))
                .andExpect(jsonPath("$.confirmationToken").value("token-abc"))
                .andExpect(jsonPath("$.toolsUsed[0]").value("delete_employee"));
    }

    @Test
    void ask_returns400_whenMessageBlank() throws Exception {
        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithMessage(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));

        verify(employeeSupervisorAgent, never())
                .process(eq(""), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ask_returns400_whenMessageMissing() throws Exception {
        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void ask_returns500_whenAgentThrowsUnexpectedException() throws Exception {
        when(employeeSupervisorAgent.process("Find employee 101", null, null))
                .thenThrow(new IllegalStateException("At least one credential source must be specified"));

        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithMessage("Find employee 101"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    // --- POST /api/agent/confirm ---

    @Test
    void confirm_returns200WithReply_whenValid() throws Exception {
        AgentConfirmRequest request = new AgentConfirmRequest();
        request.setConfirmationToken("token-abc");
        request.setApprove(true);
        request.setRole("MANAGER");

        when(employeeSupervisorAgent.confirm("token-abc", true, "MANAGER"))
                .thenReturn(new AgentReply(
                        "Employee deleted", "conv-2", false, null, List.of("delete_employee")));

        mockMvc.perform(post("/api/agent/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Employee deleted"))
                .andExpect(jsonPath("$.confirmationRequired").value(false))
                .andExpect(jsonPath("$.toolsUsed[0]").value("delete_employee"));
    }

    @Test
    void confirm_returns403_whenApproverIsNotManager() throws Exception {
        AgentConfirmRequest request = new AgentConfirmRequest();
        request.setConfirmationToken("token-abc");
        request.setApprove(true);
        request.setRole("ADMIN");

        when(employeeSupervisorAgent.confirm("token-abc", true, "ADMIN"))
                .thenThrow(new SecurityException(
                        "Only managers can approve or deny a pending confirmation."));

        mockMvc.perform(post("/api/agent/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void confirm_returns400_whenTokenBlank() throws Exception {
        AgentConfirmRequest request = new AgentConfirmRequest();
        request.setConfirmationToken("");
        request.setApprove(true);

        mockMvc.perform(post("/api/agent/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void confirm_returns404_whenTokenUnknown() throws Exception {
        AgentConfirmRequest request = new AgentConfirmRequest();
        request.setConfirmationToken("missing-token");
        request.setApprove(true);

        request.setRole("MANAGER");

        when(employeeSupervisorAgent.confirm("missing-token", true, "MANAGER"))
                .thenThrow(new com.example.employee.exception.PendingConfirmationNotFoundException(
                        "No pending confirmation found for that token. It may have already been "
                                + "resolved, or it may have expired."));

        mockMvc.perform(post("/api/agent/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Pending Confirmation Not Found"));
    }

    // --- GET /api/agent/usage ---

    @Test
    void usage_returns200WithSnapshot() throws Exception {
        when(tokenUsageTracker.snapshot())
                .thenReturn(new TokenUsageSnapshot("gpt-5.2", 3, 120, 45, 165));

        mockMvc.perform(get("/api/agent/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("gpt-5.2"))
                .andExpect(jsonPath("$.requestCount").value(3))
                .andExpect(jsonPath("$.inputTokens").value(120))
                .andExpect(jsonPath("$.outputTokens").value(45))
                .andExpect(jsonPath("$.totalTokens").value(165));
    }
}
