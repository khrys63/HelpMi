package com.helpmi.dto.response;

import java.util.UUID;

public record ChangeStatusResponse(
        TicketResponse ticket,
        UUID nextTicketId,
        String nextTicketReference
) {}
