package com.example.employee.controller;

import com.example.employee.agent.EmployeeAgent;
import com.example.employee.dto.AgentRequest;
import com.example.employee.dto.AgentResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final EmployeeAgent employeeAgent;

    public AgentController(
            EmployeeAgent employeeAgent) {

        this.employeeAgent = employeeAgent;
    }


    // ASK
    @PostMapping
    public ResponseEntity<AgentResponse> ask(
            @Valid @RequestBody AgentRequest request) {

        String reply = employeeAgent.process(request.getMessage(), request.getRole());

        return ResponseEntity.ok(
                new AgentResponse(reply)
        );
    }
}
