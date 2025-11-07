package com.example.service;

import com.example.model.Employee;
import com.example.model.ImportSummary;
import com.example.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportServiceTest {

    private static Path writeCsv(Path dir, String filename, String csv) throws IOException {
        Path p = dir.resolve(filename);
        Files.writeString(p, csv, StandardCharsets.UTF_8);
        return p;
    }

    @Test
    @DisplayName("Happy path: valid rows are imported into the directory")
    void import_ok(@TempDir Path tmp) throws Exception {
        String csv = String.join("\n",
                "firstName,lastName,email,company,position,salary",
                "Ada,Lovelace,ada@ex.com,Analytical Engines,PROGRAMISTA,12000",
                "Grace,Hopper,grace@ex.com,Navy,MANAGER,20000"
        );
        Path file = writeCsv(tmp, "ok.csv", csv);

        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary summary = importer.importFromCsv(file.toString());

        assertEquals(2, summary.getImportedCount());
        assertTrue(summary.getErrors().isEmpty(), "No errors expected");
        List<Employee> all = directory.getAllEmployees();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(e -> e.getEmail().equals("ada@ex.com")
                && e.getPosition() == Position.PROGRAMISTA && e.getSalary() == 12000));
        assertTrue(all.stream().anyMatch(e -> e.getEmail().equals("grace@ex.com")
                && e.getPosition() == Position.MANAGER && e.getSalary() == 20000));
    }

    @Test
    @DisplayName("Invalid position: importer records an error but continues with other rows")
    void import_invalidPosition_continues(@TempDir Path tmp) throws Exception {
        String csv = String.join("\n",
                "firstName,lastName,email,company,position,salary",
                "Ada,Lovelace,ada@ex.com,Analytical Engines,PROGRAMISTA,12000",
                "Eve,Polastri,eve@ex.com,MI5,NOT_A_REAL_POSITION,9000",   // invalid enum
                "Villanelle,Unknown,v@ex.com,Freelance,MANAGER,21000"
        );
        Path file = writeCsv(tmp, "bad-pos.csv", csv);

        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary summary = importer.importFromCsv(file.toString());

        // Two valid rows imported, one error captured
        assertEquals(2, summary.getImportedCount());
        assertEquals(1, summary.getErrors().size());
        // Message comes from IllegalArgumentException thrown by Enum.valueOf(...)
        assertTrue(summary.getErrors().get(0).contains("No enum constant"),
                "Should contain enum resolution failure message");
        assertEquals(2, directory.getAllEmployees().size());
    }

    @Test
    @DisplayName("Negative or zero salary: recorded as error; importer continues")
    void import_negativeSalary(@TempDir Path tmp) throws Exception {
        String csv = String.join("\n",
                "firstName,lastName,email,company,position,salary",
                "Nina,Sharp,nina@ex.com,Massive Dynamic,PROGRAMISTA,-1",
                "Peter,Bishop,peter@ex.com,Massive Dynamic,PROGRAMISTA,0",
                "Olivia,Dunham,olivia@ex.com,Massive Dynamic,PROGRAMISTA,10000"
        );
        Path file = writeCsv(tmp, "neg-sal.csv", csv);

        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary summary = importer.importFromCsv(file.toString());

        // Only the last row is valid
        assertEquals(1, summary.getImportedCount());
        assertEquals(2, summary.getErrors().size());
        assertTrue(summary.getErrors().stream().allMatch(m -> m.contains("Salary must be positive")),
                "Each error should explain salary positivity requirement");
        assertEquals(1, directory.getAllEmployees().size());
        assertEquals("olivia@ex.com", directory.getAllEmployees().get(0).getEmail());
    }

    @Test
    @DisplayName("Import summary: contains imported count and per-line errors with line numbers")
    void import_summaryShape(@TempDir Path tmp) throws Exception {
        String csv = String.join("\n",
                "firstName,lastName,email,company,position,salary",
                "A,A,a@ex.com,CORP,PROGRAMISTA,10000",
                "B,B,b@ex.com,CORP,NOPE,10000",       // invalid position
                "C,C,c@ex.com,CORP,PROGRAMISTA,-5"    // invalid salary
        );
        Path file = writeCsv(tmp, "summary.csv", csv);

        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary summary = importer.importFromCsv(file.toString());

        assertEquals(1, summary.getImportedCount());
        // Two errors, one for each invalid line; exercise requires line numbers in messages
        assertEquals(2, summary.getErrors().size());
        assertTrue(summary.getErrors().get(0).matches("Line \\d+: .*"));
        assertTrue(summary.getErrors().get(1).matches("Line \\d+: .*"));
    }

    @Test
    @DisplayName("Malformed row (wrong column count): recorded as error with line number; other rows still import")
    void import_malformedRow_wrongColumnCount(@TempDir Path tmp) throws Exception {
        String csv = String.join("\n",
                "firstName,lastName,email,company,position,salary",
                "Ada,Lovelace,ada@ex.com,ACME,PROGRAMISTA,12000",
                "BadRow,OnlyFive,cols@ex.com,ACME,PROGRAMISTA",                 // 5 columns → error
                "Grace,Hopper,grace@ex.com,ACME,MANAGER,20000"
        );
        Path file = writeCsv(tmp, "malformed.csv", csv);

        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary sum = importer.importFromCsv(file.toString());
        assertEquals(2, sum.getImportedCount());
        assertEquals(1, sum.getErrors().size());
        assertTrue(sum.getErrors().get(0).matches("Line \\d+: Expected 6 columns, got 5"));
        assertEquals(2, directory.getAllEmployees().size());
    }

    @Test
    @DisplayName("Non-numeric salary: recorded as error; other rows import")
    void import_nonNumericSalary(@TempDir Path tmp) throws Exception {
        String csv = String.join("\n",
                "firstName,lastName,email,company,position,salary",
                "A,A,a@ex.com,ACME,PROGRAMISTA,abc",           // non-numeric
                "B,B,b@ex.com,ACME,PROGRAMISTA,1000"
        );
        Path file = writeCsv(tmp, "nonnumeric.csv", csv);

        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary sum = importer.importFromCsv(file.toString());
        assertEquals(1, sum.getImportedCount());
        assertEquals(1, sum.getErrors().size());
        // Message originates from Double.parseDouble(...)
        assertTrue(sum.getErrors().get(0).contains("For input string"));
        assertEquals(1, directory.getAllEmployees().size());
        assertEquals("b@ex.com", directory.getAllEmployees().get(0).getEmail());
    }

    @Test
    @DisplayName("File not found or unreadable: ImportSummary contains a single I/O error")
    void import_fileIoError() {
        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary sum = importer.importFromCsv("definitely/does/not/exist.csv");
        assertEquals(0, sum.getImportedCount());
        assertEquals(1, sum.getErrors().size());
        assertTrue(sum.getErrors().get(0).startsWith("I/O or CSV parse error:"));
    }

    @Test
    @DisplayName("Duplicate email within CSV: second row rejected; error captured; no partial overwrite")
    void import_duplicateEmail(@TempDir Path tmp) throws Exception {
        String csv = String.join("\n",
                "firstName,lastName,email,company,position,salary",
                "Ada,Lovelace,dup@ex.com,ACME,PROGRAMISTA,12000",
                "Someone,Else,dup@ex.com,ACME,PROGRAMISTA,13000" // duplicate email
        );
        Path file = writeCsv(tmp, "dup.csv", csv);

        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary sum = importer.importFromCsv(file.toString());
        assertEquals(1, sum.getImportedCount());
        assertEquals(1, sum.getErrors().size());
        assertTrue(sum.getErrors().get(0).contains("already exists"));
        assertEquals(1, directory.getAllEmployees().size());
        assertEquals(12000, directory.getAllEmployees().get(0).getSalary());
    }

    @Test
    @DisplayName("Header only: zero data rows yields zero imports and no errors")
    void import_headerOnly(@TempDir Path tmp) throws Exception {
        String csv = "firstName,lastName,email,company,position,salary\n";
        Path file = writeCsv(tmp, "empty.csv", csv);

        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary sum = importer.importFromCsv(file.toString());
        assertEquals(0, sum.getImportedCount());
        assertTrue(sum.getErrors().isEmpty());
    }

    @Test
    @DisplayName("Trimming and case-insensitivity: surrounding spaces and mixed case position are accepted")
    void import_trimming_and_case(@TempDir Path tmp) throws Exception {
        String csv = String.join("\n",
                "firstName,lastName,email,company,position,salary",
                "  Ada  ,  Lovelace  , ada@ex.com ,  ACME  ,  programista  ,  10000  "
        );
        Path file = writeCsv(tmp, "trim.csv", csv);

        EmployeeService directory = new EmployeeService();
        ImportService importer = new ImportService(directory);

        ImportSummary sum = importer.importFromCsv(file.toString());
        assertEquals(1, sum.getImportedCount());
        assertTrue(sum.getErrors().isEmpty());
        Employee e = directory.getAllEmployees().get(0);
        assertEquals("Ada Lovelace", e.getFullName());
        assertEquals("ada@ex.com", e.getEmail());
        assertEquals("ACME", e.getCompanyName());
        assertEquals(Position.PROGRAMISTA, e.getPosition());
        assertEquals(10_000, e.getSalary());
    }

    // --- Reflection helper to reach private static parseEmployee(String) ---
    private static Employee invokeParseEmployee(String csvLine) {
        try {
            var m = ImportService.class.getDeclaredMethod("parseEmployee", String.class);
            m.setAccessible(true);
            return (Employee) m.invoke(null, csvLine);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            // Unwrap checked exception thrown by the method under test
            if (ite.getTargetException() instanceof RuntimeException re) throw re;
            if (ite.getTargetException() instanceof com.example.exception.InvalidDataException ide)
                throw new RuntimeException(ide);
            throw new RuntimeException(ite);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("parseEmployee: valid CSV → trimmed full name, enum parsed case-insensitively, positive salary")
    void parseEmployee_valid() {
        Employee e = invokeParseEmployee("  Ada  ,  Lovelace  , ada@ex.com , ACME , programista ,  12000  ");
        assertEquals("Ada Lovelace", e.getFullName());
        assertEquals("ada@ex.com", e.getEmail());
        assertEquals("ACME", e.getCompanyName());
        assertEquals(Position.PROGRAMISTA, e.getPosition());
        assertEquals(12000.0, e.getSalary());
    }

    @Test
    @DisplayName("parseEmployee: wrong column count → InvalidDataException with explicit count")
    void parseEmployee_wrongColumnCount() {
        // 5 columns
        var ex1 = assertThrows(RuntimeException.class,
                () -> invokeParseEmployee("A,B,a@x,Corp,PROGRAMISTA"));
        assertTrue(ex1.getCause() instanceof com.example.exception.InvalidDataException);
        assertTrue(ex1.getCause().getMessage().contains("Expected 6 columns, got 5"));

        // 7 columns
        var ex2 = assertThrows(RuntimeException.class,
                () -> invokeParseEmployee("A,B,a@x,Corp,PROGRAMISTA,100,EXTRA"));
        assertTrue(ex2.getCause() instanceof com.example.exception.InvalidDataException);
        assertTrue(ex2.getCause().getMessage().contains("Expected 6 columns, got 7"));
    }

    @Test
    @DisplayName("parseEmployee: unknown position enum → InvalidDataException with clear message")
    void parseEmployee_unknownPosition() {
        var ex = assertThrows(RuntimeException.class,
                () -> invokeParseEmployee("A,B,a@x,Corp,NOT_A_REAL_POSITION,1000"));
        assertTrue(ex.getCause() instanceof com.example.exception.InvalidDataException);
        assertEquals("Unknown position enum: 'NOT_A_REAL_POSITION'", ex.getCause().getMessage());
    }

    @Test
    @DisplayName("parseEmployee: non-numeric salary → InvalidDataException naming the offending token")
    void parseEmployee_nonNumericSalary() {
        var ex = assertThrows(RuntimeException.class,
                () -> invokeParseEmployee("A,B,a@x,Corp,PROGRAMISTA,tenK"));
        assertTrue(ex.getCause() instanceof com.example.exception.InvalidDataException);
        assertEquals("Salary is not a number: 'tenK'", ex.getCause().getMessage());
    }

    @Test
    @DisplayName("parseEmployee: non-positive salary (≤0) → InvalidDataException with explicit value")
    void parseEmployee_nonPositiveSalary() {
        var exNeg = assertThrows(RuntimeException.class,
                () -> invokeParseEmployee("A,B,a@x,Corp,PROGRAMISTA,-1"));
        assertTrue(exNeg.getCause() instanceof com.example.exception.InvalidDataException);
        assertEquals("Salary must be positive, got: -1.0", exNeg.getCause().getMessage());

        var exZero = assertThrows(RuntimeException.class,
                () -> invokeParseEmployee("A,B,a@x,Corp,PROGRAMISTA,0"));
        assertTrue(exZero.getCause() instanceof com.example.exception.InvalidDataException);
        assertEquals("Salary must be positive, got: 0.0", exZero.getCause().getMessage());
    }

    @Test
    @DisplayName("parseEmployee: empty first/last names still compose a trimmed fullName")
    void parseEmployee_emptyNameParts() {
        Employee e1 = invokeParseEmployee(" ,B,a@x,Corp,PROGRAMISTA,100");
        assertEquals("B", e1.getFullName());

        Employee e2 = invokeParseEmployee("A, ,b@x,Corp,PROGRAMISTA,100");
        assertEquals("A", e2.getFullName());

        Employee e3 = invokeParseEmployee("  ,  ,c@x,Corp,PROGRAMISTA,100");
        assertEquals("", e3.getFullName());
    }
}
