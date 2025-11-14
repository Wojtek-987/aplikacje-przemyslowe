package com.example.service;

import com.example.exception.InvalidDataException;
import com.example.model.Employee;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

@Service
public class ReportService {

    private final EmployeeService employeeService;
    private final FileStorageService fileStorageService;

    public ReportService(EmployeeService employeeService, FileStorageService fileStorageService) {
        this.employeeService = employeeService;
        this.fileStorageService = fileStorageService;
    }

    // ===== CSV EXPORTS =====

    public byte[] generateCsvForAllEmployees() {
        List<Employee> employees = employeeService.getAllEmployees();
        return buildCsv(employees).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] generateCsvForCompany(String companyName) {
        List<Employee> employees = employeeService.findByCompany(companyName);
        return buildCsv(employees).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String buildCsv(List<Employee> employees) {
        StringBuilder sb = new StringBuilder();
        // header
        sb.append("Full Name,Email,Company,Position,Salary\n");
        for (Employee e : employees) {
            // naive CSV (no quoting) – enough for this lab, assuming no commas in data
            sb.append(nullSafe(e.getFullName())).append(',')
                    .append(nullSafe(e.getEmail())).append(',')
                    .append(nullSafe(e.getCompanyName())).append(',')
                    .append(e.getPosition() != null ? e.getPosition().name() : "").append(',')
                    .append(e.getSalary())
                    .append('\n');
        }
        return sb.toString();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    // ===== PDF EXPORTS =====

    /**
     * Generate a PDF with statistics for a given company, persist it in the reports directory,
     * and return the raw PDF bytes to send to the client.
     */
    public byte[] generateCompanyStatisticsPdf(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            throw new IllegalArgumentException("Company name must be provided");
        }

        List<Employee> employees = employeeService.findByCompany(companyName);
        if (employees.isEmpty()) {
            throw new IllegalArgumentException("No employees found for company: " + companyName);
        }

        long count = employees.size();
        OptionalDouble avgOpt = employees.stream()
                .mapToDouble(Employee::getSalary)
                .average();
        double avgSalary = avgOpt.orElse(0.0);

        double highestSalary = employees.stream()
                .mapToDouble(Employee::getSalary)
                .max()
                .orElse(0.0);

        String topEarnerName = employees.stream()
                .max(java.util.Comparator.comparingDouble(Employee::getSalary))
                .map(Employee::getFullName)
                .orElse("");

        byte[] pdfBytes = createPdf(companyName, count, avgSalary, highestSalary, topEarnerName, employees);

        // Persist report on disk in reports/ using FileStorageService
        String baseName = "company-statistics-" + companyName;
        fileStorageService.storeReport(pdfBytes, baseName, "pdf");

        return pdfBytes;
    }

    private byte[] createPdf(String companyName,
                             long employeeCount,
                             double averageSalary,
                             double highestSalary,
                             String topEarnerName,
                             List<Employee> employees) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pl", "PL"));

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;

                content.setFont(PDType1Font.HELVETICA_BOLD, 18);
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.showText("Company statistics: " + companyName);
                content.endText();

                y -= 40;

                content.setFont(PDType1Font.HELVETICA, 12);

                // Summary lines
                y = writeLine(content, margin, y, "Employee count: " + employeeCount);
                y = writeLine(content, margin, y, "Average salary: " + currency.format(averageSalary));
                y = writeLine(content, margin, y, "Highest salary: " + currency.format(highestSalary));
                y = writeLine(content, margin, y, "Top earner: " + topEarnerName);

                y -= 20;
                y = writeLine(content, margin, y, "Employees (name – salary):");

                // Simple “table” of first N employees
                for (Employee e : employees) {
                    if (y < margin + 40) { // crude page overflow handling: stop listing
                        y = writeLine(content, margin, y, "... (more employees omitted)");
                        break;
                    }
                    String line = String.format("- %s (%s): %s",
                            e.getFullName(),
                            e.getPosition() != null ? e.getPosition().name() : "?",
                            currency.format(e.getSalary()));
                    y = writeLine(content, margin + 10, y, line);
                }
            }

            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private float writeLine(PDPageContentStream content, float x, float y, String text) throws IOException {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - 16;
    }
}
