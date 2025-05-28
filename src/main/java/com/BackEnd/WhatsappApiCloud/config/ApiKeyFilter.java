package com.BackEnd.WhatsappApiCloud.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.BackEnd.WhatsappApiCloud.model.entity.whatsapp.MessageBody;
import com.BackEnd.WhatsappApiCloud.service.whatsappApiCloud.ApiWhatsappService;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyFilter.class);

    @Value("${api.key}")
    private String apiKey;

    @Value("${api.key.header}")
    private String API_KEY_HEADER;

    private String PHONE_NUMBER;

    @Autowired
    ApiWhatsappService apiWhatsappService;

    // ======================================================
    //   Obtener el número de teléfono del usuario
    // ======================================================
    public String getPhoneNumber(String phone) {
        return PHONE_NUMBER = phone;
    }


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
        
        try {
            String receivedApiKey = request.getHeader(API_KEY_HEADER);

            if (receivedApiKey == null || !receivedApiKey.equals(apiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Prohibido: Clave API no valida\"}");
                return;
            }

            PreAuthenticatedAuthenticationToken authentication =
                new PreAuthenticatedAuthenticationToken("ForWebHook", null, null);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error durante la validacion de la clave API", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            MessageBody messageBody = new MessageBody(PHONE_NUMBER, "Lo sentimos, ocurrió un problema al procesar su solicitud. Por favor, inténtelo más tarde.");
            apiWhatsappService.sendMessage(messageBody);
        }
    }
}
