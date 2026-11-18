package com.mysms.sis.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PreconditionFailedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.sis.api.model.ApplyPromotionsRequest;
import com.mysms.sis.api.model.Operation;
import com.mysms.sis.api.model.PromotionList;
import com.mysms.sis.api.model.ProposePromotionsRequest;
import com.mysms.sis.business.AcademicsLookup.ClassPlace;
import com.mysms.sis.config.SisDatabaseConfig;
import com.mysms.sis.config.SisEventsConfig;
import com.mysms.sis.domain.PromotionEntity;
import com.mysms.sis.domain.StudentEnrollmentEntity;
import com.mysms.sis.events.SisEvents;
import com.mysms.sis.mapper.PromotionMapper;
import com.mysms.sis.repository.PromotionRepository;
import com.mysms.sis.repository.StudentEnrollmentRepository;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The year-end promotion workflow (propose → review → approve / reject → apply): sis.ProposePromotions writes a
 * proposal for each student placed in a class, and sis.ApplyPromotions turns approved promotions into next year's
 * placements. Approving and rejecting a single proposal are the plain transitions of {@link PromotionService}.
 *
 * <p>Where a class sits and which class of the next year a promotion leads to are academics' data, read through
 * {@link AcademicsLookup} with the caller's token before sis's own transaction begins; without the academics service
 * both operations answer 503.
 */
@Service
public class PromotionWorkflowService extends AppTransactionManager {

    static final String AGGREGATE = PromotionService.AGGREGATE;
    static final String PERMISSION_PROPOSE = "sis.promotion.propose_promotions";
    static final String PERMISSION_APPLY = "sis.promotion.apply_promotions";
    /** Promotions applied in one call: one transaction writes three rows for each. */
    static final int MAX_APPLY = 500;

    /** Where an approved promotion leads: the class section of the next year, or none when no section is named. */
    private record Target(UUID institutionId, UUID academicYearId, UUID gradeLevelId, UUID sectionId) {
    }

