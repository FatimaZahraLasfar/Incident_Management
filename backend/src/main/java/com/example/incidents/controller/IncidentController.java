package com.example.incidents.controller;

import com.example.incidents.dto.incident.CommentRequest;
import com.example.incidents.dto.incident.CommentResponse;
import com.example.incidents.dto.incident.IncidentCreateRequest;
import com.example.incidents.dto.incident.IncidentDetailResponse;
import com.example.incidents.dto.incident.IncidentSummaryResponse;
import com.example.incidents.dto.incident.StatusUpdateRequest;
import com.example.incidents.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Le rôle est vérifié ici (@PreAuthorize) ; le droit d'accéder à CET incident (propriétaire, technicien
 * affecté) est vérifié dans IncidentService.
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<IncidentDetailResponse> create(@Valid @RequestBody IncidentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.create(request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<IncidentSummaryResponse> myIncidents() {
        return incidentService.listMine();
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public List<IncidentSummaryResponse> assignedIncidents() {
        return incidentService.listAssigned();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<IncidentSummaryResponse> allIncidents() {
        return incidentService.listAll();
    }

    @GetMapping("/{id}")
    public IncidentDetailResponse getById(@PathVariable Long id) {
        return incidentService.getById(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public IncidentDetailResponse updateStatus(@PathVariable Long id,
                                               @Valid @RequestBody StatusUpdateRequest request) {
        return incidentService.updateStatus(id, request.status());
    }

    @PutMapping("/{id}/assign/{technicianId}")
    @PreAuthorize("hasRole('ADMIN')")
    public IncidentDetailResponse assign(@PathVariable Long id, @PathVariable Long technicianId) {
        return incidentService.assign(id, technicianId);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long id,
                                                      @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.addComment(id, request.content()));
    }
}
