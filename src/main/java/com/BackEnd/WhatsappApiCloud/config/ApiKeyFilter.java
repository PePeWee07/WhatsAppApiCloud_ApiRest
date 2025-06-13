package com.BackEnd.WhatsappApiCloud.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.key.header}")
    private String API_KEY_HEADER;


    // ======================================================
    //   Validar la clave API
    // ======================================================
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if ("/api/health".equals(path) || "/api/health/".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String receivedApiKey = request.getHeader(API_KEY_HEADER);
        if (receivedApiKey == null || !receivedApiKey.equals(apiKey)) {
            throw new BadCredentialsException("API-Key inválida o faltante");
        }

        PreAuthenticatedAuthenticationToken auth = new PreAuthenticatedAuthenticationToken("ForWebHook", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        filterChain.doFilter(request, response);
    }
}
