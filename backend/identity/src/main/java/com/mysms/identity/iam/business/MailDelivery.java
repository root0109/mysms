package com.mysms.identity.iam.business;

import com.mysms.core.error.UnavailableException;
import com.mysms.identity.iam.errors.NotConfiguredException;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * The port through which iam sends account e-mail (Adapter over Spring's {@link JavaMailSender}, chosen by
 * configuration in {@code IamMailConfig}): plain-text messages from the configured sender address. Without SMTP
 * settings there is no sender and {@link #requireConfigured()} answers 503 NOT_CONFIGURED. Messages are sent after the
 * transaction that created their secret has committed, never inside it. Addresses and bodies are never logged.
 */
public class MailDelivery implements DisposableBean {

    private static final Logger LOG = LoggerFactory.getLogger(MailDelivery.class);
    private static final int QUEUE_CAPACITY = 500;
    private static final int SHUTDOWN_SECONDS = 30;

    private final Optional<JavaMailSender> sender;
    private final String from;
    private final ThreadPoolTaskExecutor background;

    public MailDelivery(Optional<JavaMailSender> sender, String from) {
        this.sender = sender;
        this.from = from;
        this.background = new ThreadPoolTaskExecutor();
        background.setThreadNamePrefix("iam-mail-");
        background.setCorePoolSize(1);
        background.setMaxPoolSize(2);
        background.setQueueCapacity(QUEUE_CAPACITY);
        background.setWaitForTasksToCompleteOnShutdown(true);
        background.setAwaitTerminationSeconds(SHUTDOWN_SECONDS);
        background.initialize();
    }

    public boolean configured() {
        return sender.isPresent();
    }

    /** 503 NOT_CONFIGURED when this deployment has no SMTP settings; called before anything is written. */
    public void requireConfigured() {
        if (sender.isEmpty()) {
            throw new NotConfiguredException("E-mail delivery is not configured for this deployment.");
        }
    }

    /** Sends now; a refused or failed delivery is 503 (the caller may retry). */
    public void send(AccountMessages.Message message) {
        requireConfigured();
        try {
            sender.get().send(toMail(message));
        } catch (MailException e) {
            LOG.warn("account e-mail could not be delivered ({})", e.getClass().getSimpleName());
            throw new UnavailableException("The e-mail could not be sent; try again later.");
        }
    }

    /**
     * Sends in the background, for answers that must not depend on whether (or how fast) a message goes out — a
     * password reset or a sign-up answers the same for every address. Failures are logged, never reported.
     */
    public void sendLater(AccountMessages.Message message) {
        if (sender.isEmpty()) {
            LOG.warn("account e-mail is not sent: SMTP is not configured (mysms.iam.smtp.host / MYSMS_SMTP_HOST)");
            return;
        }
        try {
            background.execute(() -> {
                try {
                    sender.get().send(toMail(message));
                } catch (MailException e) {
                    LOG.warn("account e-mail could not be delivered ({})", e.getClass().getSimpleName());
                }
            });
        } catch (RejectedExecutionException e) {
            LOG.warn("account e-mail is not sent: the mail queue is full");
        }
    }

    SimpleMailMessage toMail(AccountMessages.Message message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        return mail;
    }

    @Override
    public void destroy() {
        background.shutdown();
    }
}
