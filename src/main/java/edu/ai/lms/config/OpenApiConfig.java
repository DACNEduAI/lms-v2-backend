package edu.ai.lms.config;

import java.util.List;

import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        // Define the security scheme for JWT
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .version("1.0.0")
                        .description("LMS Platform REST API with JWT Authentication. " +
                                "This API provides endpoints for managing students, teachers, and authentication.")
                        .contact(new Contact()
                                .name("LMS Platform Team")
                                .email("support@lms-platform.com")
                                .url("https://lms-platform.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from login endpoint. " +
                                                "Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")));
    }

    @Bean
    public OperationCustomizer customizePreAuthorize() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            // Check if the method has a @PreAuthorize annotation
            PreAuthorize preAuthorize = handlerMethod.getMethodAnnotation(PreAuthorize.class);

            if (preAuthorize != null) {
                // Get the existing description or start with an empty string
                String currentDescription = operation.getDescription() != null ? operation.getDescription() : "";

                // Append the security rule in a formatted HTML block
                String securityRuleHtml = String.format(
                        "<br/><br/>🛡️ <b>Roles allowed</b> <code>%s</code>",
                        preAuthorize.value()
                );

                operation.setDescription(currentDescription + securityRuleHtml);
            }

            return operation;
        };
    }
}

