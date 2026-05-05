package com.helpmi.service;

import com.helpmi.domain.Project;
import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.domain.enums.UserRole;
import com.helpmi.repository.UserProjectRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import static com.helpmi.Fixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock UserProjectRepository userProjectRepository;
    @InjectMocks NotificationService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "from", "noreply@test.com");
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:5173");
        MimeMessage msg = new MimeMessage(Session.getInstance(new Properties()));
        lenient().when(mailSender.createMimeMessage()).thenReturn(msg);
    }

    private User activeUser() {
        return User.builder().id(UUID.randomUUID()).email("u@test.com")
                .firstName("U").lastName("U").role(UserRole.USER).active(true).build();
    }

    private User inactiveUser() {
        return User.builder().id(UUID.randomUUID()).email("i@test.com")
                .firstName("I").lastName("U").role(UserRole.USER).active(false).build();
    }

    private User userWithPref(String pref, boolean value) {
        User.UserBuilder b = User.builder().id(UUID.randomUUID()).email("p@test.com")
                .firstName("P").lastName("U").role(UserRole.USER).active(true);
        return switch (pref) {
            case "notifAssigned"      -> b.notifAssigned(value).build();
            case "notifComment"       -> b.notifComment(value).build();
            case "notifStatusChanged" -> b.notifStatusChanged(value).build();
            case "notifWatcherAdded"  -> b.notifWatcherAdded(value).build();
            case "notifTicketCreated" -> b.notifTicketCreated(value).build();
            default -> throw new IllegalArgumentException(pref);
        };
    }

    // ── notifyAssigned ────────────────────────────────────────────────────────

    @Nested
    class NotifyAssigned {

        @Test
        void nullAssignee_doesNotSend() {
            service.notifyAssigned(ticket(project(), clientUser()), null, adminUser());
            verifyNoInteractions(mailSender);
        }

        @Test
        void selfAssignment_doesNotSend() {
            User actor = clientUser();
            service.notifyAssigned(ticket(project(), actor), actor, actor);
            verifyNoInteractions(mailSender);
        }

        @Test
        void inactiveAssignee_doesNotSend() {
            service.notifyAssigned(ticket(project(), adminUser()), inactiveUser(), adminUser());
            verifyNoInteractions(mailSender);
        }

        @Test
        void preferenceDisabled_doesNotSend() {
            User noNotif = userWithPref("notifAssigned", false);
            service.notifyAssigned(ticket(project(), adminUser()), noNotif, adminUser());
            verifyNoInteractions(mailSender);
        }

        @Test
        void happyPath_sendsOneEmail() {
            service.notifyAssigned(ticket(project(), adminUser()), activeUser(), adminUser());
            verify(mailSender).send(any(MimeMessage.class));
        }
    }

    // ── notifyComment ─────────────────────────────────────────────────────────

    @Nested
    class NotifyComment {

        @Test
        void authorExcluded_assigneeReceives() {
            User author = clientUser();
            Ticket ticket = ticket(project(), author); // reporter == author → exclu
            ticket.setAssignee(activeUser());
            service.notifyComment(ticket, author);
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        void noRecipientsAfterExclusion_doesNotSend() {
            User author = clientUser();
            Ticket ticket = ticket(project(), author); // reporter == author, pas d'assigné, pas de watchers
            service.notifyComment(ticket, author);
            verifyNoInteractions(mailSender);
        }

        @Test
        void inactiveRecipient_skipped() {
            User author = clientUser();
            Ticket ticket = ticket(project(), inactiveUser()); // reporter inactif
            service.notifyComment(ticket, author);
            verifyNoInteractions(mailSender);
        }

        @Test
        void preferenceDisabled_skipped() {
            User author = clientUser();
            Ticket ticket = ticket(project(), userWithPref("notifComment", false));
            service.notifyComment(ticket, author);
            verifyNoInteractions(mailSender);
        }

        @Test
        void watcherReceivesEmail() {
            User author = clientUser();
            Ticket ticket = ticket(project(), author); // reporter == author → exclu
            ticket.getWatchers().add(activeUser());
            service.notifyComment(ticket, author);
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        void reporterAndAssigneeAndWatcher_threeEmails() {
            User author = clientUser();
            Ticket ticket = ticket(project(), activeUser()); // reporter != author
            ticket.setAssignee(activeUser());
            ticket.getWatchers().add(activeUser());
            service.notifyComment(ticket, author);
            verify(mailSender, times(3)).send(any(MimeMessage.class));
        }
    }

    // ── notifyStatusChanged ───────────────────────────────────────────────────

    @Nested
    class NotifyStatusChanged {

        @Test
        void actorExcluded_reporterReceives() {
            User actor = clientUser();
            Ticket ticket = ticket(project(), activeUser()); // reporter != actor
            service.notifyStatusChanged(ticket, "OPEN", "IN_PROGRESS", actor);
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        void actorIsReporter_doesNotSendToSelf() {
            User actor = clientUser();
            Ticket ticket = ticket(project(), actor); // reporter == actor → exclu
            service.notifyStatusChanged(ticket, "OPEN", "IN_PROGRESS", actor);
            verifyNoInteractions(mailSender);
        }

        @Test
        void preferenceDisabled_skipped() {
            User actor = clientUser();
            Ticket ticket = ticket(project(), userWithPref("notifStatusChanged", false));
            service.notifyStatusChanged(ticket, "OPEN", "IN_PROGRESS", actor);
            verifyNoInteractions(mailSender);
        }
    }

    // ── notifyWatcherAdded ────────────────────────────────────────────────────

    @Nested
    class NotifyWatcherAdded {

        @Test
        void actorExcluded_doesNotSendToSelf() {
            User actor = clientUser();
            service.notifyWatcherAdded(ticket(project(), clientUser()), Set.of(actor), actor);
            verifyNoInteractions(mailSender);
        }

        @Test
        void happyPath_sendsToEachNewWatcher() {
            User actor = adminUser();
            service.notifyWatcherAdded(
                    ticket(project(), actor), Set.of(activeUser(), activeUser()), actor);
            verify(mailSender, times(2)).send(any(MimeMessage.class));
        }

        @Test
        void inactiveWatcher_skipped() {
            User actor = adminUser();
            service.notifyWatcherAdded(
                    ticket(project(), actor), Set.of(inactiveUser()), actor);
            verifyNoInteractions(mailSender);
        }

        @Test
        void preferenceDisabled_skipped() {
            User actor = adminUser();
            service.notifyWatcherAdded(
                    ticket(project(), actor), Set.of(userWithPref("notifWatcherAdded", false)), actor);
            verifyNoInteractions(mailSender);
        }
    }

    // ── notifyTicketCreated ───────────────────────────────────────────────────

    @Nested
    class NotifyTicketCreated {

        @Test
        void noManagers_doesNotSend() {
            Project project = project();
            User reporter = clientUser();
            when(userProjectRepository.findUsersByProjectIdAndRole(project.getId(), "MANAGER"))
                    .thenReturn(List.of());
            service.notifyTicketCreated(project.getId(), ticket(project, reporter), reporter);
            verifyNoInteractions(mailSender);
        }

        @Test
        void reporterIsManager_excluded() {
            Project project = project();
            User reporter = clientUser();
            when(userProjectRepository.findUsersByProjectIdAndRole(project.getId(), "MANAGER"))
                    .thenReturn(List.of(reporter));
            service.notifyTicketCreated(project.getId(), ticket(project, reporter), reporter);
            verifyNoInteractions(mailSender);
        }

        @Test
        void happyPath_sendsToAllManagers() {
            Project project = project();
            User reporter = clientUser();
            User manager1 = activeUser();
            User manager2 = activeUser();
            when(userProjectRepository.findUsersByProjectIdAndRole(project.getId(), "MANAGER"))
                    .thenReturn(List.of(manager1, manager2));
            service.notifyTicketCreated(project.getId(), ticket(project, reporter), reporter);
            verify(mailSender, times(2)).send(any(MimeMessage.class));
        }

        @Test
        void inactiveManager_skipped() {
            Project project = project();
            User reporter = clientUser();
            when(userProjectRepository.findUsersByProjectIdAndRole(project.getId(), "MANAGER"))
                    .thenReturn(List.of(inactiveUser()));
            service.notifyTicketCreated(project.getId(), ticket(project, reporter), reporter);
            verifyNoInteractions(mailSender);
        }

        @Test
        void preferenceDisabled_skipped() {
            Project project = project();
            User reporter = clientUser();
            when(userProjectRepository.findUsersByProjectIdAndRole(project.getId(), "MANAGER"))
                    .thenReturn(List.of(userWithPref("notifTicketCreated", false)));
            service.notifyTicketCreated(project.getId(), ticket(project, reporter), reporter);
            verifyNoInteractions(mailSender);
        }
    }
}
