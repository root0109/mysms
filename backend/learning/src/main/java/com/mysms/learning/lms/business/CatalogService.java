package com.mysms.learning.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.mail.MailDelivery;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.learning.lms.api.model.CatalogHome;
import com.mysms.learning.lms.api.model.CatalogBanner;
import com.mysms.learning.lms.api.model.LmsCourse;
import com.mysms.learning.lms.api.model.PublicLmsSettings;
import com.mysms.learning.lms.api.model.SendContactMessageRequest;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.config.LmsMailConfig;
import com.mysms.learning.lms.errors.NotConfiguredException;
import com.mysms.learning.lms.integration.IamGrpcClient;
import com.mysms.learning.lms.mapper.CatalogBannerMapper;
import com.mysms.learning.lms.mapper.CategoryMapper;
import com.mysms.learning.lms.mapper.CohortMapper;
import com.mysms.learning.lms.mapper.LmsCourseMapper;
import com.mysms.learning.lms.mapper.SettingsSidebarItemMapper;
import com.mysms.learning.lms.model.CatalogPage;
import com.mysms.learning.lms.repository.CatalogBannerRepository;
import com.mysms.learning.lms.repository.CategoryRepository;
import com.mysms.learning.lms.repository.CohortRepository;
import com.mysms.learning.lms.repository.LmsCourseRepository;
import com.mysms.learning.lms.repository.SettingsSidebarItemRepository;
import com.mysms.learning.lms.repository.SettingsView;
import com.mysms.learning.lms.repository.SettingsViewRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The public catalogue: its home page, course comparison, the settings it renders with, and a visitor's message to the
 * school. Courses are always shown in the guest projection, whoever asks.
 */
@Service
public class CatalogService extends AppTransactionManager {

    static final String PERMISSION_COMPARE = "lms.course.compare_courses";
    static final String SETTINGS_AGGREGATE = "lms.settings";
    /** How many of each kind the catalogue home shows besides its page of courses. */
    static final int HOME_BANNERS = 20;
    static final int HOME_CATEGORIES = 100;
    static final int HOME_COHORTS = 20;
    /** A comparison is side by side: a handful of courses. */
    static final int MAX_COMPARED = 10;
    static final int MAX_QUERY = 100;
    static final int MAX_SUBJECT = 200;
    static final int MAX_CONTENT = 10_000;

    private final LmsCourseRepository courses;
    private final LmsCourseMapper courseMapper;
    private final CatalogBannerRepository banners;
    private final CatalogBannerMapper bannerMapper;
    private final CategoryRepository categories;
    private final CategoryMapper categoryMapper;
    private final CohortRepository cohorts;
    private final CohortMapper cohortMapper;
    private final SettingsViewRepository settings;
    private final SettingsSidebarItemRepository sidebar;
    private final SettingsSidebarItemMapper sidebarMapper;
    private final IamGrpcClient iam;
    private final MailDelivery mail;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final PublicReads publicReads;

    public CatalogService(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            LmsCourseRepository courses, LmsCourseMapper courseMapper,
            CatalogBannerRepository banners, CatalogBannerMapper bannerMapper,
            CategoryRepository categories, CategoryMapper categoryMapper,
            CohortRepository cohorts, CohortMapper cohortMapper,
            SettingsViewRepository settings, SettingsSidebarItemRepository sidebar,
            SettingsSidebarItemMapper sidebarMapper, IamGrpcClient iam,
            @Qualifier(LmsMailConfig.MAIL) MailDelivery mail,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, PublicReads publicReads) {
        super(database, transactions);
        this.publicReads = publicReads;
        this.courses = courses;
        this.courseMapper = courseMapper;
        this.banners = banners;
        this.bannerMapper = bannerMapper;
        this.categories = categories;
        this.categoryMapper = categoryMapper;
        this.cohorts = cohorts;
        this.cohortMapper = cohortMapper;
        this.settings = settings;
        this.sidebar = sidebar;
        this.sidebarMapper = sidebarMapper;
        this.iam = iam;
        this.mail = mail;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * lms.GetCatalogHome: the featured and showcase banners of the listed courses, a page of the courses listed in the
     * catalogue now (optionally of one category, or matching words in their title or introduction), the active
     * categories and the published cohorts open for self-enrolment that have not ended.
     */
    public CatalogPage getCatalogHome(UUID categoryId, String query, Integer pageNumber, Integer pageSize,
            String cursor) {
        publicReads.requireTenant();
        String words = query == null ? "" : query.trim();
        if (words.length() > MAX_QUERY) {
            throw new ValidationFailedException(List.of(new FieldError("query", "at most 100 characters", "Size")));
        }
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        LocalDate today = now.toLocalDate();
        SqlFragment where = LmsCourseRepository.inCatalog(now);
        if (categoryId != null) {
            where = where.and(LmsCourseRepository.ofCategory(categoryId));
        }
        if (!words.isEmpty()) {
            where = where.and(LmsCourseRepository.matching(words));
        }
        SqlFragment listed = where;
        SortSpec sort = SortSpec.parse(List.of("title"), LmsCourseRepository.SORT_FIELDS,
                LmsCourseRepository.DEFAULT_ORDER, LmsCourseRepository.ID_COLUMN);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, categoryId + "|" + words, clock);
        SortSpec categoryOrder = SortSpec.parse(List.of("sortOrder", "name"), CategoryRepository.SORT_FIELDS,
                CategoryRepository.DEFAULT_ORDER, CategoryRepository.ID_COLUMN);
        PageRequest categoryPage = PageRequest.of(1, HOME_CATEGORIES, null, categoryOrder, "catalog", clock);
        return doInReadTransaction(() -> {
            CatalogHome home = new CatalogHome()
                    .featured(banners.ofListedCourses("featured", now, HOME_BANNERS).stream()
                            .map(bannerMapper::toDto).map(CatalogService::withoutAuthors).toList())
                    .showcase(banners.ofListedCourses("showcase", now, HOME_BANNERS).stream()
                            .map(bannerMapper::toDto).map(CatalogService::withoutAuthors).toList())
                    .categories(categories.list(categoryPage, CategoryRepository.ACTIVE).items().stream()
                            .map(categoryMapper::toDto).map(CategoryService::guestProjection).toList())
                    .openCohorts(cohorts.openForEnrollment(today, categoryId, HOME_COHORTS).stream()
                            .map(cohortMapper::toDto).map(CohortService::guestProjection).toList());
            Page<LmsCourse> listedCourses = courses.list(page, listed).map(courseMapper::toDto)
                    .map(LmsCourseService::guestProjection);
            return new CatalogPage(home, listedCourses);
        });
    }

