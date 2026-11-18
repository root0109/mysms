package com.mysms.lms.business;

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
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.api.model.SettingsPrimaryCountry;
import com.mysms.lms.api.model.SettingsPrimaryCountryUpdate;
import com.mysms.lms.api.model.SettingsSidebarItem;
import com.mysms.lms.api.model.SettingsSidebarItemUpdate;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.SettingsPrimaryCountryEntity;
import com.mysms.lms.domain.SettingsSidebarItemEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.SettingsPrimaryCountryMapper;
import com.mysms.lms.mapper.SettingsSidebarItemMapper;
import com.mysms.lms.repository.SettingsPrimaryCountryRepository;
import com.mysms.lms.repository.SettingsSidebarItemRepository;
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
import com.mysms.lms.api.model.Settings;
import com.mysms.lms.api.model.SettingsUpdate;
import com.mysms.lms.domain.SettingsEntity;
import com.mysms.lms.mapper.SettingsMapper;
import com.mysms.lms.repository.SettingsRepository;

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
    /** The contract's SettingsPrimaryCountryUpdate fields, in apply order. */
    static final List<String> SETTINGS_PRIMARY_COUNTRY_WRITABLE = List.of("countryId");

    public static final String SETTINGS_SIDEBAR_ITEM_AGGREGATE = SettingsSidebarItemEntity.TABLE;
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_LIST = "lms.settings_sidebar_item.read";
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_GET = "lms.settings_sidebar_item.read";
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_CREATE = "lms.settings_sidebar_item.create";
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_UPDATE = "lms.settings_sidebar_item.update";
    static final String SETTINGS_SIDEBAR_ITEM_PERMISSION_DELETE = "lms.settings_sidebar_item.delete";
    /** The contract's SettingsSidebarItemUpdate fields, in apply order. */
    static final List<String> SETTINGS_SIDEBAR_ITEM_WRITABLE =
            List.of("sortOrder", "itemType", "itemKey", "title", "icon", "webPageRoute", "route", "url", "isStandard",
            "isHidden", "openInNewWindow");

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
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public SettingsPrimaryCountry updateSettingsPrimaryCountry(UUID settingsId, UUID id, String ifMatch,
            SettingsPrimaryCountryUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, SETTINGS_PRIMARY_COUNTRY_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SETTINGS_PRIMARY_COUNTRY_PERMISSION_UPDATE);
            SettingsPrimaryCountryEntity entity = loadVisibleSettingsPrimaryCountry(scope, settingsId, id);
            scope.requireWritable(institutionOfSettingsPrimaryCountry(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applySettingsPrimaryCountry(entity, changes, fields);
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

    /** Applies the selected SettingsPrimaryCountryUpdate fields; explicit per field so a mask can clear values. */
    private static void applySettingsPrimaryCountry(SettingsPrimaryCountryEntity entity, SettingsPrimaryCountryUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "countryId" -> entity.setCountryId(changes.getCountryId());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public SettingsSidebarItem updateSettingsSidebarItem(UUID settingsId, UUID id, String ifMatch,
            SettingsSidebarItemUpdate changes, String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, SETTINGS_SIDEBAR_ITEM_WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(SETTINGS_SIDEBAR_ITEM_PERMISSION_UPDATE);
            SettingsSidebarItemEntity entity = loadVisibleSettingsSidebarItem(scope, settingsId, id);
            scope.requireWritable(institutionOfSettingsSidebarItem(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applySettingsSidebarItem(entity, changes, fields);
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

    /** Applies the selected SettingsSidebarItemUpdate fields; explicit per field so a mask can clear values. */
    private static void applySettingsSidebarItem(SettingsSidebarItemEntity entity, SettingsSidebarItemUpdate changes,
            List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "sortOrder" -> entity.setSortOrder(changes.getSortOrder());
                case "itemType" -> entity.setItemType(Optional.ofNullable(changes.getItemType()).map(v -> v.getValue())
                        .orElse(null));
                case "itemKey" -> entity.setItemKey(changes.getItemKey());
                case "title" -> entity.setTitle(changes.getTitle());
                case "icon" -> entity.setIcon(changes.getIcon());
                case "webPageRoute" -> entity.setWebPageRoute(changes.getWebPageRoute());
                case "route" -> entity.setRoute(changes.getRoute());
                case "url" -> entity.setUrl(changes.getUrl());
                case "isStandard" -> entity.setIsStandard(changes.getIsStandard());
                case "isHidden" -> entity.setIsHidden(changes.getIsHidden());
                case "openInNewWindow" -> entity.setOpenInNewWindow(changes.getOpenInNewWindow());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
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
    /** The contract's SettingsUpdate fields, in apply order. */
    static final List<String> SETTINGS_WRITABLE =
            List.of("livecodeUrl", "defaultCurrencyId", "applyGst", "showUsdEquivalent", "applyRounding",
            "paymentGateway", "identifyUserCategory", "personaCaptured", "lmsIsDefaultHome",
            "sendCalendarInviteForEvaluations", "allowGuestAccess", "disableSignup", "disablePwa", "allowJobPosting",
            "certifiedMembersPage", "preventSkippingVideos", "lessonDwellSeconds", "enforceVideoCompletion",
            "enforceQuizCompletion", "enforceQuizPassing", "enforceOpenEndedQuizPassing",
            "enforceAssignmentCompletion", "enforceAssignmentPassing", "enforceExerciseCompletion",
            "enforceExercisePassing", "showCourses", "showStudents", "showAssessments", "showLiveClass",
            "showDiscussions", "showEmails", "showDashboard", "showDayView", "textDirection",
            "notifyPublishedCourses", "notifyPublishedCohorts", "sendPaymentRemindersCohort",
            "sendPaymentRemindersCourse", "mentorRequestTemplateId", "mentorStatusTemplateId",
            "cohortConfirmationTemplateId", "certificateTemplateId", "paymentReminderTemplateId",
            "customSignupContent", "metaDescription", "metaKeywords", "metaImageFileId", "contactUsEmail",
            "contactUsUrl", "unsplashAccessKey", "demoDataPresent");

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
     * only its mask is returned; leaving it out keeps the stored one, naming it in the mask with no value clears it.
     * Tenant-wide grant only (403).
     */
    public Settings updateSettings(String ifMatch, SettingsUpdate changes, String updateMask, boolean validateOnly) {
        requireTenantWide(SETTINGS_PERMISSION_UPDATE);
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, SETTINGS_WRITABLE);
        if (changes.getUnsplashAccessKey() != null) {
            cipher.requireConfigured();
        }
        return doInWriteTransaction(() -> {
            Optional<SettingsEntity> stored = settingsRepository.find();
            SettingsEntity entity = stored.orElseGet(this::defaults);
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            applySettings(entity, changes, fields);
            if (changes.getUnsplashAccessKey() != null) {
                entity.setUnsplashAccessKeyEnc(cipher.encrypt(changes.getUnsplashAccessKey(), entity.getTenantId(),
                        SECRET_UNSPLASH));
            }
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

    private static void applySettings(SettingsEntity entity, SettingsUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "livecodeUrl" -> entity.setLivecodeUrl(changes.getLivecodeUrl());
                case "defaultCurrencyId" -> entity.setDefaultCurrencyId(changes.getDefaultCurrencyId());
                case "applyGst" -> entity.setApplyGst(changes.getApplyGst());
                case "showUsdEquivalent" -> entity.setShowUsdEquivalent(changes.getShowUsdEquivalent());
                case "applyRounding" -> entity.setApplyRounding(changes.getApplyRounding());
                case "paymentGateway" -> entity.setPaymentGateway(changes.getPaymentGateway());
                case "identifyUserCategory" -> entity.setIdentifyUserCategory(changes.getIdentifyUserCategory());
                case "personaCaptured" -> entity.setPersonaCaptured(changes.getPersonaCaptured());
                case "lmsIsDefaultHome" -> entity.setLmsIsDefaultHome(changes.getLmsIsDefaultHome());
                case "sendCalendarInviteForEvaluations" ->
                        entity.setSendCalendarInviteForEvaluations(changes.getSendCalendarInviteForEvaluations());
                case "allowGuestAccess" -> entity.setAllowGuestAccess(changes.getAllowGuestAccess());
                case "disableSignup" -> entity.setDisableSignup(changes.getDisableSignup());
                case "disablePwa" -> entity.setDisablePwa(changes.getDisablePwa());
                case "allowJobPosting" -> entity.setAllowJobPosting(changes.getAllowJobPosting());
                case "certifiedMembersPage" -> entity.setCertifiedMembersPage(changes.getCertifiedMembersPage());
                case "preventSkippingVideos" -> entity.setPreventSkippingVideos(changes.getPreventSkippingVideos());
                case "lessonDwellSeconds" -> entity.setLessonDwellSeconds(changes.getLessonDwellSeconds());
                case "enforceVideoCompletion" -> entity.setEnforceVideoCompletion(changes.getEnforceVideoCompletion());
                case "enforceQuizCompletion" -> entity.setEnforceQuizCompletion(changes.getEnforceQuizCompletion());
                case "enforceQuizPassing" -> entity.setEnforceQuizPassing(changes.getEnforceQuizPassing());
                case "enforceOpenEndedQuizPassing" ->
                        entity.setEnforceOpenEndedQuizPassing(changes.getEnforceOpenEndedQuizPassing());
                case "enforceAssignmentCompletion" ->
                        entity.setEnforceAssignmentCompletion(changes.getEnforceAssignmentCompletion());
                case "enforceAssignmentPassing" -> entity.setEnforceAssignmentPassing(changes.getEnforceAssignmentPassing());
                case "enforceExerciseCompletion" -> entity.setEnforceExerciseCompletion(changes.getEnforceExerciseCompletion());
                case "enforceExercisePassing" -> entity.setEnforceExercisePassing(changes.getEnforceExercisePassing());
                case "showCourses" -> entity.setShowCourses(changes.getShowCourses());
                case "showStudents" -> entity.setShowStudents(changes.getShowStudents());
                case "showAssessments" -> entity.setShowAssessments(changes.getShowAssessments());
                case "showLiveClass" -> entity.setShowLiveClass(changes.getShowLiveClass());
                case "showDiscussions" -> entity.setShowDiscussions(changes.getShowDiscussions());
                case "showEmails" -> entity.setShowEmails(changes.getShowEmails());
                case "showDashboard" -> entity.setShowDashboard(changes.getShowDashboard());
                case "showDayView" -> entity.setShowDayView(changes.getShowDayView());
                case "textDirection" -> {
                    var direction = changes.getTextDirection();
                    entity.setTextDirection(direction == null ? null : direction.getValue());
                }
                case "notifyPublishedCourses" -> {
                    var notify = changes.getNotifyPublishedCourses();
                    entity.setNotifyPublishedCourses(notify == null ? null : notify.getValue());
                }
                case "notifyPublishedCohorts" -> {
                    var notify = changes.getNotifyPublishedCohorts();
                    entity.setNotifyPublishedCohorts(notify == null ? null : notify.getValue());
                }
                case "sendPaymentRemindersCohort" ->
                        entity.setSendPaymentRemindersCohort(changes.getSendPaymentRemindersCohort());
                case "sendPaymentRemindersCourse" ->
                        entity.setSendPaymentRemindersCourse(changes.getSendPaymentRemindersCourse());
                case "mentorRequestTemplateId" -> entity.setMentorRequestTemplateId(changes.getMentorRequestTemplateId());
                case "mentorStatusTemplateId" -> entity.setMentorStatusTemplateId(changes.getMentorStatusTemplateId());
                case "cohortConfirmationTemplateId" ->
                        entity.setCohortConfirmationTemplateId(changes.getCohortConfirmationTemplateId());
                case "certificateTemplateId" -> entity.setCertificateTemplateId(changes.getCertificateTemplateId());
                case "paymentReminderTemplateId" -> entity.setPaymentReminderTemplateId(changes.getPaymentReminderTemplateId());
                case "customSignupContent" -> entity.setCustomSignupContent(changes.getCustomSignupContent());
                case "metaDescription" -> entity.setMetaDescription(changes.getMetaDescription());
                case "metaKeywords" -> entity.setMetaKeywords(changes.getMetaKeywords());
                case "metaImageFileId" -> entity.setMetaImageFileId(changes.getMetaImageFileId());
                case "contactUsEmail" -> entity.setContactUsEmail(changes.getContactUsEmail());
                case "contactUsUrl" -> entity.setContactUsUrl(changes.getContactUsUrl());
                case "demoDataPresent" -> entity.setDemoDataPresent(changes.getDemoDataPresent());
                case "unsplashAccessKey" -> {
                    if (changes.getUnsplashAccessKey() == null) {
                        entity.setUnsplashAccessKeyEnc(null);
                    }
                }
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }
}
