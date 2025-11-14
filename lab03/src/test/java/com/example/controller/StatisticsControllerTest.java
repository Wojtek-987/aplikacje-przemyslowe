package com.example.controller;

import com.example.EmployeeManagementApplication;
import com.example.config.AppConfig;
import com.example.model.Employee;
import com.example.model.EmploymentStatus;
import com.example.model.Position;
import com.example.service.EmployeeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web slice tests for StatisticsController using MockMvc.
 */
@WebMvcTest(controllers = StatisticsController.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EmployeeService employeeService;

    // ---------- Helpers ----------

    private static Employee emp(String fullName, String email, String company,
                                Position pos, double salary, EmploymentStatus st) {
        Employee e = new Employee(fullName, email, company, pos, salary);
        e.setStatus(st);
        return e;
    }

    // ---------- Tests ----------

    @Test
    @DisplayName("GET /api/statistics/salary/average - overall average from service")
    void averageSalary_overall() throws Exception {
        when(employeeService.averageSalary()).thenReturn(OptionalDouble.of(15000.0));

        mvc.perform(get("/api/statistics/salary/average").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageSalary").value(15000.0));
    }

    @Test
    @DisplayName("GET /api/statistics/salary/average?company=X - average for a company")
    void averageSalary_byCompany() throws Exception {
        var e1 = emp("A", "a@x", "TechCorp", Position.PROGRAMISTA, 10000, EmploymentStatus.ACTIVE);
        var e2 = emp("B", "b@x", "TechCorp", Position.MANAGER, 20000, EmploymentStatus.ACTIVE);
        when(employeeService.findByCompany("TechCorp")).thenReturn(List.of(e1, e2));

        mvc.perform(get("/api/statistics/salary/average").param("company", "TechCorp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageSalary").value(15000.0));
    }

    @Test
    @DisplayName("GET /api/statistics/company/{company} - detailed stats for existing company")
    void companyStats_existing() throws Exception {
        var e1 = emp("Jane Doe", "j@x", "ACME", Position.PROGRAMISTA, 9000, EmploymentStatus.ACTIVE);
        var e2 = emp("John Top", "t@x", "ACME", Position.MANAGER, 19000, EmploymentStatus.ON_LEAVE);
        when(employeeService.findByCompany("ACME")).thenReturn(List.of(e1, e2));

        mvc.perform(get("/api/statistics/company/{company}", "ACME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("ACME"))
                .andExpect(jsonPath("$.employeeCount").value(2))
                .andExpect(jsonPath("$.averageSalary").value(14000.0))
                .andExpect(jsonPath("$.highestSalary").value(19000.0))
                .andExpect(jsonPath("$.topEarnerName").value("John Top"));
    }

    @Test
    @DisplayName("GET /api/statistics/company/{company} - zeroed stats when no employees")
    void companyStats_empty() throws Exception {
        when(employeeService.findByCompany(anyString())).thenReturn(List.of());

        mvc.perform(get("/api/statistics/company/{company}", "NopeCorp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("NopeCorp"))
                .andExpect(jsonPath("$.employeeCount").value(0))
                .andExpect(jsonPath("$.averageSalary").value(0.0))
                .andExpect(jsonPath("$.highestSalary").value(0.0))
                .andExpect(jsonPath("$.topEarnerName").value(""));
    }

    @Test
    @DisplayName("GET /api/statistics/positions - counts per Position")
    void positions_distribution() throws Exception {
        when(employeeService.countByPosition()).thenReturn(Map.of(
                Position.PROGRAMISTA, 2L,
                Position.MANAGER, 1L
        ));

        mvc.perform(get("/api/statistics/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.PROGRAMISTA").value(2))
                .andExpect(jsonPath("$.MANAGER").value(1));
    }

    @Test
    @DisplayName("GET /api/statistics/status - counts per EmploymentStatus")
    void status_distribution() throws Exception {
        when(employeeService.countByStatus()).thenReturn(Map.of(
                EmploymentStatus.ACTIVE, 3L,
                EmploymentStatus.ON_LEAVE, 1L,
                EmploymentStatus.TERMINATED, 0L
        ));

        mvc.perform(get("/api/statistics/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ACTIVE").value(3))
                .andExpect(jsonPath("$.ON_LEAVE").value(1))
                .andExpect(jsonPath("$.TERMINATED").value(0));
    }
}
