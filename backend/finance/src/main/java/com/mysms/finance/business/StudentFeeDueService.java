package com.mysms.finance.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
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
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.core.web.UpdateMask;
import com.mysms.finance.api.model.GenerateFeeDuesRequest;
import com.mysms.finance.api.model.Operation;
import com.mysms.finance.api.model.StudentFeeDue;
import com.mysms.finance.api.model.StudentFeeDueUpdate;
import com.mysms.finance.config.FinanceDatabaseConfig;
import com.mysms.finance.config.FinanceEventsConfig;
import com.mysms.finance.domain.StudentFeeDueEntity;
import com.mysms.finance.events.FinanceEvents;
import com.mysms.finance.mapper.StudentFeeDueMapper;
import com.mysms.finance.model.DueTarget;
import com.mysms.finance.repository.FeeScheduleRepository;
import com.mysms.finance.repository.FeeScheduleRepository.ConcessionRow;
import com.mysms.finance.repository.FeeScheduleRepository.PlanRow;
import com.mysms.finance.repository.StudentFeeDueRepository;
import com.mysms.finance.repository.StudentFeeDueRepository.DueRun;
import com.mysms.finance.repository.StudentFeeDueRepository.RunDue;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of the StudentFeeDue aggregate: reading and correcting dues, and the fee
 * run that creates them (finance.GenerateFeeDues, and the run a published fee structure starts).
 *
 * <p>Dues are personal data, owner-only below the fee office: a caller reads the dues of the institutions where they
 * work on fees ({@link FinanceStaff}); outside that reach only their own students' dues, which sis confirms with the
 * caller's token ({@link StudentOwnershipLookup}; 503 when it cannot answer). A due changes only while it is billed on
 * no invoice and settled by no payment; its amounts never change after the run (a mistake is corrected by cancelling
 * the invoice it is on, or by a concession and a new run for the slot).
 */
@Service
public class StudentFeeDueService extends AppTransactionManager {

    public static final String AGGREGATE = StudentFeeDueEntity.TABLE;
    static final String PERMISSION_READ = "finance.student_fee_due.read";
    static final String PERMISSION_UPDATE = "finance.student_fee_due.update";
    static final String PERMISSION_GENERATE = "finance.student_fee_due.generate_fee_dues";
    /** The operation kind of a fee run started directly. */
    public static final String KIND_GENERATE = "finance.GenerateFeeDues";
    /** The contract's StudentFeeDueUpdate fields, in apply order. */
    static final List<String> WRITABLE = List.of("institutionId", "dueDate");
    /** The most students one fee run names directly (a class or a structure brings its own roster). */
    static final int MAX_NAMED = 500;
    /** How many skipped students the operation's outcome lists. */
    static final int SKIPPED_LISTED = 200;

    private static final Logger LOG = LoggerFactory.getLogger(StudentFeeDueService.class);

    /** How one student's run ended. */
    enum Outcome { GENERATED, KEPT, NO_DUES, FAILED }

    /** A fee run: the plan priced for a year, the students it charges and where the dues come from. */
    public record FeeRun(UUID feePlanId, UUID academicYearId, UUID feeStructureId, LocalDate startDate,
            BigDecimal totalOverride, List<DueTarget> targets) {
        public FeeRun {
            targets = List.copyOf(targets);
        }
    }

