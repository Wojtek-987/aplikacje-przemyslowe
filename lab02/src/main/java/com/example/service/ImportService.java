package com.example.service;

import com.example.exception.InvalidDataException;
import com.example.model.Employee;
import com.example.model.ImportSummary;
import com.example.model.Position;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportService {

    private final EmployeeService directory;

    public ImportService(EmployeeService directory) {
        this.directory = directory;
    }

    /**
     * CSV header: firstName,lastName,email,company,position,salary
     * Skips header & blank lines. Continues on errors; collects line diagnostics.
     */
    public ImportSummary importFromCsv(String filePath) {
        int imported = 0;
        List<String> errors = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> rows = reader.readAll();
            for (int i = 1; i < rows.size(); i++) { // skip header
                String[] c = rows.get(i);
                try {
                    if (c.length != 6) throw new InvalidDataException("Expected 6 columns, got " + c.length);
                    String first = c[0].trim(), last = c[1].trim(), email = c[2].trim();
                    String company = c[3].trim(), posStr = c[4].trim(), salStr = c[5].trim();

                    var pos = Position.valueOf(posStr.toUpperCase());       // may throw IllegalArgumentException
                    double salary = Double.parseDouble(salStr);              // may throw NumberFormatException
                    if (salary <= 0) throw new InvalidDataException("Salary must be positive, got: " + salary);

                    directory.addEmployee(new Employee((first + " " + last).trim(), email, company, pos, salary));
                    imported++;
                } catch (Exception ex) {
                    errors.add("Line " + (i + 1) + ": " + ex.getMessage());
                }
            }
        } catch (IOException | CsvException e) {
            errors.add("I/O or CSV parse error: " + e.getMessage());
        }


        return new ImportSummary(imported, errors);
    }


    private static Employee parseEmployee(String csvLine) throws InvalidDataException {
        String[] parts = csvLine.split(",", -1); // preserve empty trailing fields
        if (parts.length != 6) {
            throw new InvalidDataException("Expected 6 columns, got " + parts.length);
        }

        String firstName = parts[0].trim();
        String lastName  = parts[1].trim();
        String email     = parts[2].trim();
        String company   = parts[3].trim();
        String positionS = parts[4].trim();
        String salaryS   = parts[5].trim();

        // Position validation
        final Position position;
        try {
            position = Position.valueOf(positionS.toUpperCase());
        } catch (IllegalArgumentException iae) {
            throw new InvalidDataException("Unknown position enum: '" + positionS + "'");
        }

        // Salary validation (must be strictly positive)
        final double salary;
        try {
            salary = Double.parseDouble(salaryS);
        } catch (NumberFormatException nfe) {
            throw new InvalidDataException("Salary is not a number: '" + salaryS + "'");
        }
        if (salary <= 0) {
            throw new InvalidDataException("Salary must be positive, got: " + salary);
        }

        String fullName = (firstName + " " + lastName).trim();
        return new Employee(fullName, email, company, position, salary);
    }
}