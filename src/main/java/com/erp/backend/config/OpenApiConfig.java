package com.erp.backend.config;

// OpenAPI/Swagger-Annotations für Metadaten
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

// OpenAPI-Modelle für Security und Komponenten
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

// Spring-Konfiguration
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurationsklasse für Swagger / OpenAPI.
 * Sie sorgt dafür, dass in der Swagger-UI die JWT-Authentifizierung
 * eingebunden wird, sodass man direkt im UI ein Token eintragen kann.
 */
@Configuration // Markiert die Klasse als Spring-Konfigurationsklasse
@OpenAPIDefinition(
        info = @Info(title = "ERP API", version = "v1") // Grundlegende API-Infos (Titel, Version etc.)
)
public class OpenApiConfig {

    /**
     * Diese Bean erzeugt eine OpenAPI-Konfiguration,
     * die von Springdoc (Swagger-Integration) automatisch erkannt wird.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Security-Anforderung hinzufügen: Endpoints brauchen "bearerAuth"
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                // Security-Schema definieren: Bearer Token mit JWT
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)   // HTTP-Auth
                                .scheme("bearer")                 // "Bearer"-Schema (Authorization Header)
                                .bearerFormat("JWT")));           // Format JWT
    }
}
