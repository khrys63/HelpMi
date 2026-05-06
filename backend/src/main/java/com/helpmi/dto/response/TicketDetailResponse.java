package com.helpmi.dto.response;

import com.helpmi.domain.enums.ResolutionType;
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
        ResolutionType resolutionType,
        List<CommentResponse> comments,
        List<AttachmentResponse> attachments,
        List<TicketLinkResponse> links,
        List<OrganizationSummary> organizations,
        List<LabelResponse> labels,
        List<UserSummary> watchers,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt,
        boolean canAssign,
        boolean canClone
) {}
