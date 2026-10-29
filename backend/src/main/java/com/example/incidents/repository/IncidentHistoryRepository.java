package com.example.incidents.repository;

import com.example.incidents.entity.IncidentHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {

    @EntityGraph(attributePaths = "performedBy")
    List<IncidentHistory> findByIncidentIdOrderByIdAsc(Long incidentId);
}
