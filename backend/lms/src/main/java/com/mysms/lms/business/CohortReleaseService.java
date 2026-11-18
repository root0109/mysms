package com.mysms.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.lms.api.model.ReleaseGrant;
import com.mysms.lms.api.model.ReleaseGrantList;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.CohortEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.ReleaseGrantMapper;
import com.mysms.lms.model.GradeCell;
import com.mysms.lms.model.ReleaseRuleRow;
import com.mysms.lms.repository.CohortGradebookRepository;
import com.mysms.lms.repository.CohortMembershipRepository;
import com.mysms.lms.repository.CohortReleaseRepository;
import com.mysms.lms.repository.CohortRepository;
import com.mysms.lms.repository.ReleaseGrantRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Re-evaluating a cohort's conditional release (lms.EvaluateReleaseRules): the facts are read, the
 * {@link ReleaseRuleEvaluator} decides, and the grants that change are written with their audit records in the same
 * transaction. Part of the Cohort aggregate's workflow, kept beside {@link CohortService} for size.
 */
@Service
public class CohortReleaseService extends AppTransactionManager {

    static final String PERMISSION_EVALUATE = "lms.cohort.evaluate_release_rules";
    private static final String GRANT = "lms.release_grant";

    private final CohortRepository cohorts;
    private final CohortReleaseRepository releases;
    private final CohortGradebookRepository grades;
    private final CohortMembershipRepository members;
    private final ReleaseGrantRepository grants;
    private final ReleaseGrantMapper grantMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final Clock clock;

    public CohortReleaseService(CohortRepository cohorts, CohortReleaseRepository releases,
            CohortGradebookRepository grades, CohortMembershipRepository members, ReleaseGrantRepository grants,
            ReleaseGrantMapper grantMapper, @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox, @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            Clock clock) {
        super(database, transactions);
        this.cohorts = cohorts;
        this.releases = releases;
        this.grades = grades;
        this.members = members;
        this.grants = grants;
        this.grantMapper = grantMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * lms.EvaluateReleaseRules: evaluates the cohort's enabled rules for every active student (or the one given) and
     * answers the grants whose release state changed (new grants included). The cohort must be in the caller's scope
     * (404) and writable (403); an archived cohort is 409.
     */
    public ReleaseGrantList evaluateReleaseRules(UUID cohortId, UUID userId) {
        DataScope scope = DataScope.current(PERMISSION_EVALUATE);
        return doInWriteTransaction(() -> {
            CohortEntity cohort = cohorts.findById(cohortId)
                    .orElseThrow(() -> new ResourceNotFoundException(CohortAccess.COHORT, cohortId));
            if (!scope.canRead(CohortAccess.institutionOf(cohort))) {
                throw new ResourceNotFoundException(CohortAccess.COHORT, cohortId);
            }
            scope.requireWritable(CohortAccess.institutionOf(cohort));
            if ("archived".equals(cohort.getLifecycleStatus())) {
                throw new InvalidStateException("An archived cohort is read-only.");
            }
            List<UUID> students = members.activeStudentIds(cohortId).stream()
                    .filter(student -> userId == null || student.equals(userId)).toList();
            List<ReleaseRuleRow> rules = releases.rules(cohortId);
            if (students.isEmpty() || rules.isEmpty()) {
                return new ReleaseGrantList(List.of());
            }
            OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MICROS);
            ReleaseRuleEvaluator.Facts facts = facts(cohort, rules, students, now);
            List<ReleaseGrant> changed = new ArrayList<>();
            UUID actor = Actor.current().orElse(null);
            for (ReleaseRuleEvaluator.Decision decision : ReleaseRuleEvaluator.evaluate(rules, students, facts)) {
                for (UUID id : releases.grant(UuidV7.next(clock), decision.contentItemId(), decision.chapterId(),
                        decision.userId(), decision.released(), now, actor)) {
                    audit.recordTransition(GRANT, id, decision.released() ? "release" : "withdraw",
                            List.of("isReleased"), null);
                    outbox.publish(GRANT + LmsEvents.UPDATED, GRANT, id, LmsEvents.payload(id));
                    changed.add(grantMapper.toDto(grants.findById(id).orElseThrow()));
                }
            }
            return new ReleaseGrantList(changed);
        });
    }

    private ReleaseRuleEvaluator.Facts facts(CohortEntity cohort, List<ReleaseRuleRow> rules, List<UUID> students,
            OffsetDateTime now) {
        List<UUID> sources = rules.stream().map(ReleaseRuleRow::sourceItemId).distinct().toList();
        Map<String, GradeCell> cells = new LinkedHashMap<>();
        grades.cells(cohort.getId(), students, sources)
                .forEach(cell -> cells.put(cell.userId() + "|" + cell.contentItemId(), cell));
        List<UUID> lessons = rules.stream().map(ReleaseRuleRow::sourceLessonId).filter(Objects::nonNull).distinct()
                .toList();
        List<UUID> quizzes = rules.stream().map(ReleaseRuleRow::sourceQuizId).filter(Objects::nonNull).distinct()
                .toList();
        OffsetDateTime since = cohort.getStartDate().minusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        LocalDate today = now.toLocalDate();
        return new ReleaseRuleEvaluator.Facts(cells, releases.completedLessons(students, lessons),
                releases.viewed(students, sources, since), releases.attempts(students, quizzes), today);
    }
}
