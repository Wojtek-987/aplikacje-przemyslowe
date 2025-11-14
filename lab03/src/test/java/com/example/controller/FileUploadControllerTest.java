package com.example.controller;

import com.example.model.EmployeeDocument;
import com.example.model.FileType;
import com.example.model.ImportSummary;
import com.example.service.EmployeeDocumentService;
import com.example.service.EmployeeService;
import com.example.service.FileStorageService;
import com.example.service.ImportService;
import com.example.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.exception.GlobalExceptionHandler;
import org.springframework.context.annotation.Import;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(FileUploadController.class)
@Import(GlobalExceptionHandler.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private ImportService importService;

    @MockBean
    private ReportService reportService;

    @MockBean
    private EmployeeDocumentService employeeDocumentService;

    @MockBean
    private EmployeeService employeeService;

    // ===== Test 1: successful CSV upload =====

    @Test
    @DisplayName("POST /api/files/import/csv should accept CSV upload and return ImportSummary with 200 OK")
    void uploadCsv_shouldReturnImportSummary() throws Exception {
        // given
        String csvContent = "fullName,email,company,position,salary\n" +
                "John Doe,john@example.com,ACME,PROGRAMISTA,8000\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.csv",
                "text/csv",
                csvContent.getBytes()
        );

        ImportSummary summary = new ImportSummary(1, List.of());

        // mock storage & import behaviour
        given(fileStorageService.storeUpload(any(), anySet(), anyLong()))
                .willReturn("employees.csv");
        given(fileStorageService.getUploadDirectory())
                .willReturn(java.nio.file.Paths.get("uploads"));
        given(importService.importFromCsv(anyString()))
                .willReturn(summary);

        // when / then
        mockMvc.perform(multipart("/api/files/import/csv").file(file))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.importedCount", is(1)))
                .andExpect(jsonPath("$.errors.length()", is(0)));
    }

    // ===== Test 2: too large upload -> 413 =====

    @Test
    @DisplayName("POST /api/files/import/csv with file exceeding max size should return 413 Payload Too Large")
    void uploadTooLarge_shouldReturn413() throws Exception {
        // given – the actual size here doesn't matter because we mock the behaviour
        byte[] content = new byte[1024]; // 1 KB is enough

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "too-big.csv",
                "text/csv",
                content
        );

        // Simulate Spring throwing MaxUploadSizeExceededException when trying to store the file
        given(fileStorageService.storeUpload(any(), anySet(), anyLong()))
                .willThrow(new MaxUploadSizeExceededException(10L * 1024 * 1024)); // 10 MB

        // when / then
        mockMvc.perform(multipart("/api/files/import/csv").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Maximum allowed size")
                ));
    }

    // ===== Test 3: invalid extension -> 400 =====

    @Test
    @DisplayName("POST /api/files/import/csv with .txt file should return 400 Bad Request and error message")
    void uploadInvalidExtension_shouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "data.txt",
                "text/plain",
                "some text".getBytes()
        );

        given(fileStorageService.storeUpload(any(), anySet(), anyLong()))
                .willThrow(new com.example.exception.InvalidFileException("File type .txt is not allowed."));

        mockMvc.perform(multipart("/api/files/import/csv").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", containsString("File type .txt is not allowed.")));
    }

    // ===== Test 4: download CSV report =====

    @Test
    @DisplayName("GET /api/files/export/csv should return generated CSV with Content-Type text/csv")
    void downloadCsvReport_shouldReturnCsv() throws Exception {
        // given
        String csv = "fullName,email\nJohn Doe,john@example.com\n";
        byte[] csvBytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        given(reportService.generateCsvForAllEmployees())
                .willReturn(csvBytes);

        // when / then
        mockMvc.perform(get("/api/files/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("employees.csv")))
                .andExpect(content().string(csv));
    }

    // ===== Test 5: upload employee document =====

    @Test
    @DisplayName("POST /api/files/documents/{email} should save employee document and return 201 with metadata")
    void uploadEmployeeDocument_shouldReturnMetadataAnd201() throws Exception {
        // given
        String email = "john@example.com";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contract.pdf",
                "application/pdf",
                "<<pdf-bytes>>".getBytes()
        );

        EmployeeDocument doc = new EmployeeDocument(
                42L,
                email,
                "stored-contract-42.pdf",
                "contract.pdf",
                FileType.CONTRACT,
                LocalDateTime.now(),
                "/some/path/stored-contract-42.pdf"
        );

        given(employeeDocumentService.uploadDocument(eq(email), any(), eq(FileType.CONTRACT)))
                .willReturn(doc);

        // when / then
        mockMvc.perform(
                        multipart("/api/files/documents/{email}", email)
                                .file(file)
                                .param("type", "CONTRACT")
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(42)))
                .andExpect(jsonPath("$.employeeEmail", is(email)))
                .andExpect(jsonPath("$.fileName", is("stored-contract-42.pdf")))
                .andExpect(jsonPath("$.originalFileName", is("contract.pdf")))
                .andExpect(jsonPath("$.fileType", is("CONTRACT")));
    }
}
