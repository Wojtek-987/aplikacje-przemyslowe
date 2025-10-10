package com.example.service;

import com.google.gson.*;
import com.example.exception.ApiException;
import com.example.model.Employee;
import com.example.model.Position;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class ApiService {

    private static final String API_URL = "https://jsonplaceholder.typicode.com/users";

    /**
     * Maps JSONPlaceholder users to Employee:
     * - fullName: 'name'
     * - email: 'email'
     * - companyName: 'company.name'
     * - position: PROGRAMISTA
     * - salary: base salary of PROGRAMISTA
     */
    public List<Employee> fetchEmployeesFromApi() throws ApiException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .GET()
                .build();

        final HttpResponse<String> resp;
        try {
            resp = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("HTTP transport error", e);
        }

        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new ApiException("HTTP error: " + resp.statusCode());
        }

        try {
            Gson gson = new Gson();
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
