package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.lms.api.model.CohortEnrollment;
import com.mysms.lms.api.model.CohortEnrollmentUpdate;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.filter.FilterParser;
import com.mysms.core.mapping.WireValues;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.core.paging.SortSpec;
import com.mysms.core.paging.SqlFragment;
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.CohortEnrollmentEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.CohortEnrollmentMapper;
import com.mysms.lms.repository.CohortEnrollmentRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import com.mysms.authclient.AuthContext;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.id.UuidV7;
import com.mysms.lms.api.model.BatchEnrollPreview;
import com.mysms.lms.api.model.BatchEnrollPreviewList;
import com.mysms.lms.api.model.MemberAssignment;
import com.mysms.lms.api.model.RosterEntry;
import com.mysms.lms.api.model.RosterEntryList;
import com.mysms.lms.api.model.SendResult;
import com.mysms.lms.api.model.Operation;
import com.mysms.lms.api.model.ImportCohortMembersRequest;
import com.mysms.lms.api.model.SendCohortAnnouncementRequest;
import com.mysms.lms.domain.OperationEntity;
import com.mysms.lms.mapper.OperationMapper;
import com.mysms.lms.repository.OperationRepository;
import com.mysms.lms.domain.CohortEntity;
import com.mysms.lms.domain.CohortTeacherEntity;
import com.mysms.lms.model.Placement;
import com.mysms.lms.model.RosterRow;
import com.mysms.lms.repository.CohortMembershipRepository;
import com.mysms.lms.repository.CohortRepository;
import com.mysms.lms.repository.CohortTeacherRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.lang.Nullable;

/**
 * Business service of lms.cohort_enrollment: audit and domain event commit in the same transaction as every change.
 * Rows outside the caller's institutions are invisible; tenant-level rows change only through a tenant-wide grant.
 */
@Service
public class CohortEnrollmentService extends AppTransactionManager {

