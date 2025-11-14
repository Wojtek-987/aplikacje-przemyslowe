package com.example.service;

import com.example.exception.EmployeeNotFoundException;
import com.example.model.EmployeeDocument;
import com.example.model.FileType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.example.exception.FileNotFoundException;
import com.example.exception.FileStorageException;
import com.example.exception.InvalidFileException;

@Service
public class EmployeeDocumentService {

    private static final long MAX_DOC_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS =
            Set.of("pdf", "png", "jpg", "jpeg", "doc", "docx");

    private final Map<String, List<EmployeeDocument>> documentsByEmail = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1L);

    private final EmployeeService employeeService;
    private final FileStorageService fileStorageService;

    public EmployeeDocumentService(EmployeeService employeeService,
                                   FileStorageService fileStorageService) {
        this.employeeService = employeeService;
        this.fileStorageService = fileStorageService;
    }

    public EmployeeDocument uploadDocument(String email, MultipartFile file, FileType type) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Employee email must be provided");
        }

        // Validate that employee exists
        employeeService.findByEmail(email)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found: " + email));

        if (type == null) {
            throw new InvalidFileException("Document type must be provided");
        }

        // Validate file (extension + size)
        fileStorageService.validateFile(file, ALLOWED_DOCUMENT_EXTENSIONS, MAX_DOC_SIZE_BYTES);

        // Determine target directory: uploads/documents/{emailSanitised}/
        String safeEmail = sanitiseEmail(email);
        Path employeeDir = fileStorageService.getUploadDirectory()
                .resolve("documents")
                .resolve(safeEmail)
                .toAbsolutePath()
                .normalize();
        try {
            Files.createDirectories(employeeDir);
        } catch (IOException e) {
            throw new FileStorageException("Failed to create employee documents directory", e);
        }

        // Store file physically
        String storedFileName = fileStorageService.storeFile(file, employeeDir);
        Path fullPath = employeeDir.resolve(storedFileName);

        long id = idGenerator.getAndIncrement();
        EmployeeDocument doc = new EmployeeDocument(
                id,
                email,
                storedFileName,
                file.getOriginalFilename(),
                type,
                java.time.LocalDateTime.now(),
                fullPath.toString()
        );

        documentsByEmail
                .computeIfAbsent(email.toLowerCase(), k -> new ArrayList<>())
                .add(doc);

        return doc;
    }

    public List<EmployeeDocument> listDocuments(String email) {
        if (email == null || email.isBlank()) return List.of();
        return List.copyOf(documentsByEmail.getOrDefault(email.toLowerCase(), List.of()));
    }

    public Optional<EmployeeDocument> findDocument(String email, long documentId) {
        if (email == null || email.isBlank()) return Optional.empty();
        List<EmployeeDocument> list = documentsByEmail.get(email.toLowerCase());
        if (list == null) return Optional.empty();
        return list.stream()
                .filter(d -> d.getId() == documentId)
                .findFirst();
    }

    public Resource loadDocumentAsResource(EmployeeDocument doc) {
        try {
            Path path = Path.of(doc.getFilePath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileNotFoundException("Document file not found: " + doc.getFilePath());
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new FileStorageException("Failed to load document file: " + doc.getFilePath(), e);
        }
    }

    public void deleteDocument(String email, long documentId) {
        if (email == null || email.isBlank()) return;
        String key = email.toLowerCase();
        List<EmployeeDocument> list = documentsByEmail.get(key);
        if (list == null) return;

        Iterator<EmployeeDocument> it = list.iterator();
        while (it.hasNext()) {
            EmployeeDocument doc = it.next();
            if (doc.getId() == documentId) {
                // remove from registry
                it.remove();
                // best-effort delete from disk
                try {
                    Files.deleteIfExists(Path.of(doc.getFilePath()));
                } catch (IOException ignored) {
                    // we do not fail the operation if physical delete fails
                }
                break;
            }
        }

        if (list.isEmpty()) {
            documentsByEmail.remove(key);
        }
    }

    private String sanitiseEmail(String email) {
        return email.toLowerCase().replaceAll("[^a-z0-9@._-]", "_");
    }
}
