package com.mysms.platform.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.UnauthenticatedException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.payment.GatewayOrder;
import com.mysms.core.payment.GatewayPayment;
import com.mysms.core.payment.RazorpayClient;
import com.mysms.core.web.Etags;
import com.mysms.platform.api.model.CancelSubscriptionRequest;
import com.mysms.platform.api.model.ConfirmSubscriptionCheckoutRequest;
import com.mysms.platform.api.model.StartSubscriptionCheckoutRequest;
import com.mysms.platform.api.model.SubscriptionCheckout;
import com.mysms.platform.api.model.TenantSubscription;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.SaasPlanEntity;
import com.mysms.platform.domain.TenantSubscriptionEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.TenantSubscriptionMapper;
import com.mysms.platform.repository.GatewayPaymentRecord;
import com.mysms.platform.repository.SaasPlanRepository;
import com.mysms.platform.repository.SaasPlanTerms;
import com.mysms.platform.repository.TenantRepository;
import com.mysms.platform.repository.TenantSubscriptionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Business service of platform.tenant_subscription: the tenant's (or one institution's) subscription to a plan, and
 * how it is paid for through the platform's own merchant account. Checkout copies the plan's terms onto a pending
 * subscription and opens a gateway order; the subscription becomes active only once the money is settled — the
 * checkout signature the client hands back with the payment the gateway reports captured, or the gateway's signed
 * {@code order.paid} webhook — and only once, whichever arrives first. Gateway calls are never made inside a
 * database transaction. A subscription of an institution belongs to that institution's scope; a tenant-wide one
 * changes only through a tenant-wide grant.
 */
@Service
public class TenantSubscriptionService extends AppTransactionManager {

    public static final String AGGREGATE = TenantSubscriptionEntity.TABLE;
    static final String PERMISSION_READ = "platform.tenant_subscription.read";
    static final String PERMISSION_CHECKOUT = "platform.tenant_subscription.start_subscription_checkout";
    static final String PERMISSION_CANCEL = "platform.tenant_subscription.cancel_subscription";
    /** The gateway event that proves an order was paid (it carries the order, with its notes, and the payment). */
    static final String ORDER_PAID = "order.paid";
    private static final String PENDING = "pending";
    private static final String ACTIVE = "active";
    private static final Logger LOG = LoggerFactory.getLogger(TenantSubscriptionService.class);

    private final TenantSubscriptionRepository repository;
    private final TenantSubscriptionMapper mapper;
    private final SaasPlanRepository plans;
    private final TenantRepository tenants;
    private final SubscriptionGateway gateway;
    private final CurrencyLookup currencies;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final JsonMapper json;
    private final Clock clock;

    public TenantSubscriptionService(TenantSubscriptionRepository repository, TenantSubscriptionMapper mapper,
            SaasPlanRepository plans, TenantRepository tenants, SubscriptionGateway gateway,
            CurrencyLookup currencies,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit, JsonMapper json, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.plans = plans;
        this.tenants = tenants;
        this.gateway = gateway;
        this.currencies = currencies;
        this.outbox = outbox;
        this.audit = audit;
        this.json = json;
        this.clock = clock;
    }

    public TenantSubscription getTenantSubscription(UUID id) {
        return doInReadTransaction(() -> mapper.toDto(loadVisible(DataScope.current(PERMISSION_READ), id)));
    }

