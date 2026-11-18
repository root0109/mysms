package com.mysms.finance.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnauthenticatedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.id.UuidV7;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.payment.GatewayCredentials;
import com.mysms.core.payment.GatewayOrder;
import com.mysms.core.payment.GatewayPayment;
import com.mysms.core.payment.GatewayRefund;
import com.mysms.core.payment.PaymentGatewayRejectedException;
import com.mysms.core.payment.PaymentGatewayUnreachableException;
import com.mysms.core.payment.RazorpayClient;
import com.mysms.core.payment.RazorpaySignatures;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.finance.api.model.CheckoutSession;
import com.mysms.finance.api.model.OrderQuote;
import com.mysms.finance.api.model.Payment;
import com.mysms.finance.api.model.PaymentAllocationInput;
import com.mysms.finance.api.model.QuoteCheckoutRequest;
import com.mysms.finance.api.model.RecordPaymentRequest;
import com.mysms.finance.api.model.Refund;
import com.mysms.finance.api.model.RefundPaymentRequest;
import com.mysms.finance.api.model.StartCheckoutRequest;
import com.mysms.finance.config.FinancePaymentsConfig;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.domain.InvoiceEntity;
import com.mysms.finance.domain.PaymentEntity;
import com.mysms.finance.domain.RefundEntity;
import com.mysms.finance.domain.StudentFeeDueEntity;
import com.mysms.finance.errors.GatewayAmountMismatchException;
import com.mysms.finance.errors.PaymentGatewayNotConfiguredException;
import com.mysms.finance.events.FinanceEvents;
import com.mysms.finance.mapper.RefundMapper;
import com.mysms.finance.repository.GatewayRepository;
import com.mysms.finance.repository.GatewayRepository.AccountRow;
import com.mysms.finance.repository.GatewayRepository.EventRow;
import com.mysms.finance.repository.InvoiceRepository;
import com.mysms.finance.repository.LedgerRepository;
import com.mysms.finance.repository.LedgerRepository.AllocationRow;
import com.mysms.finance.repository.LedgerRepository.Slice;
import com.mysms.finance.repository.NumberSequenceRepository;
import com.mysms.finance.repository.PaymentRepository;
import com.mysms.finance.repository.RefundRepository;
import com.mysms.finance.repository.StudentFeeDueRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Business service of the Payment aggregate: reading payments, online checkout through the
 * gateway (Razorpay, core's {@link RazorpayClient}), the gateway's notifications, counter payments and refunds.
 *
 * <p><b>Money is recorded only on proof.</b> A checkout opens a gateway order and an {@code initiated} payment; the
 * money counts once the gateway's notification arrives signed with the merchant account's webhook secret, for the
 * order's own amount and currency — never on a client redirect. Notifications are kept once per gateway event id
 * ({@code finance.gateway_event}), so a redelivery changes nothing. A counter payment is recorded by the fee office
 * with its allocations. Gateway calls never run inside a database transaction: the order or refund is opened first,
 * then recorded.
 *
 * <p><b>Allocations are exact.</b> A payment settles one student's dues and invoices of one institution and currency;
 * the slices add up to the payment, each at most what is outstanding on its target, never rounded. Dues and invoices
 * are locked while a payment is allocated to them, so two payments never spend the same outstanding amount. A refund
 * gives back part or all of a payment: first the part no allocation holds, then off the payment's allocations newest
 * first ({@link Ledger}).
 *
 * <p>A gateway refund is recorded {@code initiated} before the gateway is asked and settles by the gateway's answer,
 * its notification, or a reconciliation that fetches it on a retry or before a new refund of the payment (one the
 * gateway does not hold after {@code mysms.finance.refund-reconcile-after} fails and releases its amount). A refund
 * made only in the gateway's dashboard is published as {@code finance.payment.external_refund_seen}, never booked.
 *
 * <p>Payments are owner-only below the fee office: a caller reads the payments of the institutions where they work on
 * fees ({@link FinanceStaff}), and otherwise only the payments they made.
 */
@Service
public class PaymentService extends AppTransactionManager {

    public static final String AGGREGATE = PaymentEntity.TABLE;
    static final String PERMISSION_READ = "finance.payment.read";
    static final String PERMISSION_QUOTE = "finance.payment.quote_checkout";
    static final String PERMISSION_CHECKOUT = "finance.payment.start_checkout";
    static final String PERMISSION_RECORD = "finance.payment.record";
    static final String PERMISSION_REFUND = "finance.payment.refund_payment";
    /** The one gateway finance takes money through. */
    public static final String RAZORPAY = "razorpay";
    /** The notes MySMS puts on gateway orders and refunds, echoed back on their notifications. */
    static final String NOTE_TENANT = "mysms_tenant";
    static final String NOTE_PAYMENT = "mysms_payment";
    static final String NOTE_REFUND = "mysms_refund";
    static final String KEY_SECRET = "finance.payment_gateway_account.key_secret_enc";
    static final String WEBHOOK_SECRET = "finance.payment_gateway_account.webhook_secret_enc";
    /** The most slices one payment is split into. */
    static final int MAX_ALLOCATIONS = 200;
    private static final String RECEIPT_PREFIX = "RCT-";
    private static final int REASON_MAX = 300;
    private static final Set<String> OPEN_INVOICE = Set.of("posted", "partially_paid");
    private static final Map<String, String> REFUND_METHOD_OF = Map.of("gateway", "gateway", "cash", "cash",
            "cheque", "cheque", "bank_transfer", "bank_transfer", "adjustment", "adjustment");

    /** What a notification did. */
    enum Outcome { APPLIED, IGNORED, MISMATCH }

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository repository;
    private final PaymentViewFactory views;
    private final InvoiceService invoices;
    private final InvoiceRepository invoiceRepository;
    private final StudentFeeDueRepository dues;
    private final RefundRepository refunds;
    private final RefundMapper refundMapper;
    private final LedgerRepository ledger;
    private final GatewayRepository gateway;
    private final NumberSequenceRepository numbers;
    private final CurrencyLookup currencies;
    private final RazorpayClient razorpay;
    private final FieldCipher cipher;
    private final JsonMapper json;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final Duration reconcileAfter;

    public PaymentService(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            PaymentRepository repository, PaymentViewFactory views, InvoiceService invoices,
            InvoiceRepository invoiceRepository, StudentFeeDueRepository dues, RefundRepository refunds,
            RefundMapper refundMapper, LedgerRepository ledger, GatewayRepository gateway,
            NumberSequenceRepository numbers, CurrencyLookup currencies,
            @Qualifier(FinancePaymentsConfig.GATEWAY) RazorpayClient razorpay, FieldCipher cipher, JsonMapper json,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock,
            @Value("${mysms.finance.refund-reconcile-after}") Duration reconcileAfter) {
        super(database, transactions);
        this.repository = repository;
        this.views = views;
        this.invoices = invoices;
        this.invoiceRepository = invoiceRepository;
        this.dues = dues;
        this.refunds = refunds;
        this.refundMapper = refundMapper;
        this.ledger = ledger;
        this.gateway = gateway;
        this.numbers = numbers;
        this.currencies = currencies;
        this.razorpay = razorpay;
        this.cipher = cipher;
        this.json = json;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.reconcileAfter = reconcileAfter;
    }

    // ---- reads ----

    /** One payment: in the fee office's reach, or one the caller made (404 otherwise). */
    public Payment getPayment(UUID id) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_READ);
        return doInReadTransaction(() -> {
            PaymentEntity payment = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE,
                    id));
            Optional<UUID> institution = Optional.ofNullable(payment.getInstitutionId());
            boolean office = FinanceStaff.reaches(FinanceStaff.reach(caller), scope, institution);
            if (!scope.canRead(institution) || !office && !caller.userId().equals(payment.getPayerUserId())) {
                throw new ResourceNotFoundException(AGGREGATE, id);
            }
            return views.view(payment);
        });
    }

    /** The payments the caller may read: the fee office's reach, plus the payments they made elsewhere. */
    public Page<Payment> listPayments(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(sortBy, PaymentRepository.SORT_FIELDS, PaymentRepository.DEFAULT_ORDER,
                PaymentRepository.ID_COLUMN);
        DataScope scope = DataScope.current(PERMISSION_READ);
        DataScope reach = FinanceStaff.reach(caller);
        SqlFragment where = FilterParser.parse(filter, PaymentRepository.FILTER_FIELDS)
                .and(PaymentRepository.scope(scope));
        if (!FinanceStaff.covers(reach, scope)) {
            where = where.and(PaymentRepository.reachableOrPaid(reach, caller.userId()));
        }
        SqlFragment filtered = where;
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<PaymentEntity> rows = repository.list(page, filtered);
            List<Payment> shown = views.views(rows.items());
            return new Page<>(shown, rows.page(), rows.size(), rows.nextCursor());
        });
    }

    // ---- checkout ----

    /**
     * finance.QuoteCheckout: what paying an invoice online costs now — what is outstanding on it (taxes are on the
     * invoice already; coupons apply to course purchases, not to fees). Only invoices are paid through finance's
     * checkout (422 for other purposes); the invoice must be the caller's to pay (404) and open (409).
     */
    public OrderQuote quoteCheckout(QuoteCheckoutRequest request) {
        validator.validate(request);
        requireInvoicePurpose(request.getPurpose().getValue());
        InvoiceEntity invoice = payable(request.getTargetId(), DataScope.current(PERMISSION_QUOTE));
        BigDecimal outstanding = outstanding(invoice);
        CurrencyLookup.Money money = currencies.require(invoice.getCurrencyId());
        OrderQuote quote = new OrderQuote(WireValues.decimal(outstanding), "0.00", "0.00",
                WireValues.decimal(outstanding), money.code(), Boolean.FALSE);
        String coupon = request.getCouponCode();
        if (coupon != null && !coupon.isBlank()) {
            quote.setCouponMessage("Coupons apply to course purchases, not to fees.");
        }
        return quote;
    }

    /**
     * finance.StartCheckout: opens a gateway order for what is outstanding on the invoice and records an
     * {@code initiated} payment for it; the client completes the payment with the gateway (the order id and the
     * account's public key). The money is recorded only when the gateway's signed notification arrives. The payer must
     * accept the terms (422). Without an active gateway account for the institution the answer is 503
     * {@code PAYMENT_GATEWAY_NOT_CONFIGURED}; an account of another gateway is 422; without master or the gateway, 503.
     */
    public CheckoutSession startCheckout(StartCheckoutRequest request) {
        validator.validate(request);
        requireInvoicePurpose(request.getPurpose().getValue());
        if (!Boolean.TRUE.equals(request.getPayerConsent())) {
            throw new ValidationFailedException(List.of(new FieldError("payerConsent", "the terms must be accepted",
                    "AssertTrue")));
        }
        AuthContext caller = AuthContext.require();
        InvoiceEntity invoice = payable(request.getTargetId(), DataScope.current(PERMISSION_CHECKOUT));
        BigDecimal outstanding = outstanding(invoice);
        AccountRow account = doInReadTransaction(() -> gateway.accountFor(invoice.getInstitutionId()))
                .orElseThrow(() -> new PaymentGatewayNotConfiguredException(
                        "Online payment is not set up for this institution."));
        GatewayCredentials credentials = credentials(account, invoice.getTenantId());
        CurrencyLookup.Money money = currencies.require(invoice.getCurrencyId());
        long minor = RazorpayClient.minorUnits(outstanding, money.minorUnits());
        UUID paymentId = UuidV7.next(clock);
        GatewayOrder order = razorpay.createOrder(credentials, minor, money.code(),
                paymentId.toString().replace("-", ""), Map.of(NOTE_TENANT, invoice.getTenantId().toString(),
                        NOTE_PAYMENT, paymentId.toString()));
        PaymentEntity payment = PaymentEntity.builder()
                .tenantId(invoice.getTenantId())
                .institutionId(invoice.getInstitutionId())
                .payerUserId(caller.userId())
                .purpose("invoice")
                .invoiceId(invoice.getId())
                .gatewayName(RAZORPAY)
                .gatewayOrderId(order.id())
                .originalAmount(outstanding)
                .discountAmount(BigDecimal.ZERO)
                .amount(outstanding)
                .amountWithTax(outstanding)
                .currencyId(invoice.getCurrencyId())
                .billingName(request.getBillingName().trim())
                .billingAddressId(request.getBillingAddressId())
                .taxRegistrationNumber(request.getTaxRegistrationNumber())
                .taxIdentityEnc(request.getTaxIdentity() == null ? null
                        : cipher.encrypt(request.getTaxIdentity(), invoice.getTenantId(), PaymentViewFactory.TAX_IDENTITY))
                .payerConsent(true)
                .status("initiated")
                .paymentMethod("gateway")
                .gatewayAccountId(account.id())
                .gatewayCurrency(money.code())
                .gatewayAmountMinor(minor)
                .build();
        payment.onCreatingData(Actor.current(), clock);
        payment.setId(paymentId);
        return doInWriteTransaction(() -> {
            InvoiceEntity now = invoiceRepository.lock(List.of(invoice.getId())).get(0);
            if (!OPEN_INVOICE.contains(now.getStatus()) || outstanding(now).compareTo(outstanding) != 0) {
                throw new ConflictException("CHECKOUT_STALE",
                        "The invoice changed while the checkout opened; start the checkout again.");
            }
            repository.insert(payment);
            audit.recordCreate(AGGREGATE, paymentId);
            outbox.publish(AGGREGATE + FinanceEvents.CREATED, AGGREGATE, paymentId, FinanceEvents.payload(paymentId));
            CheckoutSession session = new CheckoutSession(views.view(repository.findById(paymentId).orElseThrow()));
            session.setGatewayOrderId(order.id());
            session.setGatewayPublicKey(account.keyId());
            return session;
        });
    }

    // ---- the gateway's notifications ----

    /**
     * finance.ReceiveGatewayWebhook: a signed notification of the gateway. The tenant comes from the note MySMS put on
     * the order (an unsigned hint), and the notification counts only when it is signed with the webhook secret of the
     * merchant account of the stored order it names, over the exact body bytes (401 otherwise). A notification of an
     * order or refund MySMS does not know is acknowledged and changes nothing. Each event id is processed once:
     * {@code payment.captured} / {@code order.paid} record the money (when amount and currency match the order: 422
     * otherwise, nothing recorded) and allocate it to the invoice, {@code payment.failed} fails a waiting payment,
     * {@code refund.processed} / {@code refund.failed} settle a refund (a processed refund MySMS did not open is
     * published as {@code finance.payment.external_refund_seen}, never booked); any other event is kept without effect.
     */
    public void receiveGatewayWebhook(String gatewayName, byte[] body, String signature, String eventId) {
        if (!RAZORPAY.equals(gatewayName)) {
            throw new ResourceNotFoundException("finance.payment_gateway", gatewayName);
        }
        JsonNode event = parse(body);
        String type = event.path("event").asString("");
        JsonNode paymentEntity = event.path("payload").path("payment").path("entity");
        JsonNode refundEntity = event.path("payload").path("refund").path("entity");
        Optional<UUID> tenant = tenantHint(event, paymentEntity, refundEntity);
        if (tenant.isEmpty()) {
            return;
        }
        Outcome outcome = CallContextHolder.callAs(CallContextHolder.require().withIdentity(tenant.get(), null),
                () -> receive(tenant.get(), body, signature, eventId, type, paymentEntity, refundEntity));
        if (outcome == Outcome.MISMATCH) {
            throw new GatewayAmountMismatchException();
        }
    }

    private Outcome receive(UUID tenant, byte[] body, String signature, String eventId, String type,
            JsonNode paymentEntity, JsonNode refundEntity) {
        Optional<PaymentEntity> anchor = doInPrimaryReadTransaction(() -> anchor(paymentEntity, refundEntity));
        if (anchor.isEmpty()) {
            return Outcome.IGNORED;
        }
        AccountRow account = doInPrimaryReadTransaction(() -> Optional.ofNullable(anchor.get().getGatewayAccountId())
                .flatMap(gateway::account))
                .orElseThrow(() -> new UnauthenticatedException("UNAUTHENTICATED", "The notification cannot be verified."));
        String secret = Optional.ofNullable(account.webhookSecretEnc())
                .flatMap(envelope -> cipher.decrypt(envelope, tenant, WEBHOOK_SECRET))
                .orElseThrow(() -> new UnauthenticatedException("UNAUTHENTICATED", "The notification cannot be verified."));
        if (!RazorpaySignatures.webhookValid(body, signature, secret)) {
            throw new UnauthenticatedException("UNAUTHENTICATED", "The notification's signature is not valid.");
        }
        if (eventId == null || eventId.isBlank() || eventId.length() > 150) {
            throw new InvalidArgumentException("The notification carries no event id.");
        }
        String payload = new String(body, StandardCharsets.UTF_8);
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            EventRow kept = gateway.claimEvent(UuidV7.next(clock), account.id(), RAZORPAY, eventId,
                    type.isEmpty() ? "unknown" : truncate(type, 80), payload, now);
            if (kept.processedAt() != null || kept.processingError() != null) {
                return Outcome.IGNORED;
            }
            PaymentEntity payment = repository.lock(anchor.get().getId()).orElseThrow();
            String error = switch (type) {
                case "payment.captured", "order.paid" -> captured(payment, RazorpayClient.payment(paymentEntity), now);
                case "payment.failed" -> failed(payment, now);
                case "refund.processed", "refund.failed" -> refundSettled(payment, refundEntity,
                        "refund.processed".equals(type), now);
                default -> null;
            };
            gateway.finishEvent(kept.id(), payment.getId(), error, now);
            return "AMOUNT_MISMATCH".equals(error) ? Outcome.MISMATCH : Outcome.APPLIED;
        });
    }

    /** The money of the order arrived: recorded once, for the order's own amount and currency, and allocated. */
    private String captured(PaymentEntity payment, GatewayPayment paid, OffsetDateTime now) {
        if (!paid.captured()) {
            return null;
        }
        if (payment.getGatewayPaymentId() != null && payment.getGatewayPaymentId().equals(paid.id())) {
            return null;
        }
        if (paid.amountMinor() != payment.getGatewayAmountMinor()
                || !paid.currency().equalsIgnoreCase(payment.getGatewayCurrency())) {
            return "AMOUNT_MISMATCH";
        }
        if ("received".equals(payment.getStatus()) || "refunded".equals(payment.getStatus())) {
            outbox.publish(AGGREGATE + ".paidTwice", AGGREGATE, payment.getId(), FinanceEvents.payload(payment.getId()));
            return "SECOND_PAYMENT_ON_ORDER";
        }
        if (repository.receive(payment.getId(), paid.id(), now) == 0) {
            return "NOT_WAITING";
        }
        BigDecimal allocated = BigDecimal.ZERO;
        if (payment.getInvoiceId() != null) {
            InvoiceEntity invoice = invoiceRepository.lock(List.of(payment.getInvoiceId())).get(0);
            BigDecimal slice = outstanding(invoice).min(payment.getAmount());
            if (OPEN_INVOICE.contains(invoice.getStatus()) && slice.signum() > 0) {
                ledger.insertAllocations(payment.getId(), payment.getCurrencyId(),
                        List.of(new Slice(UuidV7.next(clock), invoice.getId(), null, slice)), now, null);
                settle(List.of(invoice.getId()), now, null);
                allocated = slice;
            }
        }
        audit.recordTransition(AGGREGATE, payment.getId(), "receive", List.of("status", "gatewayPaymentId",
                "receivedAt"), null);
        outbox.publish(AGGREGATE + ".received", AGGREGATE, payment.getId(), FinanceEvents.payload(payment.getId()));
        if (allocated.compareTo(payment.getAmount()) < 0) {
            outbox.publish(AGGREGATE + ".unallocated", AGGREGATE, payment.getId(),
                    FinanceEvents.payload(payment.getId()));
        }
        return null;
    }

    private String failed(PaymentEntity payment, OffsetDateTime now) {
        if (repository.fail(payment.getId(), now) == 1) {
            audit.recordTransition(AGGREGATE, payment.getId(), "fail", List.of("status"), null);
            outbox.publish(AGGREGATE + ".failed", AGGREGATE, payment.getId(), FinanceEvents.payload(payment.getId()));
        }
        return null;
    }

    private String refundSettled(PaymentEntity payment, JsonNode entity, boolean processed, OffsetDateTime now) {
        String gatewayRefundId = entity.path("id").asString(null);
        Optional<RefundEntity> refund = uuid(entity.path("notes").path(NOTE_REFUND).asString(null))
                .flatMap(refunds::findById)
                .or(() -> Optional.ofNullable(gatewayRefundId).flatMap(refunds::byGatewayRefund))
                .filter(found -> found.getPaymentId().equals(payment.getId()));
        if (refund.isPresent() && "initiated".equals(refund.get().getStatus())) {
            settleRefund(payment, refund.get().getId(), processed, gatewayRefundId, now, null);
        } else if (processed && (refund.isEmpty() || "failed".equals(refund.get().getStatus()))) {
            externalRefund(payment, gatewayRefundId, entity.path("amount").asLong());
        }
        return null;
    }

    /** Money went back at the gateway on a refund MySMS did not open or holds failed: published and logged, not booked. */
    private void externalRefund(PaymentEntity payment, String gatewayRefundId, long amountMinor) {
        LOG.warn("payment {} was refunded at the gateway outside MySMS (gateway refund {}, {} minor units); "
                + "not booked", payment.getId(), gatewayRefundId, amountMinor);
        Map<String, Object> payload = new HashMap<>(FinanceEvents.payload(payment.getId()));
        payload.put("gatewayRefundId", gatewayRefundId == null ? "" : gatewayRefundId);
        payload.put("amountMinor", amountMinor);
        outbox.publish(AGGREGATE + ".external_refund_seen", AGGREGATE, payment.getId(), payload);
    }

    /** The stored payment a notification is about: its order, or (a refund) the gateway payment it returns. */
    private Optional<PaymentEntity> anchor(JsonNode paymentEntity, JsonNode refundEntity) {
        String gatewayPaymentId = refundEntity.path("payment_id").asString(null);
        if (gatewayPaymentId != null) {
            return repository.byGatewayPayment(RAZORPAY, gatewayPaymentId);
        }
        String orderId = paymentEntity.path("order_id").asString(null);
        return orderId == null ? Optional.empty() : repository.byOrder(RAZORPAY, orderId);
    }

    private static Optional<UUID> tenantHint(JsonNode event, JsonNode paymentEntity, JsonNode refundEntity) {
        for (JsonNode notes : List.of(paymentEntity.path("notes"), refundEntity.path("notes"),
                event.path("payload").path("order").path("entity").path("notes"))) {
            Optional<UUID> tenant = uuid(notes.path(NOTE_TENANT).asString(null));
            if (tenant.isPresent()) {
                return tenant;
            }
        }
        return Optional.empty();
    }

    private JsonNode parse(byte[] body) {
        try {
            JsonNode node = json.readTree(body == null ? new byte[0] : body);
            if (node == null || !node.isObject()) {
                throw new InvalidArgumentException("The notification is not a JSON object.");
            }
            return node;
        } catch (JacksonException notJson) {
            throw new InvalidArgumentException("The notification is not a JSON object.");
        }
    }

    // ---- counter payments ----

    /**
     * finance.RecordPayment: money taken at the counter (cash, cheque, DD, transfer, card machine or an adjustment),
     * allocated exactly: the slices add up to the amount (422), each targets one due or one invoice (422) visible to
     * the caller (422 naming it) — dues not on an invoice (an invoiced due is paid through its invoice: 409), invoices
     * posted and not fully paid (409) — of one student, one institution the caller may write (403) and the payment's
     * currency (422), each at most what is outstanding on it (422). The targets are locked while the payment is
     * written, so two payments never spend the same amount. The receipt is numbered from the institution's
     * {@code RCT-} series. 201 with the payment and its allocations.
     */
    public Payment recordPayment(RecordPaymentRequest request) {
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_RECORD);
        BigDecimal amount = WireValues.decimal(request.getAmount());
        List<PaymentAllocationInput> slices = request.getAllocations() == null ? List.of() : request.getAllocations();
        List<BigDecimal> sliceAmounts = checkCounterPayment(request, amount, slices);
        return doInWriteTransaction(() -> {
            Targets targets = lockTargets(slices, scope, request.getCurrencyId());
            scope.requireWritable(Optional.of(targets.institutionId()));
            Ledger held = Ledger.of(ledger.allocationsTouching(targets.ids()),
                    ledger.refunded(paymentsOf(ledger.allocationsTouching(targets.ids()))));
            List<FieldError> errors = new ArrayList<>();
            for (int i = 0; i < slices.size(); i++) {
                BigDecimal left = targets.outstanding(i, held);
                if (sliceAmounts.get(i).compareTo(left) > 0) {
                    errors.add(new FieldError("allocations[" + i + "].amount",
                            "exceeds what is outstanding (" + left.toPlainString() + ")", "OVER_ALLOCATED"));
                }
            }
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            OffsetDateTime now = now();
            UUID actor = caller.userId();
            PaymentEntity payment = counterPayment(request, amount, targets, caller);
            payment.onCreatingData(Actor.current(), clock);
            payment.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
            payment.setReceiptNumber(numbers.next(targets.institutionId(), PaymentRepository.RECEIPT_NUMBERS,
                    RECEIPT_PREFIX, UuidV7.next(clock), now, actor));
            repository.insert(payment);
            List<Slice> written = new ArrayList<>();
            List<UUID> ids = orderedIds(slices.size());
            for (int i = 0; i < slices.size(); i++) {
                written.add(new Slice(ids.get(i), slices.get(i).getInvoiceId(),
                        slices.get(i).getStudentFeeDueId(), sliceAmounts.get(i)));
            }
            ledger.insertAllocations(payment.getId(), payment.getCurrencyId(), written, now, actor);
            settle(targets.invoiceIds(), now, actor);
            audit.recordCreate(AGGREGATE, payment.getId());
            outbox.publish(AGGREGATE + FinanceEvents.CREATED, AGGREGATE, payment.getId(),
                    FinanceEvents.payload(payment.getId()));
            outbox.publish(AGGREGATE + ".received", AGGREGATE, payment.getId(),
                    FinanceEvents.payload(payment.getId()));
            return views.view(repository.findById(payment.getId()).orElseThrow());
        });
    }

    /** The shape of a counter payment (422 with every failing field); answers the slice amounts. */
    private static List<BigDecimal> checkCounterPayment(RecordPaymentRequest request, BigDecimal amount,
            List<PaymentAllocationInput> slices) {
        List<FieldError> errors = new ArrayList<>();
        WireValues.checkDecimal("amount", request.getAmount(), 14, 2, errors);
        if (amount.signum() <= 0) {
            errors.add(new FieldError("amount", "must be greater than 0", "Min"));
        }
        if (request.getPayerUserId() == null && request.getPayerPersonId() == null
                && (request.getPayerName() == null || request.getPayerName().isBlank())) {
            errors.add(new FieldError("payerName", "name the payer", "REQUIRED"));
        }
        if (request.getBillingName() == null || request.getBillingName().isBlank()) {
            errors.add(new FieldError("billingName", "must not be blank", "NotBlank"));
        }
        if (slices.isEmpty() || slices.size() > MAX_ALLOCATIONS) {
            errors.add(new FieldError("allocations", "between 1 and " + MAX_ALLOCATIONS + " allocations", "Size"));
        }
        List<BigDecimal> amounts = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < slices.size(); i++) {
            PaymentAllocationInput slice = slices.get(i);
            String field = "allocations[" + i + "]";
            if ((slice.getInvoiceId() == null) == (slice.getStudentFeeDueId() == null)) {
                errors.add(new FieldError(field, "name exactly one invoice or one due", "ONE_TARGET"));
            } else if (!seen.add(slice.getInvoiceId() != null ? slice.getInvoiceId() : slice.getStudentFeeDueId())) {
                errors.add(new FieldError(field, "the target is named twice", "DUPLICATE"));
            }
            WireValues.checkDecimal(field + ".amount", slice.getAmount(), 14, 2, errors);
            BigDecimal part = WireValues.decimal(slice.getAmount());
            if (part.signum() <= 0) {
                errors.add(new FieldError(field + ".amount", "must be greater than 0", "Min"));
            }
            amounts.add(part);
            sum = sum.add(part);
        }
        if (!slices.isEmpty() && sum.compareTo(amount) != 0) {
            errors.add(new FieldError("allocations", "the allocations must add up to the amount", "ALLOCATION_TOTAL"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return amounts;
    }

    /** The targets of a payment, locked; one student, one institution, the payment's currency (422 otherwise). */
    private Targets lockTargets(List<PaymentAllocationInput> slices, DataScope scope, UUID currencyId) {
        List<UUID> dueIds = slices.stream().map(PaymentAllocationInput::getStudentFeeDueId)
                .filter(Objects::nonNull).toList();
        List<UUID> invoiceIds = slices.stream().map(PaymentAllocationInput::getInvoiceId)
                .filter(Objects::nonNull).toList();
        Map<UUID, StudentFeeDueEntity> lockedDues = new HashMap<>();
        Map<UUID, InvoiceEntity> lockedInvoices = new HashMap<>();
        dues.lock(dueIds).forEach(due -> lockedDues.put(due.getId(), due));
        invoiceRepository.lock(invoiceIds).forEach(invoice -> lockedInvoices.put(invoice.getId(), invoice));
        List<FieldError> errors = new ArrayList<>();
        Set<UUID> students = new HashSet<>();
        Set<UUID> institutions = new HashSet<>();
        for (int i = 0; i < slices.size(); i++) {
            String field = "allocations[" + i + "]";
            PaymentAllocationInput slice = slices.get(i);
            UUID student;
            UUID institution;
            UUID currency;
            if (slice.getStudentFeeDueId() != null) {
                StudentFeeDueEntity due = lockedDues.get(slice.getStudentFeeDueId());
                if (due == null || !scope.canRead(Optional.of(due.getInstitutionId()))) {
                    errors.add(new FieldError(field + ".studentFeeDueId", "due does not exist", "NOT_FOUND"));
                    continue;
                }
                if (!"draft".equals(due.getStatus()) || due.getInvoiceId() != null) {
                    throw new InvalidStateException("A due on an invoice is paid through the invoice; a cancelled due "
                            + "is not paid.");
                }
                student = due.getStudentId();
                institution = due.getInstitutionId();
                currency = due.getCurrencyId();
            } else {
                InvoiceEntity invoice = lockedInvoices.get(slice.getInvoiceId());
                if (invoice == null || invoice.getInstitutionId() == null
                        || !scope.canRead(Optional.of(invoice.getInstitutionId()))) {
                    errors.add(new FieldError(field + ".invoiceId", "invoice does not exist", "NOT_FOUND"));
                    continue;
                }
                if (!OPEN_INVOICE.contains(invoice.getStatus())) {
                    throw new InvalidStateException("Only a posted invoice that is not fully paid takes a payment.");
                }
                student = invoice.getStudentId();
                institution = invoice.getInstitutionId();
                currency = invoice.getCurrencyId();
            }
            students.add(student);
            institutions.add(institution);
            if (!currencyId.equals(currency)) {
                errors.add(new FieldError(field, "is in another currency than the payment", "OTHER_CURRENCY"));
            }
        }
        if (students.size() > 1) {
            errors.add(new FieldError("allocations", "one payment settles one student's fees", "OTHER_STUDENT"));
        }
        if (institutions.size() > 1) {
            errors.add(new FieldError("allocations", "one payment settles fees of one institution",
                    "OTHER_INSTITUTION"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        return new Targets(slices, lockedDues, lockedInvoices, institutions.iterator().next());
    }

    /** The locked targets of a counter payment, in the request's order. */
    private record Targets(List<PaymentAllocationInput> slices, Map<UUID, StudentFeeDueEntity> dues,
            Map<UUID, InvoiceEntity> invoices, UUID institutionId) {

        List<UUID> ids() {
            List<UUID> all = new ArrayList<>(dues.keySet());
            all.addAll(invoices.keySet());
            return all;
        }

        List<UUID> invoiceIds() {
            return List.copyOf(invoices.keySet());
        }

        /** What is still owed on the i-th target: the due's net or the invoice's total, less the money held for it. */
        BigDecimal outstanding(int i, Ledger held) {
            PaymentAllocationInput slice = slices.get(i);
            if (slice.getStudentFeeDueId() != null) {
                return dues.get(slice.getStudentFeeDueId()).getNetAmount()
                        .subtract(held.paidTo(slice.getStudentFeeDueId()));
            }
            return invoices.get(slice.getInvoiceId()).getTotal().subtract(held.paidTo(slice.getInvoiceId()));
        }
    }

    private PaymentEntity counterPayment(RecordPaymentRequest request, BigDecimal amount, Targets targets,
            AuthContext caller) {
        boolean oneInvoice = targets.dues().isEmpty() && targets.invoices().size() == 1;
        String payerName = request.getPayerName();
        return PaymentEntity.builder()
                .institutionId(targets.institutionId())
                .payerUserId(request.getPayerUserId())
                .payerPersonId(request.getPayerPersonId())
                .payerName(payerName == null ? null : payerName.trim())
                .purpose(oneInvoice ? "invoice" : "fees")
                .invoiceId(oneInvoice ? targets.invoiceIds().get(0) : null)
                .originalAmount(amount)
                .discountAmount(BigDecimal.ZERO)
                .amount(amount)
                .amountWithTax(amount)
                .currencyId(request.getCurrencyId())
                .billingName(request.getBillingName().trim())
                .payerConsent(false)
                .status("received")
                .receivedAt(request.getReceivedAt() == null ? now() : request.getReceivedAt())
                .paymentMethod(request.getPaymentMethod().getValue())
                .instrumentReference(request.getInstrumentReference())
                .instrumentDate(request.getInstrumentDate())
                .bankName(request.getBankName())
                .receivedByUserId(caller.userId())
                .build();
    }

    // ---- refunds ----

    /**
     * finance.RefundPayment: gives back part or the rest of a received payment, never more than is not yet refunded or
     * being refunded (422). A retry with the same Idempotency-Key answers the refund it opened, reconciled with the
     * gateway while it waits, whatever happened to the payment since. Non-gateway refunds are processed at once; body
     * etag 412, a payment not received 409.
     */
    public Refund refundPayment(UUID paymentId, UUID requestKey, RefundPaymentRequest request) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_REFUND);
        String given = request.getReason();
        String reason = given == null ? null : given.trim();
        if (reason != null && reason.length() > REASON_MAX) {
            throw new ValidationFailedException(List.of(new FieldError("reason",
                    "at most " + REASON_MAX + " characters", "Size")));
        }
        Optional<RefundEntity> retried = doInReadTransaction(() -> {
            writable(paymentId, scope);
            return refunds.byRequest(paymentId, requestKey);
        });
        if (retried.isPresent()) {
            return refundMapper.toDto(reconciled(retried.get()));
        }
        PaymentEntity payment = doInReadTransaction(() -> refundable(paymentId, scope, request.getEtag()));
        RefundPaymentRequest.RefundMethodEnum chosen = request.getRefundMethod();
        String method = chosen != null ? chosen.getValue()
                : REFUND_METHOD_OF.get(payment.getPaymentMethod());
        if (method == null) {
            throw new ValidationFailedException(List.of(new FieldError("refundMethod",
                    "say how the money goes back", "REQUIRED")));
        }
        boolean throughGateway = "gateway".equals(method);
        if (throughGateway && (!"gateway".equals(payment.getPaymentMethod()) || payment.getGatewayPaymentId() == null)) {
            throw new ValidationFailedException(List.of(new FieldError("refundMethod",
                    "only a payment taken through the gateway is refunded through it", "NOT_GATEWAY")));
        }
        Optional<BigDecimal> asked = Optional.ofNullable(request.getAmount()).map(WireValues::decimal);
        List<FieldError> errors = new ArrayList<>();
        WireValues.checkDecimal("amount", request.getAmount(), 14, 2, errors);
        if (asked.isPresent() && asked.get().signum() <= 0) {
            errors.add(new FieldError("amount", "must be greater than 0", "Min"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        GatewayCredentials credentials = throughGateway ? credentials(doInReadTransaction(
                () -> Optional.ofNullable(payment.getGatewayAccountId()).flatMap(gateway::account))
                .orElseThrow(() -> new PaymentGatewayNotConfiguredException("The payment's gateway account is gone.")),
                payment.getTenantId()) : null;
        doInReadTransaction(() -> refunds.unsettledThroughGateway(paymentId)).forEach(this::reconciled);
        RefundEntity opened = doInWriteTransaction(() -> open(paymentId, requestKey, scope, asked, method, reason,
                caller, request.getEtag()));
        if (!throughGateway || !"initiated".equals(opened.getStatus())) {
            return refundMapper.toDto(opened);
        }
        return refundMapper.toDto(throughGateway(payment, opened, credentials));
    }

    /** The payment, visible (404) and writable (403) for the caller. */
    private PaymentEntity writable(UUID paymentId, DataScope scope) {
        PaymentEntity payment = repository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, paymentId));
        Optional<UUID> institution = Optional.ofNullable(payment.getInstitutionId());
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(AGGREGATE, paymentId);
        }
        scope.requireWritable(institution);
        return payment;
    }

    /** The payment, visible (404) and writable (403) for the caller, at the body etag (412), received (409). */
    private PaymentEntity refundable(UUID paymentId, DataScope scope, String etag) {
        PaymentEntity payment = writable(paymentId, scope);
        if (etag != null && Etags.bodyVersion(etag).orElse(-1) != payment.getRowVersion()) {
            throw new PreconditionFailedException("The etag does not match the payment's current version.");
        }
        if (!"received".equals(payment.getStatus())) {
            throw new InvalidStateException("Only a received payment that is not fully refunded is refunded.");
        }
        return payment;
    }

    /** Opens the refund with the payment locked (refunds of one payment run one at a time). */
    private RefundEntity open(UUID paymentId, UUID requestKey, DataScope scope, Optional<BigDecimal> asked,
            String method, String reason, AuthContext caller, String etag) {
        repository.lock(paymentId).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, paymentId));
        Optional<RefundEntity> retried = refunds.byRequest(paymentId, requestKey);
        if (retried.isPresent()) {
            return retried.get();
        }
        PaymentEntity payment = refundable(paymentId, scope, etag);
        BigDecimal left = payment.getAmount().subtract(ledger.committedRefunds(paymentId));
        BigDecimal amount = asked.orElse(left);
        if (amount.signum() <= 0 || amount.compareTo(left) > 0) {
            throw new ValidationFailedException(List.of(new FieldError("amount",
                    "at most what is left to refund (" + left.toPlainString() + ")", "OVER_REFUND")));
        }
        boolean processed = !"gateway".equals(method);
        OffsetDateTime now = now();
        RefundEntity refund = RefundEntity.builder()
                .tenantId(payment.getTenantId())
                .paymentId(paymentId)
                .amount(amount)
                .reason(reason == null || reason.isEmpty() ? null : reason)
                .status(processed ? "processed" : "initiated")
                .refundMethod(method)
                .refundedByUserId(caller.userId())
                .refundedAt(processed ? now : null)
                .requestKey(requestKey)
                .build();
        refund.onCreatingData(Actor.current(), clock);
        refunds.insert(refund);
        audit.recordCreate(RefundService.AGGREGATE, refund.getId());
        outbox.publish(RefundService.AGGREGATE + FinanceEvents.CREATED, RefundService.AGGREGATE, refund.getId(),
                FinanceEvents.payload(refund.getId()));
        if (processed) {
            applyRefund(payment, now, caller.userId());
            outbox.publish(RefundService.AGGREGATE + ".processed", RefundService.AGGREGATE, refund.getId(),
                    FinanceEvents.payload(refund.getId()));
        }
        return refunds.findById(refund.getId()).orElseThrow();
    }

    /**
     * Asks the gateway for the refund, outside any transaction. A refusal or no connection at all fails it (nothing was
     * refunded); no answer once connected keeps it initiated (503) until its notification or a reconciliation.
     */
    private RefundEntity throughGateway(PaymentEntity payment, RefundEntity refund, GatewayCredentials credentials) {
        long minor = minorOf(refund.getAmount(), payment);
        GatewayRefund answer;
        try {
            answer = razorpay.createRefund(credentials, payment.getGatewayPaymentId(), minor,
                    Map.of(NOTE_TENANT, payment.getTenantId().toString(), NOTE_REFUND, refund.getId().toString()));
        } catch (PaymentGatewayRejectedException | PaymentGatewayUnreachableException notRefunded) {
            runInWriteTransaction(() -> settleRefund(repository.lock(payment.getId()).orElseThrow(), refund.getId(),
                    false, null, now(), Actor.current().orElse(null)));
            throw notRefunded;
        }
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            PaymentEntity locked = repository.lock(payment.getId()).orElseThrow();
            if ("processed".equals(answer.status())) {
                settleRefund(locked, refund.getId(), true, answer.id(), now, actor);
            } else {
                refunds.noteGatewayId(refund.getId(), answer.id(), now, actor);
            }
            return refunds.findById(refund.getId()).orElseThrow();
        });
    }

    /**
     * The initiated gateway refund as the gateway holds it now: settled there, settled here; pending there, its gateway
     * id noted; unknown there after {@code reconcileAfter}, failed (it never arrived). Unchanged when the gateway
     * cannot answer.
     */
    RefundEntity reconciled(RefundEntity refund) {
        if (!"initiated".equals(refund.getStatus()) || !"gateway".equals(refund.getRefundMethod())) {
            return refund;
        }
        Optional<PaymentEntity> payment = doInReadTransaction(() -> repository.findById(refund.getPaymentId()));
        if (payment.isEmpty() || payment.get().getGatewayPaymentId() == null) {
            return refund;
        }
        Optional<GatewayRefund> found;
        try {
            AccountRow account = doInReadTransaction(() -> Optional.ofNullable(payment.get().getGatewayAccountId())
                    .flatMap(gateway::account)).orElseThrow(() -> new PaymentGatewayNotConfiguredException(
                            "The payment's gateway account is gone."));
            found = razorpay.findRefund(credentials(account, payment.get().getTenantId()),
                    payment.get().getGatewayPaymentId(), NOTE_REFUND, refund.getId().toString());
        } catch (ApiException unanswered) {
            // the gateway (or its account) cannot tell now: the refund keeps waiting, its amount committed
            return refund;
        }
        Optional<String> state = found.map(GatewayRefund::status);
        boolean settled = state.filter(status -> "processed".equals(status) || "failed".equals(status)).isPresent();
        boolean lost = found.isEmpty() && refund.getCreatedAt().isBefore(now().minus(reconcileAfter));
        boolean noted = found.isPresent() && refund.getGatewayRefundId() == null;
        if (!settled && !lost && !noted) {
            return refund;
        }
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            PaymentEntity locked = repository.lock(refund.getPaymentId()).orElseThrow();
            if (settled || lost) {
                settleRefund(locked, refund.getId(), "processed".equals(state.orElse(null)),
                        found.map(GatewayRefund::id).orElse(null), now, actor);
            } else {
                refunds.noteGatewayId(refund.getId(), found.get().id(), now, actor);
            }
            return refunds.findById(refund.getId()).orElseThrow();
        });
    }

    /** Applies the gateway's word on an initiated refund once; the caller holds the payment's lock. */
    private void settleRefund(PaymentEntity payment, UUID refundId, boolean processed, String gatewayRefundId,
            OffsetDateTime now, UUID actor) {
        if (refunds.settle(refundId, processed ? "processed" : "failed", gatewayRefundId, processed ? now : null, now,
                actor) == 1) {
            if (processed) {
                applyRefund(payment, now, actor);
            }
            audit.recordTransition(RefundService.AGGREGATE, refundId, processed ? "process" : "fail",
                    List.of("status", "gatewayRefundId"), null);
            outbox.publish(RefundService.AGGREGATE + (processed ? ".processed" : ".failed"), RefundService.AGGREGATE,
                    refundId, FinanceEvents.payload(refundId));
        }
    }

    /** A processed refund's effects: the invoices it touched re-settled, and the payment refunded once all is back. */
    private void applyRefund(PaymentEntity payment, OffsetDateTime now, UUID actor) {
        List<AllocationRow> slices = ledger.allocationsOf(List.of(payment.getId()));
        Set<UUID> touched = new HashSet<>();
        slices.forEach(slice -> {
            if (slice.invoiceId() != null) {
                touched.add(slice.invoiceId());
            }
        });
        if (!touched.isEmpty()) {
            invoiceRepository.lock(touched);
            settle(touched, now, actor);
        }
        BigDecimal back = ledger.refunded(List.of(payment.getId())).getOrDefault(payment.getId(), BigDecimal.ZERO);
        if (back.compareTo(payment.getAmount()) >= 0 && repository.markRefunded(payment.getId(), now, actor) == 1) {
            audit.recordTransition(AGGREGATE, payment.getId(), "refund", List.of("status"), null);
            outbox.publish(AGGREGATE + ".refunded", AGGREGATE, payment.getId(), FinanceEvents.payload(payment.getId()));
        }
    }

    // ---- shared ----

    /** Each invoice's paid amount set to the money its allocations hold (the invoices are locked by the caller). */
    private void settle(Collection<UUID> invoiceIds, OffsetDateTime now, UUID actor) {
        if (invoiceIds.isEmpty()) {
            return;
        }
        List<AllocationRow> slices = ledger.allocationsTouching(invoiceIds);
        Ledger held = Ledger.of(slices, ledger.refunded(paymentsOf(slices)));
        for (UUID invoiceId : invoiceIds) {
            invoiceRepository.settle(invoiceId, held.paidTo(invoiceId), now, actor);
            audit.recordTransition(InvoiceService.AGGREGATE, invoiceId, "settle", List.of("amountPaid", "status"),
                    null);
        }
    }

    /** The invoice a checkout pays: the caller's to pay (404), open (409), with something left to pay (409). */
    private InvoiceEntity payable(UUID invoiceId, DataScope scope) {
        InvoiceEntity invoice = invoices.readable(invoiceId, scope);
        if (!OPEN_INVOICE.contains(invoice.getStatus())) {
            throw new InvalidStateException("Only a posted invoice that is not fully paid is paid.");
        }
        if (outstanding(invoice).signum() <= 0) {
            throw new InvalidStateException("Nothing is left to pay on the invoice.");
        }
        if (invoice.getInstitutionId() == null) {
            throw new PermissionDeniedException("A tenant-level invoice is not paid online.");
        }
        return invoice;
    }

    /**
     * Ids that sort in the order given — one UUIDv7 counted up in its last byte — so a payment's slices are peeled
     * by refunds in the reverse of the order the request listed them ({@link Ledger}).
     */
    private List<UUID> orderedIds(int count) {
        UUID base = UuidV7.next(clock);
        long low = base.getLeastSignificantBits() & ~0xFFL;
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ids.add(new UUID(base.getMostSignificantBits(), low + i));
        }
        return ids;
    }

    private static BigDecimal outstanding(InvoiceEntity invoice) {
        return invoice.getTotal().subtract(invoice.getAmountPaid());
    }

    private GatewayCredentials credentials(AccountRow account, UUID tenantId) {
        if (!account.active()) {
            throw new PaymentGatewayNotConfiguredException("The payment gateway account is not active.");
        }
        if (!RAZORPAY.equals(account.gatewayName())) {
            throw new ValidationFailedException(List.of(new FieldError("gatewayName",
                    "only razorpay is supported", "UNSUPPORTED_GATEWAY")));
        }
        String secret = Optional.ofNullable(account.keySecretEnc())
                .flatMap(envelope -> cipher.decrypt(envelope, tenantId, KEY_SECRET))
                .orElseThrow(() -> new PaymentGatewayNotConfiguredException(
                        "The payment gateway account has no key secret."));
        if (account.keyId() == null || account.keyId().isBlank()) {
            throw new PaymentGatewayNotConfiguredException("The payment gateway account has no key id.");
        }
        return new GatewayCredentials(account.keyId(), secret);
    }

    /**
     * A refund amount in the order's minor units: the exponent is the one that turned the payment's amount into the
     * order's amount (exact, never rounded).
     */
    static long minorOf(BigDecimal amount, PaymentEntity payment) {
        Long orderMinor = payment.getGatewayAmountMinor();
        for (int exponent = 0; exponent <= 4 && orderMinor != null; exponent++) {
            if (payment.getAmount().movePointRight(exponent).compareTo(BigDecimal.valueOf(orderMinor)) == 0) {
                return RazorpayClient.minorUnits(amount, exponent);
            }
        }
        throw new UnavailableException("The payment's gateway amount is not known.");
    }

    private static void requireInvoicePurpose(String purpose) {
        if (!"invoice".equals(purpose)) {
            throw new ValidationFailedException(List.of(new FieldError("purpose",
                    "finance's checkout pays invoices; course and admission purchases are paid where they are sold",
                    "UNSUPPORTED_PURPOSE")));
        }
    }

    private static List<UUID> paymentsOf(List<AllocationRow> slices) {
        return slices.stream().map(AllocationRow::paymentId).distinct().toList();
    }

    private static Optional<UUID> uuid(String value) {
        try {
            return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException notUuid) {
            return Optional.empty();
        }
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
