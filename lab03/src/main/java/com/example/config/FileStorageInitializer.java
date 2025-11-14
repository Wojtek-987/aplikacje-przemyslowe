package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FileStorageInitializer implements CommandLineRunner {

    @Value("${app.upload.directory}")
    private String uploadDir;

    @Value("${app.reports.directory}")
    private String reportsDir;

    @Override
    public void run(String... args) throws Exception {
        createDirectoryIfNotExists(uploadDir);
        createDirectoryIfNotExists(reportsDir);
    }

    private void createDirectoryIfNotExists(String dir) throws IOException {
        Path path = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(path); // idempotent: does nothing if it already exists
    }
}
