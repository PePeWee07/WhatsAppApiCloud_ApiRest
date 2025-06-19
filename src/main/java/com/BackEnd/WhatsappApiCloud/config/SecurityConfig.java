package com.BackEnd.WhatsappApiCloud.config;

import java.util.*;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import com.BackEnd.WhatsappApiCloud.config.security.RestAccessDeniedHandler;
import com.BackEnd.WhatsappApiCloud.config.security.RestAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    private final ApiKeyFilter apiKeyFilter;
    private final RestAuthenticationEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(
      ApiKeyFilter apiKeyFilter,
      RestAuthenticationEntryPoint authEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler
    ) {
        this.apiKeyFilter = apiKeyFilter;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    // ======================================================
    //   Configuración de seguridad
    // ======================================================
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex
              .authenticationEntryPoint(authEntryPoint)
              .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class); // tu filtro de API key

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // acepta cualquier puerto en localhost
        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("x-api-key","Content-Type","Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
