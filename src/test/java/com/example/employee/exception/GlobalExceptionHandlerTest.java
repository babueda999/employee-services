package com.example.employee.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/employees/1");
    }

    @Test
    void handleEmployeeNotFound_returns404WithErrorResponse() {
        EmployeeNotFoundException exception =
                new EmployeeNotFoundException("Employee not found with id: 1");

        ResponseEntity<ErrorResponse> response =
                handler.handleEmployeeNotFound(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Employee Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Employee not found with id: 1");
        assertThat(response.getBody().getPath()).isEqualTo("/api/employees/1");
    }

    @Test
    void handleDuplicateEmployee_returns409WithErrorResponse() {
        DuplicateEmployeeException exception =
                new DuplicateEmployeeException("Employee already exists with email: a@b.com");

        ResponseEntity<ErrorResponse> response =
                handler.handleDuplicateEmployee(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Duplicate Employee");
    }

    @Test
    void handleValidationException_returns400WithJoinedFieldMessages() {
        FieldError firstNameError = new FieldError(
                "employeeRequest", "firstName", "First name is required");
        FieldError emailError = new FieldError(
                "employeeRequest", "email", "Invalid email format");

        BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(firstNameError, emailError));

        MethodArgumentNotValidException exception =
                org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ErrorResponse> response =
                handler.handleValidationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getMessage())
                .contains("firstName: First name is required")
                .contains("email: Invalid email format");
    }

    @Test
    void handleGenericException_returns500WithGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGenericException(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
