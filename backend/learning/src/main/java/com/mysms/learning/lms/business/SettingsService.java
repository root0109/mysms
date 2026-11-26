package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.Patches;
import com.mysms.learning.lms.api.model.SettingsPrimaryCountry;
import com.mysms.learning.lms.api.model.SettingsPrimaryCountryUpdate;
import com.mysms.learning.lms.api.model.SettingsSidebarItem;
import com.mysms.learning.lms.api.model.SettingsSidebarItemUpdate;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.SettingsPrimaryCountryEntity;
import com.mysms.learning.lms.domain.SettingsSidebarItemEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.SettingsPrimaryCountryMapper;
import com.mysms.learning.lms.mapper.SettingsSidebarItemMapper;
import com.mysms.learning.lms.repository.SettingsPrimaryCountryRepository;
import com.mysms.learning.lms.repository.SettingsSidebarItemRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.mysms.authclient.AuthContext;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.learning.lms.api.model.Settings;
import com.mysms.learning.lms.api.model.SettingsUpdate;
import com.mysms.learning.lms.domain.SettingsEntity;
import com.mysms.learning.lms.mapper.SettingsMapper;
import com.mysms.learning.lms.repository.SettingsRepository;

/**
 * Business service of lms.settings and of the records that exist only as part of a Settings (SettingsPrimaryCountry,
 * SettingsSidebarItem): audit and domain event commit in the same transaction as every change. Rows outside the
 * caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class SettingsService extends AppTransactionManager {

    public static final String SETTINGS_PRIMARY_COUNTRY_AGGREGATE = SettingsPrimaryCountryEntity.TABLE;
    static final String SETTINGS_PRIMARY_COUNTRY_PERMISSION_LIST = "lms.settings_primary_country.read";
    static final String SETTINGS_PRIMARY_COUNTRY_PERMISSION_GET = "lms.settings_primary_country.read";
    static final String SETTINGS_PRIMARY_COUNTRY_PERMISSION_CREATE = "lms.settings_primary_country.create";
    static final String SETTINGS_PRIMARY_COUNTRY_PERMISSION_UPDATE = "lms.settings_primary_country.update";
    static final String SETTINGS_PRIMARY_COUNTRY_PERMISSION_DELETE = "lms.settings_primary_country.delete";

    public static final String SETTINGS_SIDEBAR_ITEM_AGGREGATE = SettingsSidebarItemEntity.TABLE;
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_LIST = "lms.settings_sidebar_item.read";
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_GET = "lms.settings_sidebar_item.read";
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_CREATE = "lms.settings_sidebar_item.create";
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_UPDATE = "lms.settings_sidebar_item.update";
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_DELETE = "lms.settings_sidebar_item.delete";

    private final SettingsPrimaryCountryRepository settingsPrimaryCountryRepository;
    private final SettingsPrimaryCountryMapper settingsPrimaryCountryMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final SettingsRepository settingsRepository;
    private final SettingsMapper settingsMapper;
    private final FieldCipher cipher;
    private final SettingsSidebarItemRepository settingsSidebarItemRepository;
    private final SettingsSidebarItemMapper settingsSidebarItemMapper;

    public SettingsService(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            SettingsPrimaryCountryRepository settingsPrimaryCountryRepository,
            SettingsPrimaryCountryMapper settingsPrimaryCountryMapper,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            SettingsSidebarItemRepository settingsSidebarItemRepository,
            SettingsSidebarItemMapper settingsSidebarItemMapper,
            SettingsRepository settingsRepository,
            SettingsMapper settingsMapper,
            FieldCipher cipher) {
        super(database, transactions);
        this.settingsRepository = settingsRepository;
        this.settingsMapper = settingsMapper;
        this.cipher = cipher;
        this.settingsPrimaryCountryRepository = settingsPrimaryCountryRepository;
        this.settingsPrimaryCountryMapper = settingsPrimaryCountryMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.settingsSidebarItemRepository = settingsSidebarItemRepository;
        this.settingsSidebarItemMapper = settingsSidebarItemMapper;
    }

    // ---- SettingsPrimaryCountry: a record of the Settings aggregate ----

    public SettingsPrimaryCountry getSettingsPrimaryCountry(UUID settingsId, UUID id) {
        return doInReadTransaction(() -> {
            return settingsPrimaryCountryMapper.toDto(loadVisibleSettingsPrimaryCountry(DataScope.current(
                    SETTINGS_PRIMARY_COUNTRY_PERMISSION_GET), settingsId, id));
        });
    }

    public Page<SettingsPrimaryCountry> listSettingsPrimaryCountrys(UUID settingsId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, SettingsPrimaryCountryRepository.SORT_FIELDS,
                        SettingsPrimaryCountryRepository.DEFAULT_ORDER,
                SettingsPrimaryCountryRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SettingsPrimaryCountryRepository.FILTER_FIELDS)
                .and(SettingsPrimaryCountryRepository.ofParent(settingsId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + settingsId, clock);
        return doInReadTransaction(() -> {
            Page<SettingsPrimaryCountryEntity> rows = settingsPrimaryCountryRepository.list(page, where);
            return rows.map(settingsPrimaryCountryMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public SettingsPrimaryCountry createSettingsPrimaryCountry(UUID settingsId, SettingsPrimaryCountry request,
            boolean validateOnly) {
        validator.validate(request);
        SettingsPrimaryCountryEntity entity = settingsPrimaryCountryMapper.toEntity(request);
        entity.setSettingsId(settingsId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(SETTINGS_PRIMARY_COUNTRY_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!settingsPrimaryCountryRepository.existsSettingsId(entity.getSettingsId())) {
                throw new ResourceNotFoundException("lms.settings", entity.getSettingsId());
            }
            assertValidSettingsPrimaryCountry(entity, true);
            scope.requireWritable(institutionOfSettingsPrimaryCountry(entity));
            assertUniqueSettingsPrimaryCountry(entity);
            if (validateOnly) {
                return settingsPrimaryCountryMapper.toDto(entity);
            }
            SettingsPrimaryCountryEntity stored = settingsPrimaryCountryRepository.insert(entity);
            audit.recordCreate(SETTINGS_PRIMARY_COUNTRY_AGGREGATE, stored.getId());
            outbox.publish(SETTINGS_PRIMARY_COUNTRY_AGGREGATE + LmsEvents.CREATED, SETTINGS_PRIMARY_COUNTRY_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return settingsPrimaryCountryMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public SettingsPrimaryCountry updateSettingsPrimaryCountry(UUID settingsId, UUID id, String ifMatch,
            SettingsPrimaryCountryUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SETTINGS_PRIMARY_COUNTRY_PERMISSION_UPDATE);
            SettingsPrimaryCountryEntity entity = loadVisibleSettingsPrimaryCountry(scope, settingsId, id);
            scope.requireWritable(institutionOfSettingsPrimaryCountry(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applySettingsPrimaryCountry(entity, changes);
            validator.validate(settingsPrimaryCountryMapper.toDto(entity));
            assertValidSettingsPrimaryCountry(entity, false);
            scope.requireWritable(institutionOfSettingsPrimaryCountry(entity));
            assertUniqueSettingsPrimaryCountry(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return settingsPrimaryCountryMapper.toDto(entity);
            }
            if (settingsPrimaryCountryRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(SETTINGS_PRIMARY_COUNTRY_AGGREGATE, id, fields);
            outbox.publish(SETTINGS_PRIMARY_COUNTRY_AGGREGATE + LmsEvents.UPDATED, SETTINGS_PRIMARY_COUNTRY_AGGREGATE, id,
                    LmsEvents.payload(id));
            return settingsPrimaryCountryMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSettingsPrimaryCountry(UUID settingsId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SETTINGS_PRIMARY_COUNTRY_PERMISSION_DELETE);
            SettingsPrimaryCountryEntity entity = loadVisibleSettingsPrimaryCountry(scope, settingsId, id);
            scope.requireWritable(institutionOfSettingsPrimaryCountry(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (settingsPrimaryCountryRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(SETTINGS_PRIMARY_COUNTRY_AGGREGATE, id);
            outbox.publish(SETTINGS_PRIMARY_COUNTRY_AGGREGATE + LmsEvents.DELETED, SETTINGS_PRIMARY_COUNTRY_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SettingsPrimaryCountryEntity loadVisibleSettingsPrimaryCountry(DataScope scope, UUID settingsId, UUID id) {
        SettingsPrimaryCountryEntity entity = settingsPrimaryCountryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SETTINGS_PRIMARY_COUNTRY_AGGREGATE, id));
        if (!scope.canRead(institutionOfSettingsPrimaryCountry(entity)) || !settingsId.equals(entity.getSettingsId())) {
            throw new ResourceNotFoundException(SETTINGS_PRIMARY_COUNTRY_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfSettingsPrimaryCountry(SettingsPrimaryCountryEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applySettingsPrimaryCountry(SettingsPrimaryCountryEntity entity,
            SettingsPrimaryCountryUpdate changes) {
        Patches patch = new Patches();
        patch.apply("countryId", changes.getCountryId(), entity::setCountryId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidSettingsPrimaryCountry(SettingsPrimaryCountryEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getSettingsId() == null) {
            errors.add(new FieldError("settingsId", "must not be null", "NotNull"));
        }
        if (entity.getCountryId() == null) {
            errors.add(new FieldError("countryId", "must not be null", "NotNull"));
        }
        if (entity.getSettingsId() != null && !settingsPrimaryCountryRepository.existsSettingsId(entity.getSettingsId())) {
            errors.add(new FieldError("settingsId", "settings does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueSettingsPrimaryCountry(SettingsPrimaryCountryEntity entity) {
        if (settingsPrimaryCountryRepository.takenSettingsIdCountryId1(entity)) {
            throw new DuplicateResourceException(
                    "A settings primary country with the same settings id and country id already exists.");
        }
    }

    // ---- SettingsSidebarItem: a record of the Settings aggregate ----

    public SettingsSidebarItem getSettingsSidebarItem(UUID settingsId, UUID id) {
        return doInReadTransaction(() -> {
            return settingsSidebarItemMapper.toDto(loadVisibleSettingsSidebarItem(DataScope.current(
                    SETTINGS_SIDEBAR_ITEM_PERMISSION_GET), settingsId, id));
        });
    }

    public Page<SettingsSidebarItem> listSettingsSidebarItems(UUID settingsId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, SettingsSidebarItemRepository.SORT_FIELDS,
                        SettingsSidebarItemRepository.DEFAULT_ORDER,
                SettingsSidebarItemRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, SettingsSidebarItemRepository.FILTER_FIELDS)
                .and(SettingsSidebarItemRepository.ofParent(settingsId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + settingsId, clock);
        return doInReadTransaction(() -> {
            Page<SettingsSidebarItemEntity> rows = settingsSidebarItemRepository.list(page, where);
            return rows.map(settingsSidebarItemMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public SettingsSidebarItem createSettingsSidebarItem(UUID settingsId, SettingsSidebarItem request,
            boolean validateOnly) {
        validator.validate(request);
        SettingsSidebarItemEntity entity = settingsSidebarItemMapper.toEntity(request);
        entity.setSettingsId(settingsId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(SETTINGS_SIDEBAR_ITEM_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!settingsSidebarItemRepository.existsSettingsId(entity.getSettingsId())) {
                throw new ResourceNotFoundException("lms.settings", entity.getSettingsId());
            }
            assertValidSettingsSidebarItem(entity, true);
            scope.requireWritable(institutionOfSettingsSidebarItem(entity));
            assertUniqueSettingsSidebarItem(entity);
            if (validateOnly) {
                return settingsSidebarItemMapper.toDto(entity);
            }
            SettingsSidebarItemEntity stored = settingsSidebarItemRepository.insert(entity);
            audit.recordCreate(SETTINGS_SIDEBAR_ITEM_AGGREGATE, stored.getId());
            outbox.publish(SETTINGS_SIDEBAR_ITEM_AGGREGATE + LmsEvents.CREATED, SETTINGS_SIDEBAR_ITEM_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return settingsSidebarItemMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public SettingsSidebarItem updateSettingsSidebarItem(UUID settingsId, UUID id, String ifMatch,
            SettingsSidebarItemUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SETTINGS_SIDEBAR_ITEM_PERMISSION_UPDATE);
            SettingsSidebarItemEntity entity = loadVisibleSettingsSidebarItem(scope, settingsId, id);
            scope.requireWritable(institutionOfSettingsSidebarItem(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applySettingsSidebarItem(entity, changes);
            validator.validate(settingsSidebarItemMapper.toDto(entity));
            assertValidSettingsSidebarItem(entity, false);
            scope.requireWritable(institutionOfSettingsSidebarItem(entity));
            assertUniqueSettingsSidebarItem(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return settingsSidebarItemMapper.toDto(entity);
            }
            if (settingsSidebarItemRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(SETTINGS_SIDEBAR_ITEM_AGGREGATE, id, fields);
            outbox.publish(SETTINGS_SIDEBAR_ITEM_AGGREGATE + LmsEvents.UPDATED, SETTINGS_SIDEBAR_ITEM_AGGREGATE, id,
                    LmsEvents.payload(id));
            return settingsSidebarItemMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteSettingsSidebarItem(UUID settingsId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SETTINGS_SIDEBAR_ITEM_PERMISSION_DELETE);
            SettingsSidebarItemEntity entity = loadVisibleSettingsSidebarItem(scope, settingsId, id);
            scope.requireWritable(institutionOfSettingsSidebarItem(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (settingsSidebarItemRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(SETTINGS_SIDEBAR_ITEM_AGGREGATE, id);
            outbox.publish(SETTINGS_SIDEBAR_ITEM_AGGREGATE + LmsEvents.DELETED, SETTINGS_SIDEBAR_ITEM_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private SettingsSidebarItemEntity loadVisibleSettingsSidebarItem(DataScope scope, UUID settingsId, UUID id) {
        SettingsSidebarItemEntity entity = settingsSidebarItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(SETTINGS_SIDEBAR_ITEM_AGGREGATE, id));
        if (!scope.canRead(institutionOfSettingsSidebarItem(entity)) || !settingsId.equals(entity.getSettingsId())) {
            throw new ResourceNotFoundException(SETTINGS_SIDEBAR_ITEM_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfSettingsSidebarItem(SettingsSidebarItemEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applySettingsSidebarItem(SettingsSidebarItemEntity entity, SettingsSidebarItemUpdate changes) {
        Patches patch = new Patches();
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("itemType", changes.getItemType(), v -> entity.setItemType(v == null ? null : v.getValue()));
        patch.apply("itemKey", changes.getItemKey(), entity::setItemKey);
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("icon", changes.getIcon(), entity::setIcon);
        patch.apply("webPageRoute", changes.getWebPageRoute(), entity::setWebPageRoute);
        patch.apply("route", changes.getRoute(), entity::setRoute);
        patch.apply("url", changes.getUrl(), entity::setUrl);
        patch.apply("isStandard", changes.getIsStandard(), entity::setIsStandard);
        patch.apply("isHidden", changes.getIsHidden(), entity::setIsHidden);
        patch.apply("openInNewWindow", changes.getOpenInNewWindow(), entity::setOpenInNewWindow);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidSettingsSidebarItem(SettingsSidebarItemEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getSettingsId() == null) {
            errors.add(new FieldError("settingsId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getItemType() == null) {
            errors.add(new FieldError("itemType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsStandard() == null) {
            errors.add(new FieldError("isStandard", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsHidden() == null) {
            errors.add(new FieldError("isHidden", "must not be null", "NotNull"));
        }
        if (!creating && entity.getOpenInNewWindow() == null) {
            errors.add(new FieldError("openInNewWindow", "must not be null", "NotNull"));
        }
        if (entity.getSettingsId() != null && !settingsSidebarItemRepository.existsSettingsId(entity.getSettingsId())) {
            errors.add(new FieldError("settingsId", "settings does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueSettingsSidebarItem(SettingsSidebarItemEntity entity) {

    }

    // ---- the tenant's settings ----

    public static final String SETTINGS_AGGREGATE = SettingsEntity.TABLE;
    static final String SETTINGS_PERMISSION_GET = "lms.settings.read";
    static final String SETTINGS_PERMISSION_UPDATE = "lms.settings.update";
    private static final String SECRET_UNSPLASH = "lms.settings.unsplash_access_key_enc";

    /**
     * The tenant's LMS settings; until they are first saved, the column defaults with ETag "0". Tenant-level: only a
     * tenant-wide grant may read them (403 otherwise).
     */
    public Settings getSettings() {
        requireTenantWide(SETTINGS_PERMISSION_GET);
        return doInReadTransaction(() -> settingsDto(settingsRepository.find().orElseGet(this::defaults)));
    }

    /**
     * Partial update under If-Match (ETag "0" saves the first row). The image-search key is stored encrypted and
     * only its mask is returned; leaving it out keeps the stored one, sending it as null clears it (§7.2).
     * Tenant-wide grant only (403).
     */
    public Settings updateSettings(String ifMatch, SettingsUpdate changes, boolean validateOnly) {
        requireTenantWide(SETTINGS_PERMISSION_UPDATE);
        int expected = Etags.expectedVersion(ifMatch);
        if (Patches.value(changes.getUnsplashAccessKey()) != null) {
            cipher.requireConfigured();
        }
        return doInWriteTransaction(() -> {
            Optional<SettingsEntity> stored = settingsRepository.find();
            SettingsEntity entity = stored.orElseGet(this::defaults);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applySettings(entity, changes);
            validator.validate(settingsDto(entity));
            if (entity.getLessonDwellSeconds() == null || entity.getLessonDwellSeconds() < 0) {
                throw new ValidationFailedException(List.of(new FieldError("lessonDwellSeconds",
                        "must be zero or more", "Min")));
            }
            if (validateOnly) {
                return settingsDto(entity);
            }
            SettingsEntity saved;
            if (stored.isEmpty()) {
                entity.onCreatingData(Actor.current(), clock);
                saved = settingsRepository.insert(entity);
                audit.recordCreate(SETTINGS_AGGREGATE, saved.getId());
            } else {
                entity.onUpdatingData(Actor.current(), clock);
                if (settingsRepository.update(entity, expected) == 0) {
                    throw new PreconditionFailedException("If-Match does not match the current ETag.");
                }
                entity.setRowVersion(expected + 1);
                saved = entity;
                audit.recordUpdate(SETTINGS_AGGREGATE, saved.getId(), fields);
            }
            outbox.publish(SETTINGS_AGGREGATE + LmsEvents.UPDATED, SETTINGS_AGGREGATE, saved.getId(),
                    LmsEvents.payload(saved.getId()));
            return settingsDto(saved);
        });
    }

    private static void requireTenantWide(String permission) {
        if (!AuthContext.require().hasTenantWidePermission(permission)) {
            throw new PermissionDeniedException("Only a tenant-wide administrator may use the tenant's LMS settings.");
        }
    }

    /** The unsaved settings: the column defaults, version 0. */
    private SettingsEntity defaults() {
        return SettingsEntity.builder().tenantId(CallContextHolder.require().tenantId().orElseThrow())
                .applyGst(false).showUsdEquivalent(false).applyRounding(false).identifyUserCategory(false)
                .personaCaptured(false).lmsIsDefaultHome(false).sendCalendarInviteForEvaluations(false)
                .allowGuestAccess(true).disableSignup(true).disablePwa(false).allowJobPosting(true)
                .certifiedMembersPage(false).preventSkippingVideos(false).lessonDwellSeconds(30)
                .enforceVideoCompletion(true).enforceQuizCompletion(true).enforceQuizPassing(true)
                .enforceOpenEndedQuizPassing(true).enforceAssignmentCompletion(true).enforceAssignmentPassing(false)
                .enforceExerciseCompletion(false).enforceExercisePassing(false).showCourses(true).showStudents(true)
                .showAssessments(true).showLiveClass(true).showDiscussions(true).showEmails(true).showDashboard(true)
                .showDayView(false).textDirection("auto").sendPaymentRemindersCohort(false)
                .sendPaymentRemindersCourse(false).demoDataPresent(false).rowVersion(0).build();
    }

    private Settings settingsDto(SettingsEntity entity) {
        Settings dto = settingsMapper.toDto(entity);
        if (entity.getUnsplashAccessKeyEnc() != null) {
            dto.setUnsplashAccessKeyMasked(cipher.maskStored(entity.getUnsplashAccessKeyEnc(), entity.getTenantId(),
                    SECRET_UNSPLASH));
        }
        return dto;
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private List<String> applySettings(SettingsEntity entity, SettingsUpdate changes) {
        Patches patch = new Patches();
        patch.apply("livecodeUrl", changes.getLivecodeUrl(), entity::setLivecodeUrl);
        patch.apply("defaultCurrencyId", changes.getDefaultCurrencyId(), entity::setDefaultCurrencyId);
        patch.apply("applyGst", changes.getApplyGst(), entity::setApplyGst);
        patch.apply("showUsdEquivalent", changes.getShowUsdEquivalent(), entity::setShowUsdEquivalent);
        patch.apply("applyRounding", changes.getApplyRounding(), entity::setApplyRounding);
        patch.apply("paymentGateway", changes.getPaymentGateway(), entity::setPaymentGateway);
        patch.apply("identifyUserCategory", changes.getIdentifyUserCategory(), entity::setIdentifyUserCategory);
        patch.apply("personaCaptured", changes.getPersonaCaptured(), entity::setPersonaCaptured);
        patch.apply("lmsIsDefaultHome", changes.getLmsIsDefaultHome(), entity::setLmsIsDefaultHome);
        patch.apply("sendCalendarInviteForEvaluations", changes.getSendCalendarInviteForEvaluations(),
                                entity::setSendCalendarInviteForEvaluations);
        patch.apply("allowGuestAccess", changes.getAllowGuestAccess(), entity::setAllowGuestAccess);
        patch.apply("disableSignup", changes.getDisableSignup(), entity::setDisableSignup);
        patch.apply("disablePwa", changes.getDisablePwa(), entity::setDisablePwa);
        patch.apply("allowJobPosting", changes.getAllowJobPosting(), entity::setAllowJobPosting);
        patch.apply("certifiedMembersPage", changes.getCertifiedMembersPage(), entity::setCertifiedMembersPage);
        patch.apply("preventSkippingVideos", changes.getPreventSkippingVideos(), entity::setPreventSkippingVideos);
        patch.apply("lessonDwellSeconds", changes.getLessonDwellSeconds(), entity::setLessonDwellSeconds);
        patch.apply("enforceVideoCompletion", changes.getEnforceVideoCompletion(), entity::setEnforceVideoCompletion);
        patch.apply("enforceQuizCompletion", changes.getEnforceQuizCompletion(), entity::setEnforceQuizCompletion);
        patch.apply("enforceQuizPassing", changes.getEnforceQuizPassing(), entity::setEnforceQuizPassing);
        patch.apply("enforceOpenEndedQuizPassing", changes.getEnforceOpenEndedQuizPassing(),
                                entity::setEnforceOpenEndedQuizPassing);
        patch.apply("enforceAssignmentCompletion", changes.getEnforceAssignmentCompletion(),
                                entity::setEnforceAssignmentCompletion);
        patch.apply("enforceAssignmentPassing", changes.getEnforceAssignmentPassing(), entity::setEnforceAssignmentPassing);
        patch.apply("enforceExerciseCompletion", changes.getEnforceExerciseCompletion(), entity::setEnforceExerciseCompletion);
        patch.apply("enforceExercisePassing", changes.getEnforceExercisePassing(), entity::setEnforceExercisePassing);
        patch.apply("showCourses", changes.getShowCourses(), entity::setShowCourses);
        patch.apply("showStudents", changes.getShowStudents(), entity::setShowStudents);
        patch.apply("showAssessments", changes.getShowAssessments(), entity::setShowAssessments);
        patch.apply("showLiveClass", changes.getShowLiveClass(), entity::setShowLiveClass);
        patch.apply("showDiscussions", changes.getShowDiscussions(), entity::setShowDiscussions);
        patch.apply("showEmails", changes.getShowEmails(), entity::setShowEmails);
        patch.apply("showDashboard", changes.getShowDashboard(), entity::setShowDashboard);
        patch.apply("showDayView", changes.getShowDayView(), entity::setShowDayView);
        patch.apply("textDirection", changes.getTextDirection(),
                                                v -> entity.setTextDirection(v == null ? null : v.getValue()));
        patch.apply("notifyPublishedCourses", changes.getNotifyPublishedCourses(),
                                                v -> entity.setNotifyPublishedCourses(v == null ? null : v.getValue()));
        patch.apply("notifyPublishedCohorts", changes.getNotifyPublishedCohorts(),
                                                v -> entity.setNotifyPublishedCohorts(v == null ? null : v.getValue()));
        patch.apply("sendPaymentRemindersCohort", changes.getSendPaymentRemindersCohort(),
                                                entity::setSendPaymentRemindersCohort);
        patch.apply("sendPaymentRemindersCourse", changes.getSendPaymentRemindersCourse(),
                                entity::setSendPaymentRemindersCourse);
        patch.apply("mentorRequestTemplateId", changes.getMentorRequestTemplateId(), entity::setMentorRequestTemplateId);
        patch.apply("mentorStatusTemplateId", changes.getMentorStatusTemplateId(), entity::setMentorStatusTemplateId);
        patch.apply("cohortConfirmationTemplateId", changes.getCohortConfirmationTemplateId(),
                                entity::setCohortConfirmationTemplateId);
        patch.apply("certificateTemplateId", changes.getCertificateTemplateId(), entity::setCertificateTemplateId);
        patch.apply("paymentReminderTemplateId", changes.getPaymentReminderTemplateId(), entity::setPaymentReminderTemplateId);
        patch.apply("customSignupContent", changes.getCustomSignupContent(), entity::setCustomSignupContent);
        patch.apply("metaDescription", changes.getMetaDescription(), entity::setMetaDescription);
        patch.apply("metaKeywords", changes.getMetaKeywords(), entity::setMetaKeywords);
        patch.apply("metaImageFileId", changes.getMetaImageFileId(), entity::setMetaImageFileId);
        patch.apply("contactUsEmail", changes.getContactUsEmail(), entity::setContactUsEmail);
        patch.apply("contactUsUrl", changes.getContactUsUrl(), entity::setContactUsUrl);
        patch.apply("demoDataPresent", changes.getDemoDataPresent(), entity::setDemoDataPresent);
        patch.apply("unsplashAccessKey", changes.getUnsplashAccessKey(), v -> entity.setUnsplashAccessKeyEnc(
                v == null ? null : cipher.encrypt(v, entity.getTenantId(), SECRET_UNSPLASH)));
        return patch.applied();
    }
}
