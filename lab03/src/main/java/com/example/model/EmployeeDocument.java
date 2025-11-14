package com.example.model;

import java.time.LocalDateTime;

public class EmployeeDocument {

    private final long id;
    private final String employeeEmail;
    private final String fileName;          // stored filename on disk
    private final String originalFileName;  // name uploaded by user
    private final FileType fileType;
    private final LocalDateTime uploadDate;
    private final String filePath;          // full path on disk

    public EmployeeDocument(long id,
                            String employeeEmail,
                            String fileName,
                            String originalFileName,
                            FileType fileType,
                            LocalDateTime uploadDate,
                            String filePath) {
        this.id = id;
        this.employeeEmail = employeeEmail;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.uploadDate = uploadDate;
        this.filePath = filePath;
    }

    public long getId() {
        return id;
    }

    public String getEmployeeEmail() {
        return employeeEmail;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public FileType getFileType() {
        return fileType;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public String getFilePath() {
        return filePath;
    }

    @Override
    public String toString() {
        return "EmployeeDocument{" +
                "id=" + id +
                ", employeeEmail='" + employeeEmail + '\'' +
                ", fileName='" + fileName + '\'' +
                ", originalFileName='" + originalFileName + '\'' +
                ", fileType=" + fileType +
                ", uploadDate=" + uploadDate +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
