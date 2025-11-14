package com.example.service;

import com.example.exception.ApiException;
import com.example.model.Employee;
import com.example.model.Position;
import com.google.gson.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApiService {

    private final HttpClient client;
    private final Gson gson;
    private final String apiUrl;

    /** Constructor used by Spring (explicitly marked to disambiguate). */
    @Autowired
    public ApiService(HttpClient client,
                      Gson gson,
                      @Value("${app.api.url}") String apiUrl) {
        this.client = client;
        this.gson = gson;
        this.apiUrl = apiUrl;
    }

    /** Test-only convenience constructor (not used by Spring). */
    public ApiService(HttpClient client, Gson gson, String apiUrl, boolean forTests) {
        this.client = client;
        this.gson = gson;
        this.apiUrl = apiUrl;
    }

    public List<Employee> fetchEmployeesFromApi() throws ApiException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .GET()
                .build();

        final HttpResponse<String> resp;
        try {
            resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new ApiException("HTTP transport interrupted", ie);
        } catch (IOException ioe) {
            throw new ApiException("HTTP transport error", ioe);
        }

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new ApiException("HTTP error: " + resp.statusCode());
        }

        try {
            JsonArray arr = gson.fromJson(resp.body(), JsonArray.class);
            List<Employee> list = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();
                String fullName = getString(o, "name");
                String email = getString(o, "email");
                String companyName = "";
                if (o.has("company") && o.get("company").isJsonObject()) {
                    JsonObject c = o.getAsJsonObject("company");
                    if (c.has("name") && !c.get("name").isJsonNull()) {
                        companyName = c.get("name").getAsString();
                    }
                }
                Position pos = Position.PROGRAMISTA;
                double salary = pos.getBaseSalary();
                list.add(new Employee(fullName, email, companyName, pos, salary));
            }
            return list;
        } catch (RuntimeException ex) {
            throw new ApiException("Failed to parse API payload", ex);
        }
    }

    private static String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }
}
