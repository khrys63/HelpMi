package com.helpmi.service;

import com.helpmi.domain.Ticket;
import com.helpmi.domain.TicketHistory;
import com.helpmi.domain.User;
import com.helpmi.dto.response.TicketHistoryResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.TicketHistoryRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketHistoryServiceTest {

    @Mock TicketHistoryRepository historyRepository;
    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserService currentUserService;
    @Mock ProjectService projectService;

    @InjectMocks TicketHistoryService service;

    // ── record ────────────────────────────────────────────────────────────────

    @Test
    void record_sameValue_doesNotPersist() {
        Ticket ticket = ticket(project(), agentUser());

        service.record(ticket, "status", "OPEN", "OPEN");

        verifyNoInteractions(historyRepository, currentUserService);
    }

    @Test
    void record_bothNull_doesNotPersist() {
        Ticket ticket = ticket(project(), agentUser());

        service.record(ticket, "assignee", null, null);

        verifyNoInteractions(historyRepository, currentUserService);
    }

    @Test
    void record_differentValues_savesEntry() {
        User actor = agentUser();
        Ticket ticket = ticket(project(), actor);
        when(currentUserService.getCurrentUser()).thenReturn(actor);
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.record(ticket, "status", "OPEN", "IN_PROGRESS");

        ArgumentCaptor<TicketHistory> captor = ArgumentCaptor.forClass(TicketHistory.class);
        verify(historyRepository).save(captor.capture());
        TicketHistory saved = captor.getValue();
        assertThat(saved.getField()).isEqualTo("status");
        assertThat(saved.getOldValue()).isEqualTo("OPEN");
        assertThat(saved.getNewValue()).isEqualTo("IN_PROGRESS");
        assertThat(saved.getChangedBy()).isEqualTo(actor);
        assertThat(saved.getTicket()).isEqualTo(ticket);
    }

    @Test
    void record_fromNullToValue_savesEntry() {
        User actor = agentUser();
        Ticket ticket = ticket(project(), actor);
        when(currentUserService.getCurrentUser()).thenReturn(actor);
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.record(ticket, "assignee", null, "Jane Doe");

        verify(historyRepository).save(argThat(h ->
                h.getOldValue() == null && "Jane Doe".equals(h.getNewValue())));
    }

    @Test
    void record_fromValueToNull_savesEntry() {
        User actor = agentUser();
        Ticket ticket = ticket(project(), actor);
        when(currentUserService.getCurrentUser()).thenReturn(actor);
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.record(ticket, "assignee", "John Doe", null);

        verify(historyRepository).save(argThat(h ->
                "John Doe".equals(h.getOldValue()) && h.getNewValue() == null));
    }

    // ── getHistory ────────────────────────────────────────────────────────────

    @Test
    void getHistory_ticketNotFound_throwsNotFoundException() {
        UUID projectId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getHistory(projectId, ticketId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getHistory_ticketBelongsToOtherProject_throwsNotFoundException() {
        var project1 = project();
        var project2 = project();
        Ticket ticket = ticket(project2, agentUser());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getHistory(project1.getId(), ticket.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getHistory_accessDenied_throwsForbidden() {
        var project = project();
        UUID ticketId = UUID.randomUUID();
        doThrow(new ForbiddenException("Accès refusé")).when(projectService).requireProjectAccess(project.getId());

        assertThatThrownBy(() -> service.getHistory(project.getId(), ticketId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getHistory_returnsEntriesInDescendingOrder() {
        User actor = agentUser();
        var project = project();
        Ticket ticket = ticket(project, actor);

        TicketHistory h1 = buildHistory(ticket, actor, "status", "OPEN", "IN_PROGRESS",
                LocalDateTime.of(2025, 1, 1, 10, 0));
        TicketHistory h2 = buildHistory(ticket, actor, "priority", "LOW", "HIGH",
                LocalDateTime.of(2025, 1, 2, 10, 0));

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(historyRepository.findByTicketIdOrderByChangedAtDesc(ticket.getId()))
                .thenReturn(List.of(h2, h1));

        List<TicketHistoryResponse> result = service.getHistory(project.getId(), ticket.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).field()).isEqualTo("priority");
        assertThat(result.get(1).field()).isEqualTo("status");
    }

    @Test
    void getHistory_mapsAllFields() {
        User actor = agentUser();
        var project = project();
        Ticket ticket = ticket(project, actor);
        LocalDateTime changedAt = LocalDateTime.of(2025, 6, 1, 12, 0);
        TicketHistory h = buildHistory(ticket, actor, "title", "Old", "New", changedAt);

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(historyRepository.findByTicketIdOrderByChangedAtDesc(ticket.getId()))
                .thenReturn(List.of(h));

        List<TicketHistoryResponse> result = service.getHistory(project.getId(), ticket.getId());

        assertThat(result).hasSize(1);
        TicketHistoryResponse r = result.get(0);
        assertThat(r.field()).isEqualTo("title");
        assertThat(r.oldValue()).isEqualTo("Old");
        assertThat(r.newValue()).isEqualTo("New");
        assertThat(r.changedAt()).isEqualTo(changedAt);
        assertThat(r.changedBy()).isNotNull();
        assertThat(r.changedBy().email()).isEqualTo(actor.getEmail());
    }

    @Test
    void getHistory_emptyHistory_returnsEmptyList() {
        var project = project();
        Ticket ticket = ticket(project, agentUser());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(historyRepository.findByTicketIdOrderByChangedAtDesc(ticket.getId()))
                .thenReturn(List.of());

        List<TicketHistoryResponse> result = service.getHistory(project.getId(), ticket.getId());

        assertThat(result).isEmpty();
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private TicketHistory buildHistory(Ticket ticket, User actor, String field,
                                       String oldValue, String newValue, LocalDateTime changedAt) {
        return TicketHistory.builder()
                .id(UUID.randomUUID())
                .ticket(ticket)
                .changedBy(actor)
                .changedAt(changedAt)
                .field(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
    }
}
