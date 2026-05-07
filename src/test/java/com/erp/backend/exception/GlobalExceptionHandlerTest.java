package com.erp.backend.exception;

import com.erp.backend.config.TestSecurityConfig;
import com.erp.backend.controller.CustomerController;
import com.erp.backend.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("Integration Tests: GlobalExceptionHandler → HTTP Status Mapping")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    // ========== 404 ResourceNotFoundException ==========

    @Test
    @DisplayName("ResourceNotFoundException → 404 mit ErrorResponse JSON")
    void resourceNotFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Kunde nicht gefunden mit ID: " + id))
                .when(customerService).deleteCustomerById(id);

        // Nicht vorhandener Kunde löschen → 404
        mockMvc.perform(delete("/api/customers/" + id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("nicht gefunden")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ========== 400 BusinessLogicException ==========

    @Test
    @DisplayName("BusinessLogicException → 400 mit ErrorResponse JSON")
    void businessLogicException_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new BusinessLogicException("Kunde kann nicht gelöscht werden, da aktive Verträge existieren."))
                .when(customerService).deleteCustomerById(id);

        mockMvc.perform(delete("/api/customers/" + id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(containsString("aktive Verträge")));
    }

    // ========== 409 DuplicateResourceException ==========

    @Test
    @DisplayName("DuplicateResourceException → 409 mit ErrorResponse JSON")
    void duplicateResource_returns409() throws Exception {
        when(customerService.createCustomer(any()))
                .thenThrow(new DuplicateResourceException("Ein Kunde mit dieser E-Mail-Adresse existiert bereits: test@test.de"));

        String body = """
                {
                  "firstName": "Max",
                  "lastName": "Mustermann",
                  "email": "test@test.de"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(containsString("test@test.de")));
    }

    // ========== 422 InvalidStatusTransitionException ==========

    @Test
    @DisplayName("InvalidStatusTransitionException → 422 mit ErrorResponse JSON")
    void invalidStatusTransition_returns422() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new InvalidStatusTransitionException("Abgerechnete Fälligkeiten können nicht pausiert werden"))
                .when(customerService).deleteCustomerById(id);

        mockMvc.perform(delete("/api/customers/" + id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.error").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.message").value(containsString("pausiert")));
    }

    // ========== 400 Validation (MethodArgumentNotValidException) ==========

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 mit fieldErrors")
    void validationException_returns400WithFieldErrors() throws Exception {
        // Customer mit leerem firstName → @NotBlank schlägt fehl
        String invalidBody = """
                {
                  "firstName": "",
                  "lastName": "Mustermann",
                  "email": "max@test.de"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.fieldErrors[0].message").exists());
    }

    // ========== 400 ungültiges JSON ==========

    @Test
    @DisplayName("Ungültiges JSON → 400 Bad Request")
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ ungültiges json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("JSON")));
    }

    // ========== 400 Typ-Mismatch ==========

    @Test
    @DisplayName("Falscher Parametertyp (keine UUID) → 400 Bad Request")
    void typeMismatch_returns400() throws Exception {
        mockMvc.perform(delete("/api/customers/keine-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ========== ErrorResponse Struktur ==========

    @Test
    @DisplayName("Alle ErrorResponse haben timestamp-Feld")
    void errorResponse_hasTimestamp() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Nicht gefunden"))
                .when(customerService).deleteCustomerById(id);

        mockMvc.perform(delete("/api/customers/" + id))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Erfolgreiche Requests haben kein fieldErrors-Feld (NON_NULL)")
    void successfulError_noFieldErrors() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Nicht gefunden"))
                .when(customerService).deleteCustomerById(id);

        mockMvc.perform(delete("/api/customers/" + id))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }
}
