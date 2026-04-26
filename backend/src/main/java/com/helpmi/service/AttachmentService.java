package com.helpmi.service;

import com.helpmi.config.StorageConfig;
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
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final CurrentUserService currentUserService;
    private final StorageConfig storageConfig;

    public AttachmentResponse upload(UUID ticketId, MultipartFile file) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new NotFoundException("Ticket introuvable"));

        String extension = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        String storedName = UUID.randomUUID() + extension;
        Path dest = storageConfig.getStoragePath().resolve(storedName);
        Files.createDirectories(dest.getParent());
        file.transferTo(dest);

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
    public Resource download(UUID attachmentId) throws MalformedURLException {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new NotFoundException("Pièce jointe introuvable"));
        Path path = storageConfig.getStoragePath().resolve(attachment.getStoredName());
        Resource resource = new UrlResource(path.toUri());
        if (!resource.exists()) throw new NotFoundException("Fichier introuvable sur le disque");
        return resource;
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
        Path path = storageConfig.getStoragePath().resolve(attachment.getStoredName());
        Files.deleteIfExists(path);
        attachmentRepository.delete(attachment);
    }

    private AttachmentResponse toResponse(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getTicket().getId(), a.getFileName(), a.getContentType(),
                a.getSize(), UserSummary.from(a.getUploadedBy()), a.getUploadedAt(),
                "/api/attachments/" + a.getId());
    }
}
