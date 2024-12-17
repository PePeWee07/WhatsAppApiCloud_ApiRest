package com.BackEnd.WhatsappApiCloud.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final ApiKeyFilter apiKeyFilter;

    public SecurityConfig(ApiKeyFilter apiKeyFilter) {
        this.apiKeyFilter = apiKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Deshabilitar CSRF para APIs REST
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class) // Agregar el filtro antes de UsernamePasswordAuthenticationFilter
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated() // Proteger todos los endpoints
            );

        return http.build();
    }
}
