package com.musicmatch.config.swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI musicMatchOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(new Info()
                .title("MusicMatch API")
                .description("""
                    REST API for MusicMatch — a music recommendation platform that uses
                    Singular Value Decomposition (SVD) to find users with compatible music taste
                    and generate personalized song recommendations.
                    
                    ## Authentication
                    Most endpoints require a valid JWT Bearer token. Use `/api/auth/login`
                    to obtain a token, then click **Authorize** and paste it below.
                    """)
                .version("v1.0.0")
                .contact(new Contact()
                    .name("MusicMatch Team")
                    .email("team@musicmatch.dev")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter your JWT access token (without the 'Bearer ' prefix)")));
    }
}
