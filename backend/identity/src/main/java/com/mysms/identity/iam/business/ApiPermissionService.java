package com.mysms.identity.iam.business;

import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.id.UuidV7;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.identity.iam.api.model.ApiPermission;
import com.mysms.identity.iam.config.IamDatabaseConfig;
import com.mysms.identity.iam.domain.ApiPermissionEntity;
import com.mysms.identity.iam.mapper.ApiPermissionMapper;
import com.mysms.identity.iam.repository.ApiPermissionRepository;
import com.mysms.identity.iam.repository.PermissionCatalogRepository;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The API-permission catalogue aggregate (iam.api_permission and the tenant-user codes of iam.permission it feeds),
 * JAVA-CODING-RULES §16.2. Every running module publishes its catalogue at start-up and every
 * {@code mysms.auth.catalogue-interval}; each one received marks its operations seen now (release and definition as
 * published) and its tenant-user codes live. An operation no catalogue has carried for
 * {@code mysms.iam.api-permission.retire-after} is retired, and with it every code no live tenant-user operation
 * carries — by age, so an older replica's catalogue never retires what a newer release serves. Nothing is deleted.
 * Codes a catalogue adds take the catalogue's defaults (no display name, grantable, not implicit to every role): the
 * operation table carries no such flags. Read by platform staff (iam.ListApiPermissions).
 */
@Service
public class ApiPermissionService extends AppTransactionManager {

    /** The scheme of tenant users' tokens: its operations' codes are the ones roles are composed from (rule P1). */
    static final String TENANT_USER_SCHEME = "bearerAuth";

    private static final Logger LOG = LoggerFactory.getLogger(ApiPermissionService.class);

    private final ApiPermissionRepository repository;
    private final PermissionCatalogRepository permissions;
    private final ApiPermissionMapper mapper;
    private final Clock clock;
    private final Duration retireAfter;

    public ApiPermissionService(ApiPermissionRepository repository, PermissionCatalogRepository permissions,
            ApiPermissionMapper mapper, @Qualifier(IamDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(IamDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock,
            @Value("${mysms.iam.api-permission.retire-after}") Duration retireAfter) {

        super(database, transactions);
        this.repository = repository;
        this.permissions = permissions;
        this.mapper = mapper;
        this.clock = clock;
        this.retireAfter = retireAfter;
    }

    /**
     * One module's published catalogue.
     *
     * @param sha256 the hash the module computed over its entries; equal hashes are the same catalogue
     */
    public record Catalogue(String module, String release, String sha256, List<Operation> operations) {

        public Catalogue {
            operations = List.copyOf(operations);
        }
    }

    /** One operation of a catalogue, as the module's operation table states it. */
    public record Operation(String operationId, String method, String path, String permission, List<String> schemes,
                            boolean anonymous, String audience) {

        public Operation {
            schemes = List.copyOf(schemes);
        }
    }

    /**
     * Applies a published catalogue; idempotent, and harmless when it comes late or from an older release. The
     * catalogue is platform-level, so its transaction binds no tenant.
     */
    public void apply(Catalogue catalogue) {
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.MICROS);
        List<ApiPermissionEntity> rows = catalogue.operations().stream().map(this::row).toList();
        List<String> codes = catalogue.operations().stream()
                .filter(o -> o.schemes().contains(TENANT_USER_SCHEME))
                .map(Operation::permission).distinct().sorted().toList();
        runInGlobalWriteTransaction(() -> {
            int retiredOperations = repository.apply(catalogue.module(), catalogue.release(), catalogue.sha256(),
                    rows, now, now.minus(retireAfter));
            int retiredCodes = permissions.publish(catalogue.module(),
                    codes.stream().map(ApiPermissionService::permissionId).toList(), codes, TENANT_USER_SCHEME, now);
            if (retiredOperations > 0 || retiredCodes > 0) {
                LOG.info("api-permission catalogue of {} (release {}): {} operations and {} tenant-user codes retired",
                        catalogue.module(), catalogue.release(), retiredOperations, retiredCodes);
            }
        });
    }

    public Page<ApiPermission> list(Integer pageNumber, Integer pageSize, String cursor, String filter,
                                    List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ApiPermissionRepository.SORT_FIELDS,
                ApiPermissionRepository.DEFAULT_ORDER, ApiPermissionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ApiPermissionRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInGlobalReadTransaction(() -> {
            Page<ApiPermissionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * The id of a permission code, the same in every environment and equal to the baseline seeds'
     * {@code md5('mysms.permission:' || code)::uuid}: the MD5 digest read as the UUID's 16 bytes.
     */
    static UUID permissionId(String code) {
        try {
            ByteBuffer digest = ByteBuffer.wrap(MessageDigest.getInstance("MD5")
                    .digest(("mysms.permission:" + code).getBytes(StandardCharsets.UTF_8)));
            return new UUID(digest.getLong(), digest.getLong());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is required of every Java runtime", e);
        }
    }

    private ApiPermissionEntity row(Operation operation) {
        return ApiPermissionEntity.builder()
                .id(UuidV7.next(clock))
                .operationId(operation.operationId())
                .httpMethod(operation.method())
                .pathTemplate(operation.path())
                .permissionCode(operation.permission())
                .schemes(String.join(" ", operation.schemes()))
                .isAnonymous(operation.anonymous())
                .audience(operation.audience())
                .build();
    }
}
