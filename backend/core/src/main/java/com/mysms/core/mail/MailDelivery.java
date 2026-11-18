package com.mysms.core.mail;

import com.mysms.core.error.ApiException;
import com.mysms.core.error.UnavailableException;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Sends plain-text e-mail over SMTP with STARTTLS, built from a module's {@code mysms.<module>.smtp.*} settings.
 * Without a host every send is 503 NOT_CONFIGURED, as is a failed delivery; call it outside a transaction. Addresses
 * and bodies are never logged.
 */
public final class MailDelivery {

    private static final Logger LOG = LoggerFactory.getLogger(MailDelivery.class);
    private static final int SMTP_TIMEOUT_MILLIS = 10_000;

    /** One plain-text message. */
    public record Message(String to, String subject, String body, String replyTo) {
    }

    private final Optional<JavaMailSender> sender;
    private final String from;

    MailDelivery(Optional<JavaMailSender> sender, String from) {
        this.sender = sender;
        this.from = from;
    }

    /**
     * The SMTP sender of these settings; a blank host = no sender (503 NOT_CONFIGURED on use).
     *
     * @param startTlsRequired STARTTLS is always offered; only a local relay without TLS may turn the requirement off
     */
    public static MailDelivery smtp(String host, int port, String username, String password, String from,
                                    boolean startTlsRequired) {
        if (host == null || host.isBlank()) {
            return new MailDelivery(Optional.empty(), null);
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("an SMTP sender address (MYSMS_SMTP_FROM) is required with an SMTP host");
        }
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host.trim());
        mailSender.setPort(port);
        mailSender.setDefaultEncoding("UTF-8");
        Properties mail = mailSender.getJavaMailProperties();
        mail.put("mail.transport.protocol", "smtp");
        mail.put("mail.smtp.starttls.enable", "true");
        mail.put("mail.smtp.starttls.required", Boolean.toString(startTlsRequired));
        mail.put("mail.smtp.connectiontimeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
        mail.put("mail.smtp.timeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
        mail.put("mail.smtp.writetimeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
        if (username != null && !username.isBlank()) {
            mailSender.setUsername(username);
            mailSender.setPassword(password);
            mail.put("mail.smtp.auth", "true");
        }
        return new MailDelivery(Optional.of(mailSender), from.trim());
    }

    /** 503 NOT_CONFIGURED when this deployment has no SMTP settings. */
    public void requireConfigured() {
        if (sender.isEmpty()) {
            throw new NotConfigured();
        }
    }

    /** Sends now; 503 when not configured or when the server refuses or cannot be reached. */
    public void send(Message message) {
        requireConfigured();
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.to());
        if (message.replyTo() != null) {
            mail.setReplyTo(message.replyTo());
        }
        mail.setSubject(message.subject());
        mail.setText(message.body());
        try {
            sender.get().send(mail);
        } catch (MailException e) {
            LOG.warn("e-mail could not be delivered ({})", e.getClass().getSimpleName());
            throw new UnavailableException("The e-mail could not be sent; try again later.");
        }
    }

    /** E-mail delivery is not configured for this deployment: 503 NOT_CONFIGURED, nothing was changed. */
    static final class NotConfigured extends ApiException {

        private static final long serialVersionUID = 1L;

        NotConfigured() {
            super(HttpStatus.SERVICE_UNAVAILABLE, "NOT_CONFIGURED", "E-mail delivery is not configured for this deployment.");
        }
    }
}