    private final PromotionRepository repository;
    private final StudentEnrollmentRepository enrollments;
    private final PromotionMapper mapper;
    private final AcademicsLookup academics;
    private final OperationService operations;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public PromotionWorkflowService(PromotionRepository repository, StudentEnrollmentRepository enrollments,
            PromotionMapper mapper, AcademicsLookup academics, OperationService operations,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(SisEventsConfig.AUDIT) AuditWriter audit, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.enrollments = enrollments;
        this.mapper = mapper;
        this.academics = academics;
        this.operations = operations;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * sis.ProposePromotions: a proposal (status proposed) from the class to the named year, grade and section for
     * every student placed in the class now — or for the listed ones, each of whom must be placed there (422 on
     * {@code studentIds[i]}). A student who already has a standing promotion out of the class's year keeps it: it
     * is answered as it is and nothing is written for them, so proposing a class again is harmless. The answer is
     * every promotion of the selected students out of the year, in roll order. The class must be one academics shows
     * the caller (422 on {@code fromClassSectionId}) in an institution they may write in (403); the target year must
     * differ from the class's, and the target year (org), grade and section (academics) must be the class's
     * institution's own (422 on each field), so that applying never writes an unchecked year.
     */
    public PromotionList proposePromotions(ProposePromotionsRequest request) {
        DataScope scope = DataScope.current(PERMISSION_PROPOSE);
        ClassPlace from = academics.classSection(request.getFromClassSectionId(), "Proposing promotions")
                .orElseThrow(() -> new ValidationFailedException(List.of(new FieldError("fromClassSectionId",
                        "class section does not exist", "NOT_FOUND"))));
        scope.requireWritable(Optional.ofNullable(from.institutionId()));
        if (request.getToAcademicYearId().equals(from.academicYearId())) {
            throw new ValidationFailedException(List.of(new FieldError("toAcademicYearId",
                    "must be another year than the class's", "INVALID")));
        }
        List<FieldError> target = academics.targetErrors(from.institutionId(), request.getToAcademicYearId(),
                request.getToGradeLevelId(), request.getToSectionId(), "Proposing promotions");
        if (!target.isEmpty()) {
            throw new ValidationFailedException(target);
        }
        return doInWriteTransaction(() -> {
            List<UUID> students = selected(request, from);
            Map<UUID, PromotionEntity> standing = new LinkedHashMap<>();
            if (!students.isEmpty()) {
                repository.standingOfStudents(students, from.academicYearId())
                        .forEach(promotion -> standing.put(promotion.getStudentId(), promotion));
            }
            List<PromotionEntity> answer = new ArrayList<>();
            for (UUID student : students) {
                PromotionEntity promotion = standing.get(student);
                answer.add(promotion != null ? promotion : propose(student, from, request));
            }
            return new PromotionList(answer.stream().map(mapper::toDto).toList());
        });
    }

    /**
     * sis.ApplyPromotions: for each approved promotion (409 for any other state; 404 for one the caller cannot see,
     * 403 outside their writable scope) the student's open placement of the promotion's year ends today as promoted
     * — detained when the grade stays the same — and a placement in the next year begins today in the class
     * academics has for the target year, grade and section (none when the promotion names no section; 409 when the
     * named class does not exist). The promotion becomes applied. All of it is one transaction: one refusal applies
     * nothing. A student without an open placement in the year, or already placed in the next year, is 409. The work
     * is done in the request, so the answer is the finished operation. Dates are UTC days.
     */
    public Operation applyPromotions(ApplyPromotionsRequest request) {
        DataScope scope = DataScope.current(PERMISSION_APPLY);
        List<UUID> ids = new ArrayList<>(new LinkedHashSet<>(request.getPromotionIds()));
        if (ids.isEmpty() || ids.size() > MAX_APPLY || ids.contains(null)) {
            throw new ValidationFailedException(List.of(new FieldError("promotionIds",
                    "must name between 1 and " + MAX_APPLY + " promotions", "Size")));
        }
        Map<UUID, PromotionEntity> read = doInPrimaryReadTransaction(() -> {
            Map<UUID, PromotionEntity> found = new LinkedHashMap<>();
            for (UUID id : ids) {
                found.put(id, approved(scope, id));
            }
            return found;
        });
        Map<Target, Optional<UUID>> classes = new HashMap<>();
        for (PromotionEntity promotion : read.values()) {
            classes.computeIfAbsent(targetOf(promotion), this::classOf);
        }
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        return doInWriteTransaction(() -> {
            OffsetDateTime now = now();
            UUID actor = Actor.current().orElse(null);
            for (PromotionEntity seen : read.values()) {
                PromotionEntity promotion = approved(scope, seen.getId());
                if (promotion.getRowVersion() != seen.getRowVersion()) {
                    throw new PreconditionFailedException("A promotion changed meanwhile; review it again.");
                }
                Target target = targetOf(promotion);
                UUID classSection = classes.get(target).orElse(null);
                place(promotion, classSection, today, now, actor);
                if (repository.applyPromotion(promotion.getId(), promotion.getRowVersion(), now, actor) == 0) {
                    throw new PreconditionFailedException("A promotion changed meanwhile; review it again.");
                }
                audit.recordTransition(AGGREGATE, promotion.getId(), "apply", List.of("status"), null);
                outbox.publish(AGGREGATE + ".apply", AGGREGATE, promotion.getId(),
                        SisEvents.payload(promotion.getId()));
            }
            UUID single = ids.size() == 1 ? ids.get(0) : null;
            return operations.recordFinished("sis.ApplyPromotions", AGGREGATE, single,
                    StudentEnrollmentEntity.TABLE, null);
        });
    }

    /** The students the proposal is for: the listed ones (each placed in the class), else the whole class. */
    private List<UUID> selected(ProposePromotionsRequest request, ClassPlace from) {
        List<UUID> roster = enrollments.rosterOfClass(from.classSectionId()).stream()
                .filter(placement -> from.academicYearId().equals(placement.getAcademicYearId()))
                .map(StudentEnrollmentEntity::getStudentId).distinct().toList();
        List<UUID> listed = request.getStudentIds();
        if (listed == null || listed.isEmpty()) {
            return roster;
        }
        Set<UUID> placed = Set.copyOf(roster);
        List<FieldError> errors = new ArrayList<>();
        for (int i = 0; i < listed.size(); i++) {
            if (listed.get(i) == null || !placed.contains(listed.get(i))) {
                errors.add(new FieldError("studentIds[" + i + "]", "is not placed in the class", "NOT_FOUND"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        Set<UUID> wanted = Set.copyOf(listed);
        return roster.stream().filter(wanted::contains).toList();
    }

    /** Writes one proposal, audited and announced. */
    private PromotionEntity propose(UUID student, ClassPlace from, ProposePromotionsRequest request) {
        PromotionEntity promotion = new PromotionEntity();
        promotion.setStudentId(student);
        promotion.setInstitutionId(from.institutionId());
        promotion.setFromAcademicYearId(from.academicYearId());
        promotion.setFromGradeLevelId(from.gradeLevelId());
        promotion.setFromSectionId(from.sectionId());
        promotion.setToAcademicYearId(request.getToAcademicYearId());
        promotion.setToGradeLevelId(request.getToGradeLevelId());
        promotion.setToSectionId(request.getToSectionId());
        promotion.setBasedOnExamSessionId(request.getBasedOnExamSessionId());
        promotion.setStatus("proposed");
        promotion.onCreatingData(Actor.current(), clock);
        promotion.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        PromotionEntity stored = repository.insert(promotion);
        audit.recordCreate(AGGREGATE, stored.getId());
        outbox.publish(AGGREGATE + SisEvents.CREATED, AGGREGATE, stored.getId(), SisEvents.payload(stored.getId()));
        return stored;
    }

    /** The promotion, visible and writable for the caller, approved and complete enough to apply. */
    private PromotionEntity approved(DataScope scope, UUID id) {
        PromotionEntity promotion = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        Optional<UUID> institution = Optional.ofNullable(promotion.getInstitutionId());
        if (!scope.canRead(institution)) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        scope.requireWritable(institution);
        if (!"approved".equals(promotion.getStatus())) {
            throw new InvalidStateException("Promotion " + id + " is not approved.");
        }
        if (promotion.getToAcademicYearId() == null || promotion.getToGradeLevelId() == null) {
            throw new InvalidStateException("Promotion " + id + " names no next year and grade.");
        }
        return promotion;
    }

    private static Target targetOf(PromotionEntity promotion) {
        return new Target(promotion.getInstitutionId(), promotion.getToAcademicYearId(),
                promotion.getToGradeLevelId(), promotion.getToSectionId());
    }

    /** The class section of the target in academics; none when the promotion names no section (409 when missing). */
    private Optional<UUID> classOf(Target target) {
        if (target.sectionId() == null) {
            return Optional.empty();
        }
        return Optional.of(academics.classSectionOf(target.institutionId(), target.academicYearId(),
                target.gradeLevelId(), target.sectionId(), "Applying promotions")
                .map(ClassPlace::classSectionId)
                .orElseThrow(() -> new InvalidStateException(
                        "The class of the next year that a promotion leads to does not exist in academics.")));
    }

    /** Ends the student's placement in the promotion's year and opens the next year's. */
    private void place(PromotionEntity promotion, UUID classSection, LocalDate today, OffsetDateTime now,
            UUID actor) {
        UUID student = promotion.getStudentId();
        StudentEnrollmentEntity current = enrollments.lockOpenOfStudentInYear(student,
                promotion.getFromAcademicYearId()).orElseThrow(() -> new InvalidStateException(
                        "The student of promotion " + promotion.getId() + " has no open placement in its year."));
        if (enrollments.lockOpenOfStudentInYear(student, promotion.getToAcademicYearId()).isPresent()) {
            throw new InvalidStateException(
                    "The student of promotion " + promotion.getId() + " is already placed in the next year.");
        }
        LocalDate end = current.getEffectiveFrom() != null && current.getEffectiveFrom().isAfter(today)
                ? current.getEffectiveFrom() : today;
        String outcome = Objects.equals(promotion.getFromGradeLevelId(), promotion.getToGradeLevelId())
                ? "detained" : "promoted";
        if (enrollments.close(current.getId(), current.getRowVersion(), outcome, end, null, now, actor) == 0) {
            throw new PreconditionFailedException("A placement changed meanwhile; try again.");
        }
        audit.recordUpdate(StudentEnrollmentEntity.TABLE, current.getId(), List.of("enrollmentStatus", "effectiveTo"));
        outbox.publish(StudentEnrollmentEntity.TABLE + SisEvents.UPDATED, StudentEnrollmentEntity.TABLE,
                current.getId(), SisEvents.payload(current.getId()));
        StudentEnrollmentEntity next = new StudentEnrollmentEntity();
        next.setStudentId(student);
        next.setAcademicYearId(promotion.getToAcademicYearId());
        next.setClassSectionId(classSection);
        next.setPreviousEnrollmentId(current.getId());
        next.setEffectiveFrom(end);
        next.onCreatingData(Actor.current(), clock);
        next.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        StudentEnrollmentEntity stored = enrollments.insertFollowing(next);
        audit.recordCreate(StudentEnrollmentEntity.TABLE, stored.getId());
        outbox.publish(StudentEnrollmentEntity.TABLE + SisEvents.CREATED, StudentEnrollmentEntity.TABLE,
                stored.getId(), SisEvents.payload(stored.getId()));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
