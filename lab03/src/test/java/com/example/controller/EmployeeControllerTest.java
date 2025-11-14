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
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Web slice tests for EmployeeController using MockMvc.
 */
@WebMvcTest(controllers = EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EmployeeService employeeService;

    // ---------- Helpers ----------

    private static Employee emp(String fullName, String email, String company, Position pos, double sal, EmploymentStatus st) {
        Employee e = new Employee(fullName, email, company, pos, sal);
        e.setStatus(st);
        return e;
    }

    // ---------- Tests ----------

    @Test
    @DisplayName("GET /api/employees - returns 200 and list of employees")
    void getAllEmployees_ok() throws Exception {
        var e1 = emp("Ada Lovelace", "ada@ex.com", "ACME", Position.PROGRAMISTA, 12000, EmploymentStatus.ACTIVE);
        var e2 = emp("Grace Hopper", "grace@ex.com", "Navy", Position.MANAGER, 20000, EmploymentStatus.ON_LEAVE);

        Mockito.when(employeeService.getAllEmployees()).thenReturn(List.of(e1, e2));

        mvc.perform(get("/api/employees").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email").value("ada@ex.com"))
                .andExpect(jsonPath("$[0].firstName").value("Ada"))
                .andExpect(jsonPath("$[0].lastName").value("Lovelace"))
                .andExpect(jsonPath("$[0].company").value("ACME"))
                .andExpect(jsonPath("$[0].position").value("PROGRAMISTA"))
                .andExpect(jsonPath("$[0].salary").value(12000.0))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].email").value("grace@ex.com"))
                .andExpect(jsonPath("$[1].status").value("ON_LEAVE"));
    }

    @Test
    @DisplayName("GET /api/employees?company=X - filters by company")
    void getEmployees_filterByCompany() throws Exception {
        var e = emp("Jane Doe", "jane@corp.com", "TechCorp", Position.PROGRAMISTA, 9000, EmploymentStatus.ACTIVE);
        Mockito.when(employeeService.findByCompany(eq("TechCorp"))).thenReturn(List.of(e));

        mvc.perform(get("/api/employees").param("company", "TechCorp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("jane@corp.com"))
                .andExpect(jsonPath("$[0].company").value("TechCorp"));
    }

    @Test
    @DisplayName("GET /api/employees/{email} - returns employee when found")
    void getEmployee_byEmail_found() throws Exception {
        var e = emp("Alan Turing", "alan@ex.com", "Bletchley", Position.PROGRAMISTA, 15000, EmploymentStatus.ACTIVE);
        Mockito.when(employeeService.findByEmail("alan@ex.com")).thenReturn(Optional.of(e));

        mvc.perform(get("/api/employees/{email}", "alan@ex.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alan@ex.com"))
                .andExpect(jsonPath("$.firstName").value("Alan"))
                .andExpect(jsonPath("$.lastName").value("Turing"))
                .andExpect(jsonPath("$.position").value("PROGRAMISTA"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/employees/{email} - returns 404 when not found")
    void getEmployee_byEmail_notFound() throws Exception {
        Mockito.when(employeeService.findByEmail("missing@ex.com")).thenReturn(Optional.empty());

        mvc.perform(get("/api/employees/{email}", "missing@ex.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/employees - creates employee; 201 + Location header")
    void createEmployee_created() throws Exception {
        // Service addEmployee returns true for success
        Mockito.when(employeeService.addEmployee(org.mockito.ArgumentMatchers.any(Employee.class))).thenReturn(true);

        String body = """
            {
              "firstName":"New",
              "lastName":"User",
              "email":"new@corp.com",
              "company":"Corp",
              "position":"PROGRAMISTA",
              "salary":10000,
              "status":"ACTIVE"
            }
            """;

        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                // Location uses URL-encoded email
                .andExpect(header().string("Location", "/api/employees/new%40corp.com"))
                .andExpect(jsonPath("$.email").value("new@corp.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.position").value("PROGRAMISTA"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/employees - duplicate email yields 409 Conflict")
    void createEmployee_duplicate_conflict() throws Exception {
        Mockito.when(employeeService.addEmployee(org.mockito.ArgumentMatchers.any(Employee.class)))
                .thenThrow(new IllegalArgumentException("Employee with email already exists"));

        String body = """
            {
              "firstName":"Dup",
              "lastName":"User",
              "email":"dup@corp.com",
              "company":"Corp",
              "position":"PROGRAMISTA",
              "salary":10000,
              "status":"ACTIVE"
            }
            """;

        mvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE /api/employees/{email} - returns 204 when removed")
    void deleteEmployee_noContent() throws Exception {
        Mockito.when(employeeService.removeByEmail("gone@corp.com")).thenReturn(true);

        mvc.perform(delete("/api/employees/{email}", "gone@corp.com"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("PATCH /api/employees/{email}/status - updates only status")
    void patchEmployee_status() throws Exception {
        var e = emp("Jane Doe", "jane@corp.com", "TechCorp",
                Position.PROGRAMISTA, 9000, EmploymentStatus.ACTIVE);
        Mockito.when(employeeService.findByEmail("jane@corp.com")).thenReturn(Optional.of(e));

        String body = """
            {"status": "ON_LEAVE"}
            """;

        mvc.perform(patch("/api/employees/{email}/status", "jane@corp.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@corp.com"))
                .andExpect(jsonPath("$.status").value("ON_LEAVE"));
    }
}
