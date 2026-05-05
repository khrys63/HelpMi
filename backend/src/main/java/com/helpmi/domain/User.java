package com.helpmi.domain;

import com.helpmi.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;


@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    @Builder.Default
    private String firstName = "";

    @Column(name = "last_name", nullable = false)
    @Builder.Default
    private String lastName = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_organizations",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "organization_id"))
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Organization> organizations = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<UserProject> userProjects = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private String theme = "light";

    @Column(nullable = false)
    @Builder.Default
    private String locale = "fr";

    @Column(name = "notif_assigned", nullable = false)
    @Builder.Default
    private boolean notifAssigned = true;

    @Column(name = "notif_comment", nullable = false)
    @Builder.Default
    private boolean notifComment = true;

    @Column(name = "notif_status_changed", nullable = false)
    @Builder.Default
    private boolean notifStatusChanged = true;

    @Column(name = "notif_watcher_added", nullable = false)
    @Builder.Default
    private boolean notifWatcherAdded = true;

    @Column(name = "notif_ticket_created", nullable = false)
    @Builder.Default
    private boolean notifTicketCreated = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
