package com.techcorp.model;

import java.util.Objects;

public class Employee {
    private String fullName;
    private final String email;
    private String companyName;
    private Position position;
    private double salary;

    public Employee(String fullName, String email, String companyName, Position position, double salary) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must be provided");
        }
        this.fullName = fullName;
        this.email = email;
        this.companyName = companyName;
        this.position = position;
        setSalary(salary);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        if (salary < 0) throw new IllegalArgumentException("Salary cannot be negative");
        this.salary = salary;
    }

    // equals & hashCode based solely on email (uniqueness)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee)) return false;
        Employee that = (Employee) o;
        return Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return "Employee{" +
                "fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", companyName='" + companyName + '\'' +
                ", position=" + position +
                ", salary=" + salary +
                '}';
    }
}
