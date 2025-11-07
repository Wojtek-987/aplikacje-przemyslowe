package com.example.service;

import com.example.model.Employee;
import com.example.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class EmployeeServiceTest {

    @Test
    @DisplayName("addEmployee: rejects null employee and duplicate email")
    void addEmployee_null_and_duplicate() {
        EmployeeService svc = new EmployeeService();

        // Null employee
        IllegalArgumentException ex1 =
                assertThrows(IllegalArgumentException.class, () -> svc.addEmployee(null));
        assertTrue(ex1.getMessage().toLowerCase().contains("null"));

        // Valid insert
        Employee e1 = new Employee("Ada Lovelace", "ada@ex.com", "Analytical Engines",
                Position.PROGRAMISTA, Position.PROGRAMISTA.getBaseSalary());
        assertTrue(svc.addEmployee(e1));

        // Duplicate email should fail
        Employee dup = new Employee("Ada L.", "ada@ex.com", "Elsewhere",
                Position.MANAGER, Position.MANAGER.getBaseSalary());
        IllegalArgumentException ex2 =
                assertThrows(IllegalArgumentException.class, () -> svc.addEmployee(dup));
        assertTrue(ex2.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("findByCompany: non-existent company yields empty list")
    void findByCompany_nonexistent() {
        EmployeeService svc = new EmployeeService();
        svc.addEmployee(new Employee("Grace Hopper", "grace@ex.com", "Navy",
                Position.PROGRAMISTA, 10_000));

        List<Employee> out = svc.findByCompany("NoSuchCorp");
        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    @Test
    @DisplayName("averageSalary: empty directory yields OptionalDouble.empty")
    void averageSalary_empty() {
        EmployeeService svc = new EmployeeService();
        OptionalDouble avg = svc.averageSalary();
        assertTrue(avg.isEmpty());
    }

    @Test
    @DisplayName("highestPaid: empty directory yields Optional.empty (type significance)")
    void highestPaid_empty() {
        EmployeeService svc = new EmployeeService();
        Optional<Employee> max = svc.highestPaid();
        assertTrue(max.isEmpty(), "No employees → Optional.empty signifies 'no value'");
    }

    @Test
    @DisplayName("validateSalaryConsistency: flags employees paid below base for their Position")
    void validateSalaryConsistency_flagsBelowBase() {
        EmployeeService svc = new EmployeeService();

        // One correct, one underpaid
        Employee ok = new Employee("Alan Turing", "alan@ex.com", "Bletchley",
                Position.PROGRAMISTA, Position.PROGRAMISTA.getBaseSalary());
        Employee bad = new Employee("Joan Clarke", "joan@ex.com", "Bletchley",
                Position.PROGRAMISTA, Position.PROGRAMISTA.getBaseSalary() - 1);

        svc.addEmployee(ok);
        svc.addEmployee(bad);

        List<Employee> inconsistent = svc.validateSalaryConsistency();
        assertEquals(1, inconsistent.size());
        assertEquals("joan@ex.com", inconsistent.get(0).getEmail());
    }

    @Test
    @DisplayName("addAll: null-safe and bulk insert preserves insertion order in getAllEmployees()")
    void addAll_nullSafe_and_order() {
        EmployeeService svc = new EmployeeService();

        // null-safe
        assertDoesNotThrow(() -> svc.addAll(null));
        assertTrue(svc.getAllEmployees().isEmpty());

        // bulk add
        List<Employee> batch = List.of(
                new Employee("Alan Abel", "a@x", "Corp", Position.PROGRAMISTA, 100),
                new Employee("Bea Abel", "b@x", "Corp", Position.PROGRAMISTA, 200),
                new Employee("Ada Lovelace", "c@x", "Corp", Position.MANAGER, 300)
        );
        svc.addAll(batch);

        List<Employee> all = svc.getAllEmployees();
        assertEquals(3, all.size());
        assertEquals("a@x", all.get(0).getEmail());
        assertEquals("b@x", all.get(1).getEmail());
        assertEquals("c@x", all.get(2).getEmail());
    }

    @Test
    @DisplayName("findByCompany: case-insensitive match; null company employees are excluded")
    void findByCompany_caseInsensitive_and_nullExcluded() {
        EmployeeService svc = new EmployeeService();
        svc.addEmployee(new Employee("X", "x@x", "ACME", Position.PROGRAMISTA, 1_000));
        svc.addEmployee(new Employee("Y", "y@y", "acme", Position.PROGRAMISTA, 2_000));
        svc.addEmployee(new Employee("Z", "z@z", null, Position.PROGRAMISTA, 3_000)); // null company

        List<Employee> acme = svc.findByCompany("AcMe");
        assertEquals(2, acme.size());
        assertTrue(acme.stream().noneMatch(e -> e.getEmail().equals("z@z")));
    }

    @Test
    @DisplayName("getEmployeesSortedBySurname: sorts by last token of name; ties resolved by full name")
    void getEmployeesSortedBySurname_ordering_and_tiebreaker() {
        EmployeeService svc = new EmployeeService();
        svc.addEmployee(new Employee("Bea Abel", "b@x", "Corp", Position.PROGRAMISTA, 100));
        svc.addEmployee(new Employee("Alan Abel", "a@x", "Corp", Position.PROGRAMISTA, 100));
        svc.addEmployee(new Employee("Ada Lovelace", "c@x", "Corp", Position.PROGRAMISTA, 100));

        List<Employee> sorted = svc.getEmployeesSortedBySurname();
        // Expect: Alan Abel, Bea Abel, Ada Lovelace
        assertEquals(List.of("a@x", "b@x", "c@x"),
                sorted.stream().map(Employee::getEmail).toList());
    }

    @Test
    @DisplayName("groupByPosition and countByPosition: correct buckets and tallies")
    void grouping_and_counting_by_position() {
        EmployeeService svc = new EmployeeService();
        svc.addEmployee(new Employee("A", "a@x", "Corp", Position.PROGRAMISTA, 100));
        svc.addEmployee(new Employee("B", "b@x", "Corp", Position.PROGRAMISTA, 200));
        svc.addEmployee(new Employee("C", "c@x", "Corp", Position.MANAGER, 300));

        var grouped = svc.groupByPosition();
        var counted = svc.countByPosition();

        assertEquals(2, grouped.get(Position.PROGRAMISTA).size());
        assertEquals(1, grouped.get(Position.MANAGER).size());
        assertEquals(2L, counted.get(Position.PROGRAMISTA));
        assertEquals(1L, counted.get(Position.MANAGER));
    }

    @Test
    @DisplayName("averageSalary: non-empty directory returns correct average")
    void averageSalary_nonEmpty() {
        EmployeeService svc = new EmployeeService();
        svc.addEmployee(new Employee("A", "a@x", "Corp", Position.PROGRAMISTA, 100));
        svc.addEmployee(new Employee("B", "b@x", "Corp", Position.PROGRAMISTA, 300));
        var avg = svc.averageSalary();
        assertTrue(avg.isPresent());
        assertEquals(200.0, avg.getAsDouble(), 1e-9);
    }

    @Test
    @DisplayName("highestPaid: returns the employee with maximum salary")
    void highestPaid_nonEmpty() {
        EmployeeService svc = new EmployeeService();
        svc.addEmployee(new Employee("A", "a@x", "Corp", Position.PROGRAMISTA, 100));
        svc.addEmployee(new Employee("B", "b@x", "Corp", Position.PROGRAMISTA, 500));
        svc.addEmployee(new Employee("C", "c@x", "Corp", Position.PROGRAMISTA, 400));

        var max = svc.highestPaid();
        assertTrue(max.isPresent());
        assertEquals("b@x", max.get().getEmail());
    }

    @Test
    @DisplayName("validateSalaryConsistency: equal-to-base is OK; below base is flagged")
    void validateSalaryConsistency_equalVsBelowBase() {
        EmployeeService svc = new EmployeeService();
        double base = Position.PROGRAMISTA.getBaseSalary();
        svc.addEmployee(new Employee("Base", "base@x", "Corp", Position.PROGRAMISTA, base));       // OK
        svc.addEmployee(new Employee("Low", "low@x", "Corp", Position.PROGRAMISTA, base - 0.01)); // BAD
        var bad = svc.validateSalaryConsistency();
        assertEquals(1, bad.size());
        assertEquals("low@x", bad.get(0).getEmail());
    }

    @Test
    @DisplayName("getCompanyStatistics: normalises company names and computes count/avg/top correctly")
    void companyStatistics_normalisation_and_values() {
        EmployeeService svc = new EmployeeService();
        // Normal "ACME"
        svc.addEmployee(new Employee("Top Earner", "t@x", " ACME ", Position.MANAGER, 1_000));
        svc.addEmployee(new Employee("Mid Earner", "m@x", "ACME", Position.PROGRAMISTA, 500));
        // Unknown company: null and blank collapse to "(unknown)"
        svc.addEmployee(new Employee("Anon1", "u1@x", null, Position.PROGRAMISTA, 10));
        svc.addEmployee(new Employee("Anon2", "u2@x", "   ", Position.PROGRAMISTA, 20));

        var stats = svc.getCompanyStatistics();

        assertTrue(stats.containsKey("ACME"));
        assertTrue(stats.containsKey("(unknown)"));

        var acme = stats.get("ACME");
        assertEquals(2, acme.getEmployeeCount());
        assertEquals(750.0, acme.getAverageSalary(), 1e-9);
        assertEquals("Top Earner", acme.getTopEarnerFullName());

        var unk = stats.get("(unknown)");
        assertEquals(2, unk.getEmployeeCount());
        assertEquals(15.0, unk.getAverageSalary(), 1e-9);
        assertEquals("Anon2", unk.getTopEarnerFullName());
    }

}
