package com.helpmi.service;

import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.dto.response.ManagerTrackingResponse;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.helpmi.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerTrackingServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserService currentUserService;
    @InjectMocks ManagerTrackingService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    // ── empty state ───────────────────────────────────────────────────────────

  @MockitoSettings(strictness = Strictness.LENIENT)
    @Test
  void getManagerTracking_noManagerProjects_returnsEmpty() {
        stubAllEmpty();

        ManagerTrackingResponse result = service.getManagerTracking();

        assertThat(result.projects()).isEmpty();
    }

    // ── group by project → assignee ──────────────────────────────────────────

    @Nested
    class Grouping {

        @Test
        void groupByProjectAndAssignee_singleProject() {
            UUID projId = UUID.randomUUID();
            stubAllEmpty();
            Object[] r1 = row(projId, "PRJ", "Project A", user.getId(), "Admin", "User", "admin@test.com", "OPEN", 3L);
            Object[] r2 = row(projId, "PRJ", "Project A", user.getId(), "Admin", "User", "admin@test.com", "IN_PROGRESS", 2L);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = Arrays.asList(r1, r2);
            when(ticketRepository.countTicketsByProjectAndAssignee(any())).thenReturn(rows);
            when(ticketRepository.findByProjectManagerAndAssigneeId(any(), eq(user.getId()))).thenReturn(List.of());
            when(ticketRepository.findUnassignedTicketsForProject(any())).thenReturn(List.of());

            ManagerTrackingResponse result = service.getManagerTracking();

            assertThat(result.projects()).hasSize(1);
            var proj = result.projects().get(0);
            assertThat(proj.key()).isEqualTo("PRJ");
            assertThat(proj.name()).isEqualTo("Project A");
            assertThat(proj.assignees()).hasSize(1);
            assertThat(proj.assignees().get(0).counts().total()).isEqualTo(5);
            assertThat(proj.assignees().get(0).counts().open()).isEqualTo(3);
            assertThat(proj.assignees().get(0).counts().inProgress()).isEqualTo(2);
        }

        @Test
        void groupByProjectAndAssignee_multipleAssignees() {
            UUID projId = UUID.randomUUID();
            UUID alice = UUID.randomUUID();
            UUID bob = UUID.randomUUID();
            stubAllEmpty();
            Object[] r1 = row(projId, "PRJ", "Project A", alice, "Alice", "Dupont", "alice@test.com", "OPEN", 2L);
            Object[] r2 = row(projId, "PRJ", "Project A", bob, "Bob", "Martin", "bob@test.com", "OPEN", 1L);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = Arrays.asList(r1, r2);
            when(ticketRepository.countTicketsByProjectAndAssignee(any())).thenReturn(rows);
            when(ticketRepository.findByProjectManagerAndAssigneeId(eq(projId), eq(alice))).thenReturn(List.of());
            when(ticketRepository.findByProjectManagerAndAssigneeId(eq(projId), eq(bob))).thenReturn(List.of());
            when(ticketRepository.findUnassignedTicketsForProject(eq(projId))).thenReturn(List.of());

            ManagerTrackingResponse result = service.getManagerTracking();

            assertThat(result.projects().get(0).assignees()).hasSize(2);
            assertThat(result.projects().get(0).assignees()).extracting("firstName")
                    .containsExactly("Alice", "Bob");
        }

        @Test
        void groupByProjectAndAssignee_multipleProjects() {
            UUID proj1 = UUID.randomUUID();
            UUID proj2 = UUID.randomUUID();
            stubAllEmpty();
            Object[] r1 = row(proj1, "ALPHA", "Alpha Project", user.getId(), "Admin", "User", "admin@test.com", "OPEN", 1L);
            Object[] r2 = row(proj2, "BETA", "Beta Project", user.getId(), "Admin", "User", "admin@test.com", "RESOLVED", 5L);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = Arrays.asList(r1, r2);
            when(ticketRepository.countTicketsByProjectAndAssignee(any())).thenReturn(rows);
            when(ticketRepository.findByProjectManagerAndAssigneeId(eq(proj1), eq(user.getId()))).thenReturn(List.of());
            when(ticketRepository.findByProjectManagerAndAssigneeId(eq(proj2), eq(user.getId()))).thenReturn(List.of());
            when(ticketRepository.findUnassignedTicketsForProject(eq(proj1))).thenReturn(List.of());
            when(ticketRepository.findUnassignedTicketsForProject(eq(proj2))).thenReturn(List.of());

            ManagerTrackingResponse result = service.getManagerTracking();

            assertThat(result.projects()).hasSize(2);
            assertThat(result.projects()).extracting("key").containsExactly("ALPHA", "BETA");
        }
    }

    // ── unassigned tickets ──────────────────────────────────────────────────

    @Nested
    class Unassigned {

        @Test
        void tracksUnassignedTickets() {
            UUID projId = UUID.randomUUID();
            stubAllEmpty();
            Object[] r = row(projId, "PRJ", "Project A", user.getId(), "Admin", "User", "admin@test.com", "OPEN", 0L);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = new ArrayList<>();
            rows.add(r);
            when(ticketRepository.countTicketsByProjectAndAssignee(any())).thenReturn(rows);
            com.helpmi.domain.Project p2 = com.helpmi.domain.Project.builder()
                    .id(projId).key("PRJ").name("Project A").active(true).ticketSequence(0).build();
           Ticket unassigned = com.helpmi.domain.Ticket.builder()
                    .id(UUID.randomUUID()).reference("TEST-1").title("Test")
                    .status("OPEN").priority("MEDIUM").type("TASK")
                    .project(p2).reporter(user).build();
            when(ticketRepository.findUnassignedTicketsForProject(eq(projId))).thenReturn(List.of(unassigned));
            when(ticketRepository.findByProjectManagerAndAssigneeId(eq(projId), eq(user.getId()))).thenReturn(List.of());

            ManagerTrackingResponse result = service.getManagerTracking();

            assertThat(result.projects().get(0).unassignedCounts().total()).isEqualTo(1);
            assertThat(result.projects().get(0).unassignedTickets()).hasSize(1);
        }

        @Test
        void unassignedCountsCorrect() {
            UUID projId = UUID.randomUUID();
            stubAllEmpty();
            Object[] r = row(projId, "PRJ", "Project A", user.getId(), "Admin", "User", "admin@test.com", "OPEN", 0L);
            @SuppressWarnings("unchecked")
            List<Object[]> rows = new ArrayList<>();
            rows.add(r);
            when(ticketRepository.countTicketsByProjectAndAssignee(any())).thenReturn(rows);
            com.helpmi.domain.Project p = com.helpmi.domain.Project.builder()
                    .id(projId).key("PRJ").name("Project A").active(true).ticketSequence(0).build();
            Ticket open = com.helpmi.domain.Ticket.builder().id(UUID.randomUUID()).reference("T1").title("T")
                    .status("OPEN").priority("MEDIUM").type("TASK").project(p).reporter(user).build();
            Ticket inProgress = com.helpmi.domain.Ticket.builder().id(UUID.randomUUID()).reference("T2").title("T")
                    .status("IN_PROGRESS").priority("MEDIUM").type("TASK").project(p).reporter(user).build();
            when(ticketRepository.findUnassignedTicketsForProject(eq(projId))).thenReturn(List.of(open, inProgress));
            when(ticketRepository.findByProjectManagerAndAssigneeId(eq(projId), eq(user.getId()))).thenReturn(List.of());

            ManagerTrackingResponse result = service.getManagerTracking();

            assertThat(result.projects().get(0).unassignedCounts().total()).isEqualTo(2);
            assertThat(result.projects().get(0).unassignedCounts().open()).isEqualTo(1);
            assertThat(result.projects().get(0).unassignedCounts().inProgress()).isEqualTo(1);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubAllEmpty() {
        when(ticketRepository.countTicketsByProjectAndAssignee(any())).thenReturn(List.of());
        when(ticketRepository.findUnassignedTicketsForProject(any())).thenReturn(List.of());
        when(ticketRepository.findByProjectManagerAndAssigneeId(any(), any())).thenReturn(List.of());
    }

    private static Object[] row(UUID projectId, String key, String name, UUID assigneeId,
                                String firstName, String lastName, String email,
                                String status, long count) {
        return new Object[]{projectId, key, name, assigneeId, firstName, lastName, email, status, count};
    }
}
