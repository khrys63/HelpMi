package com.helpmi.service;

import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.dto.response.*;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerTrackingService {

    private static final List<String> CLOSED_STATUSES = List.of("CLOSED", "CANCELLED");

    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    public ManagerTrackingResponse getManagerTracking() {
        User user = currentUserService.getCurrentUser();
        UUID userId = user.getId();

        List<Object[]> rows = ticketRepository.countTicketsByProjectAndAssignee(userId);
        return buildResponse(rows);
    }

    private ManagerTrackingResponse buildResponse(List<Object[]> rows) {
        // projectKey → project data
        Map<String, ManagerProjectTrackingResponse.Builder> projects = new LinkedHashMap<>();
        // projectKey → projectId
        Map<String, UUID> projectIds = new LinkedHashMap<>();
        // projectKey → assigneeId → assignee data
        Map<String, Map<UUID, AssigneeTicketResponse.Builder>> assignees = new HashMap<>();
        // projectKey → assigneeId → counts accumulator
        Map<String, Map<UUID, int[]>> counts = new HashMap<>();

        for (Object[] row : rows) {
            String projectKey = (String) row[1];
            String projectName = (String) row[2];
            UUID projectId = (UUID) row[0];
            UUID assigneeId = (UUID) row[3];
            String firstName = (String) row[4];
            String lastName = (String) row[5];
            String email = (String) row[6];
            String status = (String) row[7];
            int count = ((Long) row[8]).intValue();

            // Project
            projects.computeIfAbsent(projectKey, k ->
                ManagerProjectTrackingResponse.builder().projectId(projectId).key(projectKey).name(projectName));
            projectIds.put(projectKey, projectId);

            // Assignee
            assignees.computeIfAbsent(projectKey, k -> new LinkedHashMap<>());
            Map<UUID, AssigneeTicketResponse.Builder> assigneeMap = assignees.get(projectKey);
            assigneeMap.computeIfAbsent(assigneeId, k ->
                AssigneeTicketResponse.builder().id(assigneeId).email(email).firstName(firstName).lastName(lastName));

            // Counts accumulator: [total, open, inProgress, standBy]
            int[] c = counts.computeIfAbsent(projectKey, k -> new HashMap<>())
                            .computeIfAbsent(assigneeId, k -> new int[4]);
            c[0] += count;
            switch (status) {
                case "OPEN"        -> c[1] += count;
                case "IN_PROGRESS" -> c[2] += count;
                case "STAND_BY"    -> c[3] += count;
            }
        }

        // Build assignees with ticket lists
        List<ManagerProjectTrackingResponse> result = new ArrayList<>();
        for (Map.Entry<String, ManagerProjectTrackingResponse.Builder> projectEntry : projects.entrySet()) {
            String projectKey = projectEntry.getKey();
            ManagerProjectTrackingResponse.Builder projectBuilder = projectEntry.getValue();
            UUID projectId = projectIds.get(projectKey);

            // Assignees
            List<AssigneeTicketResponse> assigneeList = new ArrayList<>();
            for (Map.Entry<UUID, AssigneeTicketResponse.Builder> assigneeEntry : assignees.get(projectKey).entrySet()) {
                UUID assigneeId = assigneeEntry.getKey();
                AssigneeTicketResponse.Builder assigneeBuilder = assigneeEntry.getValue();
                int[] c = counts.get(projectKey).get(assigneeId);

                List<Ticket> tickets = ticketRepository.findByProjectManagerAndAssigneeId(projectId, assigneeId);
                List<TicketSummaryResponse> ticketSummaries = tickets.stream()
                        .map(TicketSummaryResponse::from).toList();

                assigneeBuilder.counts(new TicketCountResponse(c[0], c[1], c[2], c[3]));
                assigneeList.add(assigneeBuilder.tickets(ticketSummaries).build());
            }

            // Unassigned
            List<Ticket> projectUnassigned = ticketRepository.findUnassignedTicketsForProject(projectId);
            int[] uc = new int[4];
            for (Ticket t : projectUnassigned) {
                uc[0]++;
                switch (t.getStatus()) {
                    case "OPEN"        -> uc[1]++;
                    case "IN_PROGRESS" -> uc[2]++;
                    case "STAND_BY"    -> uc[3]++;
                }
            }
            projectBuilder.unassignedCounts(new TicketCountResponse(uc[0], uc[1], uc[2], uc[3]));
            projectBuilder.unassignedTickets(projectUnassigned.stream()
                    .map(TicketSummaryResponse::from)
                    .toList());

            projectBuilder.assignees(assigneeList);
            result.add(projectBuilder.build());
        }

        return new ManagerTrackingResponse(result);
    }
}
