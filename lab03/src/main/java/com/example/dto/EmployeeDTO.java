package com.example.dto;

import com.example.model.Position;

/**
 * Data Transfer Object for exposing employee information through the API.
 */
public class EmployeeDTO {

    private String firstName;
    private String lastName;
    private String email;
    private String company;
    private Position position;
    private double salary;
    private String status; // e.g., ACTIVE, INACTIVE, TERMINATED, etc.

    public EmployeeDTO() {
    }

    public EmployeeDTO(String firstName, String lastName, String email,
                       String company, Position position, double salary, String status) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.company = company;
        this.position = position;
        this.salary = salary;
        this.status = status;
    }

    // Getters and setters

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "EmployeeDTO{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", company='" + company + '\'' +
                ", position=" + position +
                ", salary=" + salary +
                ", status='" + status + '\'' +
                '}';
    }
}
