package com.helpmi.service;

import com.helpmi.domain.Attachment;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
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
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;
    private final StorageService storageService;

    public AttachmentResponse upload(UUID ticketId, MultipartFile file) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket introuvable"));

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + extension;
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        storageService.store(storedName, file.getInputStream(), file.getSize(), contentType);

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .fileName(originalName != null ? originalName : storedName)
                .storedName(storedName)
                .contentType(file.getContentType())
                .size(file.getSize())
                .uploadedBy(currentUserService.getCurrentUser())
                .build();

        return toResponse(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public Resource download(UUID attachmentId) throws IOException {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Pièce jointe introuvable"));
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
        User currentUser = currentUserService.getCurrentUser();
        if (!attachment.getUploadedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Vous ne pouvez supprimer que vos propres pièces jointes");
        }
        storageService.delete(attachment.getStoredName());
        attachmentRepository.delete(attachment);
    }

    private AttachmentResponse toResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getTicket().getId(), a.getFileName(), a.getContentType(),
                a.getSize(), UserSummary.from(a.getUploadedBy()), a.getUploadedAt(),
                "/api/attachments/" + a.getId());
    }
}
