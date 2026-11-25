package com.mysms.comms.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mysms.comms.domain.OutboundMessageEntity;
import com.mysms.comms.repository.OutboundMessageRepository;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MessageDeliveryExecutorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-01T08:00:00Z"), ZoneOffset.UTC);
    private static final OffsetDateTime NOW = OffsetDateTime.now(CLOCK);

    private final StandInTransactions standIn = new StandInTransactions();
    private final OutboundMessageRepository repository = mock(OutboundMessageRepository.class);
    private final DeliveryChannels channels = mock(DeliveryChannels.class);
    private final DeliveryChannel email = mock(DeliveryChannel.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final AuditWriter audit = mock(AuditWriter.class);
    private final MessageDeliveryExecutor dispatcher = new MessageDeliveryExecutor(repository, channels, standIn.database,
            standIn.transactions, outbox, audit, CLOCK);

    private OutboundMessageEntity message(String status) {
        OutboundMessageEntity message = OutboundMessageEntity.builder().channel("email").recipient("a@example.org")
                .subject("Bus").body("Late").status(status).build();
        message.setId(UUID.randomUUID());
        message.setRowVersion(3);
        when(repository.findById(message.getId())).thenReturn(Optional.of(message));
        return message;
    }

    @Test
    void takesAQueuedMessageDeliversItOutsideATransactionAndRecordsTheOutcome() {
        OutboundMessageEntity message = message("queued");
        when(repository.move(eq(message.getId()), eq(3), eq("queued"), eq("processing"), isNull(), isNull(), eq(NOW),
                any())).thenReturn(1);
        when(repository.move(eq(message.getId()), eq(4), eq("processing"), eq("delivered"), isNull(), eq(NOW), eq(NOW),
                any())).thenReturn(1);
        when(channels.require("email")).thenReturn(email);

        assertThat(dispatcher.dispatch(List.of(message.getId()))).isEqualTo(1);

        verify(email).deliver("a@example.org", "Bus", "Late");
        verify(audit).recordTransition(MessageDeliveryExecutor.AGGREGATE, message.getId(), "delivered",
                List.of("status", "sentAt", "providerResponse"), null);
        verify(outbox).publish(eq(MessageDeliveryExecutor.AGGREGATE + ".delivered"), eq(MessageDeliveryExecutor.AGGREGATE),
                eq(message.getId()), any());
    }

    @Test
    void recordsARefusedDeliveryAsFailedWithAFixedCodeOnly() {
        OutboundMessageEntity message = message("queued");
        when(repository.move(any(), anyInt(), eq("queued"), eq("processing"), any(), any(), any(), any()))
                .thenReturn(1);
        when(repository.move(eq(message.getId()), eq(4), eq("processing"), eq("failed"),
                eq(MessageDeliveryExecutor.UNDELIVERED), isNull(), eq(NOW), any())).thenReturn(1);
        when(channels.require("email")).thenReturn(email);
        doThrow(new UnavailableException("550 mailbox a@example.org unknown")).when(email).deliver(any(), any(), any());

        assertThat(dispatcher.dispatch(List.of(message.getId()))).isZero();

        verify(audit).recordTransition(MessageDeliveryExecutor.AGGREGATE, message.getId(), "failed",
                List.of("status", "sentAt", "providerResponse"), null);
    }

    @Test
    void anUnexpectedProviderFailureStillEndsTheMessageAsFailedNeverProcessing() {
        OutboundMessageEntity message = message("queued");
        when(repository.move(any(), anyInt(), eq("queued"), eq("processing"), any(), any(), any(), any()))
                .thenReturn(1);
        when(repository.move(eq(message.getId()), eq(4), eq("processing"), eq("failed"),
                eq(MessageDeliveryExecutor.UNDELIVERED), isNull(), eq(NOW), any())).thenReturn(1);
        when(channels.require("email")).thenReturn(email);
        doThrow(new IllegalStateException("socket closed")).when(email).deliver(any(), any(), any());

        assertThat(dispatcher.dispatch(List.of(message.getId()))).isZero();

        verify(repository).move(eq(message.getId()), eq(4), eq("processing"), eq("failed"),
                eq(MessageDeliveryExecutor.UNDELIVERED), isNull(), eq(NOW), any());
        verify(audit).recordTransition(MessageDeliveryExecutor.AGGREGATE, message.getId(), "failed",
                List.of("status", "sentAt", "providerResponse"), null);
    }

    @Test
    void leavesAMessageThatIsNoLongerQueuedOrWasTakenMeanwhile() {
        OutboundMessageEntity cancelled = message("cancelled");
        OutboundMessageEntity taken = message("queued");
        when(repository.move(eq(taken.getId()), anyInt(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        assertThat(dispatcher.dispatch(List.of(cancelled.getId(), taken.getId(), UUID.randomUUID()))).isZero();

        verify(repository, never()).move(eq(cancelled.getId()), anyInt(), any(), any(), any(), any(), any(), any());
        verify(channels, never()).require(any());
        verify(outbox, never()).publish(any(), any(), any(), any());
    }

    @Test
    void recordsNothingWhenTheMessageChangedWhileItWasBeingDelivered() {
        OutboundMessageEntity message = message("queued");
        when(repository.move(eq(message.getId()), eq(3), any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(repository.move(eq(message.getId()), eq(4), any(), any(), any(), any(), any(), any())).thenReturn(0);
        when(channels.require("email")).thenReturn(email);

        assertThat(dispatcher.dispatch(List.of(message.getId()))).isEqualTo(1);

        verify(audit, never()).recordTransition(any(), any(), any(), any(), any());
    }
}
