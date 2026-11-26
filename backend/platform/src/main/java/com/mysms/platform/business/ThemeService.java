package com.mysms.platform.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.platform.api.model.Theme;
import com.mysms.platform.api.model.ThemeUpdate;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.ThemeEntity;
import com.mysms.platform.domain.ThemeTokenEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.ThemeMapper;
import com.mysms.platform.repository.ThemeRepository;
import com.mysms.platform.validation.ThemeTokens;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of platform.theme with its design tokens (platform.theme_token, written only as part of the theme).
 * Themes are tenant-level: every signed-in user of the tenant may read them (the contract makes reading public; a
 * caller without a tenant sees none), and only a tenant-wide grant changes them. A theme shows only live, uploaded
 * files of the tenant; marking one theme as the default takes the mark off the previous one in the same transaction,
 * since public branding reads the default theme.
 */
@Service
public class ThemeService extends AppTransactionManager {

    public static final String AGGREGATE = ThemeEntity.TABLE;
    static final String PERMISSION_CREATE = "platform.theme.create";
    static final String PERMISSION_UPDATE = "platform.theme.update";
    static final String PERMISSION_DELETE = "platform.theme.delete";

    private final ThemeRepository repository;
    private final ThemeMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public ThemeService(ThemeRepository repository, ThemeMapper mapper,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /** A theme of the caller's tenant; 404 for an unknown id or a caller without a tenant. */
    public Theme getTheme(UUID id) {
        if (!inTenant()) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return doInReadTransaction(() -> withTokens(List.of(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id)))).get(0));
    }

    /** The caller's tenant's themes; an empty page for a caller without a tenant. */
    public Page<Theme> listThemes(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, ThemeRepository.SORT_FIELDS, ThemeRepository.DEFAULT_ORDER,
                ThemeRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, ThemeRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        if (!inTenant()) {
            return new Page<>(List.of(), page.page(), page.size(), "");
        }
        return doInReadTransaction(() -> {
            Page<ThemeEntity> rows = repository.list(page, where);
            List<Theme> themes = withTokens(rows.items());
            return new Page<>(themes, rows.page(), rows.size(), rows.nextCursor());
        });
    }

    /** The default theme of the bound tenant with its tokens (public branding), when the tenant marked one. */
    public Optional<Theme> defaultTheme() {
        return doInReadTransaction(() -> repository.findDefault().map(theme -> withTokens(List.of(theme)).get(0)));
    }

    /** Creates the theme and its tokens; with validateOnly every check runs and nothing is written. */
    public Theme createTheme(Theme request, boolean validateOnly) {
        validator.validate(request);
        Map<String, String> tokens = request.getTokens() == null ? Map.of() : ThemeTokens.parse(request.getTokens());
        ThemeEntity entity = mapper.toEntity(request);
        entity.setIsDefault(Boolean.TRUE.equals(entity.getIsDefault()));
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            scope.requireWritable(Optional.empty());
            assertFiles(entity);
            if (validateOnly) {
                return mapper.toDto(entity).tokens(tokens);
            }
            OffsetDateTime now = entity.getCreatedAt();
            if (entity.getIsDefault()) {
                takeDefaultFromOthers(entity.getId(), now);
            }
            ThemeEntity stored = repository.insert(entity);
            repository.replaceTokens(stored.getId(), tokens, newIds(tokens.size()), now, entity.getCreatedBy());
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, stored.getId(),
                    PlatformEvents.payload(stored.getId()));
            return mapper.toDto(stored).tokens(tokens);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; {@code tokens} sent as null removes every override.
     */
    public Theme updateTheme(UUID id, String ifMatch, ThemeUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        Optional<Object> sentTokens = changes.getTokens();
        Optional<Map<String, String>> tokens = sentTokens == null ? Optional.empty()
                : Optional.of(sentTokens.map(ThemeTokens::parse).orElse(Map.of()));
        return doInWriteTransaction(() -> {
            DataScope.current(PERMISSION_UPDATE).requireWritable(Optional.empty());
            ThemeEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = new ArrayList<>(apply(entity, changes));
            tokens.ifPresent(set -> fields.add("tokens"));
            Theme merged = withTokens(List.of(entity)).get(0);
            tokens.ifPresent(merged::setTokens);
            validator.validate(merged);
            assertFiles(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity).tokens(merged.getTokens());
            }
            if (entity.getIsDefault()) {
                takeDefaultFromOthers(id, entity.getUpdatedAt());
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            tokens.ifPresent(set -> repository.replaceTokens(id, set, newIds(set.size()), entity.getUpdatedAt(),
                    entity.getUpdatedBy()));
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + PlatformEvents.UPDATED, AGGREGATE, id, PlatformEvents.payload(id));
            return mapper.toDto(entity).tokens(merged.getTokens());
        });
    }

    /** Soft delete under If-Match, with its tokens; a deleted theme is no longer the default. */
    public void deleteTheme(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope.current(PERMISSION_DELETE).requireWritable(Optional.empty());
            ThemeEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            repository.replaceTokens(id, Map.of(), List.of(), entity.getUpdatedAt(), entity.getUpdatedBy());
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PlatformEvents.DELETED, AGGREGATE, id, PlatformEvents.payload(id));
        });
    }

    private static boolean inTenant() {
        return CallContextHolder.current().flatMap(CallContext::tenantId).isPresent();
    }

    /** The themes as contract DTOs with their tokens, read for all of them in one statement. */
    private List<Theme> withTokens(List<ThemeEntity> themes) {
        Map<UUID, Map<String, String>> tokens = repository.tokensOf(themes.stream().map(ThemeEntity::getId).toList())
                .stream().collect(Collectors.groupingBy(ThemeTokenEntity::getThemeId, LinkedHashMap::new,
                        Collectors.toMap(ThemeTokenEntity::getTokenKey, ThemeTokenEntity::getValue,
                                (first, second) -> first, LinkedHashMap::new)));
        return themes.stream().map(theme -> mapper.toDto(theme).tokens(tokens.getOrDefault(theme.getId(), Map.of())))
                .toList();
    }

    /** One theme is the default: the others lose the mark, each audited and announced as an update. */
    private void takeDefaultFromOthers(UUID keep, OffsetDateTime at) {
        OffsetDateTime now = at.withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        for (UUID previous : repository.clearOtherDefaults(keep, now, Actor.current().orElse(null))) {
            audit.recordUpdate(AGGREGATE, previous, List.of("isDefault"));
            outbox.publish(AGGREGATE + PlatformEvents.UPDATED, AGGREGATE, previous, PlatformEvents.payload(previous));
        }
    }

    private List<UUID> newIds(int count) {
        List<UUID> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(UuidV7.next(clock));
        }
        return ids;
    }

    /**
     * Applies the column fields the request carries (§7.2; the tokens are rows, replaced separately); answers their
     * names for the audit record.
     */
    private static List<String> apply(ThemeEntity entity, ThemeUpdate changes) {
        Patches patch = new Patches();
        patch.apply("name", changes.getName(), entity::setName);
        patch.apply("description", changes.getDescription(), entity::setDescription);
        patch.apply("thumbnailFileId", changes.getThumbnailFileId(), entity::setThumbnailFileId);
        patch.apply("thumbnailAltFileId", changes.getThumbnailAltFileId(), entity::setThumbnailAltFileId);
        patch.apply("logoFileId", changes.getLogoFileId(), entity::setLogoFileId);
        patch.apply("isDefault", changes.getIsDefault(), v -> entity.setIsDefault(Boolean.TRUE.equals(v)));
        return patch.applied();
    }

    /** Every file the theme shows is a live, uploaded file of the tenant (422 naming each one that is not). */
    private void assertFiles(ThemeEntity entity) {
        List<FieldError> errors = new ArrayList<>();
        Map<String, UUID> files = new LinkedHashMap<>();
        files.put("thumbnailFileId", entity.getThumbnailFileId());
        files.put("thumbnailAltFileId", entity.getThumbnailAltFileId());
        files.put("logoFileId", entity.getLogoFileId());
        files.forEach((field, file) -> {
            if (file != null && !repository.fileAvailable(file)) {
                errors.add(new FieldError(field, "not an uploaded file of this tenant", "NOT_FOUND"));
            }
        });
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }
}
