package com.helpmi.controller;

import com.helpmi.dto.response.AttachmentResponse;
import com.helpmi.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/api/tickets/{ticketId}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse upload(@PathVariable UUID ticketId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return attachmentService.upload(ticketId, file);
    }

    @GetMapping("/api/attachments/{attachmentId}")
    public ResponseEntity<Resource> download(@PathVariable UUID attachmentId) throws IOException {
        Resource resource = attachmentService.download(attachmentId);
        var entity = attachmentService.getAttachmentEntity(attachmentId);
        String contentType = entity.getContentType() != null ? entity.getContentType() : "application/octet-stream";
        String safeFilename = entity.getFileName()
                .replaceAll("[\"\\\\]", "_")
                .replaceAll("[\\r\\n]", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFilename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping("/api/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID attachmentId) throws IOException {
        attachmentService.delete(attachmentId);
    }
}
