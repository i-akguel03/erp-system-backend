package com.erp.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ERP API")
                        .version("v1")
                        .description("""
                                **Login:** `POST /auth/login`

                                | Benutzer | Passwort | Rolle |
                                |----------|----------|-------|
                                | string    | stringst    | ADMIN |
                                | user     | user     | USER  |

                                Token aus der Antwort kopieren → **Authorize** (oben rechts) → `Bearer <token>` eintragen.
                                Das Token bleibt nach dem Browser-Refresh gespeichert.
                                """))
                .tags(List.of(
                        new Tag().name("Authentication"),
                        new Tag().name("Dashboard"),
                        new Tag().name("Kunden"),
                        new Tag().name("Verträge"),
                        new Tag().name("Abonnements"),
                        new Tag().name("Rechnungen"),
                        new Tag().name("Offene Posten"),
                        new Tag().name("Fälligkeitspläne"),
                        new Tag().name("Produkte"),
                        new Tag().name("Bestellungen"),
                        new Tag().name("Zahlungen"),
                        new Tag().name("Lagerbestand"),
                        new Tag().name("Vorgänge"),
                        new Tag().name("Adressen"),
                        new Tag().name("Benachrichtigungen"),
                        new Tag().name("Benutzerverwaltung"),
                        new Tag().name("Audit-Logs"),
                        new Tag().name("Init")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
