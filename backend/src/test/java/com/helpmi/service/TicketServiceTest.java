package com.helpmi.service;

import com.helpmi.domain.Client;
import com.helpmi.domain.Label;
import com.helpmi.domain.Project;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.dto.request.CreateTicketRequest;
import com.helpmi.dto.request.UpdateTicketRequest;
import com.helpmi.dto.response.ChangeStatusResponse;
import com.helpmi.dto.response.ClientResponse;
import com.helpmi.dto.response.LabelResponse;
import com.helpmi.dto.response.TicketDetailResponse;
import com.helpmi.dto.response.TicketResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.*;
import com.helpmi.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock UserRepository userRepository;
    @Mock CommentRepository commentRepository;
    @Mock AttachmentRepository attachmentRepository;
    @Mock TicketLinkRepository ticketLinkRepository;
    @Mock ClientRepository clientRepository;
    @Mock LabelRepository labelRepository;
    @Mock ProjectService projectService;
    @Mock CurrentUserService currentUserService;

    @InjectMocks TicketService service;

    private User reporter;
    private Project project;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        reporter = agentUser();
        project = project();
        ticket = ticket(project, reporter);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubGetTicket() {
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId())).thenReturn(List.of());
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticket.getId())).thenReturn(List.of());
        when(ticketLinkRepository.findBySourceTicketId(ticket.getId())).thenReturn(List.of());
        when(ticketLinkRepository.findByTargetTicketId(ticket.getId())).thenReturn(List.of());
    }

    // ── getTickets ────────────────────────────────────────────────────────────

    @Test
    void getTickets_returnsMappedPage() {
        when(ticketRepository.findByProjectIdWithFilters(
                eq(project.getId()), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ticket)));

        var result = service.getTickets(project.getId(), null, null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).reference()).isEqualTo("TEST-1");
    }

    // ── getTicket ────────────────────────────────────────────────────────────

    @Test
    void getTicket_wrongProject_throws() {
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getTicket(UUID.randomUUID(), ticket.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getTicket_notFound_throws() {
        when(ticketRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicket(project.getId(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getTicket_happy_returnsFullDetail() {
        stubGetTicket();

        TicketDetailResponse result = service.getTicket(project.getId(), ticket.getId());

        assertThat(result.reference()).isEqualTo("TEST-1");
        assertThat(result.title()).isEqualTo("Test Ticket");
        assertThat(result.projectId()).isEqualTo(project.getId());
        assertThat(result.clients()).isEmpty();
        assertThat(result.labels()).isEmpty();
        assertThat(result.links()).isEmpty();
    }

    // ── createTicket ─────────────────────────────────────────────────────────

    @Test
    void createTicket_happy_persistsAndReturns() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.findActive(project.getId())).thenReturn(project);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-1");
        when(ticketRepository.save(any())).thenReturn(ticket);

        CreateTicketRequest req = new CreateTicketRequest("My Ticket", "Desc", "MEDIUM", "TASK", null, null);
        TicketResponse result = service.createTicket(project.getId(), req);

        assertThat(result.reference()).isEqualTo("TEST-1");
        verify(ticketRepository).save(argThat(t ->
                t.getTitle().equals("My Ticket") && t.getReporter().equals(reporter)));
    }

    @Test
    void createTicket_withAssignee_loadsAssignee() {
        User assignee = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.findActive(project.getId())).thenReturn(project);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-1");
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(any())).thenReturn(ticket);

        CreateTicketRequest req = new CreateTicketRequest("Ticket", null, "HIGH", "TASK", assignee.getId(), null);
        service.createTicket(project.getId(), req);

        verify(ticketRepository).save(argThat(t -> t.getAssignee().equals(assignee)));
    }

    @Test
    void createTicket_defaultPriorityAndType_whenNullProvided() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.findActive(project.getId())).thenReturn(project);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-1");
        when(ticketRepository.save(any())).thenReturn(ticket);

        service.createTicket(project.getId(), new CreateTicketRequest("Title", null, null, null, null, null));

        verify(ticketRepository).save(argThat(t ->
                t.getPriority().equals("MEDIUM") && t.getType().equals("TASK")));
    }

    // ── updateTicket ─────────────────────────────────────────────────────────

    @Test
    void updateTicket_notFound_throws() {
        when(ticketRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTicket(project.getId(), UUID.randomUUID(),
                new UpdateTicketRequest("t", null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateTicket_notAuthorized_throws() {
        User other = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(other);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateTicket(project.getId(), ticket.getId(),
                new UpdateTicketRequest("t", null, null, null, null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateTicket_reporter_canModify() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.updateTicket(project.getId(), ticket.getId(),
                new UpdateTicketRequest("New Title", null, null, null, null));

        assertThat(ticket.getTitle()).isEqualTo("New Title");
    }

    @Test
    void updateTicket_partialUpdate_onlyChangesProvidedFields() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.updateTicket(project.getId(), ticket.getId(),
                new UpdateTicketRequest(null, null, "HIGH", null, null));

        assertThat(ticket.getPriority()).isEqualTo("HIGH");
        assertThat(ticket.getTitle()).isEqualTo("Test Ticket");
    }

    // ── changeStatus ─────────────────────────────────────────────────────────

    @Test
    void changeStatus_toOpen_clearsClosedAt() {
        ticket.setStatus("CLOSED");
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "OPEN");

        assertThat(ticket.getClosedAt()).isNull();
    }

    @Test
    void changeStatus_toClosed_setsClosedAt() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        assertThat(ticket.getClosedAt()).isNotNull();
    }

    @Test
    void changeStatus_toResolved_setsClosedAt() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "RESOLVED");

        assertThat(ticket.getClosedAt()).isNotNull();
    }

    @Test
    void changeStatus_toCancelled_setsClosedAt() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "CANCELLED");

        assertThat(ticket.getClosedAt()).isNotNull();
    }

    // ── moveTicket ───────────────────────────────────────────────────────────

    @Test
    void moveTicket_updatesProjectAndReference() {
        Project target = project();
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(projectService.findActive(target.getId())).thenReturn(target);
        when(projectService.generateTicketReference(target.getId())).thenReturn("TARGET-1");
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.moveTicket(project.getId(), ticket.getId(), target.getId());

        assertThat(ticket.getProject()).isEqualTo(target);
        assertThat(ticket.getReference()).isEqualTo("TARGET-1");
    }

    @Test
    void moveTicket_sameProject_throws() {
        assertThatThrownBy(() -> service.moveTicket(project.getId(), ticket.getId(), project.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("déjà");
    }

    @Test
    void moveTicket_sourceNotFound_throws() {
        when(ticketRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moveTicket(project.getId(), UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    // ── cloneTicket ──────────────────────────────────────────────────────────

    @Test
    void cloneTicket_copiesFieldsWithoutLinks() {
        Label label = label("bug");
        Client client = client("Acme");
        LocalDate dueDate = LocalDate.of(2025, 6, 1);
        ticket.getLabels().add(label);
        ticket.getClients().add(client);
        ticket.setDueDate(dueDate);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-2");
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TicketResponse result = service.cloneTicket(project.getId(), ticket.getId());

        assertThat(result.reference()).isEqualTo("TEST-2");
        assertThat(result.dueDate()).isEqualTo(dueDate);
        verify(ticketRepository).save(argThat(t ->
                t.getTitle().equals("[Copie] Test Ticket") &&
                t.getPriority().equals("MEDIUM") &&
                t.getType().equals("TASK") &&
                dueDate.equals(t.getDueDate()) &&
                t.getLabels().contains(label) &&
                t.getClients().contains(client)));
    }

    @Test
    void cloneTicket_sourceNotFound_throws() {
        when(ticketRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cloneTicket(project.getId(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    // ── changeStatus periodic ────────────────────────────────────────────────

    @Test
    void changeStatus_recurringTicket_closed_autoCreatesNextOccurrence() {
        ticket.setType("ANNUEL");
        LocalDate origDueDate = LocalDate.of(2025, 3, 10);
        ticket.setDueDate(origDueDate);
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-2");

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        verify(ticketRepository, times(2)).save(any());
        verify(ticketRepository).save(argThat(t ->
                "TEST-2".equals(t.getReference()) &&
                LocalDate.of(2026, 3, 10).equals(t.getDueDate()) &&
                "OPEN".equals(t.getStatus())));
    }

    @Test
    void changeStatus_recurringTicket_noDueDate_autoCreatesWithNullDueDate() {
        ticket.setType("ANNUEL");
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-2");

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        verify(ticketRepository).save(argThat(t ->
                "TEST-2".equals(t.getReference()) && t.getDueDate() == null));
    }

    @Test
    void changeStatus_nonPeriodic_closed_doesNotAutoCreate() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        verify(ticketRepository, times(1)).save(any());
        verify(projectService, never()).generateTicketReference(any());
    }

    @Test
    void changeStatus_recurringTicket_resolved_doesNotAutoCreate() {
        ticket.setType("ANNUEL");
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "RESOLVED");

        verify(ticketRepository, times(1)).save(any());
        verify(projectService, never()).generateTicketReference(any());
    }

    @Test
    void changeStatus_recurringTicket_cancelled_doesNotAutoCreate() {
        ticket.setType("ANNUEL");
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "CANCELLED");

        verify(ticketRepository, times(1)).save(any());
        verify(projectService, never()).generateTicketReference(any());
    }

    @Test
    void changeStatus_recurringTicket_closed_responseIncludesNextReference() {
        ticket.setType("ANNUEL");
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-2");

        ChangeStatusResponse result = service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        assertThat(result.nextTicketReference()).isEqualTo("TEST-2");
        assertThat(result.nextTicketId()).isNotNull();
    }

    @Test
    void changeStatus_nonRecurring_closed_responseHasNullNextTicket() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        ChangeStatusResponse result = service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        assertThat(result.nextTicketReference()).isNull();
        assertThat(result.nextTicketId()).isNull();
    }

    @Test
    void changeStatus_mensuelTicket_closed_autoCreatesNextMonth() {
        ticket.setType("MENSUEL");
        ticket.setDueDate(LocalDate.of(2025, 1, 31));
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-2");

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        verify(ticketRepository).save(argThat(t ->
                "TEST-2".equals(t.getReference()) &&
                LocalDate.of(2025, 2, 28).equals(t.getDueDate())));
    }

    @Test
    void changeStatus_trimestrielTicket_closed_autoCreatesNextQuarter() {
        ticket.setType("TRIMESTRIEL");
        ticket.setDueDate(LocalDate.of(2025, 1, 15));
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-2");

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        verify(ticketRepository).save(argThat(t ->
                "TEST-2".equals(t.getReference()) &&
                LocalDate.of(2025, 4, 15).equals(t.getDueDate())));
    }

    // ── deleteTicket ─────────────────────────────────────────────────────────

    @Test
    void deleteTicket_notAdmin_throws() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);

        assertThatThrownBy(() -> service.deleteTicket(project.getId(), ticket.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deleteTicket_admin_deletes() {
        User admin = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        service.deleteTicket(project.getId(), ticket.getId());

        verify(ticketRepository).delete(ticket);
    }

    @Test
    void deleteTicket_recurringTicket_doesNotAutoCreate() {
        User admin = adminUser();
        ticket.setType("ANNUEL");
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        service.deleteTicket(project.getId(), ticket.getId());

        verify(ticketRepository).delete(ticket);
        verify(projectService, never()).generateTicketReference(any());
    }

    // ── setClients ────────────────────────────────────────────────────────────

    @Test
    void setClients_replacesExistingClients() {
        Client c1 = client("Acme");
        Client c2 = client("Beta");
        ticket.getClients().add(c1);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(clientRepository.findAllById(List.of(c2.getId()))).thenReturn(List.of(c2));

        List<ClientResponse> result = service.setClients(project.getId(), ticket.getId(), List.of(c2.getId()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Beta");
        assertThat(ticket.getClients()).containsExactly(c2);
    }

    @Test
    void setClients_emptyList_clearsClients() {
        Client c = client("Acme");
        ticket.getClients().add(c);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        List<ClientResponse> result = service.setClients(project.getId(), ticket.getId(), List.of());

        assertThat(result).isEmpty();
        assertThat(ticket.getClients()).isEmpty();
    }

    @Test
    void setClients_nullList_clearsClients() {
        Client c = client("Acme");
        ticket.getClients().add(c);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        List<ClientResponse> result = service.setClients(project.getId(), ticket.getId(), null);

        assertThat(result).isEmpty();
    }

    // ── setLabels ─────────────────────────────────────────────────────────────

    @Test
    void setLabels_replacesExistingLabels() {
        Label l1 = label("urgent");
        Label l2 = label("bug");
        ticket.getLabels().add(l1);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(labelRepository.findAllById(List.of(l2.getId()))).thenReturn(List.of(l2));

        List<LabelResponse> result = service.setLabels(project.getId(), ticket.getId(), List.of(l2.getId()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("bug");
        assertThat(ticket.getLabels()).containsExactly(l2);
    }

    @Test
    void setLabels_emptyList_clearsLabels() {
        ticket.getLabels().add(label("urgent"));
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        List<LabelResponse> result = service.setLabels(project.getId(), ticket.getId(), List.of());

        assertThat(result).isEmpty();
        assertThat(ticket.getLabels()).isEmpty();
    }
}
