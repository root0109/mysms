package com.mysms.comms.config;

import com.mysms.core.mail.MailDelivery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * comms's outgoing e-mail: core's {@link MailDelivery} over SMTP from {@code mysms.comms.smtp.*} (the shared
 * {@code MYSMS_SMTP_*} variables by default). Without a host there is no sender and e-mail is not offered.
 */
@Configuration(proxyBeanMethods = false)
public class CommsMailConfig {

    /** The qualifier of comms's sender (the monolith holds one per module that sends mail). */
    public static final String MAIL = "commsMailDelivery";

    @Bean(MAIL)
    public MailDelivery commsMailDelivery(
            @Value("${mysms.comms.smtp.host}") String host,
            @Value("${mysms.comms.smtp.port}") int port,
            @Value("${mysms.comms.smtp.username}") String username,
            @Value("${mysms.comms.smtp.password}") String password,
            @Value("${mysms.comms.smtp.from}") String from,
            @Value("${mysms.comms.smtp.starttls-required}") boolean startTlsRequired) {
        return MailDelivery.smtp(host, port, username, password, from, startTlsRequired);
    }
}
