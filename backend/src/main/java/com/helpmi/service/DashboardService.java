package com.helpmi.service;

import com.helpmi.domain.User;
import com.helpmi.dto.response.DashboardResponse;
import com.helpmi.dto.response.ProjectTicketStatsResponse;
import com.helpmi.dto.response.TicketSummaryResponse;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final List<String> EXCLUDED_STATUSES = List.of("CLOSED", "CANCELLED", "RESOLVED");
    private static final List<String> ACTIVE_STATUSES = List.of("OPEN", "IN_PROGRESS", "STAND_BY");

    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;

    public DashboardResponse getDashboard() {
        User user = currentUserService.getCurrentUser();
        UUID userId = user.getId();
        LocalDate today = LocalDate.now();

        return new DashboardResponse(
                ticketRepository.findReportedByUserAndStatusNotIn(userId, EXCLUDED_STATUSES)
                        .stream().map(TicketSummaryResponse::from).toList(),
                ticketRepository.findAssignedToUserAndStatusNotIn(userId, EXCLUDED_STATUSES)
                        .stream().map(TicketSummaryResponse::from).toList(),
                ticketRepository.findWatchedByUserIdAndStatusNotIn(userId, EXCLUDED_STATUSES)
                        .stream().map(TicketSummaryResponse::from).toList(),
                ticketRepository.findDueSoonForUser(userId, today, today.plusDays(7), EXCLUDED_STATUSES)
                        .stream().map(TicketSummaryResponse::from).toList(),
                buildProjectStats(userId)
        );
    }

    private List<ProjectTicketStatsResponse> buildProjectStats(UUID userId) {
        List<Object[]> rows = ticketRepository.countTicketsByProjectAndStatus(userId, ACTIVE_STATUSES);

        Map<UUID, int[]> counts = new LinkedHashMap<>();
        Map<UUID, String[]> meta = new LinkedHashMap<>();

        for (Object[] row : rows) {
            UUID id = (UUID) row[0];
            String key = (String) row[1];
            String name = (String) row[2];
            String status = (String) row[3];
            int n = ((Long) row[4]).intValue();

            meta.putIfAbsent(id, new String[]{key, name});
            int[] c = counts.computeIfAbsent(id, x -> new int[2]);
            switch (status) {
                case "OPEN"                    -> c[0] += n;
                case "IN_PROGRESS", "STAND_BY" -> c[1] += n;
            }
        }

        List<ProjectTicketStatsResponse> result = new ArrayList<>();
        for (Map.Entry<UUID, String[]> e : meta.entrySet()) {
            int[] c = counts.get(e.getKey());
            result.add(new ProjectTicketStatsResponse(e.getKey(), e.getValue()[0], e.getValue()[1], c[0], c[1]));
        }
        return result;
    }
}
