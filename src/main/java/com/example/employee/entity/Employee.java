package com.example.employee.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "employees",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_email",
                        columnNames = "email"
                )
        }
)
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private Double salary;

    @Column
    private Boolean remoteWorkEligible;

    public Employee() {
    }

    public Employee(
            String firstName,
            String lastName,
            String email,
            String department,
            Double salary,
            Boolean remoteWorkEligible) {

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

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Double getSalary() {
        return salary;
    }

    public void setSalary(Double salary) {
        this.salary = salary;
    }

    public Boolean getRemoteWorkEligible() {
        return remoteWorkEligible;
    }

    public void setRemoteWorkEligible(Boolean remoteWorkEligible) {
        this.remoteWorkEligible = remoteWorkEligible;
    }
}