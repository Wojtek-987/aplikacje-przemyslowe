package com.example.controller;

import com.example.model.Employee;
import com.example.model.EmployeeDocument;
import com.example.model.FileType;
import com.example.model.ImportSummary;
import com.example.service.EmployeeDocumentService;
import com.example.service.EmployeeService;
import com.example.service.FileStorageService;
import com.example.service.ImportService;
import com.example.service.ReportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.example.exception.FileStorageException;
import com.example.exception.InvalidFileException;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private static final long MAX_UPLOAD_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final long MAX_PHOTO_SIZE_BYTES = 2L * 1024 * 1024;   // 2 MB

    private static final Set<String> CSV_EXTENSIONS = Set.of("csv");
    private static final Set<String> XML_EXTENSIONS = Set.of("xml");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final FileStorageService fileStorageService;
    private final ImportService importService;
    private final ReportService reportService;
    private final EmployeeDocumentService employeeDocumentService;
    private final EmployeeService employeeService;

    public FileUploadController(FileStorageService fileStorageService,
                                ImportService importService,
                                ReportService reportService,
                                EmployeeDocumentService employeeDocumentService,
                                EmployeeService employeeService) {
        this.fileStorageService = fileStorageService;
        this.importService = importService;
        this.reportService = reportService;
        this.employeeDocumentService = employeeDocumentService;
        this.employeeService = employeeService;
    }

    // ===== IMPORT ENDPOINTS =====

    /**
     * POST /api/files/import/csv
     */
    @PostMapping("/import/csv")
    public ResponseEntity<ImportSummary> importCsv(@RequestParam("file") MultipartFile file) {
        String storedFileName = fileStorageService.storeUpload(file, CSV_EXTENSIONS, MAX_UPLOAD_SIZE_BYTES);
        Path fullPath = fileStorageService.getUploadDirectory().resolve(storedFileName);

        ImportSummary summary = importService.importFromCsv(fullPath.toString());
        return buildResponseFromSummary(summary);
    }

    /**
     * POST /api/files/import/xml
     */
    @PostMapping("/import/xml")
    public ResponseEntity<ImportSummary> importXml(@RequestParam("file") MultipartFile file) {
        String storedFileName = fileStorageService.storeUpload(file, XML_EXTENSIONS, MAX_UPLOAD_SIZE_BYTES);
        Path fullPath = fileStorageService.getUploadDirectory().resolve(storedFileName);

        ImportSummary summary = importService.importFromXml(fullPath.toString());
        return buildResponseFromSummary(summary);
    }

    private ResponseEntity<ImportSummary> buildResponseFromSummary(ImportSummary summary) {
        List<String> errors = summary.getErrors();
        var status = (errors == null || errors.isEmpty())
                ? org.springframework.http.HttpStatus.OK
                : org.springframework.http.HttpStatus.BAD_REQUEST;

        return ResponseEntity.status(status).body(summary);
    }

    // ===== EXPORT ENDPOINTS =====

    /**
     * GET /api/files/export/csv
     * GET /api/files/export/csv?company=X
     */
    @GetMapping("/export/csv")
    public ResponseEntity<Resource> exportCsv(@RequestParam(name = "company", required = false) String companyName) {
        byte[] csvBytes = (companyName == null || companyName.isBlank())
                ? reportService.generateCsvForAllEmployees()
                : reportService.generateCsvForCompany(companyName);

        String filename = (companyName == null || companyName.isBlank())
                ? "employees.csv"
                : "employees-" + companyName + ".csv";

        ByteArrayResource resource = new ByteArrayResource(csvBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

    /**
     * GET /api/files/reports/statistics/{companyName}
     */
    @GetMapping("/reports/statistics/{companyName}")
    public ResponseEntity<Resource> exportCompanyStatisticsPdf(@PathVariable String companyName) {
        byte[] pdfBytes = reportService.generateCompanyStatisticsPdf(companyName);

        String filename = "company-statistics-" + companyName + ".pdf";
        ByteArrayResource resource = new ByteArrayResource(pdfBytes);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(resource.contentLength())
                .body(resource);
    }

    // ===== EMPLOYEE DOCUMENT ENDPOINTS =====

    /**
     * POST /api/files/documents/{email}
     */
    @PostMapping("/documents/{email}")
    public ResponseEntity<EmployeeDocument> uploadEmployeeDocument(
            @PathVariable String email,
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") FileType type) {

        EmployeeDocument doc = employeeDocumentService.uploadDocument(email, file, type);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(doc);
    }

    /**
     * GET /api/files/documents/{email}
     */
    @GetMapping("/documents/{email}")
    public ResponseEntity<List<EmployeeDocument>> listEmployeeDocuments(@PathVariable String email) {
        List<EmployeeDocument> docs = employeeDocumentService.listDocuments(email);
        return ResponseEntity.ok(docs);
    }

    /**
     * GET /api/files/documents/{email}/{documentId}
     */
    @GetMapping("/documents/{email}/{documentId}")
    public ResponseEntity<Resource> downloadEmployeeDocument(
            @PathVariable String email,
            @PathVariable long documentId) {

        return employeeDocumentService.findDocument(email, documentId)
                .map(doc -> {
                    Resource resource = employeeDocumentService.loadDocumentAsResource(doc);

                    String contentType = guessContentType(doc.getOriginalFileName());
                    String disposition = "attachment; filename=\"" +
                            (doc.getOriginalFileName() != null ? doc.getOriginalFileName() : doc.getFileName()) +
                            "\"";

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                            .contentType(MediaType.parseMediaType(contentType))
                            .body(resource);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/files/documents/{email}/{documentId}
     */
    @DeleteMapping("/documents/{email}/{documentId}")
    public ResponseEntity<Void> deleteEmployeeDocument(
            @PathVariable String email,
            @PathVariable long documentId) {

        employeeDocumentService.deleteDocument(email, documentId);
        return ResponseEntity.noContent().build();
    }

    // ===== EMPLOYEE PHOTO ENDPOINTS =====

    /**
     * POST /api/files/photos/{email}
     *
     * Validates that employee exists, checks file is an image (JPG/PNG) and <= 2MB,
     * stores it under uploads/photos/ with a name corresponding to the email,
     * and updates Employee.photoFileName.
     */
    @PostMapping("/photos/{email}")
    public ResponseEntity<Employee> uploadEmployeePhoto(
            @PathVariable String email,
            @RequestParam("file") MultipartFile file) {

        Employee employee = employeeService.findByEmail(email)
                .orElseThrow(() -> new com.example.exception.EmployeeNotFoundException("Employee not found: " + email));

        // 1) basic validation: extension + size
        fileStorageService.validateFile(
                file,
                IMAGE_EXTENSIONS,
                MAX_PHOTO_SIZE_BYTES,
                Set.of("image/jpeg", "image/png")
        );

        // 2) optional validation: check file is actually an image
        ensureIsImage(file);

        // 3) determine target directory: uploads/photos/
        Path photosDir = fileStorageService.getUploadDirectory()
                .resolve("photos")
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(photosDir);
        } catch (IOException e) {
            throw new FileStorageException("Failed to create photos directory", e);
        }

        // 4) build filename based on email
        String extension = getFileExtension(file.getOriginalFilename());
        if (extension.isEmpty()) {
            throw new InvalidFileException("Photo file must have an extension");
        }

        String safeEmail = sanitiseEmail(email);
        String storedFileName = safeEmail + "." + extension.toLowerCase();

        Path targetPath = photosDir.resolve(storedFileName);
        try {
            Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store photo", e);
        }

        // 5) update employee metadata
        employee.setPhotoFileName(storedFileName);

        // return updated employee
        return ResponseEntity.ok(employee);
    }

    /**
     * GET /api/files/photos/{email}
     *
     * Returns the employee's photo as Resource with proper image Content-Type.
     * If no photo is set, returns 404 Not Found.
     */
    @GetMapping("/photos/{email}")
    public ResponseEntity<Resource> getEmployeePhoto(@PathVariable String email) {
        Employee employee = employeeService.findByEmail(email)
                .orElseThrow(() -> new com.example.exception.EmployeeNotFoundException("Employee not found: " + email));

        String photoFileName = employee.getPhotoFileName();
        if (photoFileName == null || photoFileName.isBlank()) {
            // Could return a placeholder image here instead
            return ResponseEntity.notFound().build();
        }

        Path photosDir = fileStorageService.getUploadDirectory()
                .resolve("photos")
                .toAbsolutePath()
                .normalize();
        Path photoPath = photosDir.resolve(photoFileName).normalize();

        Resource resource;
        try {
            resource = new UrlResource(photoPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }

        String contentType = guessContentType(photoFileName);
        if (!contentType.startsWith("image/")) {
            // fallback just in case
            contentType = "image/jpeg";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // ===== Helpers =====

    private String guessContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String probed = URLConnection.guessContentTypeFromName(filename);
        return probed != null ? probed : "application/octet-stream";
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) return "";
        return filename.substring(lastDot + 1);
    }

    private String sanitiseEmail(String email) {
        if (email == null) return "";
        return email.toLowerCase().replaceAll("[^a-z0-9@._-]", "_");
    }

    private void ensureIsImage(MultipartFile file) {
        try {
            // ImageIO.read returns null if the stream does not contain a known image format
            BufferedImage img = ImageIO.read(file.getInputStream());
            if (img == null) {
                throw new InvalidFileException("Uploaded file is not a valid image");
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to inspect uploaded image", e);
        }
    }
}
