package com.example.incidents.service;

import com.example.incidents.dto.incident.CommentResponse;
import com.example.incidents.dto.incident.IncidentCreateRequest;
import com.example.incidents.dto.incident.IncidentDetailResponse;
import com.example.incidents.dto.incident.IncidentSummaryResponse;
import com.example.incidents.entity.Incident;
import com.example.incidents.entity.IncidentComment;
import com.example.incidents.entity.IncidentHistory;
import com.example.incidents.entity.User;
import com.example.incidents.enums.HistoryAction;
import com.example.incidents.enums.Role;
import com.example.incidents.enums.Status;
import com.example.incidents.exception.BadRequestException;
import com.example.incidents.exception.ForbiddenException;
import com.example.incidents.exception.ResourceNotFoundException;
import com.example.incidents.mapper.EntityMapper;
import com.example.incidents.repository.IncidentCommentRepository;
import com.example.incidents.repository.IncidentHistoryRepository;
import com.example.incidents.repository.IncidentRepository;
import com.example.incidents.repository.UserRepository;
import com.example.incidents.security.CurrentUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Règles métier des incidents. Les contrôles d'accès (propriétaire, technicien affecté) sont faits ici,
 * côté serveur, indépendamment de ce que React affiche.
 *
 * Cycle de vie :
 *   OPEN --affectation (ADMIN)--> ASSIGNED --> IN_PROGRESS --> RESOLVED --(ADMIN)--> CLOSED
 *   RESOLVED --> IN_PROGRESS (reprise) ; CLOSED --(ADMIN uniquement)--> IN_PROGRESS (réouverture, historisée)
 */
@Service
public class IncidentService {

    private static final Map<Status, Set<Status>> ALLOWED_TRANSITIONS = Map.of(
            Status.OPEN, EnumSet.of(Status.IN_PROGRESS),
            Status.ASSIGNED, EnumSet.of(Status.IN_PROGRESS),
            Status.IN_PROGRESS, EnumSet.of(Status.RESOLVED),
            Status.RESOLVED, EnumSet.of(Status.IN_PROGRESS, Status.CLOSED),
            Status.CLOSED, EnumSet.of(Status.IN_PROGRESS));

