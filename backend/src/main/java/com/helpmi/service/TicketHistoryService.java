package com.helpmi.service;

import com.helpmi.domain.Ticket;
import com.helpmi.domain.TicketHistory;
import com.helpmi.domain.User;
import com.helpmi.dto.response.TicketHistoryResponse;
import com.helpmi.dto.response.UserSummary;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.TicketHistoryRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketHistoryService {

    private final TicketHistoryRepository historyRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;
    private final ProjectService projectService;

    public void record(Ticket ticket, String field, String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) return;
        User actor = currentUserService.getCurrentUser();
        historyRepository.save(TicketHistory.builder()
                .ticket(ticket)
                .changedBy(actor)
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }

    @Transactional(readOnly = true)
    public List<TicketHistoryResponse> getHistory(UUID projectId, UUID ticketId) {
        projectService.requireProjectAccess(projectId);
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket introuvable"));
        if (!ticket.getProject().getId().equals(projectId)) {
            throw new NotFoundException("Ticket introuvable dans ce projet");
        }
        return historyRepository.findByTicketIdOrderByChangedAtDesc(ticketId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TicketHistoryResponse toResponse(TicketHistory h) {
        return new TicketHistoryResponse(
                h.getId(),
                h.getField(),
                h.getOldValue(),
                h.getNewValue(),
                UserSummary.from(h.getChangedBy()),
                h.getChangedAt()
        );
    }
}
