package com.helpmi.service;

import com.helpmi.domain.Ticket;
import com.helpmi.domain.User;
import com.helpmi.repository.UserProjectRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final UserProjectRepository userProjectRepository;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.base-url}")
    private String baseUrl;

    // ── API publique ──────────────────────────────────────────────────────────

    @Async("mailExecutor")
    public void notifyAssigned(Ticket ticket, User newAssignee, User actor) {
        if (newAssignee == null) return;
        if (newAssignee.getId().equals(actor.getId())) return;
        if (!newAssignee.isActive() || !newAssignee.isNotifAssigned()) return;

        String url = ticketUrl(ticket);
        String subject = "[HelpMi] Ticket " + ticket.getReference() + " vous a été assigné";
        sendSafe(newAssignee.getEmail(), subject, wrap(
            "Bonjour " + newAssignee.getFirstName() + ","
            + "<br><br><b>" + actor.getFullName() + "</b> vous a assigné le ticket "
            + link(url, ticket.getReference() + " — " + escape(ticket.getTitle())) + "."
        ));
    }

    @Async("mailExecutor")
    public void notifyComment(Ticket ticket, User author) {
        String url = ticketUrl(ticket);
        String subject = "[HelpMi] Nouveau commentaire sur " + ticket.getReference();
        for (User recipient : buildRecipients(ticket, author)) {
            if (!recipient.isActive() || !recipient.isNotifComment()) continue;
            sendSafe(recipient.getEmail(), subject, wrap(
                "Bonjour " + recipient.getFirstName() + ","
                + "<br><br><b>" + author.getFullName() + "</b> a ajouté un commentaire sur "
                + link(url, ticket.getReference() + " — " + escape(ticket.getTitle())) + "."
            ));
        }
    }

    @Async("mailExecutor")
    public void notifyStatusChanged(Ticket ticket, String oldStatus, String newStatus, User actor) {
        String url = ticketUrl(ticket);
        String subject = "[HelpMi] " + ticket.getReference() + " → " + newStatus;
        for (User recipient : buildRecipients(ticket, actor)) {
            if (!recipient.isActive() || !recipient.isNotifStatusChanged()) continue;
            sendSafe(recipient.getEmail(), subject, wrap(
                "Bonjour " + recipient.getFirstName() + ","
                + "<br><br><b>" + actor.getFullName() + "</b> a changé le statut du ticket "
                + link(url, ticket.getReference() + " — " + escape(ticket.getTitle()))
                + " de <b>" + oldStatus + "</b> à <b>" + newStatus + "</b>."
            ));
        }
    }

    @Async("mailExecutor")
    public void notifyWatcherAdded(Ticket ticket, Set<User> newlyAdded, User actor) {
        String url = ticketUrl(ticket);
        for (User watcher : newlyAdded) {
            if (watcher.getId().equals(actor.getId())) continue;
            if (!watcher.isActive() || !watcher.isNotifWatcherAdded()) continue;
            String subject = "[HelpMi] Vous suivez maintenant " + ticket.getReference();
            sendSafe(watcher.getEmail(), subject, wrap(
                "Bonjour " + watcher.getFirstName() + ","
                + "<br><br><b>" + actor.getFullName() + "</b> vous a ajouté comme observateur du ticket "
                + link(url, ticket.getReference() + " — " + escape(ticket.getTitle())) + "."
            ));
        }
    }

    @Async("mailExecutor")
    public void notifyTicketCreated(UUID projectId, Ticket ticket, User reporter) {
        List<User> managers = userProjectRepository.findUsersByProjectIdAndRole(projectId, "MANAGER");
        String url = ticketUrl(ticket);
        String subject = "[HelpMi] Nouveau ticket " + ticket.getReference() + " — " + escape(ticket.getTitle());
        for (User manager : managers) {
            if (manager.getId().equals(reporter.getId())) continue;
            if (!manager.isActive() || !manager.isNotifTicketCreated()) continue;
            sendSafe(manager.getEmail(), subject, wrap(
                "Bonjour " + manager.getFirstName() + ","
                + "<br><br><b>" + reporter.getFullName() + "</b> a créé le ticket "
                + link(url, ticket.getReference() + " — " + escape(ticket.getTitle()))
                + " dans le projet <b>" + escape(ticket.getProject().getName()) + "</b>."
            ));
        }
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private Set<User> buildRecipients(Ticket ticket, User excludedActor) {
        Set<User> recipients = new HashSet<>();
        if (ticket.getReporter() != null) recipients.add(ticket.getReporter());
        if (ticket.getAssignee() != null) recipients.add(ticket.getAssignee());
        recipients.addAll(ticket.getWatchers());
        recipients.removeIf(u -> u.getId().equals(excludedActor.getId()));
        return recipients;
    }

    private void sendSafe(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.debug("Email envoyé à {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Échec envoi email à {} ({}): {}", to, subject, e.getMessage());
        }
    }

    private String ticketUrl(Ticket ticket) {
        return baseUrl + "/projects/" + ticket.getProject().getId() + "/tickets/" + ticket.getId();
    }

    private static String link(String url, String label) {
        return "<a href=\"" + url + "\" style=\"color:#3b82f6;\">" + label + "</a>";
    }

    private static String wrap(String content) {
        return "<html><body style=\"font-family:sans-serif;font-size:14px;color:#333;line-height:1.6;\">"
             + content
             + "<br><br>"
             + "<hr style=\"border:none;border-top:1px solid #e5e7eb;\"/>"
             + "<p style=\"color:#9ca3af;font-size:12px;margin:8px 0 0;\">HelpMi — vous recevez cet email car vous êtes concerné par ce ticket.<br>"
             + "Gérez vos préférences dans votre profil.</p>"
             + "</body></html>";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
