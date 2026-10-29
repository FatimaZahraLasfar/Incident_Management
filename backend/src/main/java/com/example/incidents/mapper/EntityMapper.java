package com.example.incidents.mapper;

import com.example.incidents.dto.UserResponse;
import com.example.incidents.dto.UserSummary;
import com.example.incidents.dto.incident.CommentResponse;
import com.example.incidents.dto.incident.HistoryEntryResponse;
import com.example.incidents.dto.incident.IncidentDetailResponse;
import com.example.incidents.dto.incident.IncidentSummaryResponse;
import com.example.incidents.entity.Incident;
import com.example.incidents.entity.IncidentComment;
import com.example.incidents.entity.IncidentHistory;
import com.example.incidents.entity.User;

import java.util.List;

/** Conversion entités → DTO. Les entités ne sont jamais exposées directement (pas de hash de mot de passe). */
public final class EntityMapper {

    private EntityMapper() { }

    public static UserResponse toUserResponse(User u) {
        return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }

    public static UserSummary toUserSummary(User u) {
        return u == null ? null : new UserSummary(u.getId(), u.getFullName());
    }

    public static IncidentSummaryResponse toSummary(Incident i) {
        return new IncidentSummaryResponse(i.getId(), i.getTitle(), i.getCategory(), i.getPriority(), i.getStatus(),
                i.getCreatedAt(), i.getUpdatedAt(), toUserSummary(i.getCreatedBy()), toUserSummary(i.getAssignedTo()));
    }

    public static HistoryEntryResponse toHistory(IncidentHistory h) {
        return new HistoryEntryResponse(h.getId(), h.getActionType(), h.getOldValue(), h.getNewValue(),
                h.getMessage(), toUserSummary(h.getPerformedBy()), h.getCreatedAt());
    }

    public static CommentResponse toComment(IncidentComment c) {
        return new CommentResponse(c.getId(), c.getContent(), toUserSummary(c.getAuthor()), c.getCreatedAt());
    }

    public static IncidentDetailResponse toDetail(Incident i, List<IncidentHistory> history,
                                                  List<IncidentComment> comments) {
        return new IncidentDetailResponse(i.getId(), i.getTitle(), i.getDescription(), i.getCategory(),
                i.getPriority(), i.getStatus(), i.getCreatedAt(), i.getUpdatedAt(),
                toUserSummary(i.getCreatedBy()), toUserSummary(i.getAssignedTo()),
                history.stream().map(EntityMapper::toHistory).toList(),
                comments.stream().map(EntityMapper::toComment).toList());
    }
}