    /**
     * lms.CompareCourses: the published courses among the ids (at most ten), side by side in the guest projection;
     * ids the public may not see are left out, as for the public list.
     */
    public Page<LmsCourse> compareCourses(List<UUID> courseIds, Integer pageNumber, Integer pageSize, String cursor) {
        publicReads.requireTenant();
        if (courseIds.isEmpty()) {
            throw new InvalidArgumentException("courseIds: name at least one course.");
        }
        if (courseIds.size() > MAX_COMPARED) {
            throw new ValidationFailedException(List.of(new FieldError("courseIds", "name 1 to 10 courses", "Size")));
        }
        SortSpec sort = SortSpec.parse(List.of("title"), LmsCourseRepository.SORT_FIELDS,
                LmsCourseRepository.DEFAULT_ORDER, LmsCourseRepository.ID_COLUMN);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, courseIds.toString(), clock);
        SqlFragment where = LmsCourseRepository.among(courseIds).and(LmsCourseRepository.PUBLISHED);
        return doInReadTransaction(() -> courses.list(page, where).map(courseMapper::toDto)
                .map(LmsCourseService::guestProjection));
    }

    /**
     * lms.GetPublicSettings: what the public catalogue renders with, or the column defaults when the school saved no
     * settings. A school that keeps guests out shows them only what its sign-in page needs. The default currency's
     * code lives in master and is not filled.
     */
    public PublicLmsSettings getPublicSettings() {
        boolean guest = PublicReads.requireSchoolNamed();
        return doInReadTransaction(() -> {
            SettingsView view = settings.find();
            PublicLmsSettings out = new PublicLmsSettings(view.allowGuestAccess(), view.disableSignup(),
                    view.showCourses(), view.showLiveClass(), view.textDirection());
            out.setCustomSignupContent(view.customSignupContent());
            if (guest && !view.allowGuestAccess()) {
                return out;
            }
            out.setContactUsEmail(view.contactUsEmail());
            out.setContactUsUrl(view.contactUsUrl());
            out.setMetaDescription(view.metaDescription());
            out.setMetaKeywords(view.metaKeywords());
            out.setSidebarItems(view.id() == null ? List.of()
                    : sidebar.visible(view.id()).stream().map(sidebarMapper::toDto).toList());
            return out;
        });
    }

    /**
     * lms.SendContactMessage: a signed-in user writes to the school's LMS contact address (lms.settings
     * contact_us_email). 503 NOT_CONFIGURED when the school set no address or the deployment no SMTP; the caller's own
     * address (iam) is the reply-to. The message itself is not kept; an audit record notes that one was sent.
     */
    public void sendContactMessage(SendContactMessageRequest request) {
        validator.validate(request);
        List<FieldError> errors = new ArrayList<>();
        if (request.getSubject().isBlank() || request.getSubject().length() > MAX_SUBJECT
                || request.getSubject().contains("\n") || request.getSubject().contains("\r")) {
            errors.add(new FieldError("subject", "must be one line of 1 to 200 characters", "Size"));
        }
        if (request.getContent().isBlank() || request.getContent().length() > MAX_CONTENT) {
            errors.add(new FieldError("content", "must be 1 to 10000 characters", "Size"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        mail.requireConfigured();
        UUID user = AuthContext.require().userId();
        SettingsView view = doInReadTransaction(settings::find);
        if (view.contactUsEmail() == null || view.contactUsEmail().isBlank()) {
            throw new NotConfiguredException("The school has not set an LMS contact address.");
        }
        Optional<String> replyTo = iam.callerEmail();
        mail.send(new MailDelivery.Message(view.contactUsEmail(), request.getSubject().trim(),
                request.getContent() + "\n\n-- \nSent from the LMS by user " + user + ".", replyTo.orElse(null)));
        UUID settingsId = view.id();
        runInWriteTransaction(() -> audit.recordTransition(SETTINGS_AGGREGATE, settingsId, "send_contact_message",
                List.of(), null));
    }

    /** The public's view of a banner: who created and changed it is internal. */
    private static CatalogBanner withoutAuthors(CatalogBanner banner) {
        banner.setCreatedBy(null);
        banner.setUpdatedBy(null);
        return banner;
    }
}
