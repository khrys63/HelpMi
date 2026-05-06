package com.helpmi.service;

import com.helpmi.domain.Ticket;
import com.helpmi.domain.TicketLink;
import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.request.CreateTicketLinkRequest;
import com.helpmi.dto.response.TicketLinkResponse;
import com.helpmi.dto.response.TicketSummary;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.ProjectRepository;
import com.helpmi.repository.TicketLinkRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.repository.UserProjectRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketLinkService {

    private final TicketLinkRepository linkRepository;
    private final TicketRepository ticketRepository;
    private final ProjectRepository projectRepository;
    private final UserProjectRepository userProjectRepository;
    private final CurrentUserService currentUserService;

    public TicketLinkResponse createLink(UUID sourceTicketId, CreateTicketLinkRequest req) {
        Ticket source = findTicket(sourceTicketId);
        requireEditable(source);
        Ticket target = findTicket(req.targetTicketId());

        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Un ticket ne peut pas être lié à lui-même");
        }
        if (linkRepository.existsBySourceTicketIdAndTargetTicketIdAndLinkType(
                sourceTicketId, req.targetTicketId(), req.linkType())) {
            throw new IllegalArgumentException("Ce lien existe déjà");
        }

        User user = currentUserService.getCurrentUser();
        UUID targetProjectId = target.getProject().getId();
        if (!projectRepository.isProjectAccessibleToUser(targetProjectId, user.getId())) {
            throw new ForbiddenException("Accès refusé au projet du ticket cible");
        }

        TicketLink link = TicketLink.builder()
                .sourceTicket(source)
                .targetTicket(target)
                .linkType(req.linkType())
                .createdBy(user)
                .build();

        return toResponse(linkRepository.save(link), sourceTicketId);
    }

    public void deleteLink(UUID linkId) {
        TicketLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Lien introuvable"));
        requireEditable(link.getSourceTicket());
        User user = currentUserService.getCurrentUser();
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        boolean isGestionnaire = !isAdmin && userProjectRepository
                .findByUserIdAndProjectId(user.getId(), link.getSourceTicket().getProject().getId())
                .map(up -> "MANAGER".equals(up.getRole()))
                .orElse(false);
        boolean isCreator = link.getCreatedBy() != null && link.getCreatedBy().getId().equals(user.getId());
        if (!isAdmin && !isGestionnaire && !isCreator) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à supprimer ce lien");
        }
        linkRepository.delete(link);
    }

    @Transactional(readOnly = true)
    public List<TicketSummary> search(String q, UUID excludeId) {
        if (q == null || q.isBlank() || q.length() < 2 || q.length() > 100) return List.of(); // A3-F3
        String pattern = "%" + q.toUpperCase() + "%";
        User user = currentUserService.getCurrentUser();

        List<UUID> projectIds = projectRepository.findIdsByUserId(user.getId());
        if (projectIds.isEmpty()) return List.of();
        List<Ticket> results = ticketRepository.searchByQueryInProjects(pattern, projectIds, Pageable.ofSize(10));
        return results.stream()
                .filter(t -> excludeId == null || !t.getId().equals(excludeId))
                .map(TicketSummary::from)
                .toList();
    }

    private Ticket findTicket(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket introuvable"));
    }

    private static void requireEditable(Ticket ticket) {
        if ("CLOSED".equals(ticket.getStatus()) || "CANCELLED".equals(ticket.getStatus())) {
            throw new ForbiddenException("Ce ticket est clôturé et ne peut plus être modifié");
        }
    }

    private TicketLinkResponse toResponse(TicketLink link, UUID sourceTicketId) {
        return new TicketLinkResponse(link.getId(),
                TicketSummary.from(link.getTargetTicket()), link.getLinkType(), "OUTGOING");
    }
}
