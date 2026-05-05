package com.helpmi.service;

import com.helpmi.domain.Attachment;
import com.helpmi.domain.Comment;
import com.helpmi.domain.Label;
import com.helpmi.domain.Organization;
import com.helpmi.domain.Project;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.dto.request.CreateTicketRequest;
import com.helpmi.dto.request.UpdateTicketRequest;
import com.helpmi.dto.response.ChangeStatusResponse;
import com.helpmi.dto.response.LabelResponse;
import com.helpmi.dto.response.OrganizationSummary;
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
import static org.assertj.core.api.Assertions.assertThatCode;
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
    @Mock OrganizationRepository organizationRepository;
    @Mock LabelRepository labelRepository;
    @Mock ProjectService projectService;
    @Mock CurrentUserService currentUserService;
    @Mock TicketHistoryService ticketHistoryService;

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
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
    }

    // ── getTickets ────────────────────────────────────────────────────────────

    @Test
    void getTickets_returnsMappedPage() {
        when(ticketRepository.findByProjectIdWithFilters(
                eq(project.getId()),
                eq(List.of()), eq(0),
                eq(List.of()), eq(0),
                eq(List.of()), eq(0),
                isNull(), any(Pageable.class)))
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
        assertThat(result.organizations()).isEmpty();
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
        when(userRepository.isAssignableToProject(assignee.getId(), project.getId())).thenReturn(true);
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
    void updateTicket_nonGestionnaire_cannotSetAssignee_throws() {
        User assignee = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.isGestionnaire(reporter.getId(), project.getId())).thenReturn(false);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.updateTicket(project.getId(), ticket.getId(),
                new UpdateTicketRequest(null, null, null, null, assignee.getId())))
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
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "OPEN");

        assertThat(ticket.getClosedAt()).isNull();
    }

    @Test
    void changeStatus_toClosed_setsClosedAt() {
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        assertThat(ticket.getClosedAt()).isNotNull();
    }

    @Test
    void changeStatus_toResolved_setsClosedAt() {
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "RESOLVED");

        assertThat(ticket.getClosedAt()).isNotNull();
    }

    @Test
    void changeStatus_toCancelled_setsClosedAt() {
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "CANCELLED");

        assertThat(ticket.getClosedAt()).isNotNull();
    }

    // ── moveTicket ───────────────────────────────────────────────────────────

    @Test
    void moveTicket_updatesProjectAndReference() {
        Project target = project();
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
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
        Organization org = organization();
        LocalDate dueDate = LocalDate.of(2025, 6, 1);
        ticket.getLabels().add(label);
        ticket.getOrganizations().add(org);
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
                t.getOrganizations().contains(org)));
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
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-2");

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        verify(ticketRepository).save(argThat(t ->
                "TEST-2".equals(t.getReference()) && t.getDueDate() == null));
    }

    @Test
    void changeStatus_nonPeriodic_closed_doesNotAutoCreate() {
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        verify(ticketRepository, times(1)).save(any());
        verify(projectService, never()).generateTicketReference(any());
    }

    @Test
    void changeStatus_recurringTicket_resolved_doesNotAutoCreate() {
        ticket.setType("ANNUEL");
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "RESOLVED");

        verify(ticketRepository, times(1)).save(any());
        verify(projectService, never()).generateTicketReference(any());
    }

    @Test
    void changeStatus_recurringTicket_cancelled_doesNotAutoCreate() {
        ticket.setType("ANNUEL");
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        service.changeStatus(project.getId(), ticket.getId(), "CANCELLED");

        verify(ticketRepository, times(1)).save(any());
        verify(projectService, never()).generateTicketReference(any());
    }

    @Test
    void changeStatus_recurringTicket_closed_responseIncludesNextReference() {
        ticket.setType("ANNUEL");
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

    // ── setOrganizations ──────────────────────────────────────────────────────

    @Test
    void setOrganizations_replacesExistingOrgs() {
        Organization o1 = organization();
        Organization o2 = organization();
        ticket.getOrganizations().add(o1);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(organizationRepository.findAllById(List.of(o2.getId()))).thenReturn(List.of(o2));

        List<OrganizationSummary> result = service.setOrganizations(project.getId(), ticket.getId(), List.of(o2.getId()));

        assertThat(result).hasSize(1);
        assertThat(ticket.getOrganizations()).containsExactly(o2);
    }

    @Test
    void setOrganizations_emptyList_clearsOrgs() {
        ticket.getOrganizations().add(organization());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        List<OrganizationSummary> result = service.setOrganizations(project.getId(), ticket.getId(), List.of());

        assertThat(result).isEmpty();
        assertThat(ticket.getOrganizations()).isEmpty();
    }

    @Test
    void setOrganizations_nullList_clearsOrgs() {
        ticket.getOrganizations().add(organization());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        List<OrganizationSummary> result = service.setOrganizations(project.getId(), ticket.getId(), null);

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

    // ── H1 — autorisation manquante (nouveaux tests de sécurité) ─────────────

    @Test
    void moveTicket_unauthorizedClient_throwsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(clientUser());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.moveTicket(project.getId(), ticket.getId(), UUID.randomUUID()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void setDueDate_member_updatesDueDate() {
        LocalDate newDate = LocalDate.of(2025, 12, 31);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        service.setDueDate(project.getId(), ticket.getId(), newDate);

        assertThat(ticket.getDueDate()).isEqualTo(newDate);
    }


    @Test
    void cloneTicket_unauthorizedClient_throwsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(clientUser());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.cloneTicket(project.getId(), ticket.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── setAssignee ───────────────────────────────────────────────────────────

    @Test
    void setAssignee_validUser_setsAssignee() {
        User assignee = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.isGestionnaire(reporter.getId(), project.getId())).thenReturn(true);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.isAssignableToProject(assignee.getId(), project.getId())).thenReturn(true);
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.setAssignee(project.getId(), ticket.getId(), assignee.getId());

        assertThat(ticket.getAssignee()).isEqualTo(assignee);
    }

    @Test
    void setAssignee_nullId_clearsAssignee() {
        ticket.setAssignee(agentUser());
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.isGestionnaire(reporter.getId(), project.getId())).thenReturn(true);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.setAssignee(project.getId(), ticket.getId(), null);

        assertThat(ticket.getAssignee()).isNull();
    }

    @Test
    void setAssignee_notAssignable_throws() {
        User other = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.isGestionnaire(reporter.getId(), project.getId())).thenReturn(true);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.isAssignableToProject(other.getId(), project.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.setAssignee(project.getId(), ticket.getId(), other.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── updateTicket — assignee branch ──────────────────────────────────────

    @Test
    void updateTicket_withAssignee_loadsAndSetsAssignee() {
        User assignee = clientUser();
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.isGestionnaire(reporter.getId(), project.getId())).thenReturn(true);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.isAssignableToProject(assignee.getId(), project.getId())).thenReturn(true);
        when(userRepository.findById(assignee.getId())).thenReturn(Optional.of(assignee));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.updateTicket(project.getId(), ticket.getId(),
                new UpdateTicketRequest(null, null, null, null, assignee.getId()));

        assertThat(ticket.getAssignee()).isEqualTo(assignee);
    }

    @Test
    void updateTicket_assigneeNotFound_throwsNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.isGestionnaire(reporter.getId(), project.getId())).thenReturn(true);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.isAssignableToProject(unknownId, project.getId())).thenReturn(true);
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTicket(project.getId(), ticket.getId(),
                new UpdateTicketRequest(null, null, null, null, unknownId)))
                .isInstanceOf(NotFoundException.class);
    }

    // ── setAssignee — assignee not found ─────────────────────────────────────

    @Test
    void setAssignee_assigneeIdValidButUserMissing_throwsNotFoundException() {
        UUID ghostId = UUID.randomUUID();
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.isGestionnaire(reporter.getId(), project.getId())).thenReturn(true);
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(userRepository.isAssignableToProject(ghostId, project.getId())).thenReturn(true);
        when(userRepository.findById(ghostId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setAssignee(project.getId(), ticket.getId(), ghostId))
                .isInstanceOf(NotFoundException.class);
    }

    // ── createTicket — assignee not found ────────────────────────────────────

    @Test
    void createTicket_assigneeNotFound_throwsNotFoundException() {
        UUID ghostId = UUID.randomUUID();
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.findActive(project.getId())).thenReturn(project);
        when(userRepository.isAssignableToProject(ghostId, project.getId())).thenReturn(true);
        when(userRepository.findById(ghostId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createTicket(project.getId(),
                new CreateTicketRequest("T", null, null, null, ghostId, null)))
                .isInstanceOf(NotFoundException.class);
    }

    // ── getTicket — mapping lambdas ───────────────────────────────────────────

    @Test
    void getTicket_withCommentsAndAttachments_mapsCorrectly() {
        User author = agentUser();
        Comment comment = Comment.builder()
                .id(UUID.randomUUID()).body("hello").author(author).ticket(ticket).build();
        Attachment attachment = Attachment.builder()
                .id(UUID.randomUUID()).fileName("doc.pdf").contentType("application/pdf")
                .size(100L).uploadedBy(author).ticket(ticket).build();

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId()))
                .thenReturn(List.of(comment));
        when(attachmentRepository.findByTicketIdOrderByUploadedAtDesc(ticket.getId()))
                .thenReturn(List.of(attachment));
        when(ticketLinkRepository.findBySourceTicketId(ticket.getId())).thenReturn(List.of());
        when(ticketLinkRepository.findByTargetTicketId(ticket.getId())).thenReturn(List.of());
        when(currentUserService.getCurrentUser()).thenReturn(author);

        TicketDetailResponse result = service.getTicket(project.getId(), ticket.getId());

        assertThat(result.comments()).hasSize(1);
        assertThat(result.comments().get(0).body()).isEqualTo("hello");
        assertThat(result.attachments()).hasSize(1);
        assertThat(result.attachments().get(0).fileName()).isEqualTo("doc.pdf");
    }

    // ── autoCloneRecurring — unknown type → null dueDate ─────────────────────

    @Test
    void changeStatus_annuelTicket_noDueDate_clonesWithNullDueDate() {
        ticket.setType("ANNUEL");
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-2");

        service.changeStatus(project.getId(), ticket.getId(), "CLOSED");

        verify(ticketRepository).save(argThat(t ->
                "TEST-2".equals(t.getReference()) && t.getDueDate() == null));
    }

    // ── H1 — isolation par organisation ──────────────────────────────────────

    @Test
    void getTickets_callsRequireProjectAccess() {
        when(ticketRepository.findByProjectIdWithFilters(
                eq(project.getId()),
                eq(List.of()), eq(0), eq(List.of()), eq(0), eq(List.of()), eq(0),
                isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getTickets(project.getId(), null, null, null, null, Pageable.unpaged());

        verify(projectService).requireProjectAccess(project.getId());
    }

    @Test
    void getTickets_projectAccessDenied_throws() {
        doThrow(new ForbiddenException("Accès refusé")).when(projectService).requireProjectAccess(project.getId());

        assertThatThrownBy(() -> service.getTickets(project.getId(), null, null, null, null, Pageable.unpaged()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createTicket_callsRequireProjectAccess() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(projectService.findActive(project.getId())).thenReturn(project);
        when(projectService.generateTicketReference(project.getId())).thenReturn("TEST-1");
        when(ticketRepository.save(any())).thenReturn(ticket);

        service.createTicket(project.getId(), new CreateTicketRequest("T", null, null, null, null, null));

        verify(projectService).requireProjectAccess(project.getId());
    }

    @Test
    void createTicket_projectAccessDenied_throws() {
        doThrow(new ForbiddenException("Accès refusé")).when(projectService).requireProjectAccess(project.getId());

        assertThatThrownBy(() -> service.createTicket(project.getId(),
                new CreateTicketRequest("T", null, null, null, null, null)))
                .isInstanceOf(ForbiddenException.class);
        verify(projectService, never()).findActive(any());
    }

    @Test
    void getTicket_callsRequireProjectAccess() {
        stubGetTicket();

        service.getTicket(project.getId(), ticket.getId());

        verify(projectService).requireProjectAccess(project.getId());
    }

    @Test
    void getTicket_projectAccessDenied_throws() {
        doThrow(new ForbiddenException("Accès refusé")).when(projectService).requireProjectAccess(project.getId());

        assertThatThrownBy(() -> service.getTicket(project.getId(), ticket.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── F3 — parseFilter limite à 20 valeurs ─────────────────────────────────

    @Test
    void getTickets_tooManyFilterValues_throwsIllegalArgument() {
        String tooMany = String.join(",", java.util.Collections.nCopies(21, "OPEN"));

        assertThatThrownBy(() -> service.getTickets(project.getId(), tooMany, null, null, null, Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max 20");
    }

    @Test
    void getTickets_exactlyMaxFilterValues_doesNotThrow() {
        String maxValues = String.join(",", java.util.Collections.nCopies(20, "OPEN"));
        when(ticketRepository.findByProjectIdWithFilters(
                any(), any(), eq(20), any(), eq(0), any(), eq(0), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThatCode(() -> service.getTickets(project.getId(), maxValues, null, null, null, Pageable.unpaged()))
                .doesNotThrowAnyException();
    }
}
