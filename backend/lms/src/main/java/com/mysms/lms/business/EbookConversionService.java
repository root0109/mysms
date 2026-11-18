package com.mysms.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.lms.api.model.Ebook;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.EbookEntity;
import com.mysms.lms.errors.ProviderUnavailableException;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.EbookMapper;
import com.mysms.lms.repository.EbookConversionRepository;
import com.mysms.lms.repository.EbookConversionRepository.Head;
import com.mysms.lms.repository.EbookRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Converting an e-book's uploaded source document through the deployment's conversion service (ConvertEbook) and the
 * service's callbacks (ReceiveEbookConversion). The job usually finishes within the configured wait and the converted
 * file is stored at once; a slower job's outcome arrives by callback, and the next ConvertEbook call stores the file
 * under the caller's identity (the callback has none, so it cannot write to the platform file store). Unconfigured,
 * the operations answer 503 {@code CONVERSION_NOT_CONFIGURED}; a service that refuses or cannot be reached is 503
 * {@code CONVERSION_PROVIDER_FAILED}.
 */
@Service
public class EbookConversionService extends AppTransactionManager {

    static final String AGGREGATE = EbookEntity.TABLE;
    static final String PERMISSION = "lms.ebook.convert_ebook";
    static final String TENANT_KEY = "mysms_tenant";
    static final String EBOOK_KEY = "mysms_ebook";
    static final String TARGET_FORMAT = "pdf";
    static final long MAX_OUTPUT_BYTES = 200L * 1024 * 1024;
    static final int MAX_ERROR_LENGTH = 300;
    /** Who a callback's outcome is recorded as changed by: lms itself, acting for the conversion service. */
    static final UUID SERVICE_ACTOR = AuthContext.serviceUserId("lms");

    private final EbookConversionRepository repository;
    private final EbookRepository ebooks;
    private final EbookMapper mapper;
    private final Map<String, EbookConversionProvider> providers;
    private final PlatformFiles files;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final JsonMapper json;
    private final Clock clock;
    private final String providerName;
    private final String apiUrl;
    private final String apiKey;
    private final String callbackSecret;
    private final Duration wait;

    public EbookConversionService(EbookConversionRepository repository, EbookRepository ebooks, EbookMapper mapper,
            List<EbookConversionProvider> providers, PlatformFiles files,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, JsonMapper json, Clock clock,
            @Value("${mysms.lms.conversion.provider}") String providerName,
            @Value("${mysms.lms.conversion.api-url}") String apiUrl,
            @Value("${mysms.lms.conversion.api-key}") String apiKey,
            @Value("${mysms.lms.conversion.callback-secret}") String callbackSecret,
            @Value("${mysms.lms.conversion.wait}") Duration wait) {
        super(database, transactions);
        this.repository = repository;
        this.ebooks = ebooks;
        this.mapper = mapper;
        this.providers = providers.stream()
                .collect(Collectors.toMap(EbookConversionProvider::name, Function.identity()));
        this.files = files;
        this.outbox = outbox;
        this.audit = audit;
        this.json = json;
        this.clock = clock;
        this.providerName = providerName;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.callbackSecret = callbackSecret;
        this.wait = wait;
    }

    /**
     * Sends the e-book's source document for conversion and, when the job finishes within the wait, stores the
     * converted file as the caller's. Called again after a callback, it stores a finished job's file, surfaces a
     * failed job as 409 {@code CONVERSION_FAILED} (clearing it, so the next call retries), and resubmits after an
     * earlier failure. An e-book without a source document is 409.
     */
    public Ebook convertEbook(UUID id) {
        DataScope scope = DataScope.current(PERMISSION);
        UUID actor = AuthContext.require().userId();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        Head head = doInReadTransaction(() -> repository.head(id))
                .filter(h -> scope.canRead(Optional.ofNullable(h.institutionId())))
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        scope.requireWritable(Optional.ofNullable(head.institutionId()));
        if (head.outputUrl() != null) {
            return store(head, head.outputUrl(), actor);
        }
        if (head.error() != null) {
            String error = head.error();
            runInWriteTransaction(() -> repository.outcome(id, head.jobRef(), null, null, now(), actor));
            throw new ConflictException("CONVERSION_FAILED", error);
        }
        if (head.sourceFileId() == null) {
            throw new InvalidStateException("The e-book has no uploaded source document.");
        }
        EbookConversionProvider provider = provider();
        EbookConversionProvider.Service service = service();
        String sourceUrl = files.link(head.sourceFileId(), "ebookId").getUrl();
        String jobRef;
        Optional<EbookConversionProvider.Result> result;
        try {
            jobRef = provider.submit(service, new EbookConversionProvider.Conversion(sourceUrl, fileName(head),
                    TARGET_FORMAT, Map.of(TENANT_KEY, tenant.toString(), EBOOK_KEY, id.toString())));
            result = provider.await(service, jobRef);
        } catch (EbookConversionProvider.Failure e) {
            throw failed();
        }
        if (result.isPresent() && result.get().succeeded()) {
            return store(sent(head, jobRef, actor), result.get().outputUrl(), actor);
        }
        if (result.isPresent()) {
            throw new ConflictException("CONVERSION_FAILED", result.get().error());
        }
        return sentDto(sent(head, jobRef, actor));
    }

