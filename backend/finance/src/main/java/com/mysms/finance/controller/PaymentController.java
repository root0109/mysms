package com.mysms.finance.controller;

import com.mysms.core.paging.PagingInfo;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.api.PaymentApi;
import com.mysms.finance.api.model.CheckoutSession;
import com.mysms.finance.api.model.OrderQuote;
import com.mysms.finance.api.model.Payment;
import com.mysms.finance.api.model.QuoteCheckoutRequest;
import com.mysms.finance.api.model.RecordPaymentRequest;
import com.mysms.finance.api.model.Refund;
import com.mysms.finance.api.model.RefundPaymentRequest;
import com.mysms.finance.api.model.StartCheckoutRequest;
import com.mysms.finance.business.PaymentService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Payment operations. The gateway's webhook is public at the edge; the service authenticates it by its
 * signature.
 */
@RestController
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public ResponseEntity<Payment> getPayment(UUID paymentId, String fields) {
        Payment found = paymentService.getPayment(paymentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Payment>> listPayments(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Payment> list =
                PagingUtil.pagingInfo(paymentService.listPayments(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<OrderQuote> quoteCheckout(QuoteCheckoutRequest quoteCheckoutRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(paymentService.quoteCheckout(quoteCheckoutRequest));
    }

    @Override
    public ResponseEntity<Void> receiveGatewayWebhook(String gateway, byte[] body, String signature, String eventId) {
        paymentService.receiveGatewayWebhook(gateway, body, signature, eventId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Payment> recordPayment(UUID idempotencyKey, RecordPaymentRequest recordPaymentRequest) {
        Payment recorded = paymentService.recordPayment(recordPaymentRequest);
        return ResponseEntity.created(URI.create("/v1/finance/payments/" + recorded.getId()))
                .eTag(recorded.getEtag()).body(recorded);
    }

    @Override
    public ResponseEntity<Refund> refundPayment(UUID paymentId, UUID idempotencyKey,
            RefundPaymentRequest refundPaymentRequest) {
        Refund refund = paymentService.refundPayment(paymentId, idempotencyKey, refundPaymentRequest);
        return ResponseEntity.ok().eTag(refund.getEtag()).body(refund);
    }

    @Override
    public ResponseEntity<CheckoutSession> startCheckout(UUID idempotencyKey,
            StartCheckoutRequest startCheckoutRequest) {
        return ResponseEntity.ok(paymentService.startCheckout(startCheckoutRequest));
    }
}
