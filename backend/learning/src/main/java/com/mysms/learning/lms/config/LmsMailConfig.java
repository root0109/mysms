package com.mysms.learning.lms.config;

import com.mysms.core.mail.MailDelivery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * lms's outgoing e-mail: core's {@link MailDelivery} over SMTP from {@code mysms.lms.smtp.*} (the shared
 * {@code MYSMS_SMTP_*} variables by default). Without a host there is no sender and the operations that must send
 * answer 503 NOT_CONFIGURED.
 */
@Configuration(proxyBeanMethods = false)
public class LmsMailConfig {

    /** The qualifier of lms's sender (the monolith holds one per module that sends mail). */
    public static final String MAIL = "lmsMailDelivery";

    @Bean(MAIL)
    public MailDelivery lmsMailDelivery(
            @Value("${mysms.lms.smtp.host}") String host,
            @Value("${mysms.lms.smtp.port}") int port,
            @Value("${mysms.lms.smtp.username}") String username,
            @Value("${mysms.lms.smtp.password}") String password,
            @Value("${mysms.lms.smtp.from}") String from,
            @Value("${mysms.lms.smtp.starttls-required}") boolean startTlsRequired) {
        return MailDelivery.smtp(host, port, username, password, from, startTlsRequired);
    }
}
