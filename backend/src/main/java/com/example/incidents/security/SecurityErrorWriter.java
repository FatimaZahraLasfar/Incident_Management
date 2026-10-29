package com.example.incidents.security;

import com.example.incidents.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Écrit l'erreur JSON {status, message} pour les refus produits par les filtres Spring Security. */
final class SecurityErrorWriter {

    private SecurityErrorWriter() { }

    static void write(ObjectMapper objectMapper, HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ErrorResponse.of(status, message));
    }
}
