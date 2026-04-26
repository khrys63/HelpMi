package com.helpmi.repository;

import com.helpmi.domain.TicketLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketLinkRepository extends JpaRepository<TicketLink, UUID> {
    List<TicketLink> findBySourceTicketId(UUID ticketId);
    List<TicketLink> findByTargetTicketId(UUID ticketId);
    boolean existsBySourceTicketIdAndTargetTicketIdAndLinkType(UUID sourceId, UUID targetId, String linkType);
    long countByLinkType(String linkType);
}
