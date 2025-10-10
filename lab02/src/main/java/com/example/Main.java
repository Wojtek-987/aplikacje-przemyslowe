package com.example;

import com.example.exception.ApiException;
import com.example.model.CompanyStatistics;
import com.example.model.Employee;
import com.example.model.ImportSummary;
import com.example.service.ApiService;
import com.example.service.EmployeeService;
import com.example.service.ImportService;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        EmployeeService directory = new EmployeeService();
        ImportService importService = new ImportService(directory);
        ApiService apiService = new ApiService();

        System.out.println("=== CSV import ===");
        ImportSummary summary = importService.importFromCsv("src/main/java/com/example/employees.csv");
        System.out.println("Imported: " + summary.getImportedCount());
        summary.getErrors().forEach(System.out::println);

        System.out.println("\n=== API fetch ===");
        try {
            List<Employee> apiEmployees = apiService.fetchEmployeesFromApi();
            directory.addAll(apiEmployees);
            System.out.println("Fetched from API: " + apiEmployees.size());
        } catch (ApiException e) {
            System.err.println("API error: " + e.getMessage());
        }

        System.out.println("\n=== Salary consistency (below base) ===");
        directory.validateSalaryConsistency().forEach(System.out::println);

        System.out.println("\n=== Company statistics ===");
        Map<String, CompanyStatistics> stats = directory.getCompanyStatistics();
        stats.forEach((k, v) -> System.out.println(k + " -> " + v));
    }
}
