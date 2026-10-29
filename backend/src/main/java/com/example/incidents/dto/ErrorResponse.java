package com.example.incidents.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Format d'erreur unique de l'API : {"status": 404, "message": "..."} (+ "errors" pour la validation). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String message, Map<String, String> errors) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, null);
    }
}
