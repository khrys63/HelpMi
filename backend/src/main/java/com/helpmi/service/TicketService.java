package com.helpmi.service;

import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.CreateTicketRequest;
import com.helpmi.dto.request.UpdateTicketRequest;
import com.helpmi.dto.response.AttachmentResponse;
import com.helpmi.dto.response.ChangeStatusResponse;
import com.helpmi.dto.response.CommentResponse;
import com.helpmi.dto.response.LabelResponse;
import com.helpmi.dto.response.OrganizationSummary;
import com.helpmi.dto.response.TicketDetailResponse;
import com.helpmi.dto.response.TicketLinkResponse;
import com.helpmi.dto.response.TicketResponse;
import com.helpmi.dto.response.TicketSummary;
import com.helpmi.dto.response.UserSummary;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.AttachmentRepository;
import com.helpmi.repository.CommentRepository;
import com.helpmi.repository.OrganizationRepository;
import com.helpmi.repository.LabelRepository;
import com.helpmi.repository.TicketLinkRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.repository.UserRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final TicketLinkRepository ticketLinkRepository;
    private final OrganizationRepository organizationRepository;
    private final LabelRepository labelRepository;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<TicketResponse> getTickets(UUID projectId, String status,
            String priority, String type, UUID assigneeId, Pageable pageable) {
        projectService.requireProjectAccess(projectId);
        List<String> statuses   = parseFilter(status);
        List<String> priorities = parseFilter(priority);
        List<String> types      = parseFilter(type);
        return ticketRepository.findByProjectIdWithFilters(
                projectId,
                statuses,   statuses.size(),
                priorities, priorities.size(),
                types,      types.size(),
                assigneeId, pageable)
                .map(this::toResponse);
    }

    private static final int MAX_FILTER_VALUES = 20;

    private List<String> parseFilter(String param) {
        if (param == null || param.isBlank()) return List.of();
        String[] parts = param.split(",");
        if (parts.length > MAX_FILTER_VALUES)
            throw new IllegalArgumentException("Trop de valeurs de filtre (max " + MAX_FILTER_VALUES + ")");
        return Arrays.asList(parts);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicket(UUID projectId, UUID ticketId) {
        Ticket ticket = findTicket(projectId, ticketId);
        var comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(c -> new CommentResponse(c.getId(), ticketId, UserSummary.from(c.getAuthor()),
                        c.getBody(), c.isEdited(), c.getCreatedAt(), c.getUpdatedAt()))
                .toList();
        var attachments = attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticketId).stream()
                .map(a -> new AttachmentResponse(a.getId(), ticketId, a.getFileName(), a.getContentType(),
                        a.getSize(), UserSummary.from(a.getUploadedBy()), a.getUploadedAt(),
                        "/api/attachments/" + a.getId()))
                .toList();
        var outgoing = ticketLinkRepository.findBySourceTicketId(ticketId).stream()
                .map(l -> new TicketLinkResponse(l.getId(), TicketSummary.from(l.getTargetTicket()), l.getLinkType(), "OUTGOING"))
                .toList();
        var incoming = ticketLinkRepository.findByTargetTicketId(ticketId).stream()
                .map(l -> new TicketLinkResponse(l.getId(), TicketSummary.from(l.getSourceTicket()), l.getLinkType(), "INCOMING"))
                .toList();
        var links = new ArrayList<TicketLinkResponse>(outgoing);
        links.addAll(incoming);
        var organizations = ticket.getOrganizations().stream()
                .map(OrganizationSummary::from)
                .sorted(Comparator.comparing(OrganizationSummary::name))
                .toList();
        var labels = ticket.getLabels().stream()
                .map(LabelResponse::from)
                .sorted(Comparator.comparing(LabelResponse::name))
                .toList();
        var watchers = ticket.getWatchers().stream()
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .map(UserSummary::from)
                .toList();
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        boolean isGestionnaire = projectService.isGestionnaire(currentUser.getId(), projectId);
        boolean canAssign = isAdmin || isGestionnaire;
        boolean canClone = isAdmin || isGestionnaire
                || (ticket.getReporter() != null && ticket.getReporter().getId().equals(currentUser.getId()))
                || (ticket.getAssignee() != null && ticket.getAssignee().getId().equals(currentUser.getId()));
        return new TicketDetailResponse(ticket.getId(), ticket.getReference(), ticket.getTitle(),
                ticket.getDescription(), ticket.getStatus(), ticket.getPriority(), ticket.getType(),
                ticket.getDueDate(),
                ticket.getProject().getId(), ticket.getProject().getName(), ticket.getProject().getKey(),
                UserSummary.from(ticket.getReporter()), UserSummary.from(ticket.getAssignee()),
                comments, attachments, links, organizations, labels, watchers,
                ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getClosedAt(), canAssign, canClone);
    }

    public TicketResponse createTicket(UUID projectId, CreateTicketRequest req) {
        projectService.requireProjectAccess(projectId);
        User currentUser = currentUserService.getCurrentUser();
        var project = projectService.findActive(projectId);
        User assignee = null;
        if (req.assigneeId() != null) {
            validateAssignee(req.assigneeId(), projectId);
            assignee = userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new NotFoundException("Assigné introuvable"));
        }
        String reference = projectService.generateTicketReference(projectId);
        Ticket ticket = Ticket.builder()
                .reference(reference)
                .title(req.title())
                .description(req.description())
                .priority(req.priority() != null ? req.priority() : "MEDIUM")
                .type(req.type() != null ? req.type() : "TASK")
                .dueDate(req.dueDate())
                .project(project)
                .reporter(currentUser)
                .assignee(assignee)
                .build();
        Ticket saved = ticketRepository.save(ticket);
        ticketHistoryService.record(saved, "created", null, saved.getReference());
        saved.getProject().getId();
        notificationService.notifyTicketCreated(projectId, saved, currentUser);
        return toResponse(saved);
    }

    public TicketResponse updateTicket(UUID projectId, UUID ticketId, UpdateTicketRequest req) {
        Ticket ticket = findTicket(projectId, ticketId);
        requireEditable(ticket);
        User currentUser = currentUserService.getCurrentUser();

        String oldTitle    = ticket.getTitle();
        String oldDesc     = ticket.getDescription();
        String oldPriority = ticket.getPriority();
        String oldType     = ticket.getType();
        String oldAssignee = userName(ticket.getAssignee());

        if (req.title() != null) ticket.setTitle(req.title());
        if (req.description() != null) ticket.setDescription(req.description());
        if (req.priority() != null) ticket.setPriority(req.priority());
        if (req.type() != null) ticket.setType(req.type());
        if (req.assigneeId() != null) {
            if (currentUser.getRole() != UserRole.ADMIN && !projectService.isGestionnaire(currentUser.getId(), projectId)) {
                throw new ForbiddenException("Seuls les gestionnaires peuvent modifier l'assigné");
            }
            validateAssignee(req.assigneeId(), projectId);
            ticket.setAssignee(userRepository.findById(req.assigneeId())
                    .orElseThrow(() -> new NotFoundException("Assigné introuvable")));
        }

        Ticket saved = ticketRepository.save(ticket);
        ticketHistoryService.record(saved, "title",       oldTitle,    saved.getTitle());
        ticketHistoryService.record(saved, "description", oldDesc,     saved.getDescription());
        ticketHistoryService.record(saved, "priority",    oldPriority, saved.getPriority());
        ticketHistoryService.record(saved, "type",        oldType,     saved.getType());
        ticketHistoryService.record(saved, "assignee",    oldAssignee, userName(saved.getAssignee()));
        return toResponse(saved);
    }

    public ChangeStatusResponse changeStatus(UUID projectId, UUID ticketId, String newStatus) {
        Ticket ticket = findTicket(projectId, ticketId);
        User actor = currentUserService.getCurrentUser();
        if (!"OPEN".equals(newStatus)) requireEditable(ticket);
        String oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        boolean closing = List.of("CLOSED", "RESOLVED", "CANCELLED").contains(newStatus);
        ticket.setClosedAt(closing ? LocalDateTime.now() : null);
        Ticket saved = ticketRepository.save(ticket);
        ticketHistoryService.record(saved, "status", oldStatus, newStatus);
        if (saved.getReporter() != null) saved.getReporter().getId();
        if (saved.getAssignee() != null) saved.getAssignee().getId();
        saved.getWatchers().size();
        saved.getProject().getId();
        notificationService.notifyStatusChanged(saved, oldStatus, newStatus, actor);
        TicketResponse response = toResponse(saved);
        UUID nextTicketId = null;
        String nextTicketReference = null;
        if ("CLOSED".equals(newStatus) && RECURRING_TYPES.contains(ticket.getType())) {
            Ticket clone = autoCloneRecurring(ticket);
            nextTicketId = clone.getId();
            nextTicketReference = clone.getReference();
        }
        return new ChangeStatusResponse(response, nextTicketId, nextTicketReference);
    }

    private static final java.util.Set<String> RECURRING_TYPES = java.util.Set.of("ANNUEL", "MENSUEL", "TRIMESTRIEL");

    private Ticket autoCloneRecurring(Ticket source) {
        String reference = projectService.generateTicketReference(source.getProject().getId());
        LocalDate nextDueDate = null;
        if (source.getDueDate() != null) {
            nextDueDate = switch (source.getType()) {
                case "ANNUEL"       -> source.getDueDate().plusYears(1);
                case "TRIMESTRIEL"  -> source.getDueDate().plusMonths(3);
                case "MENSUEL"      -> source.getDueDate().plusMonths(1);
                default             -> null;
            };
        }
        Ticket clone = Ticket.builder()
                .reference(reference)
                .title(source.getTitle())
                .description(source.getDescription())
                .priority(source.getPriority())
                .type(source.getType())
                .dueDate(nextDueDate)
                .project(source.getProject())
                .reporter(source.getReporter())
                .assignee(source.getAssignee())
                .build();
        clone.getOrganizations().addAll(source.getOrganizations());
        clone.getLabels().addAll(source.getLabels());
        return ticketRepository.save(clone);
    }

    public TicketResponse cloneTicket(UUID projectId, UUID ticketId) {
        Ticket source = findTicket(projectId, ticketId);
        User currentUser = currentUserService.getCurrentUser();
        requireCanClone(currentUser, source);
        String reference = projectService.generateTicketReference(projectId);
        Ticket clone = Ticket.builder()
                .reference(reference)
                .title("[Copie] " + source.getTitle())
                .description(source.getDescription())
                .priority(source.getPriority())
                .type(source.getType())
                .dueDate(source.getDueDate())
                .project(source.getProject())
                .reporter(currentUser)
                .assignee(source.getAssignee())
                .build();
        clone.getOrganizations().addAll(source.getOrganizations());
        clone.getLabels().addAll(source.getLabels());
        return toResponse(ticketRepository.save(clone));
    }

    public TicketResponse moveTicket(UUID projectId, UUID ticketId, UUID targetProjectId) {
        if (projectId.equals(targetProjectId)) {
            throw new IllegalArgumentException("Le ticket est déjà dans ce projet");
        }
        Ticket ticket = findTicket(projectId, ticketId);
        requireEditable(ticket);
        requireCanModify(currentUserService.getCurrentUser(), ticket);
        projectService.requireProjectAccess(targetProjectId);
        String oldProject = ticket.getProject().getKey();
        var targetProject = projectService.findActive(targetProjectId);
        String newReference = projectService.generateTicketReference(targetProjectId);
        ticket.setProject(targetProject);
        ticket.setReference(newReference);
        Ticket saved = ticketRepository.save(ticket);
        ticketHistoryService.record(saved, "project", oldProject, targetProject.getKey());
        return toResponse(saved);
    }

    public TicketResponse setDueDate(UUID projectId, UUID ticketId, LocalDate dueDate) {
        Ticket ticket = findTicket(projectId, ticketId);
        requireEditable(ticket);
        String oldDate = ticket.getDueDate() != null ? ticket.getDueDate().toString() : null;
        String newDate = dueDate != null ? dueDate.toString() : null;
        ticket.setDueDate(dueDate);
        Ticket saved = ticketRepository.save(ticket);
        ticketHistoryService.record(saved, "dueDate", oldDate, newDate);
        return toResponse(saved);
    }

    public void deleteTicket(UUID projectId, UUID ticketId) {
        if (currentUserService.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Seuls les administrateurs peuvent supprimer des tickets");
        }
        ticketRepository.delete(findTicket(projectId, ticketId));
    }

    public List<OrganizationSummary> setOrganizations(UUID projectId, UUID ticketId, List<UUID> orgIds) {
        Ticket ticket = findTicket(projectId, ticketId);
        requireEditable(ticket);
        String oldOrgs = collectionNames(ticket.getOrganizations().stream()
                .map(o -> o.getName()).sorted().toList());
        var newOrgs = orgIds == null || orgIds.isEmpty()
                ? new HashSet<com.helpmi.domain.Organization>()
                : new HashSet<>(organizationRepository.findAllById(orgIds));
        ticket.getOrganizations().clear();
        ticket.getOrganizations().addAll(newOrgs);
        ticketRepository.save(ticket);
        String newOrgsStr = collectionNames(ticket.getOrganizations().stream()
                .map(o -> o.getName()).sorted().toList());
        ticketHistoryService.record(ticket, "organizations", oldOrgs, newOrgsStr);
        return ticket.getOrganizations().stream()
                .map(OrganizationSummary::from)
                .sorted(Comparator.comparing(OrganizationSummary::name))
                .toList();
    }

    public List<LabelResponse> setLabels(UUID projectId, UUID ticketId, List<UUID> labelIds) {
        Ticket ticket = findTicket(projectId, ticketId);
        requireEditable(ticket);
        String oldLabels = collectionNames(ticket.getLabels().stream()
                .map(l -> l.getName()).sorted().toList());
        var newLabels = labelIds == null || labelIds.isEmpty()
                ? new HashSet<com.helpmi.domain.Label>()
                : new HashSet<>(labelRepository.findAllById(labelIds));
        ticket.getLabels().clear();
        ticket.getLabels().addAll(newLabels);
        ticketRepository.save(ticket);
        String newLabelsStr = collectionNames(ticket.getLabels().stream()
                .map(l -> l.getName()).sorted().toList());
        ticketHistoryService.record(ticket, "labels", oldLabels, newLabelsStr);
        return ticket.getLabels().stream()
                .map(LabelResponse::from)
                .sorted(Comparator.comparing(LabelResponse::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getEligibleWatchers(UUID projectId, UUID ticketId) {
        Ticket ticket = findTicket(projectId, ticketId);
        return eligibleWatcherUsers(ticket, projectId).stream()
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .map(UserSummary::from)
                .toList();
    }

    public List<UserSummary> setWatchers(UUID projectId, UUID ticketId, List<UUID> userIds) {
        Ticket ticket = findTicket(projectId, ticketId);
        requireEditable(ticket);
        User actor = currentUserService.getCurrentUser();
        Set<UUID> previousWatcherIds = ticket.getWatchers().stream()
                .map(User::getId).collect(Collectors.toSet());
        String old = collectionNames(ticket.getWatchers().stream()
                .map(u -> u.getFirstName() + " " + u.getLastName()).sorted().toList());
        Set<User> newWatchers;
        if (userIds == null || userIds.isEmpty()) {
            newWatchers = new HashSet<>();
        } else {
            Set<UUID> eligibleIds = eligibleWatcherUsers(ticket, projectId).stream()
                    .map(User::getId).collect(Collectors.toSet());
            boolean anyIneligible = userIds.stream().anyMatch(id -> !eligibleIds.contains(id));
            if (anyIneligible) {
                throw new ForbiddenException("Certains utilisateurs ne sont pas éligibles pour ce ticket");
            }
            newWatchers = new HashSet<>(userRepository.findAllById(userIds));
        }
        ticket.getWatchers().clear();
        ticket.getWatchers().addAll(newWatchers);
        ticketRepository.save(ticket);
        String updated = collectionNames(ticket.getWatchers().stream()
                .map(u -> u.getFirstName() + " " + u.getLastName()).sorted().toList());
        ticketHistoryService.record(ticket, "watchers", old, updated);
        Set<User> added = ticket.getWatchers().stream()
                .filter(u -> !previousWatcherIds.contains(u.getId()))
                .collect(Collectors.toSet());
        if (!added.isEmpty()) {
            ticket.getProject().getId();
            notificationService.notifyWatcherAdded(ticket, added, actor);
        }
        return ticket.getWatchers().stream()
                .sorted(Comparator.comparing(User::getFirstName).thenComparing(User::getLastName))
                .map(UserSummary::from)
                .toList();
    }

    private Set<User> eligibleWatcherUsers(Ticket ticket, UUID projectId) {
        Set<User> eligible = new LinkedHashSet<>(userRepository.findAssignableByProjectId(projectId));
        Set<UUID> orgIds = ticket.getOrganizations().stream()
                .map(o -> o.getId()).collect(Collectors.toSet());
        if (!orgIds.isEmpty()) {
            eligible.addAll(userRepository.findByOrganizationIds(orgIds));
        }
        return eligible;
    }

    public TicketResponse setAssignee(UUID projectId, UUID ticketId, UUID assigneeId) {
        Ticket ticket = findTicket(projectId, ticketId);
        requireEditable(ticket);
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN && !projectService.isGestionnaire(currentUser.getId(), projectId)) {
            throw new ForbiddenException("Seuls les gestionnaires peuvent modifier l'assigné");
        }
        User oldAssigneeUser = ticket.getAssignee();
        String oldAssignee = userName(oldAssigneeUser);
        if (assigneeId == null) {
            ticket.setAssignee(null);
        } else {
            validateAssignee(assigneeId, projectId);
            ticket.setAssignee(userRepository.findById(assigneeId)
                    .orElseThrow(() -> new NotFoundException("Assigné introuvable")));
        }
        Ticket saved = ticketRepository.save(ticket);
        ticketHistoryService.record(saved, "assignee", oldAssignee, userName(saved.getAssignee()));
        if (saved.getAssignee() != null
                && (oldAssigneeUser == null || !oldAssigneeUser.getId().equals(saved.getAssignee().getId()))) {
            saved.getProject().getId();
            saved.getWatchers().size();
            notificationService.notifyAssigned(saved, saved.getAssignee(), currentUser);
        }
        return toResponse(saved);
    }

    private void validateAssignee(UUID assigneeId, UUID projectId) {
        if (!userRepository.isAssignableToProject(assigneeId, projectId)) {
            throw new IllegalArgumentException("Cet utilisateur n'est pas assignable à ce projet");
        }
    }

    private static void requireEditable(Ticket ticket) {
        if ("CLOSED".equals(ticket.getStatus()) || "CANCELLED".equals(ticket.getStatus())) {
            throw new ForbiddenException("Ce ticket est clôturé et ne peut plus être modifié");
        }
    }

    private void requireCanModify(User user, Ticket ticket) {
        if (!canActOnTicket(user, ticket)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à modifier ce ticket");
        }
    }

    private void requireCanClone(User user, Ticket ticket) {
        if (!canActOnTicket(user, ticket)) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à cloner ce ticket");
        }
    }

    private boolean canActOnTicket(User user, Ticket ticket) {
        if (user.getRole() == UserRole.ADMIN) return true;
        boolean isGestionnaire = projectService.isGestionnaire(user.getId(), ticket.getProject().getId());
        boolean isReporter = ticket.getReporter() != null && ticket.getReporter().getId().equals(user.getId());
        boolean isAssignee = ticket.getAssignee() != null && ticket.getAssignee().getId().equals(user.getId());
        return isGestionnaire || isReporter || isAssignee;
    }

    private Ticket findTicket(UUID projectId, UUID ticketId) {
        projectService.requireProjectAccess(projectId);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket introuvable"));
        if (!ticket.getProject().getId().equals(projectId)) {
            throw new NotFoundException("Ticket introuvable dans ce projet");
        }
        return ticket;
    }

    private static String userName(User user) {
        if (user == null) return null;
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private static String collectionNames(List<String> names) {
        if (names == null || names.isEmpty()) return null;
        return String.join(", ", names);
    }

    private TicketResponse toResponse(Ticket t) {
        return new TicketResponse(t.getId(), t.getReference(), t.getTitle(), t.getStatus(), t.getPriority(),
                t.getType(), t.getDueDate(), t.getProject().getId(), t.getProject().getKey(),
                UserSummary.from(t.getReporter()), UserSummary.from(t.getAssignee()),
                t.getCreatedAt(), t.getUpdatedAt(), t.getClosedAt());
    }
}
