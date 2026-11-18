package com.mysms.finance.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.payment.GatewayCredentials;
import com.mysms.core.payment.GatewayRefund;
import com.mysms.core.payment.RazorpayClient;
import com.mysms.core.validation.DtoValidator;
import com.mysms.finance.domain.PaymentEntity;
import com.mysms.finance.domain.RefundEntity;
import com.mysms.finance.mapper.RefundMapper;
import com.mysms.finance.repository.GatewayRepository;
import com.mysms.finance.repository.GatewayRepository.AccountRow;
import com.mysms.finance.repository.InvoiceRepository;
import com.mysms.finance.repository.LedgerRepository;
import com.mysms.finance.repository.NumberSequenceRepository;
import com.mysms.finance.repository.PaymentRepository;
import com.mysms.finance.repository.RefundRepository;
import com.mysms.finance.repository.StudentFeeDueRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * A gateway refund that waits for the gateway, reconciled with what the gateway holds (stand-ins for the repositories
 * and the gateway; the HTTP paths are GatewayComponentTest's): processed or failed there settles it here, pending there
 * notes the gateway's id, unknown there fails it only once a request still on its way can be ruled out, and a gateway
 * that cannot answer changes nothing. A refund is never asked for twice.
 */
class PaymentRefundReconciliationTest {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final Duration AFTER = Duration.ofMinutes(15);

    private final StandInTransactions tx = new StandInTransactions();
    private final PaymentRepository payments = mock(PaymentRepository.class);
    private final RefundRepository refunds = mock(RefundRepository.class);
    private final LedgerRepository ledger = mock(LedgerRepository.class);
    private final GatewayRepository gateway = mock(GatewayRepository.class);
    private final RazorpayClient razorpay = mock(RazorpayClient.class);
    private final FieldCipher cipher = mock(FieldCipher.class);
    private final AuditWriter audit = mock(AuditWriter.class);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final PaymentService service = new PaymentService(tx.database, tx.transactions, payments,
            mock(PaymentViewFactory.class), mock(InvoiceService.class), mock(InvoiceRepository.class),
            mock(StudentFeeDueRepository.class), refunds, mock(RefundMapper.class), ledger, gateway,
            mock(NumberSequenceRepository.class), mock(CurrencyLookup.class), razorpay, cipher,
            JsonMapper.builder().build(), outbox, audit, mock(DtoValidator.class), Clock.fixed(NOW, ZoneOffset.UTC),
            AFTER);
    private final UUID tenant = UUID.randomUUID();
    private final UUID account = UUID.randomUUID();
    private final PaymentEntity payment = PaymentEntity.builder().id(UUID.randomUUID()).tenantId(tenant)
            .amount(new BigDecimal("1000.00")).gatewayPaymentId("pay_1").gatewayAccountId(account)
            .paymentMethod("gateway").status("received").build();

    @BeforeEach
    void theGatewayAccount() {
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(payments.lock(payment.getId())).thenReturn(Optional.of(payment));
        when(gateway.account(account)).thenReturn(Optional.of(new AccountRow(account, UUID.randomUUID(),
                PaymentService.RAZORPAY, "rzp_key", "enc-secret", "enc-hook", true)));
        when(cipher.decrypt("enc-secret", tenant, PaymentService.KEY_SECRET)).thenReturn(Optional.of("secret"));
        when(ledger.allocationsOf(any())).thenReturn(List.of());
        when(ledger.refunded(any())).thenReturn(Map.of());
        when(refunds.settle(any(), anyString(), any(), any(), any(), any())).thenReturn(1);
    }

    @Test
    void aRefundTheGatewayNeverGotFailsOnceARequestOnItsWayIsRuledOut() {
        RefundEntity lost = initiated(NOW.minus(AFTER).minusSeconds(1), null);
        when(razorpay.findRefund(any(), eq("pay_1"), eq(PaymentService.NOTE_REFUND), eq(lost.getId().toString())))
                .thenReturn(Optional.empty());
        RefundEntity failed = refund(lost.getId(), "failed");
        when(refunds.findById(lost.getId())).thenReturn(Optional.of(failed));

        assertThat(service.reconciled(lost)).isSameAs(failed);
        verify(refunds).settle(eq(lost.getId()), eq("failed"), isNull(), isNull(), any(), any());
        verify(ledger, never()).allocationsOf(any());
        verify(outbox).publish(eq(RefundService.AGGREGATE + ".failed"), eq(RefundService.AGGREGATE),
                eq(lost.getId()), any());
        verify(razorpay, never()).createRefund(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any());
    }

    @Test
    void aRefundTheGatewayDoesNotHoldYetKeepsWaitingWhileItsRequestMayStillArrive() {
        RefundEntity recent = initiated(NOW.minusSeconds(30), null);
        when(razorpay.findRefund(any(), any(), any(), any())).thenReturn(Optional.empty());

        assertThat(service.reconciled(recent)).isSameAs(recent);
        verify(refunds, never()).settle(any(), anyString(), any(), any(), any(), any());
    }

    @Test
    void aRefundTheGatewayProcessedSettlesAndGivesTheMoneyBack() {
        RefundEntity waiting = initiated(NOW.minusSeconds(30), null);
        when(razorpay.findRefund(any(GatewayCredentials.class), any(), any(), any()))
                .thenReturn(Optional.of(new GatewayRefund("rfnd_9", "pay_1", 40000, "processed")));
        when(refunds.findById(waiting.getId())).thenReturn(Optional.of(refund(waiting.getId(), "processed")));

        assertThat(service.reconciled(waiting).getStatus()).isEqualTo("processed");
        verify(refunds).settle(eq(waiting.getId()), eq("processed"), eq("rfnd_9"), any(), any(), any());
        verify(ledger).allocationsOf(List.of(payment.getId()));
    }

    @Test
    void aRefundTheGatewayRefusedFailsAndOneStillPendingThereKeepsItsGatewayId() {
        RefundEntity refused = initiated(NOW.minusSeconds(30), "rfnd_1");
        when(razorpay.findRefund(any(), any(), any(), eq(refused.getId().toString())))
                .thenReturn(Optional.of(new GatewayRefund("rfnd_1", "pay_1", 40000, "failed")));
        when(refunds.findById(refused.getId())).thenReturn(Optional.of(refund(refused.getId(), "failed")));
        service.reconciled(refused);
        verify(refunds).settle(eq(refused.getId()), eq("failed"), eq("rfnd_1"), isNull(), any(), any());

        RefundEntity pending = initiated(NOW.minusSeconds(30), null);
        when(razorpay.findRefund(any(), any(), any(), eq(pending.getId().toString())))
                .thenReturn(Optional.of(new GatewayRefund("rfnd_2", "pay_1", 40000, "pending")));
        when(refunds.findById(pending.getId())).thenReturn(Optional.of(pending));
        assertThat(service.reconciled(pending).getStatus()).isEqualTo("initiated");
        verify(refunds).noteGatewayId(eq(pending.getId()), eq("rfnd_2"), any(), any());
        verify(refunds, never()).settle(eq(pending.getId()), anyString(), any(), any(), any(), any());

        RefundEntity noted = initiated(NOW.minus(AFTER).minusSeconds(1), "rfnd_3");
        when(razorpay.findRefund(any(), any(), any(), eq(noted.getId().toString())))
                .thenReturn(Optional.of(new GatewayRefund("rfnd_3", "pay_1", 40000, "pending")));
        assertThat(service.reconciled(noted)).as("nothing new to record").isSameAs(noted);
    }

    @Test
    void aGatewayThatCannotAnswerChangesNothingAndOtherRefundsAreNotAsked() {
        RefundEntity waiting = initiated(NOW.minus(Duration.ofDays(1)), null);
        when(razorpay.findRefund(any(), any(), any(), any())).thenThrow(new UnavailableException("down"));
        assertThat(service.reconciled(waiting)).isSameAs(waiting);
        verify(refunds, never()).settle(any(), anyString(), any(), any(), any(), any());

        RefundEntity atTheCounter = refund(UUID.randomUUID(), "processed");
        atTheCounter.setRefundMethod("cash");
        assertThat(service.reconciled(atTheCounter)).isSameAs(atTheCounter);
        RefundEntity settled = refund(UUID.randomUUID(), "failed");
        assertThat(service.reconciled(settled)).isSameAs(settled);
        verifyNoInteractions(audit);
    }

    private RefundEntity initiated(Instant created, String gatewayRefundId) {
        RefundEntity refund = refund(UUID.randomUUID(), "initiated");
        refund.setCreatedAt(OffsetDateTime.ofInstant(created, ZoneOffset.UTC));
        refund.setGatewayRefundId(gatewayRefundId);
        return refund;
    }

    private RefundEntity refund(UUID id, String status) {
        return RefundEntity.builder().id(id).tenantId(tenant).paymentId(payment.getId())
                .amount(new BigDecimal("400.00")).status(status).refundMethod("gateway")
                .createdAt(OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC)).build();
    }
}
