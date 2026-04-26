package com.helpmi.dto.response;

import java.util.UUID;

public record TicketLinkResponse(UUID id, TicketSummary linkedTicket, String linkType, String direction) {}