    private final IncidentRepository incidentRepository;
    private final IncidentHistoryRepository historyRepository;
    private final IncidentCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public IncidentService(IncidentRepository incidentRepository, IncidentHistoryRepository historyRepository,
                           IncidentCommentRepository commentRepository, UserRepository userRepository,
                           CurrentUserService currentUserService) {
        this.incidentRepository = incidentRepository;
        this.historyRepository = historyRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    // ---------------------------------------------------------------- création et consultation

    @Transactional
    public IncidentDetailResponse create(IncidentCreateRequest request) {
        User me = currentUserService.getCurrentUser();
        if (me.getRole() != Role.EMPLOYEE) {
            throw new ForbiddenException("Seul un employé peut déclarer un incident");
        }
        Incident incident = new Incident();
        incident.setTitle(request.title().trim());
        incident.setDescription(request.description().trim());
        incident.setCategory(request.category());
        incident.setPriority(request.priority());
        incident.setStatus(Status.OPEN);
        incident.setCreatedBy(me);
        incident = incidentRepository.save(incident);
        record(incident, HistoryAction.CREATED, null, Status.OPEN.name(), "Incident créé", me);
        return toDetail(incident, me);
    }

    @Transactional(readOnly = true)
    public List<IncidentSummaryResponse> listMine() {
        User me = currentUserService.getCurrentUser();
        return incidentRepository.findByCreatedByIdOrderByCreatedAtDesc(me.getId())
                .stream().map(EntityMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentSummaryResponse> listAssigned() {
        User me = currentUserService.getCurrentUser();
        return incidentRepository.findByAssignedToIdOrderByUpdatedAtDesc(me.getId())
                .stream().map(EntityMapper::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<IncidentSummaryResponse> listAll() {
        return incidentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(EntityMapper::toSummary).toList();
    }

    /** 404 si l'incident n'existe pas, 403 s'il existe mais n'est pas accessible à l'utilisateur. */
    @Transactional(readOnly = true)
    public IncidentDetailResponse getById(Long id) {
        User me = currentUserService.getCurrentUser();
        Incident incident = findIncident(id);
        assertCanView(incident, me);
        return toDetail(incident, me);
    }

    // ---------------------------------------------------------------- statut

    @Transactional
    public IncidentDetailResponse updateStatus(Long id, Status target) {
        User me = currentUserService.getCurrentUser();
        Incident incident = findIncident(id);
        boolean admin = me.getRole() == Role.ADMIN;

        if (!admin && !(me.getRole() == Role.TECHNICIAN && isAssignedTo(incident, me))) {
            throw new ForbiddenException("Cet incident ne vous est pas affecté");
        }
        Status current = incident.getStatus();
        if (current == Status.CLOSED && !admin) {
            throw new ForbiddenException("Incident clôturé : seul un administrateur peut le modifier");
        }
        if (target == Status.CLOSED && !admin) {
            throw new ForbiddenException("Seul un administrateur peut clôturer un incident");
        }
        if (current == target) {
            throw new BadRequestException("L'incident a déjà le statut " + target);
        }
        if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
            throw new BadRequestException("Transition de statut non autorisée : " + current + " -> " + target
                    + (target == Status.ASSIGNED ? " (utilisez l'affectation)" : ""));
        }
        if (target == Status.IN_PROGRESS && incident.getAssignedTo() == null) {
            throw new BadRequestException("Affectez d'abord un technicien à cet incident");
        }

        incident.setStatus(target);
        incidentRepository.saveAndFlush(incident);
        String message = current == Status.CLOSED
                ? "Incident rouvert : " + current + " -> " + target
                : current + " -> " + target;
        record(incident, HistoryAction.STATUS_CHANGED, current.name(), target.name(), message, me);
        return toDetail(incident, me);
    }

    // ---------------------------------------------------------------- affectation

    @Transactional
    public IncidentDetailResponse assign(Long incidentId, Long technicianId) {
        User me = currentUserService.getCurrentUser();
        if (me.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Seul un administrateur peut affecter un incident");
        }
        Incident incident = findIncident(incidentId);
        if (incident.getStatus() == Status.CLOSED) {
            throw new BadRequestException("Incident clôturé : impossible de l'affecter");
        }
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        if (technician.getRole() != Role.TECHNICIAN) {
            throw new BadRequestException("L'utilisateur sélectionné n'est pas un technicien");
        }
        if (!technician.isEnabled()) {
            throw new BadRequestException("Le technicien sélectionné est désactivé");
        }
        User previous = incident.getAssignedTo();
        if (previous != null && previous.getId().equals(technician.getId())) {
            throw new BadRequestException("L'incident est déjà affecté à ce technicien");
        }

        incident.setAssignedTo(technician);
        String message = previous == null
                ? "Affecté à " + technician.getFullName()
                : "Réaffecté de " + previous.getFullName() + " à " + technician.getFullName();
        record(incident, HistoryAction.ASSIGNED, previous == null ? null : previous.getFullName(),
                technician.getFullName(), message, me);

        if (incident.getStatus() == Status.OPEN) {
            incident.setStatus(Status.ASSIGNED);
            record(incident, HistoryAction.STATUS_CHANGED, Status.OPEN.name(), Status.ASSIGNED.name(),
                    Status.OPEN + " -> " + Status.ASSIGNED, me);
        }
        incidentRepository.saveAndFlush(incident);
        return toDetail(incident, me);
    }

    // ---------------------------------------------------------------- notes de traitement

    @Transactional
    public CommentResponse addComment(Long incidentId, String content) {
        User me = currentUserService.getCurrentUser();
        Incident incident = findIncident(incidentId);
        boolean admin = me.getRole() == Role.ADMIN;

        if (!admin && !(me.getRole() == Role.TECHNICIAN && isAssignedTo(incident, me))) {
            throw new ForbiddenException("Cet incident ne vous est pas affecté");
        }
        if (incident.getStatus() == Status.CLOSED && !admin) {
            throw new ForbiddenException("Incident clôturé : seul un administrateur peut le modifier");
        }
        IncidentComment comment = commentRepository.save(new IncidentComment(incident, me, content.trim()));
        return EntityMapper.toComment(comment);
    }

    // ---------------------------------------------------------------- utilitaires

    private Incident findIncident(Long id) {
        return incidentRepository.findWithUsersById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident introuvable"));
    }

    private boolean isAssignedTo(Incident incident, User user) {
        return incident.getAssignedTo() != null && incident.getAssignedTo().getId().equals(user.getId());
    }

    /** ADMIN : tout ; EMPLOYEE : ses incidents ; TECHNICIAN : les incidents qui lui sont affectés. */
    private void assertCanView(Incident incident, User user) {
        boolean allowed = switch (user.getRole()) {
            case ADMIN -> true;
            case EMPLOYEE -> incident.getCreatedBy().getId().equals(user.getId());
            case TECHNICIAN -> isAssignedTo(incident, user);
        };
        if (!allowed) {
            throw new ForbiddenException("Accès interdit à cet incident");
        }
    }

    private void record(Incident incident, HistoryAction action, String oldValue, String newValue,
                        String message, User performedBy) {
        historyRepository.save(new IncidentHistory(incident, action, oldValue, newValue, message, performedBy));
    }

    /** Les notes de traitement sont internes : elles ne sont pas retournées à l'employé. */
    private IncidentDetailResponse toDetail(Incident incident, User viewer) {
        List<IncidentHistory> history = historyRepository.findByIncidentIdOrderByIdAsc(incident.getId());
        List<IncidentComment> comments = viewer.getRole() == Role.EMPLOYEE
                ? List.of()
                : commentRepository.findByIncidentIdOrderByIdAsc(incident.getId());
        return EntityMapper.toDetail(incident, history, comments);
    }
}
