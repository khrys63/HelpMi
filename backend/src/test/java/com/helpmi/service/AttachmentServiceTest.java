package com.helpmi.service;

import com.helpmi.domain.Attachment;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.dto.response.AttachmentResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.AttachmentRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import com.helpmi.storage.StorageService;
import org.apache.tika.Tika;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock AttachmentRepository attachmentRepository;
    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserService currentUserService;
    @Mock StorageService storageService;
    @Mock ProjectService projectService;
    @Mock AuditService auditService;
    @Mock Tika tika;

    @InjectMocks AttachmentService service;

    // ── upload ────────────────────────────────────────────────────────────────

    @Test
    void upload_ticketNotFound_throwsNotFoundException() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.upload(ticketId, file))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void upload_savesAttachmentAndReturnsResponse() throws IOException {
        User uploader = adminUser();
        Ticket ticket = ticket(project(), uploader);
        MultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(tika.detect(any(byte[].class), anyString())).thenReturn("application/pdf");
        when(currentUserService.getCurrentUser()).thenReturn(uploader);
        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            return Attachment.builder()
                    .id(UUID.randomUUID())
                    .ticket(a.getTicket())
                    .fileName(a.getFileName())
                    .storedName(a.getStoredName())
                    .contentType(a.getContentType())
                    .size(a.getSize())
                    .uploadedBy(a.getUploadedBy())
                    .build();
        });

        AttachmentResponse response = service.upload(ticket.getId(), file);

        assertThat(response.fileName()).isEqualTo("report.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.size()).isEqualTo(3L);
        assertThat(response.downloadUrl()).startsWith("/api/attachments/");
        verify(storageService).store(any(), any(), eq(3L), eq("application/pdf"));
    }

    @Test
    void upload_preservesOriginalFileExtension() throws IOException {
        User uploader = adminUser();
        Ticket ticket = ticket(project(), uploader);
        MultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1});

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(tika.detect(any(byte[].class), anyString())).thenReturn("image/png");
        when(currentUserService.getCurrentUser()).thenReturn(uploader);
        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            return Attachment.builder().id(UUID.randomUUID())
                    .ticket(a.getTicket()).fileName(a.getFileName())
                    .storedName(a.getStoredName()).contentType(a.getContentType())
                    .size(a.getSize()).uploadedBy(a.getUploadedBy()).build();
        });

        service.upload(ticket.getId(), file);

        verify(storageService).store(argThat(key -> key.endsWith(".png")), any(), anyLong(), any());
    }

    @Test
    void upload_noExtension_storesWithoutExtension() throws IOException {
        User uploader = adminUser();
        Ticket ticket = ticket(project(), uploader);
        MultipartFile file = new MockMultipartFile("file", "Makefile", "text/plain", new byte[]{1});

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(tika.detect(any(byte[].class), anyString())).thenReturn("text/plain");
        when(currentUserService.getCurrentUser()).thenReturn(uploader);
        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            return Attachment.builder().id(UUID.randomUUID())
                    .ticket(a.getTicket()).fileName(a.getFileName())
                    .storedName(a.getStoredName()).contentType(a.getContentType())
                    .size(a.getSize()).uploadedBy(a.getUploadedBy()).build();
        });

        AttachmentResponse response = service.upload(ticket.getId(), file);

        assertThat(response.fileName()).isEqualTo("Makefile");
    }

    // ── validation MIME (fix M1) ──────────────────────────────────────────────

    @Test
    void upload_disallowedType_throwsIllegalArgument() throws IOException {
        User uploader = agentUser();
        Ticket ticket = ticket(project(), uploader);
        MultipartFile file = new MockMultipartFile("file", "malicious.html", "text/html",
                "<script>alert(1)</script>".getBytes());
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(tika.detect(any(byte[].class), anyString())).thenReturn("text/html");

        assertThatThrownBy(() -> service.upload(ticket.getId(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non autorisé");
    }

    @Test
    void upload_usesDetectedType_notClientProvided() throws IOException {
        User uploader = adminUser();
        Ticket ticket = ticket(project(), uploader);
        // Le client déclare text/html mais Tika détecte image/png
        MultipartFile file = new MockMultipartFile("file", "image.png", "text/html", new byte[]{1});
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        when(tika.detect(any(byte[].class), anyString())).thenReturn("image/png");
        when(currentUserService.getCurrentUser()).thenReturn(uploader);
        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            return Attachment.builder().id(UUID.randomUUID())
                    .ticket(a.getTicket()).fileName(a.getFileName())
                    .storedName(a.getStoredName()).contentType(a.getContentType())
                    .size(a.getSize()).uploadedBy(a.getUploadedBy()).build();
        });

        AttachmentResponse response = service.upload(ticket.getId(), file);

        assertThat(response.contentType()).isEqualTo("image/png");
        verify(storageService).store(any(), any(), anyLong(), eq("image/png"));
    }

    // ── download ──────────────────────────────────────────────────────────────

    @Test
    void download_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(attachmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void download_fileNotInStorage_throwsNotFoundException() throws Exception {
        Attachment attachment = attachmentFor(adminUser(), "ghost.pdf");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(storageService.retrieve(attachment.getStoredName())).thenThrow(new NotFoundException("Fichier introuvable"));

        assertThatThrownBy(() -> service.download(attachment.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void download_existingFile_returnsResource() throws Exception {
        Attachment attachment = attachmentFor(adminUser(), "existing.txt");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(storageService.retrieve(attachment.getStoredName()))
                .thenReturn(new ByteArrayInputStream("hello".getBytes()));

        var resource = service.download(attachment.getId());

        assertThat(resource).isNotNull();
        assertThat(resource.getInputStream()).isNotNull();
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(attachmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_otherUsersAttachment_asClient_throwsForbidden() throws IOException {
        User owner = adminUser();
        User other = clientUser();
        Attachment attachment = attachmentFor(owner, "secret.pdf");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(currentUserService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> service.delete(attachment.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_admin_canDeleteOtherUsersAttachment() throws IOException {
        User owner = agentUser();
        User admin = adminUser();
        Attachment attachment = attachmentFor(owner, "todelete.pdf");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        service.delete(attachment.getId());

        verify(storageService).delete(attachment.getStoredName());
        verify(attachmentRepository).delete(attachment);
    }

    @Test
    void delete_ownAttachment_deletesSuccessfully() throws IOException {
        User owner = agentUser();
        Attachment attachment = attachmentFor(owner, "mine.pdf");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        service.delete(attachment.getId());

        verify(storageService).delete(attachment.getStoredName());
        verify(attachmentRepository).delete(attachment);
    }

    @Test
    void delete_fileAlreadyMissing_stillDeletesRecord() throws IOException {
        User owner = agentUser();
        Attachment attachment = attachmentFor(owner, "already_gone.pdf");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        service.delete(attachment.getId());

        verify(attachmentRepository).delete(attachment);
    }

    // ── contrôle d'accès projet (fix H1) ─────────────────────────────────────

    @Test
    void upload_accessDenied_throwsForbidden() {
        User uploader = agentUser();
        Ticket ticket = ticket(project(), uploader);
        MultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1});
        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
        doThrow(new ForbiddenException("Accès refusé")).when(projectService).requireProjectAccess(any());

        assertThatThrownBy(() -> service.upload(ticket.getId(), file))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void download_accessDenied_throwsForbidden() throws Exception {
        Attachment attachment = attachmentFor(agentUser(), "secret.pdf");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        doThrow(new ForbiddenException("Accès refusé")).when(projectService).requireProjectAccess(any());

        assertThatThrownBy(() -> service.download(attachment.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private Attachment attachmentFor(User uploader, String storedName) {
        Ticket t = ticket(project(), uploader);
        return Attachment.builder()
                .id(UUID.randomUUID())
                .ticket(t)
                .fileName(storedName)
                .storedName(storedName)
                .contentType("application/octet-stream")
                .size(10L)
                .uploadedBy(uploader)
                .build();
    }
}
