package com.example.spring_aplication.doc

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Spring Boot Aplication")
                    .version("1.0.0")
                    .version("www.JwtAutentication.com.br")
                    .description("API Rest Spring Boot Kotlin")
            )


    }
}