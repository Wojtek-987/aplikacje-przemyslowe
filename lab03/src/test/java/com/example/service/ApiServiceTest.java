package com.example.service;

import com.example.exception.ApiException;
import com.example.model.Employee;
import com.example.model.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import com.google.gson.Gson;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiServiceTest {

    @Mock
    HttpClient client;

    @Mock
    HttpResponse<String> response;

    @Test
    @DisplayName("Successful JSON: maps to Employee objects with correct defaults")
    void fetchEmployees_success() throws Exception {
        // Given: a JSON array like jsonplaceholder /users
        String json = """
            [
              {"name":"Ada Lovelace","email":"ada@ex.com","company":{"name":"ACME"}},
              {"name":"Grace Hopper","email":"grace@ex.com","company":{"name":null}},
              {"name":"No Company","email":"no@ex.com"}
            ]
            """;

        when(client.send(
                ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())
        ).thenReturn(response);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(json);

        ApiService svc = new ApiService(client, new Gson(), "https://jsonplaceholder.typicode.com/users", true);

        // When
        List<Employee> employees = svc.fetchEmployeesFromApi();

        // Then
        assertEquals(3, employees.size());

        Employee e1 = employees.get(0);
        assertEquals("Ada Lovelace", e1.getFullName());
        assertEquals("ada@ex.com", e1.getEmail());
        assertEquals("ACME", e1.getCompanyName());
        assertEquals(Position.PROGRAMISTA, e1.getPosition());
        assertEquals(Position.PROGRAMISTA.getBaseSalary(), e1.getSalary());

        Employee e2 = employees.get(1);
        assertEquals("Grace Hopper", e2.getFullName());
        assertEquals("grace@ex.com", e2.getEmail());
        assertEquals("", e2.getCompanyName(), "null company.name → empty string");

        Employee e3 = employees.get(2);
        assertEquals("No Company", e3.getFullName());
        assertEquals("no@ex.com", e3.getEmail());
        assertEquals("", e3.getCompanyName(), "missing company → empty string");
    }

    @ParameterizedTest(name = "HTTP error status {0} → ApiException")
    @ValueSource(ints = {404, 500})
    @DisplayName("HTTP error responses: throw ApiException with status code")
    void fetchEmployees_httpErrors(int status) throws Exception {
        when(client.send(
                ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())
        ).thenReturn(response);

        when(response.statusCode()).thenReturn(status);

        ApiService svc = new ApiService(client, new Gson(), "https://jsonplaceholder.typicode.com/users", true);

        ApiException ex = assertThrows(ApiException.class, svc::fetchEmployeesFromApi);
        assertTrue(ex.getMessage().contains("HTTP error: " + status));
    }

    @Test
    @DisplayName("Parsing failure: invalid JSON payload → ApiException with parse message")
    void fetchEmployees_parseFailure() throws Exception {
        String notAnArray = """
                { "unexpected":"shape" }
        """;

        when(client.send(
                ArgumentMatchers.any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())
        ).thenReturn(response);

        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(notAnArray);

        ApiService svc = new ApiService(client, new Gson(), "https://jsonplaceholder.typicode.com/users", true);

        ApiException ex = assertThrows(ApiException.class, svc::fetchEmployeesFromApi);
        assertTrue(ex.getMessage().contains("Failed to parse API payload"));
    }
}
