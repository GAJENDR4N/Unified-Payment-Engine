package io.gomobi.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Exposes API docs at /swagger-ui.html and /v3/api-docs. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentEngineOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Engine API")
                        .description("Unified payment processing across QR, E-Wallet and Online Banking, multi-provider adapter architecture")
                        .version("1.0.0")
                        .contact(new Contact().name("GoMobi Payments Engineering")))
                .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))
                .schemaRequirement("ApiKeyAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-Api-Key"));
    }
}
