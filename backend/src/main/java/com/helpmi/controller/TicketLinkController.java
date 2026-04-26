package com.helpmi.controller;

import com.helpmi.dto.request.CreateTicketLinkRequest;
import com.helpmi.dto.response.TicketLinkResponse;
import com.helpmi.dto.response.TicketSummary;
import com.helpmi.service.TicketLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TicketLinkController {

    private final TicketLinkService ticketLinkService;

    @PostMapping("/api/tickets/{ticketId}/links")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketLinkResponse createLink(@PathVariable UUID ticketId,
            @Valid @RequestBody CreateTicketLinkRequest req) {
        return ticketLinkService.createLink(ticketId, req);
    }

    @DeleteMapping("/api/ticket-links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLink(@PathVariable UUID linkId) {
        ticketLinkService.deleteLink(linkId);
    }

    @GetMapping("/api/tickets/search")
    public List<TicketSummary> search(@RequestParam String q,
            @RequestParam(required = false) UUID excludeId) {
        return ticketLinkService.search(q, excludeId);
    }
}
