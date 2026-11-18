package com.mysms.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.lms.api.model.ImportQuestionsRequest;
import com.mysms.lms.api.model.Operation;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.model.CoursePackage;
import com.mysms.lms.repository.QuestionImportRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * ImportQuestions: a GIFT or CSV file of the caller's (at most 5 MB, 2,000 questions) becomes bank questions, as a
 * long-running operation answered 202. A file with problems imports nothing: the operation fails with
 * {@code QUESTIONS_INVALID} and its message names the first problems by line. The bank is tenant-wide, so a tenant-wide
 * grant is needed (403). QTI, ExamView and WebCT files are refused (422).
 */
@Service
public class QuestionImportService extends AppTransactionManager {

    static final String KIND = "lms.ImportQuestions";
    static final String PERMISSION = "lms.question.import_questions";
    static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Map<String, List<String>> EXTENSIONS = Map.of("gift", List.of(".gift", ".txt"),
            "csv", List.of(".csv"));
    private static final int MESSAGE_PROBLEMS = 5;

    private final QuestionImportRepository repository;
    private final QuestionBankWriter writer;
    private final PlatformFiles files;
    private final OperationService operations;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public QuestionImportService(QuestionImportRepository repository, QuestionBankWriter writer, PlatformFiles files,
            OperationService operations, @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.writer = writer;
        this.files = files;
        this.operations = operations;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public Operation importQuestions(ImportQuestionsRequest request) {
        validator.validate(request);
        String format = request.getFormat().getValue();
        if (!EXTENSIONS.containsKey(format)) {
            throw new ValidationFailedException(List.of(new FieldError("format",
                    "only gift and csv files are imported by this release", "AllowedValues")));
        }
        DataScope.current(PERMISSION).requireWritable(java.util.Optional.empty());
        UUID actor = AuthContext.require().userId();
        UUID pool = request.getQuestionPoolId();
        if (pool != null && !doInReadTransaction(() -> repository.poolExists(pool))) {
            throw new ValidationFailedException(List.of(new FieldError("questionPoolId",
                    "question pool does not exist", "NOT_FOUND")));
        }
        PlatformFiles.Read file = files.read(request.getFileId(), "fileId", EXTENSIONS.get(format), MAX_BYTES);
        UUID operationId = operations.start(KIND, "lms.question_pool", pool);
        QuestionFileParser.Parsed parsed = QuestionFileParser.parse(format, file.content());
        if (!parsed.problems().isEmpty()) {
            List<String> shown = parsed.problems().subList(0, Math.min(MESSAGE_PROBLEMS, parsed.problems().size()));
            String message = String.join("; ", shown) + (parsed.problems().size() > shown.size() ? "; …" : "");
            operations.fail(operationId, "QUESTIONS_INVALID", message.length() > 500 ? message.substring(0, 497)
                    + "…" : message);
            return operations.getOperation(operationId);
        }
        try {
            runInWriteTransaction(() -> {
                OffsetDateTime now = now();
                for (CoursePackage.Question question : parsed.questions()) {
                    UUID id = writer.write(question, now, actor);
                    audit.recordCreate("lms.question", id);
                    outbox.publish("lms.question" + LmsEvents.CREATED, "lms.question", id, LmsEvents.payload(id));
                    if (pool != null) {
                        repository.addToPool(UuidV7.next(clock), pool, id, now, actor);
                    }
                }
            });
        } catch (RuntimeException e) {
            operations.fail(operationId, "INTERNAL", "The import stopped before it finished; nothing was imported.");
            throw e;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("imported", parsed.questions().size());
        if (pool != null) {
            result.put("questionPoolId", pool.toString());
        }
        return operations.succeed(operationId, result);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
