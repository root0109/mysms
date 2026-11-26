package com.mysms.identity.iam.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.mysms.core.error.UnavailableException;
import com.mysms.identity.iam.errors.NotConfiguredException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** The mail port: the message it hands to SMTP, and its answers when SMTP is missing or fails. */
class MailDeliveryTest {

    private static final AccountMessages.Message MESSAGE = new AccountMessages.Message("a@school.test", "Subject",
            "Body");

    @Test
    void aMessageGoesFromTheConfiguredSenderAsPlainText() {
        JavaMailSender sender = mock(JavaMailSender.class);
        MailDelivery delivery = new MailDelivery(Optional.of(sender), "no-reply@mysms.test");
        delivery.send(MESSAGE);
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(sent.capture());
        assertThat(sent.getValue().getFrom()).isEqualTo("no-reply@mysms.test");
        assertThat(sent.getValue().getTo()).containsExactly("a@school.test");
        assertThat(sent.getValue().getSubject()).isEqualTo("Subject");
        assertThat(sent.getValue().getText()).isEqualTo("Body");
        delivery.destroy();
    }

    @Test
    void withoutSmtpTheMailingOperationsAreNotConfigured() {
        MailDelivery delivery = new MailDelivery(Optional.empty(), null);
        assertThat(delivery.configured()).isFalse();
        assertThatThrownBy(delivery::requireConfigured).isInstanceOf(NotConfiguredException.class);
        assertThatThrownBy(() -> delivery.send(MESSAGE)).isInstanceOf(NotConfiguredException.class);
        delivery.sendLater(MESSAGE);
        delivery.destroy();
    }

    @Test
    void aFailedDeliveryIsUnavailableNowAndOnlyLoggedInTheBackground() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("refused")).when(sender).send(any(SimpleMailMessage.class));
        MailDelivery delivery = new MailDelivery(Optional.of(sender), "no-reply@mysms.test");
        assertThatThrownBy(() -> delivery.send(MESSAGE)).isInstanceOf(UnavailableException.class);
        delivery.sendLater(MESSAGE);
        verify(sender, timeout(5_000).times(2)).send(any(SimpleMailMessage.class));
        delivery.destroy();
    }
}
