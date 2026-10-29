package com.example.incidents.service;

import com.example.incidents.dto.admin.StatisticsResponse;
import com.example.incidents.dto.dashboard.DashboardResponse;
import com.example.incidents.dto.incident.IncidentSummaryResponse;
import com.example.incidents.entity.Incident;
import com.example.incidents.entity.User;
import com.example.incidents.enums.Priority;
import com.example.incidents.enums.Status;
import com.example.incidents.mapper.EntityMapper;
import com.example.incidents.repository.IncidentRepository;
import com.example.incidents.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tableau de bord adapté au rôle de l'utilisateur connecté (clés documentées dans DashboardResponse). */
@Service
public class DashboardService {

    private static final List<Status> DONE = List.of(Status.RESOLVED, Status.CLOSED);

    private final IncidentRepository incidentRepository;
    private final StatisticsService statisticsService;
    private final CurrentUserService currentUserService;

    public DashboardService(IncidentRepository incidentRepository, StatisticsService statisticsService,
                            CurrentUserService currentUserService) {
        this.incidentRepository = incidentRepository;
        this.statisticsService = statisticsService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        User me = currentUserService.getCurrentUser();
        Long id = me.getId();
        Map<String, Long> stats = new LinkedHashMap<>();
        List<Incident> recent;

        switch (me.getRole()) {
            case EMPLOYEE -> {
                stats.put("total", incidentRepository.countByCreatedById(id));
                stats.put("open", incidentRepository.countByCreatedByIdAndStatus(id, Status.OPEN));
                stats.put("assigned", incidentRepository.countByCreatedByIdAndStatus(id, Status.ASSIGNED));
                stats.put("inProgress", incidentRepository.countByCreatedByIdAndStatus(id, Status.IN_PROGRESS));
                stats.put("resolvedOrClosed", incidentRepository.countByCreatedByIdAndStatusIn(id, DONE));
                recent = incidentRepository.findTop5ByCreatedByIdOrderByCreatedAtDesc(id);
            }
            case TECHNICIAN -> {
                stats.put("assigned", incidentRepository.countByAssignedToId(id));
                stats.put("critical", incidentRepository.countByAssignedToIdAndPriorityAndStatusNotIn(
                        id, Priority.CRITICAL, DONE));
                stats.put("inProgress", incidentRepository.countByAssignedToIdAndStatus(id, Status.IN_PROGRESS));
                stats.put("resolved", incidentRepository.countByAssignedToIdAndStatusIn(id, DONE));
                recent = incidentRepository.findTop5ByAssignedToIdOrderByUpdatedAtDesc(id);
            }
            default -> {
                StatisticsResponse s = statisticsService.getStatistics();
                stats.put("totalUsers", s.totalUsers());
                stats.put("employees", s.employees());
                stats.put("technicians", s.technicians());
                stats.put("totalIncidents", s.totalIncidents());
                stats.put("open", s.open());
                stats.put("inProgress", s.inProgress());
                stats.put("closed", s.closed());
                recent = incidentRepository.findTop5ByOrderByCreatedAtDesc();
            }
        }

        List<IncidentSummaryResponse> recentIncidents = recent.stream().map(EntityMapper::toSummary).toList();
        return new DashboardResponse(me.getRole(), stats, recentIncidents);
    }
}
