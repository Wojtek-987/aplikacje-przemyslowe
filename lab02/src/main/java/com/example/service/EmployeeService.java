package com.example.service;

import com.example.model.CompanyStatistics;
import com.example.model.Employee;
import com.example.model.Position;

import java.util.*;
import java.util.stream.Collectors;

public class EmployeeService {

    private final Map<String, Employee> byEmail = new LinkedHashMap<>();

    public boolean addEmployee(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Cannot add null employee");
        }

        String email = employee.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Employee email must not be null or blank");
        }

        if (byEmail.containsKey(email)) {
            throw new IllegalArgumentException("Employee with email already exists: " + email);
        }

        byEmail.put(email, employee);
        return true;
    }

    public void addAll(Collection<Employee> employees) {
        if (employees == null) return;
        for (Employee e : employees) {
            addEmployee(e);
        }
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

    // --------------------- NEW ANALYTICS ---------------------

    /**
     * Returns employees whose salary is below the base salary for their Position.
     */
    public List<Employee> validateSalaryConsistency() {
        return byEmail.values().stream()
                .filter(e -> e.getSalary() < e.getPosition().getBaseSalary())
                .collect(Collectors.toList());
    }

    /**
     * Groups by companyName (blank/null coalesced to "(unknown)"), then for each group computes:
     *  - employee count
     *  - average salary
     *  - full name of the highest paid employee
     */
    public Map<String, CompanyStatistics> getCompanyStatistics() {
        // group by normalised company name
        Map<String, List<Employee>> byCompany = byEmail.values().stream()
                .collect(Collectors.groupingBy(e -> normaliseCompany(e.getCompanyName())));

        return byCompany.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            List<Employee> list = entry.getValue();
                            long count = list.size();
                            double avg = list.stream().mapToDouble(Employee::getSalary).average().orElse(0.0);
                            String top = list.stream()
                                    .max(Comparator.comparingDouble(Employee::getSalary))
                                    .map(Employee::getFullName)
                                    .orElse("");
                            return new CompanyStatistics(count, avg, top);
                        }
                ));
    }

    private static String normaliseCompany(String c) {
        if (c == null) return "(unknown)";
        String t = c.trim();
        return t.isEmpty() ? "(unknown)" : t;
    }
}
