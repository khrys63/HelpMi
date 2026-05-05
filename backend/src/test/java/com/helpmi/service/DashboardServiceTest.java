package com.helpmi.service;

import com.helpmi.domain.Project;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.dto.response.DashboardResponse;
import com.helpmi.dto.response.ProjectTicketStatsResponse;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class DashboardServiceTest {

    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserService currentUserService;
    @InjectMocks DashboardService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = adminUser();
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    // ── getDashboard — sections personnelles ──────────────────────────────────

    @Test
    void getDashboard_allEmpty_returnsEmptyResponse() {
        stubAllEmpty();

        DashboardResponse result = service.getDashboard();

        assertThat(result.myOpenTickets()).isEmpty();
        assertThat(result.assignedToMe()).isEmpty();
        assertThat(result.watchedTickets()).isEmpty();
        assertThat(result.dueSoon()).isEmpty();
        assertThat(result.projectStats()).isEmpty();
    }

    @Test
    void getDashboard_myOpenTickets_mappedCorrectly() {
        Project p = project();
        Ticket t = ticket(p, user);
        when(ticketRepository.findReportedByUserAndStatusNotIn(eq(user.getId()), any())).thenReturn(List.of(t));
        when(ticketRepository.findAssignedToUserAndStatusNotIn(any(), any())).thenReturn(List.of());
        when(ticketRepository.findWatchedByUserIdAndStatusNotIn(any(), any())).thenReturn(List.of());
        when(ticketRepository.findDueSoonForUser(any(), any(), any(), any())).thenReturn(List.of());
        when(ticketRepository.countTicketsByProjectAndStatus(any(), any())).thenReturn(List.of());

        DashboardResponse result = service.getDashboard();

        assertThat(result.myOpenTickets()).hasSize(1);
        assertThat(result.myOpenTickets().get(0).reference()).isEqualTo("TEST-1");
        assertThat(result.myOpenTickets().get(0).projectName()).isEqualTo("Test Project");
        assertThat(result.myOpenTickets().get(0).projectKey()).isEqualTo("TEST");
    }

    @Test
    void getDashboard_excludesClosedAndCancelledStatuses() {
        stubAllEmpty();

        service.getDashboard();

        verify(ticketRepository).findReportedByUserAndStatusNotIn(
                eq(user.getId()),
                argThat(s -> s.contains("CLOSED") && s.contains("CANCELLED")));
    }

    @Test
    void getDashboard_passesCurrentUserIdToAllQueries() {
        stubAllEmpty();

        service.getDashboard();

        verify(ticketRepository).findReportedByUserAndStatusNotIn(eq(user.getId()), any());
        verify(ticketRepository).findAssignedToUserAndStatusNotIn(eq(user.getId()), any());
        verify(ticketRepository).findWatchedByUserIdAndStatusNotIn(eq(user.getId()), any());
        verify(ticketRepository).countTicketsByProjectAndStatus(eq(user.getId()), any());
    }

    // ── buildProjectStats ─────────────────────────────────────────────────────

    @Nested
    class ProjectStats {

        @Test
        void singleProject_countsCorrectly() {
            UUID projId = UUID.randomUUID();
            stubAllEmpty();
            when(ticketRepository.countTicketsByProjectAndStatus(any(), any())).thenReturn(List.of(
                    row(projId, "PROJ", "Project A", "OPEN", 3L),
                    row(projId, "PROJ", "Project A", "IN_PROGRESS", 2L),
                    row(projId, "PROJ", "Project A", "RESOLVED", 5L)
            ));

            ProjectTicketStatsResponse stats = service.getDashboard().projectStats().get(0);

            assertThat(stats.open()).isEqualTo(3);
            assertThat(stats.inProgress()).isEqualTo(2);
            assertThat(stats.resolved()).isEqualTo(5);
            assertThat(stats.projectKey()).isEqualTo("PROJ");
            assertThat(stats.projectName()).isEqualTo("Project A");
        }

        @Test
        void standByGroupedWithInProgress() {
            UUID projId = UUID.randomUUID();
            stubAllEmpty();
            when(ticketRepository.countTicketsByProjectAndStatus(any(), any())).thenReturn(List.of(
                    row(projId, "PROJ", "Project A", "IN_PROGRESS", 2L),
                    row(projId, "PROJ", "Project A", "STAND_BY", 4L)
            ));

            assertThat(service.getDashboard().projectStats().get(0).inProgress()).isEqualTo(6);
        }

        @Test
        void multipleProjects_allPresentInOrder() {
            UUID proj1 = UUID.randomUUID();
            UUID proj2 = UUID.randomUUID();
            stubAllEmpty();
            when(ticketRepository.countTicketsByProjectAndStatus(any(), any())).thenReturn(List.of(
                    row(proj1, "ALPHA", "Alpha Project", "OPEN", 1L),
                    row(proj2, "BETA", "Beta Project", "RESOLVED", 7L)
            ));

            List<ProjectTicketStatsResponse> stats = service.getDashboard().projectStats();

            assertThat(stats).hasSize(2);
            assertThat(stats).extracting(ProjectTicketStatsResponse::projectKey)
                    .containsExactly("ALPHA", "BETA");
        }

        @Test
        void multipleStatusRowsSameProject_accumulated() {
            UUID projId = UUID.randomUUID();
            stubAllEmpty();
            when(ticketRepository.countTicketsByProjectAndStatus(any(), any())).thenReturn(List.of(
                    row(projId, "PROJ", "Project A", "OPEN", 4L),
                    row(projId, "PROJ", "Project A", "STAND_BY", 1L),
                    row(projId, "PROJ", "Project A", "IN_PROGRESS", 3L),
                    row(projId, "PROJ", "Project A", "RESOLVED", 2L)
            ));

            ProjectTicketStatsResponse stats = service.getDashboard().projectStats().get(0);

            assertThat(stats.open()).isEqualTo(4);
            assertThat(stats.inProgress()).isEqualTo(4); // 3 + 1 STAND_BY
            assertThat(stats.resolved()).isEqualTo(2);
        }

        @Test
        void noActiveTickets_returnsEmptyList() {
            stubAllEmpty();
            when(ticketRepository.countTicketsByProjectAndStatus(any(), any())).thenReturn(List.of());

            assertThat(service.getDashboard().projectStats()).isEmpty();
        }

        @Test
        void queriesActiveStatuses_includesStandBy() {
            stubAllEmpty();

            service.getDashboard();

            verify(ticketRepository).countTicketsByProjectAndStatus(
                    any(),
                    argThat(s -> s.contains("OPEN") && s.contains("IN_PROGRESS")
                            && s.contains("STAND_BY") && s.contains("RESOLVED")));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubAllEmpty() {
        when(ticketRepository.findReportedByUserAndStatusNotIn(any(), any())).thenReturn(List.of());
        when(ticketRepository.findAssignedToUserAndStatusNotIn(any(), any())).thenReturn(List.of());
        when(ticketRepository.findWatchedByUserIdAndStatusNotIn(any(), any())).thenReturn(List.of());
        when(ticketRepository.findDueSoonForUser(any(), any(), any(), any())).thenReturn(List.of());
        when(ticketRepository.countTicketsByProjectAndStatus(any(), any())).thenReturn(List.of());
    }

    private static Object[] row(UUID projId, String key, String name, String status, long count) {
        return new Object[]{projId, key, name, status, count};
    }
}
