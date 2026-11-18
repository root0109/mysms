package com.mysms.core.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.error.ApiException;
import com.mysms.core.error.UnavailableException;
import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.Test;

class MailDeliveryTest {

    private static final MailDelivery.Message MESSAGE =
            new MailDelivery.Message("student@example.org", "Hello", "Body", "school@example.org");

    @Test
    void withoutAHostNothingIsSentAnd503NotConfigured() {
        MailDelivery none = MailDelivery.smtp(" ", 587, null, null, null, true);
        assertThatThrownBy(none::requireConfigured).isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("NOT_CONFIGURED"));
        assertThatThrownBy(() -> none.send(MESSAGE)).isInstanceOf(MailDelivery.NotConfigured.class);
    }

    @Test
    void aHostNeedsASenderAddress() {
        assertThatThrownBy(() -> MailDelivery.smtp("smtp.example.org", 587, "user", "secret", "", true))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aServerThatCannotBeReachedIs503() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        MailDelivery closed = MailDelivery.smtp("localhost", port, "user", "secret", "lms@example.org", false);
        closed.requireConfigured();
        assertThatThrownBy(() -> closed.send(MESSAGE)).isInstanceOf(UnavailableException.class);
    }
}