    public Page<TenantSubscription> listTenantSubscriptions(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, TenantSubscriptionRepository.SORT_FIELDS,
                TenantSubscriptionRepository.DEFAULT_ORDER, TenantSubscriptionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, TenantSubscriptionRepository.FILTER_FIELDS)
                .and(TenantSubscriptionRepository.scope(DataScope.current(PERMISSION_READ)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /** The most live files the tenant may keep under the tenant-wide subscription in force on {@code today}. */
    public Optional<Integer> fileLimit(LocalDate today) {
        return doInReadTransaction(() -> repository.fileLimit(today));
    }

    /**
     * platform.StartSubscriptionCheckout: for an active plan with a price, opens a gateway order for the plan's price
     * in its currency and records a pending subscription with the plan's terms copied. The answer carries what the
     * client's checkout needs; {@code paymentId} is the pending subscription (the platform keeps its own payments).
     */
    public SubscriptionCheckout startSubscriptionCheckout(StartSubscriptionCheckoutRequest request) {
        gateway.requireCheckout();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        Optional<UUID> institution = Optional.ofNullable(request.getInstitutionId());
        DataScope.current(PERMISSION_CHECKOUT).requireWritable(institution);
        SaasPlanEntity plan = doInReadTransaction(() -> plans.findById(request.getPlanId()))
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ValidationFailedException(List.of(
                        new FieldError("planId", "not a plan on sale", "NOT_FOUND"))));
        if (plan.getPrice() == null || plan.getPrice().signum() <= 0) {
            throw new ValidationFailedException(List.of(
                    new FieldError("planId", "a plan without a price is not bought through checkout", "FREE_PLAN")));
        }
        CurrencyLookup.Money currency = currencies.of(plan.getCurrencyId());
        long amountMinor = minorUnits(plan.getPrice(), currency.exponent());
        TenantSubscriptionEntity subscription = TenantSubscriptionEntity.builder()
                .tenantId(tenant)
                .institutionId(request.getInstitutionId())
                .planId(plan.getId())
                .requestedByUserId(AuthContext.require().userId())
                .price(plan.getPrice())
                .currencyId(plan.getCurrencyId())
                .billingCycleMonths(plan.getBillingCycleMonths())
                .gatewayName(SubscriptionGateway.NAME)
                .build();
        subscription.onCreatingData(Actor.current(), clock);
        GatewayOrder order = gateway.openOrder(amountMinor, currency.code(), tenant, subscription.getId());
        subscription.setGatewayTxnRef(order.id());
        SaasPlanTerms terms = new SaasPlanTerms(plan.getMaxMembers(), plan.getMaxEvents(), plan.getMaxFolders(),
                plan.getMaxFiles(), plan.getMaxVideos(), plan.getMaxAudios(), plan.getMaxBulletins(),
                plan.getMaxGroups());
        TenantSubscriptionEntity stored = doInWriteTransaction(() -> {
            TenantSubscriptionEntity inserted = repository.insertCheckout(subscription, terms);
            audit.recordCreate(AGGREGATE, inserted.getId());
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, inserted.getId(),
                    PlatformEvents.payload(inserted.getId()));
            return inserted;
        });
        return new SubscriptionCheckout(stored.getId()).gatewayOrderId(order.id())
                .gatewayPublicKey(gateway.publicKey());
    }

