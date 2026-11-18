package com.mysms.careers.config;

import com.mysms.core.mail.MailDelivery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * careers's outgoing e-mail: core's {@link MailDelivery} over SMTP from {@code mysms.careers.smtp.*} (the shared
 * {@code MYSMS_SMTP_*} variables by default). Without a host there is no sender and the operations that must send
 * answer 503 NOT_CONFIGURED.
 */
@Configuration(proxyBeanMethods = false)
public class CareersMailConfig {

    /** The qualifier of careers's sender (the monolith holds one per module that sends mail). */
    public static final String MAIL = "careersMailDelivery";

    @Bean(MAIL)
    public MailDelivery careersMailDelivery(
            @Value("${mysms.careers.smtp.host}") String host,
            @Value("${mysms.careers.smtp.port}") int port,
            @Value("${mysms.careers.smtp.username}") String username,
            @Value("${mysms.careers.smtp.password}") String password,
            @Value("${mysms.careers.smtp.from}") String from,
            @Value("${mysms.careers.smtp.starttls-required}") boolean startTlsRequired) {
        return MailDelivery.smtp(host, port, username, password, from, startTlsRequired);
    }
}
