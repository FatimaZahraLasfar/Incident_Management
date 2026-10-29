package com.example.incidents.service;

import com.example.incidents.dto.admin.StatisticsResponse;
import com.example.incidents.enums.Category;
import com.example.incidents.enums.Priority;
import com.example.incidents.enums.Role;
import com.example.incidents.enums.Status;
import com.example.incidents.repository.IncidentRepository;
import com.example.incidents.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsService {

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;

    public StatisticsService(UserRepository userRepository, IncidentRepository incidentRepository) {
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        return new StatisticsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.EMPLOYEE),
                userRepository.countByRole(Role.TECHNICIAN),
                userRepository.countByRole(Role.ADMIN),
                incidentRepository.count(),
                incidentRepository.countByStatus(Status.OPEN),
                incidentRepository.countByStatus(Status.ASSIGNED),
                incidentRepository.countByStatus(Status.IN_PROGRESS),
                incidentRepository.countByStatus(Status.RESOLVED),
                incidentRepository.countByStatus(Status.CLOSED),
                toMap(Category.class, incidentRepository.countGroupedByCategory()),
                toMap(Priority.class, incidentRepository.countGroupedByPriority()));
    }

    /** Toutes les valeurs de l'énumération sont présentes, avec 0 si aucun incident. */
    private <E extends Enum<E>> Map<String, Long> toMap(Class<E> type, List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (E value : type.getEnumConstants()) {
            result.put(value.name(), 0L);
        }
        for (Object[] row : rows) {
            result.put(((Enum<?>) row[0]).name(), ((Number) row[1]).longValue());
        }
        return result;
    }
}