    /**
     * platform.ConfirmSubscriptionCheckout: the checkout signature must prove the subscription's order (422); the
     * subscription activates only when the gateway reports the payment captured, otherwise it stays pending for the
     * webhook. Already active answers unchanged; cancelled is 409.
     */
    public TenantSubscription confirmSubscriptionCheckout(UUID id, ConfirmSubscriptionCheckoutRequest request) {
        gateway.requireCheckout();
        DataScope scope = DataScope.current(PERMISSION_CHECKOUT);
        TenantSubscriptionEntity proved = doInReadTransaction(() -> {
            TenantSubscriptionEntity subscription = loadVisible(scope, id);
            scope.requireWritable(institutionOf(subscription));
            if (!request.getGatewayOrderId().equals(subscription.getGatewayTxnRef())
                    || !gateway.checkoutProves(request.getGatewayOrderId(), request.getGatewayPaymentId(),
                            request.getGatewaySignature())) {
                throw new ValidationFailedException(List.of(new FieldError("gatewaySignature",
                        "does not prove a payment of this subscription's order", "INVALID_SIGNATURE")));
            }
            requireConfirmable(subscription);
            return subscription;
        });
        if (ACTIVE.equals(proved.getStatus())) {
            return mapper.toDto(proved);
        }
        Optional<GatewayPayment> captured = gateway.capturedPayment(request.getGatewayOrderId(),
                request.getGatewayPaymentId());
        if (captured.isEmpty()) {
            return mapper.toDto(proved);
        }
        return doInWriteTransaction(() -> {
            TenantSubscriptionEntity subscription = loadVisible(scope, id);
            requireConfirmable(subscription);
            if (PENDING.equals(subscription.getStatus())) {
                String proof = json.writeValueAsString(Map.of("razorpay_order_id", request.getGatewayOrderId(),
                        "razorpay_payment_id", request.getGatewayPaymentId()));
                activate(subscription, new GatewayPaymentRecord(captured.get().status(), captured.get().method(),
                        null, null, proof));
            }
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    /** Pending (to activate) or already active (answered as it is); cancelled meanwhile is 409. */
    private static void requireConfirmable(TenantSubscriptionEntity subscription) {
        if (!PENDING.equals(subscription.getStatus()) && !ACTIVE.equals(subscription.getStatus())) {
            throw new InvalidStateException("The subscription was cancelled before its payment was confirmed.");
        }
    }

    /**
     * platform.ReceiveSubscriptionWebhook: only a body the webhook secret signed is read (401 otherwise). An
     * {@code order.paid} event activates the pending subscription its order notes name, in the tenant they name; every
     * other event, and an order the platform did not open (no tenant and subscription notes, or ones naming no tenant
     * or subscription of this platform), is acknowledged and changes nothing, so the gateway stops redelivering. A
     * repeat finds the subscription active and changes nothing (idempotent by state).
     */
    public void receiveSubscriptionWebhook(String signature, byte[] body) {
        if (!gateway.webhookProves(body, signature)) {
            throw new UnauthenticatedException("UNAUTHENTICATED", "The webhook signature does not match the body.");
        }
        JsonNode event;
        try {
            event = json.readTree(body);
        } catch (JacksonException notJson) {
            throw new InvalidArgumentException("The webhook body is not JSON.");
        }
        if (!ORDER_PAID.equals(event.path("event").asString(""))) {
            return;
        }
        JsonNode order = event.path("payload").path("order").path("entity");
        JsonNode payment = event.path("payload").path("payment").path("entity");
        Optional<UUID> tenantNote = noteId(order, SubscriptionGateway.NOTE_TENANT);
        Optional<UUID> subscriptionNote = noteId(order, SubscriptionGateway.NOTE_SUBSCRIPTION);
        if (tenantNote.isEmpty() || subscriptionNote.isEmpty()) {
            LOG.warn("a paid gateway order carries no subscription notes (not opened by the platform); acknowledged");
            return;
        }
        UUID tenant = tenantNote.get();
        UUID subscription = subscriptionNote.get();
        if (doInGlobalReadTransaction(() -> tenants.findById(tenant)).isEmpty()) {
            LOG.warn("a paid gateway order names no tenant of this platform; acknowledged without change");
            return;
        }
        String trace = CallContextHolder.current().map(CallContext::traceId).orElseThrow();
        CallContextHolder.callAs(CallContext.anonymous(trace).withIdentity(tenant, null), () -> {
            runInWriteTransaction(() -> settle(subscription, order.path("id").asString(""), payment));
            return null;
        });
    }

    /**
     * platform.CancelSubscription: a pending or active subscription is cancelled (409 otherwise); an active one stays
     * in force until its end date, which does not change. A stale body etag is 412; the reason is audited.
     */
    public TenantSubscription cancelSubscription(UUID id, CancelSubscriptionRequest request) {
        DataScope scope = DataScope.current(PERMISSION_CANCEL);
        return doInWriteTransaction(() -> {
            TenantSubscriptionEntity subscription = loadVisible(scope, id);
            scope.requireWritable(institutionOf(subscription));
            OptionalInt expected = Etags.bodyVersion(request.getEtag());
            if (expected.isPresent() && expected.getAsInt() != subscription.getRowVersion()) {
                throw new PreconditionFailedException("The etag does not match the current version.");
            }
            if (!PENDING.equals(subscription.getStatus()) && !ACTIVE.equals(subscription.getStatus())) {
                throw new InvalidStateException("Only a pending or active subscription can be cancelled.");
            }
            if (repository.cancel(id, subscription.getRowVersion(), now(), Actor.current().orElse(null)) == 0) {
                throw new PreconditionFailedException("The row changed since it was read.");
            }
            audit.recordTransition(AGGREGATE, id, "cancel", List.of("status"), request.getReason());
            outbox.publish(AGGREGATE + ".cancel", AGGREGATE, id, PlatformEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    /** The webhook's work in the subscription's tenant: activate a pending one, keep late money on a cancelled one. */
    private void settle(UUID id, String orderId, JsonNode payment) {
        Optional<TenantSubscriptionEntity> found = repository.findById(id)
                .filter(s -> orderId.equals(s.getGatewayTxnRef()));
        if (found.isEmpty()) {
            LOG.warn("a paid gateway order names subscription {} which did not open it; acknowledged", id);
            return;
        }
        TenantSubscriptionEntity subscription = found.get();
        String raw = json.writeValueAsString(payment);
        if (PENDING.equals(subscription.getStatus())) {
            activate(subscription, new GatewayPaymentRecord(payment.path("status").asString("captured"),
                    payment.path("method").asString(null), payment.path("email").asString(null),
                    payment.path("contact").asString(null), raw));
        } else if (!ACTIVE.equals(subscription.getStatus())) {
            if (repository.recordLatePayment(id, subscription.getRowVersion(), raw, now()) == 0) {
                throw new PreconditionFailedException("The subscription changed while the payment was recorded.");
            }
            LOG.warn("subscription {} was paid after it was cancelled; the payment is kept for a refund", id);
            audit.recordUpdate(AGGREGATE, id, List.of("gatewayStatus"));
        }
    }

    /**
     * Starts the paid period: today, or the day after the last day this scope has already paid for (a renewal), for
     * the plan's billing cycle; audited, with the {@code platform.tenant_subscription.activate} event.
     */
    private void activate(TenantSubscriptionEntity subscription, GatewayPaymentRecord payment) {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate start = repository.paidUntil(subscription.getInstitutionId()).map(day -> day.plusDays(1))
                .filter(day -> day.isAfter(today)).orElse(today);
        LocalDate end = start.plusMonths(subscription.getBillingCycleMonths()).minusDays(1);
        if (repository.activate(subscription.getId(), subscription.getRowVersion(), start, end, payment, now(),
                Actor.current().orElse(null)) == 0) {
            throw new PreconditionFailedException("The subscription changed while its payment was confirmed.");
        }
        audit.recordTransition(AGGREGATE, subscription.getId(), "activate",
                List.of("status", "startDate", "endDate", "amountPaid", "paidAt", "gatewayStatus"), null);
        outbox.publish(AGGREGATE + ".activate", AGGREGATE, subscription.getId(),
                PlatformEvents.payload(subscription.getId()));
    }

    /** The id a note of the order carries; empty when the note is absent or not an id. */
    private static Optional<UUID> noteId(JsonNode order, String note) {
        try {
            return Optional.of(UUID.fromString(order.path("notes").path(note).asString("")));
        } catch (IllegalArgumentException missing) {
            return Optional.empty();
        }
    }

    /** The price in minor units for the gateway; a price with more decimals than the currency has is refused. */
    private static long minorUnits(BigDecimal price, int exponent) {
        try {
            return RazorpayClient.minorUnits(price, exponent);
        } catch (ArithmeticException tooPrecise) {
            throw new ValidationFailedException(List.of(new FieldError("planId",
                    "the plan's price has more decimals than its currency", "PRECISION")));
        }
    }

    /** The live row, if the caller may see it: a row outside the caller's scope is 404. */
    private TenantSubscriptionEntity loadVisible(DataScope scope, UUID id) {
        TenantSubscriptionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution the subscription covers (none: the whole tenant). */
    private static Optional<UUID> institutionOf(TenantSubscriptionEntity entity) {
        return Optional.ofNullable(entity.getInstitutionId());
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
