package com.helpmi.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_ticket_id", nullable = false)
    private Ticket sourceTicket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_ticket_id", nullable = false)
    private Ticket targetTicket;

    @Column(name = "link_type", nullable = false, length = 50)
    private String linkType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
