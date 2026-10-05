package com.example.employee.dto;

public class EmployeeResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private Double salary;
    private Boolean remoteWorkEligible;

    public EmployeeResponse() {
    }

    public EmployeeResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String department,
            Double salary,
            Boolean remoteWorkEligible) {

        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.department = department;
        this.salary = salary;
        this.remoteWorkEligible = remoteWorkEligible;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getDepartment() {
        return department;
    }

    public Double getSalary() {
        return salary;
    }

    public Boolean getRemoteWorkEligible() {
        return remoteWorkEligible;
    }
}