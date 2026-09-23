package com.example.employee.controller;

import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    private EmployeeRequest validRequest() {
        EmployeeRequest request = new EmployeeRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setDepartment("Engineering");
        request.setSalary(75000.0);
        return request;
    }

    private EmployeeResponse sampleResponse(Long id) {
        return new EmployeeResponse(
                id, "John", "Doe", "john.doe@example.com", "Engineering", 75000.0);
    }

    // --- POST /api/employees ---

    @Test
    void createEmployee_returns201_whenValid() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequest.class)))
                .thenReturn(sampleResponse(1L));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void createEmployee_returns400_whenFirstNameBlank() throws Exception {
        EmployeeRequest request = validRequest();
        request.setFirstName("");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    @Test
    void createEmployee_returns400_whenSalaryNotPositive() throws Exception {
        EmployeeRequest request = validRequest();
        request.setSalary(-100.0);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEmployee_returns400_whenEmailInvalid() throws Exception {
        EmployeeRequest request = validRequest();
        request.setEmail("not-an-email");

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEmployee_returns409_whenEmailAlreadyExists() throws Exception {
        when(employeeService.createEmployee(any(EmployeeRequest.class)))
                .thenThrow(new DuplicateEmployeeException(
                        "Employee already exists with email: john.doe@example.com"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Duplicate Employee"));
    }

    // --- GET /api/employees/{id} ---

    @Test
    void getEmployeeById_returns200_whenFound() throws Exception {
        when(employeeService.getEmployeeById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get("/api/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getEmployeeById_returns404_whenMissing() throws Exception {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Employee Not Found"));
    }

    // --- GET /api/employees ---

    @Test
    void getAllEmployees_returns200WithList() throws Exception {
        when(employeeService.getAllEmployees())
                .thenReturn(List.of(sampleResponse(1L), sampleResponse(2L)));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllEmployees_returns200WithEmptyList() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of());

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // --- PUT /api/employees/{id} ---

    @Test
    void updateEmployee_returns200_whenValid() throws Exception {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class)))
                .thenReturn(sampleResponse(1L));

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateEmployee_returns404_whenMissing() throws Exception {
        when(employeeService.updateEmployee(eq(99L), any(EmployeeRequest.class)))
                .thenThrow(new EmployeeNotFoundException("Employee not found with id: 99"));

        mockMvc.perform(put("/api/employees/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateEmployee_returns409_whenEmailUsedByAnotherEmployee() throws Exception {
        when(employeeService.updateEmployee(eq(1L), any(EmployeeRequest.class)))
                .thenThrow(new DuplicateEmployeeException(
                        "Email already used by another employee: john.doe@example.com"));

        mockMvc.perform(put("/api/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict());
    }

    // --- DELETE /api/employees/{id} ---

    @Test
    void deleteEmployee_returns204_whenDeleted() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(employeeService).deleteEmployee(1L);
    }

    @Test
    void deleteEmployee_returns404_whenMissing() throws Exception {
        org.mockito.Mockito.doThrow(new EmployeeNotFoundException("Employee not found with id: 99"))
                .when(employeeService).deleteEmployee(99L);

        mockMvc.perform(delete("/api/employees/99"))
                .andExpect(status().isNotFound());
    }

    // --- GET /api/employees/search ---

    @Test
    void searchEmployees_returns200WithMatches() throws Exception {
        when(employeeService.searchEmployeesByName("John"))
                .thenReturn(List.of(sampleResponse(1L)));

        mockMvc.perform(get("/api/employees/search").param("name", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    void searchEmployees_returns200WithEmptyList_whenNoMatches() throws Exception {
        when(employeeService.searchEmployeesByName("zzz")).thenReturn(List.of());

        mockMvc.perform(get("/api/employees/search").param("name", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchEmployees_returns400_whenNameMissing() throws Exception {
        mockMvc.perform(get("/api/employees/search"))
                .andExpect(status().isBadRequest());
    }
}
