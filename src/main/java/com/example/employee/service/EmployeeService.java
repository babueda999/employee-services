package com.example.employee.service;



import com.example.employee.dto.EmployeeRequest;
import com.example.employee.dto.EmployeeResponse;

import java.util.List;

public interface EmployeeService {

    EmployeeResponse createEmployee(
            EmployeeRequest request);

    EmployeeResponse getEmployeeById(
            Long id);

    List<EmployeeResponse> getAllEmployees();

    EmployeeResponse updateEmployee(
            Long id,
            EmployeeRequest request);

    void deleteEmployee(Long id);
}