package com.mysms.identity.iam.config;

import com.mysms.identity.iam.business.AccountMessages;
import com.mysms.identity.iam.business.MailDelivery;
import java.util.Optional;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * iam's outgoing e-mail (password reset, verification, sign-up): Spring's {@link JavaMailSenderImpl} over SMTP with
 * STARTTLS, configured by property ({@code mysms.iam.smtp.*}) and otherwise by environment variable
 * ({@code MYSMS_SMTP_HOST}, {@code _PORT}, {@code _USERNAME}, {@code _PASSWORD}, {@code _FROM},
 * {@code _STARTTLS_REQUIRED}). Without a host there is no sender: the operations that must send mail answer 503
 * NOT_CONFIGURED. {@code mysms.iam.mail.app-url} ({@code MYSMS_APP_URL}) is the web app the links in the messages
 * open; without it the messages carry the code alone.
 */
@Configuration(proxyBeanMethods = false)
public class IamMailConfig {

    private static final int SMTP_TIMEOUT_MILLIS = 10_000;

    @Bean
    public MailDelivery mailDelivery(
            @Value("${mysms.iam.smtp.host}") String host,
            @Value("${mysms.iam.smtp.port}") int port,
            @Value("${mysms.iam.smtp.username}") String username,
            @Value("${mysms.iam.smtp.password}") String password,
            @Value("${mysms.iam.smtp.from}") String from,
            // STARTTLS is always offered; only a local relay without TLS may turn the requirement off
            @Value("${mysms.iam.smtp.starttls-required}") boolean startTlsRequired) {
        if (host == null || host.isBlank()) {
            return new MailDelivery(Optional.empty(), null);
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("mysms.iam.smtp.from (MYSMS_SMTP_FROM) is required when an SMTP host is set");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host.trim());
        sender.setPort(port);
        sender.setDefaultEncoding("UTF-8");
        Properties mail = sender.getJavaMailProperties();
        mail.put("mail.transport.protocol", "smtp");
        mail.put("mail.smtp.starttls.enable", "true");
        mail.put("mail.smtp.starttls.required", Boolean.toString(startTlsRequired));
        mail.put("mail.smtp.connectiontimeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
        mail.put("mail.smtp.timeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
        mail.put("mail.smtp.writetimeout", Integer.toString(SMTP_TIMEOUT_MILLIS));
        if (username != null && !username.isBlank()) {
            sender.setUsername(username);
            sender.setPassword(password);
            mail.put("mail.smtp.auth", "true");
        }
        return new MailDelivery(Optional.of(sender), from.trim());
    }

    @Bean
    public AccountMessages accountMessages(@Value("${mysms.iam.mail.app-url}") String appUrl) {
        return AccountMessages.ofCatalogue(appUrl);
    }
}
