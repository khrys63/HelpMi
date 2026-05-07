package com.helpmi.service;

import com.helpmi.domain.Attachment;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.domain.enums.AuditAction;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.dto.response.AttachmentResponse;
import com.helpmi.dto.response.UserSummary;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.AttachmentRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import com.helpmi.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf", "text/plain", "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip"
    );

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;
    private final StorageService storageService;
    private final ProjectService projectService;
    private final AuditService auditService;
    private final Tika tika;

    public AttachmentResponse upload(UUID ticketId, MultipartFile file) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket introuvable"));
        requireEditable(ticket);
        projectService.requireProjectAccess(ticket.getProject().getId());

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + extension;

        byte[] bytes = file.getBytes();
        String detectedType = tika.detect(bytes, originalName != null ? originalName : "");
        if (!ALLOWED_TYPES.contains(detectedType)) {
            throw new IllegalArgumentException("Type de fichier non autorisé");
        }

        storageService.store(storedName, new ByteArrayInputStream(bytes), (long) bytes.length, detectedType);

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .fileName(originalName != null ? originalName : storedName)
                .storedName(storedName)
                .contentType(detectedType)
                .size((long) bytes.length)
                .uploadedBy(currentUserService.getCurrentUser())
                .build();

        return toResponse(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public Resource download(UUID attachmentId) throws IOException {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Pièce jointe introuvable"));
        projectService.requireProjectAccess(attachment.getTicket().getProject().getId());
        return new InputStreamResource(storageService.retrieve(attachment.getStoredName()));
    }

    @Transactional(readOnly = true)
    public Attachment getAttachmentEntity(UUID attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Pièce jointe introuvable"));
    }

    public void delete(UUID attachmentId) throws IOException {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Pièce jointe introuvable"));
        requireEditable(attachment.getTicket());
        User currentUser = currentUserService.getCurrentUser();
        if (!attachment.getUploadedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres pièces jointes");
        }
        String fileName = attachment.getFileName();
        String ticketRef = attachment.getTicket().getReference();
        storageService.delete(attachment.getStoredName());
        attachmentRepository.delete(attachment);
        auditService.log(AuditAction.ATTACHMENT_DELETED, "ATTACHMENT", fileName,
                "ticket=" + ticketRef);
    }

    private static void requireEditable(Ticket ticket) {
        if ("CLOSED".equals(ticket.getStatus()) || "CANCELLED".equals(ticket.getStatus())) {
            throw new ForbiddenException("Ce ticket est clôturé et ne peut plus être modifié");
        }
    }

    private AttachmentResponse toResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getTicket().getId(), a.getFileName(), a.getContentType(),
                a.getSize(), UserSummary.from(a.getUploadedBy()), a.getUploadedAt(),
                "/api/attachments/" + a.getId());
    }
}
