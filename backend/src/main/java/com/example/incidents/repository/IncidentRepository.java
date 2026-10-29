package com.example.incidents.repository;

import com.example.incidents.entity.Incident;
import com.example.incidents.enums.Priority;
import com.example.incidents.enums.Status;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    Optional<Incident> findWithUsersById(Long id);

    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Incident> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Incident> findByAssignedToIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Incident> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Incident> findTop5ByCreatedByIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Incident> findTop5ByAssignedToIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Incident> findTop5ByOrderByCreatedAtDesc();

    long countByStatus(Status status);

    long countByCreatedById(Long userId);

    long countByCreatedByIdAndStatus(Long userId, Status status);

    long countByCreatedByIdAndStatusIn(Long userId, Collection<Status> statuses);

    long countByAssignedToId(Long userId);

    long countByAssignedToIdAndStatus(Long userId, Status status);

    long countByAssignedToIdAndStatusIn(Long userId, Collection<Status> statuses);

    long countByAssignedToIdAndPriorityAndStatusNotIn(Long userId, Priority priority, Collection<Status> statuses);

    @Query("select i.category, count(i) from Incident i group by i.category")
    List<Object[]> countGroupedByCategory();

    @Query("select i.priority, count(i) from Incident i group by i.priority")
    List<Object[]> countGroupedByPriority();
}