    private final StudentFeeDueRepository repository;
    private final FeeScheduleRepository schedules;
    private final StudentFeeDueMapper mapper;
    private final StudentOwnershipLookup ownership;
    private final RosterLookup rosters;
    private final OperationService operations;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public StudentFeeDueService(@Qualifier(FinanceDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(FinanceDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            StudentFeeDueRepository repository, FeeScheduleRepository schedules, StudentFeeDueMapper mapper,
            StudentOwnershipLookup ownership, RosterLookup rosters, OperationService operations,
            @Qualifier(FinanceEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(FinanceEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.schedules = schedules;
        this.mapper = mapper;
        this.ownership = ownership;
        this.rosters = rosters;
        this.operations = operations;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    /** One due: in the fee office's reach, or of the caller's own student (404 otherwise). */
    public StudentFeeDue getStudentFeeDue(UUID id) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.current(PERMISSION_READ);
        StudentFeeDueEntity entity = doInReadTransaction(() -> loadVisible(scope, id));
        Optional<UUID> institution = Optional.of(entity.getInstitutionId());
        if (!FinanceStaff.reaches(FinanceStaff.reach(caller), scope, institution)
                && ownership.relationTo(caller, entity.getStudentId()).isEmpty()) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return mapper.toDto(entity);
    }

    /** The dues the caller may read: the fee office's reach, plus their own students' dues elsewhere. */
    public Page<StudentFeeDue> listStudentFeeDues(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(sortBy, StudentFeeDueRepository.SORT_FIELDS,
                StudentFeeDueRepository.DEFAULT_ORDER, StudentFeeDueRepository.ID_COLUMN);
        DataScope scope = DataScope.current(PERMISSION_READ);
        DataScope reach = FinanceStaff.reach(caller);
        SqlFragment where = FilterParser.parse(filter, StudentFeeDueRepository.FILTER_FIELDS)
                .and(StudentFeeDueRepository.scope(scope));
        if (!FinanceStaff.covers(reach, scope)) {
            where = where.and(StudentFeeDueRepository.reachableOrOwn(reach, ownership.ownStudents(caller)));
        }
        SqlFragment filtered = where;
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, filtered).map(mapper::toDto));
    }

    /**
     * Moves a due's date or institution under If-Match while it is unbilled (409 otherwise); the institution of a due
     * a payment settles stays (409). The due is locked first, as RecordPayment locks it.
     */
    public StudentFeeDue updateStudentFeeDue(UUID id, String ifMatch, StudentFeeDueUpdate changes, String updateMask,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        List<String> fields = UpdateMask.fields(updateMask, changes, WRITABLE);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            StudentFeeDueEntity entity = repository.lock(List.of(id)).stream()
                    .filter(due -> scope.canRead(Optional.of(due.getInstitutionId())))
                    .findFirst().orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
            scope.requireWritable(Optional.of(entity.getInstitutionId()));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (!"draft".equals(entity.getStatus())) {
                throw new InvalidStateException("A due billed on an invoice, or cancelled, does not change.");
            }
            UUID institutionBefore = entity.getInstitutionId();
            apply(entity, changes, fields);
            if (entity.getInstitutionId() == null) {
                throw new ValidationFailedException(List.of(new FieldError("institutionId", "must not be null",
                        "NotNull")));
            }
            scope.requireWritable(Optional.of(entity.getInstitutionId()));
            if (!institutionBefore.equals(entity.getInstitutionId())
                    && !repository.withAllocations(List.of(id)).isEmpty()) {
                throw new InvalidStateException("A payment settles this due; its institution stays.");
            }
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            if (repository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(AGGREGATE, id, fields);
            outbox.publish(AGGREGATE + FinanceEvents.UPDATED, AGGREGATE, id, FinanceEvents.payload(id));
            return mapper.toDto(entity);
        });
    }

    /**
     * finance.GenerateFeeDues: the plan's dues for the named students, a class section's roster and named course
     * registrations, for the academic year, answered as the operation that records the run. The plan's institution must
     * be one the caller may write (403). Named students are logical references and taken as given; a roster and
     * registrations are read from sis with the caller's token before anything is written (503 when it cannot answer).
     */
    public Operation generateFeeDues(GenerateFeeDuesRequest request) {
        validator.validate(request);
        List<FieldError> errors = new ArrayList<>();
        WireValues.checkDecimal("totalAmount", request.getTotalAmount(), 14, 2, errors);
        List<UUID> named = request.getStudentIds() == null ? List.of() : request.getStudentIds();
        List<UUID> registrations =
                request.getProgramEnrollmentIds() == null ? List.of() : request.getProgramEnrollmentIds();
        if (named.size() > MAX_NAMED) {
            errors.add(new FieldError("studentIds", "at most " + MAX_NAMED + " students", "Size"));
        }
        if (registrations.size() > MAX_NAMED) {
            errors.add(new FieldError("programEnrollmentIds", "at most " + MAX_NAMED + " registrations", "Size"));
        }
        if (named.isEmpty() && registrations.isEmpty() && request.getClassSectionId() == null) {
            errors.add(new FieldError("studentIds", "name students, a class section or course registrations",
                    "REQUIRED"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        requirePlanWritable(request.getFeePlanId(), DataScope.current(PERMISSION_GENERATE));
        Map<UUID, DueTarget> targets = new LinkedHashMap<>();
        named.forEach(student -> targets.putIfAbsent(student, DueTarget.student(student)));
        if (request.getClassSectionId() != null) {
            rosters.classRoster(request.getClassSectionId(), request.getAcademicYearId())
                    .forEach(target -> targets.putIfAbsent(target.studentId(), target));
        }
        if (!registrations.isEmpty()) {
            rosters.registrations(registrations, request.getAcademicYearId())
                    .forEach(target -> targets.putIfAbsent(target.studentId(), target));
        }
        FeeRun run = new FeeRun(request.getFeePlanId(), request.getAcademicYearId(), null, request.getStartDate(),
                WireValues.decimal(request.getTotalAmount()), List.copyOf(targets.values()));
        return generate(run, KIND_GENERATE, "finance.fee_plan", request.getFeePlanId());
    }

    /** The plan's institution, when the caller may write there (404 for a plan they cannot see, 403 to write). */
    UUID requirePlanWritable(UUID feePlanId, DataScope scope) {
        PlanRow plan = doInReadTransaction(() -> schedules.plan(feePlanId))
                .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("feePlanId",
                        "fee plan does not exist", "NOT_FOUND"))));
        if (!scope.canRead(Optional.of(plan.institutionId()))) {
            throw new ValidationFailedException(List.of(new FieldError("feePlanId", "fee plan does not exist",
                    "NOT_FOUND")));
        }
        scope.requireWritable(Optional.of(plan.institutionId()));
        return plan.institutionId();
    }

    /**
     * Runs a fee run as a long-running operation: the plan is priced once (422 before it starts), then each student's
     * dues are written in a transaction of their own; a student who fails is listed as {@code FAILED} and the run goes
     * on. Safe to repeat and to race: a slot already charged is kept ({@code uq_student_fee_due_slot}).
     */
    public Operation generate(FeeRun run, String kind, String resourceType, UUID resourceId) {
        FeeContext context = doInPrimaryReadTransaction(() -> priced(run));
        UUID operationId = operations.start(kind, resourceType, resourceId);
        try {
            Map<UUID, Outcome> outcomes = new LinkedHashMap<>();
            int written = 0;
            int reported = 0;
            for (int done = 0; done < run.targets().size(); done++) {
                DueTarget target = run.targets().get(done);
                List<ConcessionRow> own = context.concessions().getOrDefault(target.studentId(), List.of());
                Outcome outcome;
                try {
                    int count = doInWriteTransaction(() -> write(run, context, target, own));
                    written += count;
                    outcome = count > 0 ? Outcome.GENERATED
                            : context.schedule().slots().isEmpty() ? Outcome.NO_DUES : Outcome.KEPT;
                } catch (RuntimeException failed) {
                    // this student's transaction rolled back whole; the others are charged regardless
                    LOG.warn("the fee run could not charge student {} ({})", target.studentId(),
                            failed.getClass().getSimpleName());
                    outcome = Outcome.FAILED;
                }
                outcomes.put(target.studentId(), outcome);
                int percent = (done + 1) * 100 / run.targets().size();
                if (percent >= reported + 10 && percent < 100) {
                    reported = percent;
                    if (!operations.progress(operationId, percent)) {
                        break;
                    }
                }
            }
            return operations.succeed(operationId, summary(run, outcomes, written));
        } catch (RuntimeException e) {
            LOG.error("fee due generation stopped ({})", e.getClass().getSimpleName());
            operations.fail(operationId, "INTERNAL", "The fee run stopped before it finished. The dues already "
                    + "generated are kept; run it again for the rest.");
            throw e;
        }
    }

    /** The plan priced for the run, and the concessions of its students for the year. */
    private record FeeContext(UUID institutionId, FeeSchedule schedule, Map<UUID, List<ConcessionRow>> concessions) {
    }

    private FeeContext priced(FeeRun run) {
        PlanRow plan = schedules.plan(run.feePlanId()).orElseThrow(() -> new ValidationFailedException(
                List.of(new FieldError("feePlanId", "fee plan does not exist", "NOT_FOUND"))));
        List<FeeScheduleRepository.InstallmentRow> installments = schedules.installments(plan.id());
        FeeSchedule schedule = FeeSchedule.of(plan, installments,
                schedules.components(installments.stream().map(FeeScheduleRepository.InstallmentRow::id).toList()),
                run.totalOverride(), run.startDate());
        Map<UUID, List<ConcessionRow>> concessions = new HashMap<>();
        Set<UUID> students = new LinkedHashSet<>();
        run.targets().forEach(target -> students.add(target.studentId()));
        if (!students.isEmpty()) {
            schedules.concessions(students, run.academicYearId())
                    .forEach(c -> concessions.computeIfAbsent(c.studentId(), k -> new ArrayList<>()).add(c));
        }
        return new FeeContext(plan.institutionId(), schedule, concessions);
    }

    /** One student's dues, written in one statement; answers how many were new. Each new due is audited. */
    private int write(FeeRun run, FeeContext context, DueTarget target, List<ConcessionRow> concessions) {
        List<FeeSchedule.PricedDue> priced = context.schedule().price(concessions);
        if (priced.isEmpty()) {
            return 0;
        }
        OffsetDateTime now = now();
        UUID actor = Actor.current().orElse(null);
        List<RunDue> dues = priced.stream().map(due -> new RunDue(UuidV7.next(clock), due.slot().installmentId(),
                due.slot().feeHeadId(), due.slot().dueDate(), due.slot().amount(), due.concessionId(),
                due.discountAmount(), due.net())).toList();
        DueRun shared = new DueRun(context.institutionId(), target.studentId(), target.programEnrollmentId(),
                target.studentEnrollmentId(), context.schedule().currencyId(), priced.get(0).discountPercent(),
                run.academicYearId(), run.feeStructureId());
        List<UUID> written = repository.insertRun(shared, dues, now, actor);
        written.forEach(id -> audit.recordCreate(AGGREGATE, id));
        if (!written.isEmpty()) {
            outbox.publish(AGGREGATE + ".generated", AGGREGATE, target.studentId(),
                    Map.of("studentId", target.studentId().toString(), "feePlanId", run.feePlanId().toString(),
                            "academicYearId", run.academicYearId().toString()));
        }
        return written.size();
    }

    /**
     * The outcome the operation keeps: what the run was for, the counts, and who was skipped and why ({@code NO_DUES}:
     * nothing to charge; {@code FAILED}: their dues could not be written — run it again for them).
     */
    private static Map<String, Object> summary(FeeRun run, Map<UUID, Outcome> outcomes, int written) {
        List<Map<String, String>> skipped = new ArrayList<>();
        int generated = 0;
        int kept = 0;
        int failed = 0;
        int skippedCount = 0;
        for (Map.Entry<UUID, Outcome> entry : outcomes.entrySet()) {
            if (entry.getValue() == Outcome.FAILED) {
                failed++;
            }
            switch (entry.getValue()) {
                case GENERATED -> generated++;
                case KEPT -> kept++;
                default -> {
                    skippedCount++;
                    if (skipped.size() < SKIPPED_LISTED) {
                        skipped.add(Map.of("studentId", entry.getKey().toString(), "reason", entry.getValue().name()));
                    }
                }
            }
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("feePlanId", run.feePlanId().toString());
        summary.put("academicYearId", run.academicYearId().toString());
        if (run.feeStructureId() != null) {
            summary.put("feeStructureId", run.feeStructureId().toString());
        }
        summary.put("students", generated);
        summary.put("kept", kept);
        summary.put("failed", failed);
        summary.put("dues", written);
        summary.put("skippedCount", skippedCount);
        summary.put("skipped", skipped);
        return summary;
    }

    /** The live row, if the caller's read scope shows its institution (404 otherwise). */
    private StudentFeeDueEntity loadVisible(DataScope scope, UUID id) {
        StudentFeeDueEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(Optional.of(entity.getInstitutionId()))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** Applies the selected StudentFeeDueUpdate fields; explicit per field so a mask can clear values. */
    private static void apply(StudentFeeDueEntity entity, StudentFeeDueUpdate changes, List<String> fields) {
        for (String field : fields) {
            switch (field) {
                case "institutionId" -> entity.setInstitutionId(changes.getInstitutionId());
                case "dueDate" -> entity.setDueDate(changes.getDueDate());
                default -> throw new IllegalStateException("unhandled writable field " + field);
            }
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
