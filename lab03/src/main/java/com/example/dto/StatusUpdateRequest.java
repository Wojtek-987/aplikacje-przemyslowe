package com.example.dto;

public class StatusUpdateRequest {
    private String status; // ACTIVE | ON_LEAVE | TERMINATED

    public StatusUpdateRequest() {}
    public StatusUpdateRequest(String status) { this.status = status; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
