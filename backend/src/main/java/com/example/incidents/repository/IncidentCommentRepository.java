package com.example.incidents.repository;

import com.example.incidents.entity.IncidentComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {

    @EntityGraph(attributePaths = "author")
    List<IncidentComment> findByIncidentIdOrderByIdAsc(Long incidentId);
}
