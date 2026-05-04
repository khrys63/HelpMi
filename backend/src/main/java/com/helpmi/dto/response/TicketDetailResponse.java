package com.helpmi.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketDetailResponse(
        UUID id,
        String reference,
        String title,
        String description,
        String status,
        String priority,
        String type,
        LocalDate dueDate,
        UUID projectId,
        String projectName,
        String projectKey,
        UserSummary reporter,
        UserSummary assignee,
        List<CommentResponse> comments,
        List<AttachmentResponse> attachments,
        List<TicketLinkResponse> links,
        List<ClientResponse> clients,
        List<LabelResponse> labels,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt,
        boolean canAssign,
        boolean canClone
) {}
