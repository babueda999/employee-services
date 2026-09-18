# employee-servicves
# Employee Service - Spring Boot CRUD Microservice

A production-style Employee Management REST API built using **Java 17, Spring Boot, Spring Data JPA, Hibernate, and H2 Database**.

This project demonstrates a clean layered architecture with:

- REST APIs
- CRUD operations
- DTOs
- Entity/DTO mapping
- Service layer
- Repository layer
- H2 database
- Bean Validation
- Global exception handling
- Duplicate email validation
- Proper HTTP status codes
- Maven build

---

## Table of Contents

- [Project Overview](#project-overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Employee Object](#employee-object)
- [Create Employee](#1-create-employee)
- [Get Employee](#2-get-employee-by-id)
- [Get All Employees](#3-get-all-employees)
- [Update Employee](#4-update-employee)
- [Delete Employee](#5-delete-employee)
- [Validation](#validation)
- [Exception Handling](#exception-handling)
- [H2 Database](#h2-database)
- [How to Run](#how-to-run)
- [Maven Commands](#maven-commands)
- [Postman Testing](#postman-testing)
- [HTTP Status Codes](#http-status-codes)
- [Design Patterns](#design-patterns)
- [Future Enhancements](#future-enhancements)
- [Author](#author)

---

# Project Overview

The Employee Service provides REST APIs to manage employee information.

The service supports:

```text
CREATE
READ
UPDATE
DELETE
