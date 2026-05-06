package com.helpmi.dto.response;

public record TicketCountResponse(int total, int open, int inProgress, int standBy, int resolved) {}
