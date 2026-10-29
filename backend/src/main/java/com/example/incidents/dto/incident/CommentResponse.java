package com.example.incidents.dto.incident;

import com.example.incidents.dto.UserSummary;

import java.time.Instant;

public record CommentResponse(Long id, String content, UserSummary author, Instant createdAt) {
}
