package com.helpmi.service;

import com.helpmi.config.StorageConfig;
import com.helpmi.domain.Attachment;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.dto.response.AttachmentResponse;
import com.helpmi.exception.ForbiddenException;
import com.helpmi.exception.NotFoundException;
import com.helpmi.repository.AttachmentRepository;
import com.helpmi.repository.TicketRepository;
import com.helpmi.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static com.helpmi.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @TempDir Path tempDir;

    @Mock AttachmentRepository attachmentRepository;
    @Mock TicketRepository ticketRepository;
    @Mock CurrentUserService currentUserService;
    @Mock StorageConfig storageConfig;

    @InjectMocks AttachmentService service;

    @BeforeEach
    void setupStorage() {
        lenient().when(storageConfig.getStoragePath()).thenReturn(tempDir);
    }

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
        when(currentUserService.getCurrentUser()).thenReturn(uploader);
        when(attachmentRepository.save(any())).thenAnswer(inv -> {
            Attachment a = inv.getArgument(0);
            // Simulate JPA setting an ID
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
    }

    @Test
    void upload_preservesOriginalFileExtension() throws IOException {
        User uploader = adminUser();
        Ticket ticket = ticket(project(), uploader);
        MultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1});

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
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

        service.upload(ticket.getId(), file);

        // Verify a file with .png extension was written to the temp directory
        boolean pngWritten = Files.list(tempDir).anyMatch(p -> p.toString().endsWith(".png"));
        assertThat(pngWritten).isTrue();
    }

    @Test
    void upload_noExtension_storesWithoutExtension() throws IOException {
        User uploader = adminUser();
        Ticket ticket = ticket(project(), uploader);
        MultipartFile file = new MockMultipartFile("file", "Makefile", "text/plain", new byte[]{1});

        when(ticketRepository.findById(ticket.getId())).thenReturn(Optional.of(ticket));
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

    // ── download ──────────────────────────────────────────────────────────────

    @Test
    void download_notFound_throwsNotFoundException() {
        UUID id = UUID.randomUUID();
        when(attachmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void download_fileNotOnDisk_throwsNotFoundException() throws Exception {
        Attachment attachment = attachmentFor(adminUser(), "ghost.pdf");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> service.download(attachment.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("disque");
    }

    @Test
    void download_existingFile_returnsResource() throws Exception {
        Attachment attachment = attachmentFor(adminUser(), "existing.txt");
        Files.writeString(tempDir.resolve("existing.txt"), "hello");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));

        var resource = service.download(attachment.getId());

        assertThat(resource.exists()).isTrue();
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
        Files.writeString(tempDir.resolve("todelete.pdf"), "data");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        service.delete(attachment.getId());

        verify(attachmentRepository).delete(attachment);
    }

    @Test
    void delete_ownAttachment_deletesSuccessfully() throws IOException {
        User owner = agentUser();
        Attachment attachment = attachmentFor(owner, "mine.pdf");
        Files.writeString(tempDir.resolve("mine.pdf"), "data");
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        service.delete(attachment.getId());

        verify(attachmentRepository).delete(attachment);
        assertThat(Files.exists(tempDir.resolve("mine.pdf"))).isFalse();
    }

    @Test
    void delete_fileAlreadyMissing_stillDeletesRecord() throws IOException {
        User owner = agentUser();
        Attachment attachment = attachmentFor(owner, "already_gone.pdf");
        // file does NOT exist on disk — deleteIfExists should not throw
        when(attachmentRepository.findById(attachment.getId())).thenReturn(Optional.of(attachment));
        when(currentUserService.getCurrentUser()).thenReturn(owner);

        service.delete(attachment.getId());

        verify(attachmentRepository).delete(attachment);
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
