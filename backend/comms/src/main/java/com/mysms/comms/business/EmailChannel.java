package com.mysms.comms.business;

import com.mysms.comms.config.CommsMailConfig;
import com.mysms.core.mail.MailDelivery;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** E-mail through the deployment's SMTP server. */
@Component
public class EmailChannel implements DeliveryChannel {

    static final String NAME = "email";
    /** The subject of a message that has none of its own. */
    static final String DEFAULT_SUBJECT = "Message from your school";

    private final MailDelivery mail;

    public EmailChannel(@Qualifier(CommsMailConfig.MAIL) MailDelivery mail) {
        this.mail = mail;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void requireConfigured() {
        mail.requireConfigured();
    }

    @Override
    public void deliver(String to, String subject, String body) {
        mail.send(new MailDelivery.Message(to, subject == null || subject.isBlank() ? DEFAULT_SUBJECT : subject, body,
                null));
    }
}