    /**
     * The conversion service's callback, signed the MySMS way with the configured callback secret (401 otherwise):
     * the job's outcome is recorded on the e-book that sent it — the tenant and e-book come from the echoed
     * references, a job the e-book no longer waits for is 404.
     */
    public void receiveEbookConversion(String provider, String signature, String timestamp, byte[] body) {
        if (!providers.containsKey(provider)) {
            throw new ResourceNotFoundException("lms.conversion_provider", provider);
        }
        if (!provider.equals(providerName) || callbackSecret.isBlank()) {
            throw notConfigured("The conversion service is not configured.");
        }
        WebhookSignature.verify(callbackSecret, timestamp, signature, body, clock.instant());
        Map<String, Object> parsed;
        try {
            parsed = json.readValue(body, Map.class);
        } catch (JacksonException e) {
            throw WebhookSignature.refused();
        }
        EbookConversionProvider.Event event = providers.get(provider).event(parsed).orElse(null);
        if (event == null) {
            return;
        }
        UUID tenant;
        UUID ebookId;
        try {
            tenant = UUID.fromString(event.metadata().getOrDefault(TENANT_KEY, ""));
            ebookId = UUID.fromString(event.metadata().getOrDefault(EBOOK_KEY, ""));
        } catch (IllegalArgumentException e) {
            throw WebhookSignature.refused();
        }
        String trace = CallContextHolder.current().map(CallContext::traceId).orElse("");
        CallContextHolder.callAs(CallContext.anonymous(trace).withIdentity(tenant, null), () -> {
            record(ebookId, event);
            return null;
        });
    }

    private void record(UUID ebookId, EbookConversionProvider.Event event) {
        String error = event.result().error();
        runInWriteTransaction(() -> {
            int changed = repository.outcome(ebookId, event.jobRef(), event.result().outputUrl(),
                    error == null ? null : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH)),
                    now(), SERVICE_ACTOR);
            if (changed == 0) {
                throw new ResourceNotFoundException(AGGREGATE, ebookId);
            }
            audit.recordTransition(AGGREGATE, ebookId, "receiveConversion", List.of("conversionJobRef"), null);
            outbox.publish(AGGREGATE + ".receiveConversion", AGGREGATE, ebookId, LmsEvents.payload(ebookId));
        });
    }

    /** Fetches the converted file, stores it as the caller's and makes it the e-book's. */
    private Ebook store(Head head, String outputUrl, UUID actor) {
        byte[] content = files.fetch(outputUrl, MAX_OUTPUT_BYTES);
        UUID fileId = files.write(fileName(head), "application/pdf", content);
        return doInWriteTransaction(() -> {
            Head locked = repository.lock(head.id())
                    .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, head.id()));
            if (repository.finished(head.id(), locked.rowVersion(), fileId, now(), actor) == 0) {
                throw new ConflictException("CONVERSION_CONFLICT", "The e-book changed meanwhile; read it again.");
            }
            audit.recordTransition(AGGREGATE, head.id(), "convert", List.of("fileId"), null);
            outbox.publish(AGGREGATE + ".convert", AGGREGATE, head.id(), LmsEvents.payload(head.id()));
            return mapper.toDto(ebooks.findById(head.id()).orElseThrow());
        });
    }

    /** Remembers the sent job on the e-book; answers the row as sent. */
    private Head sent(Head head, String jobRef, UUID actor) {
        runInWriteTransaction(() -> {
            if (repository.sent(head.id(), head.rowVersion(), jobRef, now(), actor) == 0) {
                throw new ConflictException("CONVERSION_CONFLICT", "The e-book changed meanwhile; read it again.");
            }
            audit.recordTransition(AGGREGATE, head.id(), "convert", List.of("conversionJobRef"), null);
            outbox.publish(AGGREGATE + ".convert", AGGREGATE, head.id(), LmsEvents.payload(head.id()));
        });
        return head;
    }

    private Ebook sentDto(Head head) {
        return doInReadTransaction(() -> mapper.toDto(ebooks.findById(head.id()).orElseThrow()));
    }

    private EbookConversionProvider provider() {
        EbookConversionProvider provider = providers.get(providerName);
        if (provider == null) {
            throw notConfigured("No conversion service is configured.");
        }
        return provider;
    }

    private EbookConversionProvider.Service service() {
        if (apiUrl.isBlank() || apiKey.isBlank()) {
            throw notConfigured("The conversion service has no API URL or key.");
        }
        return new EbookConversionProvider.Service(apiUrl, apiKey, wait);
    }

    private String fileName(Head head) {
        String safe = head.title() == null ? "" : head.title().replaceAll("[^\\p{L}\\p{N} ._-]", "").trim();
        return (safe.isBlank() ? "ebook" : safe) + "." + TARGET_FORMAT;
    }

    private static ProviderUnavailableException notConfigured(String detail) {
        return new ProviderUnavailableException(ProviderUnavailableException.CONVERSION_NOT_CONFIGURED, detail);
    }

    private static ProviderUnavailableException failed() {
        return new ProviderUnavailableException(ProviderUnavailableException.CONVERSION_PROVIDER_FAILED,
                "The conversion service refused the request or could not be reached.");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