    public static final String AGGREGATE = CohortEnrollmentEntity.TABLE;
    static final String PERMISSION_LIST = "lms.cohort_enrollment.read";
    static final String PERMISSION_GET = "lms.cohort_enrollment.read";
    static final String PERMISSION_UPDATE = "lms.cohort_enrollment.update";
    static final String PERMISSION_DELETE = "lms.cohort_enrollment.delete";
    /** The contract's CohortEnrollmentUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("isActive", "hasAccommodation", "accommodationRef", "surveyStatus");


    private final CohortEnrollmentRepository repository;
    private final CohortEnrollmentMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final CohortRepository cohorts;
    private final CohortMembershipRepository members;
    private final CohortTeacherRepository teachers;
    private final CohortAccess access;
    private final MemberDirectory directory;
    private final OperationRepository operations;
    private final OperationMapper operationMapper;
    private final PlatformFiles files;

    public CohortEnrollmentService(CohortEnrollmentRepository repository, CohortEnrollmentMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator, Clock clock, CohortRepository cohorts, CohortMembershipRepository members,
            CohortTeacherRepository teachers, CohortAccess access, MemberDirectory directory,
            OperationRepository operations, OperationMapper operationMapper, PlatformFiles files) {

        super(database, transactions);
        this.operations = operations;
        this.operationMapper = operationMapper;
        this.files = files;
        this.cohorts = cohorts;
        this.members = members;
        this.teachers = teachers;
        this.access = access;
        this.directory = directory;
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public CohortEnrollment getCohortEnrollment(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<CohortEnrollment> listCohortEnrollments(Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, CohortEnrollmentRepository.SORT_FIELDS,
                        CohortEnrollmentRepository.DEFAULT_ORDER,
                CohortEnrollmentRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, CohortEnrollmentRepository.FILTER_FIELDS)
                .and(CohortEnrollmentRepository.scope(DataScope.current(PERMISSION_LIST)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<CohortEnrollmentEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * Partial update under If-Match: an updateMask sets the listed fields (clearing absent ones), otherwise the
     * non-null fields apply; the merged row must validate and stay in the caller's scope.
     */
    public CohortEnrollment updateCohortEnrollment(UUID id, String ifMatch, CohortEnrollmentUpdate changes,
            String updateMask, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            CohortEnrollmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            apply(entity, changes, fields);
            validator.validate(mapper.toDto(entity));
            assertValid(entity, false);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteCohortEnrollment(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            CohortEnrollmentEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.DELETED, AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private CohortEnrollmentEntity loadVisible(DataScope scope, UUID id) {
        CohortEnrollmentEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** The institution of the lms.cohort the row belongs to. */
    private Optional<UUID> institutionOf(CohortEnrollmentEntity entity) {
        return repository.institutionOf(entity.getCohortId());
    }

    /** Applies the selected CohortEnrollmentUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(CohortEnrollmentEntity entity, CohortEnrollmentUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "isActive" -> entity.setIsActive(changes.getIsActive());
                case "hasAccommodation" -> entity.setHasAccommodation(changes.getHasAccommodation());
                case "accommodationRef" -> entity.setAccommodationRef(changes.getAccommodationRef());
                case "surveyStatus" -> entity.setSurveyStatus(changes.getSurveyStatus());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(CohortEnrollmentEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getCohortId() == null) {
            errors.add(new FieldError("cohortId", "must not be null", "NotNull"));
        }
        if (entity.getUserId() == null) {
            errors.add(new FieldError("userId", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsActive() == null) {
            errors.add(new FieldError("isActive", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("finalGpa", WireValues.decimal(entity.getFinalGpa()), 4, 2, errors);
        if (!creating && entity.getHasAccommodation() == null) {
            errors.add(new FieldError("hasAccommodation", "must not be null", "NotNull"));
        }
        if (entity.getCohortId() != null && !repository.existsCohortId(entity.getCohortId())) {
            errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(CohortEnrollmentEntity entity) {
        if (repository.takenCohortIdUserId1(entity)) {
            throw new DuplicateResourceException(
                    "A cohort enrollment with the same cohort id and user id already exists.");
        }
    }

    // ---- the cohort's membership: self-enrolment, enrolment by staff, setting members, the roster ----

    static final String PERMISSION_ENROLL = "lms.cohort.enroll_in_cohort";
    static final String PERMISSION_BATCH_ENROLL = "lms.cohort.batch_enroll_in_cohort";
    static final String PERMISSION_SET_MEMBERS = "lms.cohort.set_cohort_members";
    static final String PERMISSION_ROSTER = "lms.cohort.get_cohort_roster";
    static final String PERMISSION_ANNOUNCE = "lms.cohort.send_cohort_announcement";
    static final String PERMISSION_IMPORT = "lms.cohort.import_cohort_members";
    /** The event comms delivers a cohort announcement from. */
    static final String ANNOUNCEMENT_REQUESTED = CohortAccess.COHORT + ".announcement_requested";
    static final String OPERATION_IMPORT_MEMBERS = "lms.ImportCohortMembers";
    static final long MAX_IMPORT_BYTES = 5 * 1024 * 1024;
    static final int MAX_IMPORT_ROWS = 2000;
    private static final String TEACHER_AGGREGATE = "lms.cohort_teacher";
    private static final String CHANGED = "A member changed meanwhile; try again.";

    /**
     * lms.EnrollInCohort: the caller enrols themself. The cohort must be published and open (not suspended, completed
     * or archived), take self-enrolment, be inside its enrolment window and have a seat left (409 with the reason's
     * code); a paid cohort is joined through its payment, not here (409 PAYMENT_REQUIRED). Seats are counted with the
     * cohort row locked, so two last-seat enrolments cannot both pass. A student who left comes back on their row.
     */
    public CohortEnrollment enrollInCohort(UUID cohortId, UUID paymentId) {
        AuthContext caller = CohortAccess.member();
        return doInWriteTransaction(() -> {
            CohortEntity cohort = access.visible(cohortId, PERMISSION_ENROLL);
            cohort = cohorts.findForUpdate(cohortId).orElseThrow();
            OffsetDateTime now = now();
            if (!Boolean.TRUE.equals(cohort.getIsPublished())
                    || !List.of("draft", "active").contains(cohort.getLifecycleStatus())) {
                throw new ConflictException("COHORT_NOT_OPEN", "The cohort is not open for enrolment.");
            }
            if (!Boolean.TRUE.equals(cohort.getAllowSelfEnrollment())) {
                throw new ConflictException("SELF_ENROLLMENT_CLOSED", "The cohort does not take self-enrolment.");
            }
            if (cohort.getEnrollmentOpensAt() != null && now.isBefore(cohort.getEnrollmentOpensAt())
                    || cohort.getEnrollmentClosesAt() != null && now.isAfter(cohort.getEnrollmentClosesAt())) {
                throw new ConflictException("ENROLLMENT_CLOSED", "Enrolment is outside the cohort's enrolment window.");
            }
            if (Boolean.TRUE.equals(cohort.getIsPaid())) {
                throw new ConflictException("PAYMENT_REQUIRED", "A paid cohort is joined through its payment.");
            }
            Optional<Placement> placed = members.enrollmentOf(cohortId, caller.userId());
            if (placed.isPresent() && placed.get().isActive()) {
                throw new ConflictException("ALREADY_ENROLLED", "The caller is already enrolled in the cohort.");
            }
            requireSeats(cohort, 1);
            UUID id = enrol(cohort, caller.userId(), paymentId, true, placed, now);
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    /**
     * lms.BatchEnrollInCohort: staff enrol many students at once, or preview it. Each student is reported as already
     * enrolled or not, and whether the cohort asks a payment no payment id was given for (paymentIds, when sent, pair
     * with userIds one to one — 422 otherwise). Enrolling is all or nothing: the new students must fit the free seats
     * (409 COHORT_FULL), and an archived cohort takes nobody (409).
     */
    public BatchEnrollPreviewList batchEnrollInCohort(UUID cohortId, List<UUID> userIds, List<UUID> paymentIds,
            boolean validateOnly) {
        List<UUID> payments = paymentIds == null ? List.of() : paymentIds;
        if (!payments.isEmpty() && payments.size() != userIds.size()) {
            throw new ValidationFailedException(List.of(new FieldError("paymentIds",
                    "must pair with userIds one to one", "SIZE_MISMATCH")));
        }
        DataScope scope = DataScope.current(PERMISSION_BATCH_ENROLL);
        return doInWriteTransaction(() -> {
            CohortEntity cohort = writableCohort(scope, cohortId);
            if (userIds == null || userIds.isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("userIds", "must not be empty",
                        "NotEmpty")));
            }
            Map<UUID, UUID> paymentOf = new LinkedHashMap<>();
            for (int i = 0; i < userIds.size(); i++) {
                paymentOf.putIfAbsent(userIds.get(i), payments.isEmpty() ? null : payments.get(i));
            }
            List<BatchEnrollPreview> preview = new ArrayList<>();
            Map<UUID, Optional<Placement>> placements = new LinkedHashMap<>();
            for (Map.Entry<UUID, UUID> student : paymentOf.entrySet()) {
                Optional<Placement> placed = members.enrollmentOf(cohortId, student.getKey());
                placements.put(student.getKey(), placed);
                preview.add(new BatchEnrollPreview(student.getKey(), placed.map(Placement::isActive).orElse(false),
                        Boolean.TRUE.equals(cohort.getIsPaid()) && student.getValue() == null));
            }
            if (validateOnly) {
                return new BatchEnrollPreviewList(preview);
            }
            List<UUID> newcomers = preview.stream().filter(item -> !item.getAlreadyEnrolled())
                    .map(BatchEnrollPreview::getUserId).toList();
            requireSeats(cohort, newcomers.size());
            OffsetDateTime now = now();
            for (UUID student : newcomers) {
                enrol(cohort, student, paymentOf.get(student), false, placements.get(student), now);
            }
            return new BatchEnrollPreviewList(preview);
        });
    }

    /**
     * lms.SetCohortMembers: adds or updates members and removes others in one call. A member given a role (roleId or
     * roleLabel) is placed as staff, any other as a student; students added must fit the free seats (409
     * COHORT_FULL). Removed members keep their rows, inactive. A user both set and removed is 422. Answers the
     * roster entries of everyone the call touched, named through iam after the change is committed.
     */
    public RosterEntryList setCohortMembers(UUID cohortId, @Nullable List<MemberAssignment> given,
            @Nullable List<UUID> removeUserIds) {
        List<MemberAssignment> assignments = given == null ? List.of() : given;
        List<UUID> removals = removeUserIds == null ? List.of() : removeUserIds.stream().distinct().toList();
        Set<UUID> assigned = new LinkedHashSet<>();
        assignments.forEach(member -> assigned.add(member.getUserId()));
        if (assigned.size() != assignments.size() || removals.stream().anyMatch(assigned::contains)) {
            throw new ValidationFailedException(List.of(new FieldError("members",
                    "each user may be set once and not also removed", "DUPLICATE_MEMBER")));
        }
        DataScope scope = DataScope.current(PERMISSION_SET_MEMBERS);
        List<RosterRow> touched = doInWriteTransaction(() -> {
            CohortEntity cohort = writableCohort(scope, cohortId);
            if (assignments.isEmpty() && removals.isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("members",
                        "must name at least one member or removal", "NotEmpty")));
            }
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            List<MemberAssignment> students = assignments.stream()
                    .filter(member -> member.getRoleId() == null && member.getRoleLabel() == null).toList();
            long newcomers = students.stream().filter(member -> members.enrollmentOf(cohortId, member.getUserId())
                    .map(row -> !row.isActive()).orElse(true)).count();
            requireSeats(cohort, (int) newcomers);
            List<RosterRow> rows = new ArrayList<>();
            for (MemberAssignment member : assignments) {
                if (students.contains(member)) {
                    Optional<Placement> placed = members.enrollmentOf(cohortId, member.getUserId());
                    UUID id = placed.filter(Placement::isActive).map(Placement::id)
                            .orElseGet(() -> enrol(cohort, member.getUserId(), null, false, placed, now));
                    rows.add(new RosterRow(id, member.getUserId(), RosterRow.STUDENT, true, now));
                } else {
                    rows.add(new RosterRow(placeTeacher(cohort, member, now, actor), member.getUserId(),
                            RosterRow.TEACHER, true, now));
                }
            }
            for (UUID user : removals) {
                rows.addAll(remove(cohortId, user, now, actor));
            }
            return rows;
        });
        return new RosterEntryList(entries(touched, "Setting the cohort's members"));
    }

    /**
     * lms.GetCohortRoster: the cohort's students and staff (one role when roleCode is given; inactive members when
     * asked), for its teaching staff only (403 for anybody else who sees the cohort). Names come from iam.
     */
    public Page<RosterEntry> getCohortRoster(UUID cohortId, String roleCode, Boolean includeInactive,
            Integer pageNumber, Integer pageSize, String cursor) {
        if (roleCode != null && !List.of(RosterRow.STUDENT, RosterRow.TEACHER).contains(roleCode)) {
            throw new InvalidArgumentException("roleCode must be student or teacher.");
        }
        boolean inactive = Boolean.TRUE.equals(includeInactive);
        SortSpec sort = SortSpec.parse(null, CohortMembershipRepository.ROSTER_SORT, "createTime", "r.id");
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort,
                cohortId + "|" + roleCode + "|" + inactive, clock);
        Page<RosterRow> rows = doInReadTransaction(() -> {
            access.taught(cohortId, PERMISSION_ROSTER);
            return members.roster(page, cohortId, roleCode, inactive);
        });
        List<RosterEntry> entries = entries(rows.items(), "Reading the roster");
        return new Page<>(entries, rows.page(), rows.size(), rows.nextCursor());
    }

    /**
     * lms.SendCohortAnnouncement: the cohort's teaching staff e-mail every active student. Delivery is comms' work:
     * one announcement event carries the recipients, and the answer counts them.
     */
    public SendResult sendCohortAnnouncement(UUID cohortId, SendCohortAnnouncementRequest request) {
        validator.validate(request);
        return doInWriteTransaction(() -> {
            CohortEntity cohort = access.taught(cohortId, PERMISSION_ANNOUNCE);
            if ("archived".equals(cohort.getLifecycleStatus())) {
                throw new InvalidStateException("An archived cohort is read-only.");
            }
            List<UUID> students = members.activeStudentIds(cohortId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("cohortId", cohortId.toString());
            payload.put("subject", request.getSubject());
            payload.put("content", request.getContent());
            if (request.getReplyTo() != null) {
                payload.put("replyTo", request.getReplyTo());
            }
            payload.put("userIds", students.stream().map(UUID::toString).toList());
            audit.recordTransition(CohortAccess.COHORT, cohortId, "send_announcement", List.of(), null);
            outbox.publish(ANNOUNCEMENT_REQUESTED, CohortAccess.COHORT, cohortId, payload);
            return new SendResult(students.size(), 0);
        });
    }

    /**
     * lms.ImportCohortMembers: bulk-add members from a CSV file stored at platform (columns user_id or email, and
     * role_id / role_label for staff rows). Rows that cannot be read are reported, the rest are placed like
     * SetCohortMembers places them; with validateOnly nothing is written. The answer is a finished operation whose
     * result counts the rows and lists every row error.
     */
    public Operation importCohortMembers(UUID cohortId, ImportCohortMembersRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_IMPORT);
        doInReadTransaction(() -> {
            CohortEntity cohort = access.visible(cohortId, PERMISSION_IMPORT);
            scope.requireWritable(CohortAccess.institutionOf(cohort));
            if ("archived".equals(cohort.getLifecycleStatus())) {
                throw new InvalidStateException("An archived cohort is read-only.");
            }
            return cohort;
        });
        boolean staffKind = "staff".equals(request.getMemberKind().getValue());
        boolean anyKind = "any".equals(request.getMemberKind().getValue());
        List<List<String>> rows = Csv.read(
                files.read(request.getFileId(), "fileId", List.of(".csv"), MAX_IMPORT_BYTES).content());
        if (rows.size() > MAX_IMPORT_ROWS + 1) {
            throw new ValidationFailedException(List.of(new FieldError("fileId",
                    "at most " + MAX_IMPORT_ROWS + " rows; split the file", "TOO_MANY_ROWS")));
        }
        Columns columns = Columns.of(rows);
        Map<String, UUID> byEmail = columns.email() < 0 ? Map.of()
                : directory.byEmail(rows.stream().skip(1).map(row -> columns.cell(row, columns.email())).toList(),
                        "Importing cohort members");
        List<Map<String, Object>> rowErrors = new ArrayList<>();
        List<MemberAssignment> assignments = new ArrayList<>();
        List<Boolean> asStaff = new ArrayList<>();
        Set<UUID> seen = new LinkedHashSet<>();
        for (int at = 1; at < rows.size(); at++) {
            List<String> row = rows.get(at);
            String fault = null;
            UUID userId = null;
            String id = columns.cell(row, columns.userId());
            String email = columns.cell(row, columns.email());
            if (id != null) {
                try {
                    userId = UUID.fromString(id);
                } catch (IllegalArgumentException invalid) {
                    fault = "user_id is not a UUID";
                }
            } else if (email != null) {
                userId = byEmail.get(email.toLowerCase(java.util.Locale.ROOT));
                fault = userId == null ? "no account has this e-mail" : null;
            } else {
                fault = "user_id or email is required";
            }
            if (fault == null && !seen.add(userId)) {
                fault = "the user appears twice";
            }
            String roleId = columns.cell(row, columns.roleId());
            String roleLabel = columns.cell(row, columns.roleLabel());
            boolean staffRow = staffKind || anyKind && (roleId != null || roleLabel != null);
            if (fault == null && roleId != null) {
                try {
                    UUID.fromString(roleId);
                } catch (IllegalArgumentException invalid) {
                    fault = "role_id is not a UUID";
                }
            }
            if (fault != null) {
                rowErrors.add(Map.of("row", at + 1, "message", fault));
                continue;
            }
            MemberAssignment member = new MemberAssignment(userId);
            if (staffRow) {
                member.setRoleId(roleId == null ? null : UUID.fromString(roleId));
                member.setRoleLabel(roleLabel == null && roleId == null ? "teacher" : roleLabel);
            }
            assignments.add(member);
            asStaff.add(staffRow);
        }
        boolean dryRun = Boolean.TRUE.equals(request.getValidateOnly());
        int placed = dryRun ? 0 : assignments.size();
        UUID tenant = com.mysms.core.context.CallContextHolder.require().tenantId().orElseThrow();
        return doInWriteTransaction(() -> {
            CohortEntity cohort = writableCohort(scope, cohortId);
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            if (!dryRun) {
                long newcomers = 0;
                for (int at = 0; at < assignments.size(); at++) {
                    if (!asStaff.get(at) && members.enrollmentOf(cohortId, assignments.get(at).getUserId())
                            .map(row -> !row.isActive()).orElse(true)) {
                        newcomers++;
                    }
                }
                requireSeats(cohort, (int) newcomers);
                for (int at = 0; at < assignments.size(); at++) {
                    MemberAssignment member = assignments.get(at);
                    if (asStaff.get(at)) {
                        placeTeacher(cohort, member, now, actor);
                    } else {
                        Optional<Placement> here = members.enrollmentOf(cohortId, member.getUserId());
                        if (here.isEmpty() || !here.get().isActive()) {
                            enrol(cohort, member.getUserId(), null, false, here, now);
                        }
                    }
                }
            }
            OperationEntity operation = OperationEntity.builder().operationName(OPERATION_IMPORT_MEMBERS)
                    .resourceType(CohortAccess.COHORT).resourceId(cohortId).resultEntityType(CohortAccess.COHORT)
                    .resultEntityId(cohortId).startedByUserId(Actor.current().orElse(null)).build();
            operation.onCreatingData(Actor.current(), clock);
            operation.setTenantId(tenant);
            operation.setStartedAt(operation.getCreatedAt());
            operation.setFinishedAt(operation.getCreatedAt());
            Operation done = operationMapper.toDto(operations.insertCompleted(operation));
            done.setResult(Map.of("rows", rows.size() - 1, dryRun ? "valid" : "placed", dryRun ? assignments.size()
                    : placed, "skipped", rowErrors.size(), "rowErrors", rowErrors));
            audit.recordCreate("lms.operation", done.getOperationId());
            return done;
        });
    }

    /** Where the import file's columns sit (by the header row); -1 = the column is absent. */
    private record Columns(int userId, int email, int roleId, int roleLabel) {

        static Columns of(List<List<String>> rows) {
            if (rows.isEmpty()) {
                throw new ValidationFailedException(List.of(new FieldError("fileId", "the file has no header row",
                        "EMPTY_FILE")));
            }
            List<String> header = rows.get(0).stream().map(cell -> cell.strip().toLowerCase(java.util.Locale.ROOT))
                    .toList();
            Columns columns = new Columns(header.indexOf("user_id"), header.indexOf("email"),
                    header.indexOf("role_id"), header.indexOf("role_label"));
            if (columns.userId() < 0 && columns.email() < 0) {
                throw new ValidationFailedException(List.of(new FieldError("fileId",
                        "the header must name user_id or email", "MISSING_COLUMN")));
            }
            return columns;
        }

        /** The row's cell under this column; blank and absent are null. */
        String cell(List<String> row, int at) {
            if (at < 0 || at >= row.size() || row.get(at).isBlank()) {
                return null;
            }
            return row.get(at).strip();
        }
    }

    /** The cohort, if in the caller's scope for the operation (404) and writable there (403); archived is 409. */
    private CohortEntity writableCohort(DataScope scope, UUID cohortId) {
        CohortEntity cohort = cohorts.findForUpdate(cohortId)
                .orElseThrow(() -> new ResourceNotFoundException(CohortAccess.COHORT, cohortId));
        if (!scope.canRead(CohortAccess.institutionOf(cohort))) {
            throw new ResourceNotFoundException(CohortAccess.COHORT, cohortId);
        }
        scope.requireWritable(CohortAccess.institutionOf(cohort));
        if ("archived".equals(cohort.getLifecycleStatus())) {
            throw new InvalidStateException("An archived cohort is read-only.");
        }
        return cohort;
    }

    /** Refuses more students than the free seats (the cohort row is locked by the caller). */
    private void requireSeats(CohortEntity cohort, int newcomers) {
        if (cohort.getSeatCount() != null && newcomers > 0
                && members.activeStudents(cohort.getId()) + newcomers > cohort.getSeatCount()) {
            throw new ConflictException("COHORT_FULL", "The cohort has no seat left for every new student.");
        }
    }

    /** Places a student (a new row, or their old one again) and enrols them in the cohort's courses. */
    private UUID enrol(CohortEntity cohort, UUID userId, UUID paymentId, boolean self, Optional<Placement> placed,
            OffsetDateTime now) {
        UUID actor = Actor.current().orElse(null);
        UUID id;
        if (placed.isPresent()) {
            if (members.reactivateEnrollment(placed.get(), paymentId, self, now, actor) == 0) {
                throw new PreconditionFailedException(CHANGED);
            }
            id = placed.get().id();
            audit.recordTransition(AGGREGATE, id, "reactivate", List.of("isActive", "removedAt", "removalReason"),
                    null);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, id, LmsEvents.payload(id));
        } else {
            CohortEnrollmentEntity entity = CohortEnrollmentEntity.builder().cohortId(cohort.getId()).userId(userId)
                    .paymentId(paymentId).isSelfEnrolled(self).isActive(true).build();
            entity.onCreatingData(Actor.current(), clock);
            entity.setTenantId(cohort.getTenantId());
            id = repository.insert(entity).getId();
            audit.recordCreate(AGGREGATE, id);
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, id, LmsEvents.payload(id));
        }
        for (UUID course : members.coursesOf(cohort.getId())) {
            members.enrolInCourse(UuidV7.next(clock), cohort.getId(), course, userId, paymentId, now, actor);
        }
        return id;
    }

    /** Places (or updates) a staff member with their role. */
    private UUID placeTeacher(CohortEntity cohort, MemberAssignment member, OffsetDateTime now, UUID actor) {
        Optional<Placement> placed = members.teacherOf(cohort.getId(), member.getUserId());
        if (placed.isPresent()) {
            if (members.setTeacher(placed.get(), member.getRoleId(), member.getRoleLabel(), now, actor) == 0) {
                throw new PreconditionFailedException(CHANGED);
            }
            audit.recordUpdate(TEACHER_AGGREGATE, placed.get().id(), List.of("isActive", "roleId", "roleLabel"));
            outbox.publish(TEACHER_AGGREGATE + LmsEvents.UPDATED, TEACHER_AGGREGATE, placed.get().id(),
                    LmsEvents.payload(placed.get().id()));
            return placed.get().id();
        }
        CohortTeacherEntity entity = CohortTeacherEntity.builder().cohortId(cohort.getId())
                .userId(member.getUserId()).roleId(member.getRoleId()).roleLabel(member.getRoleLabel()).isActive(true)
                .build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(cohort.getTenantId());
        UUID id = teachers.insert(entity).getId();
        audit.recordCreate(TEACHER_AGGREGATE, id);
        outbox.publish(TEACHER_AGGREGATE + LmsEvents.CREATED, TEACHER_AGGREGATE, id, LmsEvents.payload(id));
        return id;
    }

    /** Takes a user out of the cohort as a student and as staff (whichever they are). */
    private List<RosterRow> remove(UUID cohortId, UUID user, OffsetDateTime now, UUID actor) {
        List<RosterRow> rows = new ArrayList<>();
        Optional<Placement> student = members.enrollmentOf(cohortId, user).filter(Placement::isActive);
        if (student.isPresent()) {
            if (members.removeEnrollment(student.get(), now, actor) == 0) {
                throw new PreconditionFailedException(CHANGED);
            }
            audit.recordTransition(AGGREGATE, student.get().id(), "remove",
                    List.of("isActive", "removedAt", "removalReason"), null);
            outbox.publish(AGGREGATE + LmsEvents.UPDATED, AGGREGATE, student.get().id(),
                    LmsEvents.payload(student.get().id()));
            rows.add(new RosterRow(student.get().id(), user, RosterRow.STUDENT, false, now));
        }
        Optional<Placement> staff = members.teacherOf(cohortId, user).filter(Placement::isActive);
        if (staff.isPresent()) {
            if (members.removeTeacher(staff.get(), now, actor) == 0) {
                throw new PreconditionFailedException(CHANGED);
            }
            audit.recordUpdate(TEACHER_AGGREGATE, staff.get().id(), List.of("isActive", "removedAt"));
            outbox.publish(TEACHER_AGGREGATE + LmsEvents.UPDATED, TEACHER_AGGREGATE, staff.get().id(),
                    LmsEvents.payload(staff.get().id()));
            rows.add(new RosterRow(staff.get().id(), user, RosterRow.TEACHER, false, now));
        }
        return rows;
    }

    /** Roster entries of the rows, named through iam (outside any transaction). */
    private List<RosterEntry> entries(List<RosterRow> rows, String purpose) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<UUID, MemberDirectory.Member> named = directory.lookup(rows.stream().map(RosterRow::userId).toList(),
                purpose);
        List<RosterEntry> entries = new ArrayList<>();
        for (RosterRow row : rows) {
            MemberDirectory.Member member = named.getOrDefault(row.userId(),
                    MemberDirectory.Member.unknown(row.userId()));
            RosterEntry entry = new RosterEntry(row.userId(), member.displayName(), row.roleCode(), row.isActive());
            entry.setEmail(member.email());
            if (RosterRow.STUDENT.equals(row.roleCode())) {
                entry.setEnrollmentId(row.id());
            } else {
                entry.setTeacherId(row.id());
            }
            entries.add(entry);
        }
        return entries;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
