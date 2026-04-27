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
    private final CurrentUserService currentUserService;

    public TicketLinkResponse createLink(UUID sourceTicketId, CreateTicketLinkRequest req) {
        Ticket source = findTicket(sourceTicketId);
        Ticket target = findTicket(req.targetTicketId());

        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException("Un ticket ne peut pas être lié à lui-même");
        }
        if (linkRepository.existsBySourceTicketIdAndTargetTicketIdAndLinkType(
                sourceTicketId, req.targetTicketId(), req.linkType())) {
            throw new IllegalArgumentException("Ce lien existe déjà");
        }

        TicketLink link = TicketLink.builder()
                .sourceTicket(source)
                .targetTicket(target)
                .linkType(req.linkType())
                .createdBy(currentUserService.getCurrentUser())
                .build();

        return toResponse(linkRepository.save(link), sourceTicketId);
    }

    public void deleteLink(UUID linkId) {
        TicketLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new NotFoundException("Lien introuvable"));
        var user = currentUserService.getCurrentUser();
        boolean isAdminOrAgent = user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.AGENT;
        boolean isCreator = link.getCreatedBy() != null && link.getCreatedBy().getId().equals(user.getId());
        if (!isAdminOrAgent && !isCreator) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à supprimer ce lien");
        }
        linkRepository.delete(link);
    }

    @Transactional(readOnly = true)
    public List<TicketSummary> search(String q, UUID excludeId) {
        if (q == null || q.isBlank() || q.length() < 2) return List.of();
        String pattern = "%" + q.toUpperCase() + "%";
        User user = currentUserService.getCurrentUser();

        List<Ticket> results;
        if (user.getRole() == UserRole.ADMIN) {
            results = ticketRepository.searchByQuery(pattern, Pageable.ofSize(10));
        } else if (user.getOrganization() == null) {
            return List.of();
        } else {
            List<UUID> projectIds = projectRepository.findIdsByOrganizationId(user.getOrganization().getId());
            if (projectIds.isEmpty()) return List.of();
            results = ticketRepository.searchByQueryInProjects(pattern, projectIds, Pageable.ofSize(10));
        }
        return results.stream()
                .filter(t -> !t.getId().equals(excludeId))
                .map(TicketSummary::from)
                .toList();
    }

    private TicketLinkResponse toResponse(TicketLink link, UUID currentTicketId) {
        boolean isSource = link.getSourceTicket().getId().equals(currentTicketId);
        Ticket linked = isSource ? link.getTargetTicket() : link.getSourceTicket();
        return new TicketLinkResponse(link.getId(), TicketSummary.from(linked), link.getLinkType(),
                isSource ? "OUTGOING" : "INCOMING");
    }

    private Ticket findTicket(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket introuvable : " + id));
    }
}
