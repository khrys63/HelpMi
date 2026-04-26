package com.helpmi.repository;

import com.helpmi.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByTicketIdOrderByUploadedAtDesc(UUID ticketId);
    long countByTicketId(UUID ticketId);
}
