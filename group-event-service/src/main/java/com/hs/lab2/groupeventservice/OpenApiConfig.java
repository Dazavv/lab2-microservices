package com.hs.lab2.groupeventservice;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI groupEventServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Group event Service API")
                        .version("v0"))
                .servers(List.of(
                        new Server().url("/")
                ));
    }
}
