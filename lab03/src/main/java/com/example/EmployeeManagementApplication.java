package com.example;

import com.example.model.CompanyStatistics;
import com.example.model.Employee;
import com.example.model.ImportSummary;
import com.example.service.ApiService;
import com.example.service.EmployeeService;
import com.example.service.ImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import org.springframework.core.io.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@ImportResource("classpath:employees-beans.xml")
public class EmployeeManagementApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EmployeeManagementApplication.class);

    private final ImportService importService;
    private final EmployeeService employeeService;
    private final ApiService apiService;
    private final List<Employee> xmlEmployees;
    private final Resource csvResource;

    public EmployeeManagementApplication(
            ImportService importService,
            EmployeeService employeeService,
            ApiService apiService,
            @Qualifier("xmlEmployees") List<Employee> xmlEmployees,
            @Value("${app.import.csv-file}") Resource csvResource
    ) {
        this.importService = importService;
        this.employeeService = employeeService;
        this.apiService = apiService;
        this.xmlEmployees = xmlEmployees;
        this.csvResource = csvResource;
    }

    public static void main(String[] args) {
        SpringApplication.run(EmployeeManagementApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("=== Employee Management demo starting ===");

        // 1) Import employees from CSV
        try {
            String resolvedPath = resolveResourceToPath(csvResource);
            log.info("Importing employees from CSV at '{}'", resolvedPath);
            ImportSummary summary = importService.importFromCsv(resolvedPath);
            log.info("CSV import: {} imported, {} errors", summary.getImportedCount(), summary.getErrors().size());
            summary.getErrors().forEach(err -> log.warn("CSV error: {}", err));
        } catch (Exception e) {
            log.error("CSV import failed: {}", e.getMessage(), e);
        }

        // 2) Add XML-defined employees
        log.info("Adding {} XML-defined employees", xmlEmployees.size());
        xmlEmployees.forEach(e -> {
            try {
                employeeService.addEmployee(e);
            } catch (IllegalArgumentException dup) {
                log.warn("Skipping XML employee '{}': {}", e.getEmail(), dup.getMessage());
            }
        });

        // 3) Fetch employees from external REST API and add them
        try {
            List<Employee> apiEmployees = apiService.fetchEmployeesFromApi();
            log.info("Fetched {} employees from API", apiEmployees.size());
            apiEmployees.forEach(e -> {
                try {
                    employeeService.addEmployee(e);
                } catch (IllegalArgumentException dup) {
                    log.warn("Skipping API employee '{}': {}", e.getEmail(), dup.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("API retrieval failed: {}", e.getMessage(), e);
        }

        // 4) Show statistics for a chosen company (example: TechCorp)
        String company = "TechCorp";
        Map<String, CompanyStatistics> stats = employeeService.getCompanyStatistics();
        CompanyStatistics s = stats.get(company);
        if (s != null) {
            log.info("Stats for {} -> count={}, avgSalary={}, topEarner={}",
                    company, s.getEmployeeCount(), String.format("%.2f", s.getAverageSalary()), s.getTopEarnerFullName());
        } else {
            log.info("No statistics found for '{}'", company);
        }

        // 5) Validate salary consistency
        var underpaid = employeeService.validateSalaryConsistency();
        if (underpaid.isEmpty()) {
            log.info("Salary consistency check: no anomalies.");
        } else {
            log.warn("Salary consistency check: {} employee(s) below base salary:", underpaid.size());
            underpaid.forEach(e ->
                    log.warn(" - {} ({}, position={}, salary={})",
                            e.getFullName(), e.getEmail(), e.getPosition(), e.getSalary()));
        }

        log.info("=== Employee Management demo complete ===");
    }

    private static String resolveResourceToPath(Resource resource) throws IOException {
        try {
            // Works when running from IDE or unpacked classes
            return resource.getFile().getAbsolutePath();
        } catch (IOException | IllegalStateException notFileBased) {
            // Packaged in JAR: stream it out to a temp file
            Path tmp = Files.createTempFile("employees-", ".csv");
            try (var in = resource.getInputStream()) {
                Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return tmp.toAbsolutePath().toString();
        }
    }
}
