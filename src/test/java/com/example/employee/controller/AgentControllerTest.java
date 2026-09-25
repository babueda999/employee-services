package com.example.employee.controller;

import com.example.employee.agent.EmployeeAgent;
import com.example.employee.dto.AgentRequest;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private EmployeeAgent employeeAgent;

    private AgentRequest requestWithMessage(String message) {
        AgentRequest request = new AgentRequest();
        request.setMessage(message);
        return request;
    }

    // --- POST /api/agent ---

    @Test
    void ask_returns200WithReply_whenValid() throws Exception {
        when(employeeAgent.process("Find employee 101", null))
                .thenReturn("Employee 101 is John Doe.");

        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithMessage("Find employee 101"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Employee 101 is John Doe."));
    }

    @Test
    void ask_returns400_whenMessageBlank() throws Exception {
        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithMessage(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));

        verify(employeeAgent, never()).process(eq(""), org.mockito.ArgumentMatchers.any());
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
        when(employeeAgent.process("Find employee 101", null))
                .thenThrow(new IllegalStateException("At least one credential source must be specified"));

        mockMvc.perform(post("/api/agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithMessage("Find employee 101"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}
