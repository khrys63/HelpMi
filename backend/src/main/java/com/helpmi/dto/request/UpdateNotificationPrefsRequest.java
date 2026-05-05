package com.helpmi.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationPrefsRequest(
        @NotNull Boolean notifAssigned,
        @NotNull Boolean notifComment,
        @NotNull Boolean notifStatusChanged,
        @NotNull Boolean notifWatcherAdded,
        @NotNull Boolean notifTicketCreated
) {}
