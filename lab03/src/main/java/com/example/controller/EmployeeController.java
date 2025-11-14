package com.example.controller;

import com.example.model.Employee;
import com.example.model.Position;
import com.example.service.EmployeeService;
import com.example.dto.EmployeeDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import com.example.dto.StatusUpdateRequest;
import com.example.model.EmploymentStatus;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employees;

    public EmployeeController(EmployeeService employees) {
        this.employees = employees;
    }

    /**
     * GET /api/employees
     * Optional ?company=X filter; if absent, returns all.
     */
    @GetMapping
    public ResponseEntity<List<EmployeeDTO>> getAll(@RequestParam(name = "company", required = false) String company) {
        List<Employee> src = (company == null || company.isBlank())
                ? employees.getAllEmployees()
                : employees.findByCompany(company);

        List<EmployeeDTO> out = src.stream().map(this::toDto).toList();
        return ResponseEntity.ok(out);
    }

    /**
     * GET /api/employees/{email}
     */
    @GetMapping("/{email}")
    public ResponseEntity<EmployeeDTO> getByEmail(@PathVariable String email) {
        Optional<Employee> found = employees.findByEmail(email);
        return found
                .map(e -> ResponseEntity.ok(toDto(e)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * POST /api/employees
     * Creates a new employee from EmployeeDTO; returns 201 + Location header.
     */
    @PostMapping
    public ResponseEntity<EmployeeDTO> create(@RequestBody EmployeeDTO dto) {
        // Minimal validation consistent with your domain (email is required by Employee constructor)
        if (!StringUtils.hasText(dto.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Employee entity = fromDto(dto);
        try {
            employees.addEmployee(entity);
        } catch (IllegalArgumentException dup) {
            // Duplicate email (your service throws on existing email)
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        String emailEnc = URLEncoder.encode(entity.getEmail(), StandardCharsets.UTF_8);
        URI location = URI.create("/api/employees/" + emailEnc);
        return ResponseEntity.created(location).body(toDto(entity));
    }

    /**
     * PUT /api/employees/{email}
     * Updates mutable fields; email in path is the identity key and is not changed.
     */
    @PutMapping("/{email}")
    public ResponseEntity<EmployeeDTO> update(@PathVariable String email, @RequestBody EmployeeDTO dto) {
        Optional<Employee> maybe = employees.findByEmail(email);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Employee e = maybe.get();

        // Update fields (email is immutable key here)
        String fullName = buildFullName(dto.getFirstName(), dto.getLastName());
        if (StringUtils.hasText(fullName)) {
            e.setFullName(fullName);
        }
        if (StringUtils.hasText(dto.getCompany())) {
            e.setCompanyName(dto.getCompany());
        }
        if (dto.getPosition() != null) {
            e.setPosition(dto.getPosition());
        }
        if (dto.getSalary() > 0) {
            e.setSalary(dto.getSalary());
        }

        return ResponseEntity.ok(toDto(e));
    }

    /**
     * DELETE /api/employees/{email}
     */
    @DeleteMapping("/{email}")
    public ResponseEntity<Void> delete(@PathVariable String email) {
        boolean removed = employees.removeByEmail(email);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * PATCH /api/employees/{email}/status
     * Body: { "status": "ACTIVE|ON_LEAVE|TERMINATED" }
     */
    @PatchMapping("/{email}/status")
    public ResponseEntity<EmployeeDTO> updateStatus(@PathVariable String email,
                                                    @RequestBody StatusUpdateRequest body) {
        Optional<Employee> maybe = employees.findByEmail(email);
        if (maybe.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (body == null || body.getStatus() == null || body.getStatus().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        EmploymentStatus newStatus;
        try {
            newStatus = EmploymentStatus.valueOf(body.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().build();
        }
        Employee e = maybe.get();
        e.setStatus(newStatus);
        return ResponseEntity.ok(toDto(e));
    }

    /**
     * GET /api/employees/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<EmployeeDTO>> getByStatus(@PathVariable String status) {
        EmploymentStatus st;
        try {
            st = EmploymentStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().build();
        }
        List<EmployeeDTO> out = employees.findByStatus(st).stream().map(this::toDto).toList();
        return ResponseEntity.ok(out);
    }


    // ---------- Mapping helpers ----------

    // imports already include EmploymentStatus

    private EmployeeDTO toDto(Employee e) {
        String[] split = (e.getFullName() == null ? "" : e.getFullName().trim()).split("\\s+", -1);
        String first = split.length > 0 ? split[0] : "";
        String last  = split.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(split, 1, split.length)) : "";

        EmployeeDTO dto = new EmployeeDTO();
        dto.setFirstName(first);
        dto.setLastName(last);
        dto.setEmail(e.getEmail());
        dto.setCompany(e.getCompanyName());
        dto.setPosition(e.getPosition());
        dto.setSalary(e.getSalary());
        dto.setStatus(e.getStatus() == null ? "ACTIVE" : e.getStatus().name());
        return dto;
    }

    private Employee fromDto(EmployeeDTO dto) {
        Position pos = dto.getPosition() != null ? dto.getPosition() : Position.PROGRAMISTA;
        double salary = dto.getSalary() > 0 ? dto.getSalary() : pos.getBaseSalary();
        String fullName = buildFullName(dto.getFirstName(), dto.getLastName());

        Employee e = new Employee(fullName, dto.getEmail(), dto.getCompany(), pos, salary);
        // set status after construction
        e.setStatus(parseStatusOrDefault(dto.getStatus()));
        return e;
    }


    private static EmploymentStatus parseStatusOrDefault(String s) {
        if (s == null || s.isBlank()) return EmploymentStatus.ACTIVE;
        try {
            return EmploymentStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return EmploymentStatus.ACTIVE;
        }
    }

    private static String buildFullName(String first, String last) {
        String f = (first == null ? "" : first.trim());
        String l = (last  == null ? "" : last.trim());
        return (f + " " + l).trim();
    }
}
