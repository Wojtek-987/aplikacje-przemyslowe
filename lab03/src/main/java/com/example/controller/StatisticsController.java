package com.example.controller;

import com.example.dto.CompanyStatisticsDTO;
import com.example.model.Employee;
import com.example.model.EmploymentStatus;
import com.example.model.Position;
import com.example.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final EmployeeService employees;

    public StatisticsController(EmployeeService employees) {
        this.employees = employees;
    }

    // --- 1) Average salary (optionally by company) ---------------------------------------------

    /**
     * GET /api/statistics/salary/average
     * Optional ?company=X -> average for that company; otherwise overall average.
     * Returns: { "averageSalary": <double> }
     */
    @GetMapping("/salary/average")
    public ResponseEntity<Map<String, Double>> averageSalary(
            @RequestParam(name = "company", required = false) String company
    ) {
        double avg;
        if (StringUtils.hasText(company)) {
            List<Employee> list = employees.findByCompany(company);
            avg = list.stream().mapToDouble(Employee::getSalary).average().orElse(0.0);
        } else {
            avg = employees.averageSalary().orElse(0.0);
        }
        return ResponseEntity.ok(Map.of("averageSalary", avg));
    }

    // --- 2) Detailed company statistics ---------------------------------------------------------

    /**
     * GET /api/statistics/company/{companyName}
     * Returns a CompanyStatisticsDTO built from EmployeeService analytics.
     */
    @GetMapping("/company/{companyName}")
    public ResponseEntity<CompanyStatisticsDTO> companyStats(@PathVariable String companyName) {
        // Pull the employees for that company (service already does case-insensitive match)
        List<Employee> list = employees.findByCompany(companyName);
        if (list.isEmpty()) {
            // 200 with zeros, or 404? The brief doesn’t mandate; returning 200 with zeros is friendlier.
            CompanyStatisticsDTO dto = new CompanyStatisticsDTO(companyName, 0, 0.0, 0.0, "");
            return ResponseEntity.ok(dto);
        }

        long count = list.size();
        double avg = list.stream().mapToDouble(Employee::getSalary).average().orElse(0.0);
        Employee top = list.stream().max(Comparator.comparingDouble(Employee::getSalary)).orElse(null);
        double highest = (top != null) ? top.getSalary() : 0.0;
        String topName = (top != null) ? top.getFullName() : "";

        CompanyStatisticsDTO dto = new CompanyStatisticsDTO(companyName, count, avg, highest, topName);
        return ResponseEntity.ok(dto);
    }

    // --- 3) Count by positions -----------------------------------------------------------------

    /**
     * GET /api/statistics/positions
     * Returns counts per Position as Map<String, Integer>.
     */
    @GetMapping("/positions")
    public ResponseEntity<Map<String, Integer>> countByPositions() {
        Map<Position, Long> counts = employees.countByPosition();
        Map<String, Integer> out = counts.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        e -> Math.toIntExact(e.getValue())
                ));
        return ResponseEntity.ok(out);
    }

    // --- 4) Distribution by employment status ---------------------------------------------------
    // This will be replaced in Exercise 6 when EmploymentStatus is introduced on the model.

    /**
     * GET /api/statistics/status
     * For now (pre-Exercise 6), everyone is treated as ACTIVE.
     * Returns: { "ACTIVE": N }
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Integer>> statusDistribution() {
        Map<EmploymentStatus, Long> counts = employees.countByStatus();
        Map<String, Integer> out = counts.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), e -> Math.toIntExact(e.getValue())));
        return ResponseEntity.ok(out);
    }
}
