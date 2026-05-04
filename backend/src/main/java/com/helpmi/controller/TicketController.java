package com.helpmi.controller;

import com.helpmi.dto.request.ChangeStatusRequest;
import com.helpmi.dto.request.CreateTicketRequest;
import com.helpmi.dto.request.DueDateRequest;
import com.helpmi.dto.request.MoveTicketRequest;
import com.helpmi.dto.request.SetAssigneeRequest;
import com.helpmi.dto.request.UpdateTicketRequest;
import com.helpmi.dto.response.ChangeStatusResponse;
import com.helpmi.dto.response.LabelResponse;
import com.helpmi.dto.response.OrganizationSummary;
import com.helpmi.dto.response.TicketDetailResponse;
import com.helpmi.dto.response.TicketHistoryResponse;
import com.helpmi.dto.response.TicketResponse;
import com.helpmi.service.TicketHistoryService;
import com.helpmi.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketHistoryService ticketHistoryService;

    @GetMapping
    public Page<TicketResponse> list(
            @PathVariable UUID projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID assigneeId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ticketService.getTickets(projectId, status, priority, type, assigneeId, pageable);
    }

    @GetMapping("/{ticketId}")
    public TicketDetailResponse get(@PathVariable UUID projectId, @PathVariable UUID ticketId) {
        return ticketService.getTicket(projectId, ticketId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@PathVariable UUID projectId, @Valid @RequestBody CreateTicketRequest req) {
        return ticketService.createTicket(projectId, req);
    }

    @PutMapping("/{ticketId}")
    public TicketResponse update(@PathVariable UUID projectId, @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest req) {
        return ticketService.updateTicket(projectId, ticketId, req);
    }

    @PatchMapping("/{ticketId}/status")
    public ChangeStatusResponse changeStatus(@PathVariable UUID projectId, @PathVariable UUID ticketId,
            @Valid @RequestBody ChangeStatusRequest req) {
        return ticketService.changeStatus(projectId, ticketId, req.status());
    }

    @PatchMapping("/{ticketId}/assignee")
    public TicketResponse setAssignee(@PathVariable UUID projectId, @PathVariable UUID ticketId,
            @RequestBody SetAssigneeRequest req) {
        return ticketService.setAssignee(projectId, ticketId, req.assigneeId());
    }

    @PatchMapping("/{ticketId}/due-date")
    public TicketResponse setDueDate(@PathVariable UUID projectId, @PathVariable UUID ticketId,
            @Valid @RequestBody DueDateRequest req) {
        return ticketService.setDueDate(projectId, ticketId, req.dueDate());
    }

    @PutMapping("/{ticketId}/organizations")
    public List<OrganizationSummary> setOrganizations(@PathVariable UUID projectId, @PathVariable UUID ticketId,
            @RequestBody Map<String, List<UUID>> body) {
        return ticketService.setOrganizations(projectId, ticketId, body.get("organizationIds"));
    }

    @PutMapping("/{ticketId}/labels")
    public List<LabelResponse> setLabels(@PathVariable UUID projectId, @PathVariable UUID ticketId,
            @RequestBody Map<String, List<UUID>> body) {
        return ticketService.setLabels(projectId, ticketId, body.get("labelIds"));
    }

    @PostMapping("/{ticketId}/move")
    public TicketResponse move(@PathVariable UUID projectId, @PathVariable UUID ticketId,
            @Valid @RequestBody MoveTicketRequest req) {
        return ticketService.moveTicket(projectId, ticketId, req.targetProjectId());
    }

    @PostMapping("/{ticketId}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse clone(@PathVariable UUID projectId, @PathVariable UUID ticketId) {
        return ticketService.cloneTicket(projectId, ticketId);
    }

    @GetMapping("/{ticketId}/history")
    public List<TicketHistoryResponse> history(@PathVariable UUID projectId, @PathVariable UUID ticketId) {
        return ticketHistoryService.getHistory(projectId, ticketId);
    }

    @DeleteMapping("/{ticketId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID projectId, @PathVariable UUID ticketId) {
        ticketService.deleteTicket(projectId, ticketId);
    }
}
