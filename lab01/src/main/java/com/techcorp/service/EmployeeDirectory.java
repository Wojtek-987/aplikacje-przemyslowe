package com.techcorp.service;

import com.techcorp.model.Employee;
import com.techcorp.model.Position;

import java.util.*;
import java.util.stream.Collectors;

public class EmployeeDirectory {

    private final Map<String, Employee> byEmail = new LinkedHashMap<>();

    public void addEmployee(Employee employee) {
        Objects.requireNonNull(employee);
        String email = employee.getEmail();
        if (byEmail.containsKey(email)) {
            throw new IllegalArgumentException("Employee with email already exists: " + email);
        }
        byEmail.put(email, employee);
    }

    public List<Employee> getAllEmployees() {
        return List.copyOf(byEmail.values());
    }

    public List<Employee> findByCompany(String companyName) {
        return byEmail.values().stream()
                .filter(e -> e.getCompanyName() != null
                        && e.getCompanyName().equalsIgnoreCase(companyName))
                .collect(Collectors.toList());
    }

    public List<Employee> getEmployeesSortedBySurname() {
        return byEmail.values().stream()
                .sorted(Comparator
                        .comparing((Employee e) -> extractSurname(e.getFullName()),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Employee::getFullName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    public Map<Position, List<Employee>> groupByPosition() {
        return byEmail.values().stream()
                .collect(Collectors.groupingBy(Employee::getPosition));
    }

    public Map<Position, Long> countByPosition() {
        return byEmail.values().stream()
                .collect(Collectors.groupingBy(Employee::getPosition, Collectors.counting()));
    }


    public OptionalDouble averageSalary() {
        return byEmail.values().stream()
                .mapToDouble(Employee::getSalary)
                .average();
    }

    public Optional<Employee> highestPaid() {
        return byEmail.values().stream()
                .max(Comparator.comparingDouble(Employee::getSalary));
    }

    private static String extractSurname(String fullName) {
        if (fullName == null || fullName.isBlank()) return "";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }
}