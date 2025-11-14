package com.example.service;

import com.example.exception.FileNotFoundException;
import com.example.exception.FileStorageException;
import com.example.exception.InvalidFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadDirectory;
    private final Path reportsDirectory;

    public FileStorageService(
            @Value("${app.upload.directory}") String uploadDir,
            @Value("${app.reports.directory}") String reportsDir
    ) {
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.reportsDirectory = Paths.get(reportsDir).toAbsolutePath().normalize();
    }

    /**
     * Save a file into the uploads directory.
     */
    public String storeUpload(MultipartFile file,
                              Set<String> allowedExtensions,
                              long maxSizeBytes) {
        validateFile(file, allowedExtensions, maxSizeBytes);
        return storeFile(file, uploadDirectory);
    }

    /**
     * Save a file into the reports directory.
     */
    public String storeReport(MultipartFile file,
                              Set<String> allowedExtensions,
                              long maxSizeBytes) {
        validateFile(file, allowedExtensions, maxSizeBytes);
        return storeFile(file, reportsDirectory);
    }

    /**
     * Store already-generated report bytes (e.g. PDF) in reports directory.
     */
    public String storeReport(byte[] data, String baseName, String extension) {
        if (data == null || data.length == 0) {
            throw new InvalidFileException("Report data is empty");
        }

        String safeBase = (baseName == null || baseName.isBlank())
                ? "report"
                : baseName.replaceAll("[^a-zA-Z0-9-_]", "_");

        String ext = (extension == null || extension.isBlank())
                ? "bin"
                : extension;

        String uniqueName = safeBase + "-" + UUID.randomUUID() + "." + ext;
        Path targetLocation = reportsDirectory.resolve(uniqueName);

        try {
            Files.write(targetLocation, data);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store report file " + uniqueName, ex);
        }

        return uniqueName;
    }

    /**
     * Load a file from the uploads directory as a Spring Resource.
     */
    public Resource loadUploadAsResource(String filename) {
        return loadFileAsResource(uploadDirectory, filename);
    }

    /**
     * Load a file from the reports directory as a Spring Resource.
     */
    public Resource loadReportAsResource(String filename) {
        return loadFileAsResource(reportsDirectory, filename);
    }

    /**
     * Delete a file from the uploads directory.
     */
    public void deleteUpload(String filename) {
        deleteFile(uploadDirectory, filename);
    }

    /**
     * Delete a file from the reports directory.
     */
    public void deleteReport(String filename) {
        deleteFile(reportsDirectory, filename);
    }

    /**
     * Validate extension, size and optionally MIME type.
     *
     * @param file               multipart file
     * @param allowedExtensions  extensions without dot (e.g. "csv", "xml")
     * @param maxSizeBytes       max allowed size in bytes
     */
    public void validateFile(MultipartFile file,
                             Set<String> allowedExtensions,
                             long maxSizeBytes) {
        validateFile(file, allowedExtensions, maxSizeBytes, null);
    }

    /**
     * Overload that allows specifying allowed MIME types.
     *
     * @param allowedContentTypes content-types like "text/csv", "image/jpeg"
     */
    public void validateFile(MultipartFile file,
                             Set<String> allowedExtensions,
                             long maxSizeBytes,
                             Set<String> allowedContentTypes) {

        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty.");
        }

        String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "File name is required");
        String extension = getFileExtension(originalFilename);

        if (extension.isEmpty()) {
            throw new InvalidFileException("Uploaded file has no extension.");
        }

        boolean allowedExt = allowedExtensions == null || allowedExtensions.isEmpty()
                || allowedExtensions.stream().anyMatch(allowedExtVal -> allowedExtVal.equalsIgnoreCase(extension));

        if (!allowedExt) {
            throw new InvalidFileException("File type ." + extension + " is not allowed.");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new InvalidFileException("File is too large. Max allowed size is " + maxSizeBytes + " bytes.");
        }

        if (allowedContentTypes != null && !allowedContentTypes.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                throw new InvalidFileException("File content type is missing or could not be determined.");
            }

            boolean allowedCt = allowedContentTypes.stream()
                    .anyMatch(allowedCtVal -> allowedCtVal.equalsIgnoreCase(contentType));

            if (!allowedCt) {
                throw new InvalidFileException("File content type '" + contentType + "' is not allowed.");
            }
        }
    }

    // ===== Public helper used by other services =====

    public String storeFile(MultipartFile file, Path targetDirectory) {
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);

        String uniqueName = UUID.randomUUID().toString();
        if (!extension.isEmpty()) {
            uniqueName = uniqueName + "." + extension;
        }

        Path targetLocation = targetDirectory.resolve(uniqueName);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store file " + originalFilename, ex);
        }

        return uniqueName;
    }

    private Resource loadFileAsResource(Path directory, String filename) {
        try {
            Path filePath = directory.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileNotFoundException("File not found: " + filename);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new FileStorageException("Failed to resolve file path: " + filename, ex);
        }
    }

    private void deleteFile(Path directory, String filename) {
        try {
            Path filePath = directory.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to delete file: " + filename, ex);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    public Path getReportsDirectory() {
        return reportsDirectory;
    }
}
