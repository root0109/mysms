package com.mysms.learning.lms.business;

import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.DuplicateResourceException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
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
import com.mysms.core.web.Patches;
import com.mysms.learning.lms.api.model.Question;
import com.mysms.learning.lms.api.model.QuestionAcceptedAnswer;
import com.mysms.learning.lms.api.model.QuestionAcceptedAnswerUpdate;
import com.mysms.learning.lms.api.model.QuestionBlank;
import com.mysms.learning.lms.api.model.QuestionBlankUpdate;
import com.mysms.learning.lms.api.model.QuestionHotspot;
import com.mysms.learning.lms.api.model.QuestionHotspotUpdate;
import com.mysms.learning.lms.api.model.QuestionMatchPair;
import com.mysms.learning.lms.api.model.QuestionMatchPairUpdate;
import com.mysms.learning.lms.api.model.QuestionOption;
import com.mysms.learning.lms.api.model.QuestionOptionUpdate;
import com.mysms.learning.lms.api.model.QuestionPattern;
import com.mysms.learning.lms.api.model.QuestionPatternUpdate;
import com.mysms.learning.lms.api.model.QuestionUpdate;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import com.mysms.learning.lms.config.LmsEventsConfig;
import com.mysms.learning.lms.domain.QuestionAcceptedAnswerEntity;
import com.mysms.learning.lms.domain.QuestionBlankEntity;
import com.mysms.learning.lms.domain.QuestionEntity;
import com.mysms.learning.lms.domain.QuestionHotspotEntity;
import com.mysms.learning.lms.domain.QuestionMatchPairEntity;
import com.mysms.learning.lms.domain.QuestionOptionEntity;
import com.mysms.learning.lms.domain.QuestionPatternEntity;
import com.mysms.learning.lms.events.LmsEvents;
import com.mysms.learning.lms.mapper.QuestionAcceptedAnswerMapper;
import com.mysms.learning.lms.mapper.QuestionBlankMapper;
import com.mysms.learning.lms.mapper.QuestionHotspotMapper;
import com.mysms.learning.lms.mapper.QuestionMapper;
import com.mysms.learning.lms.mapper.QuestionMatchPairMapper;
import com.mysms.learning.lms.mapper.QuestionOptionMapper;
import com.mysms.learning.lms.mapper.QuestionPatternMapper;
import com.mysms.learning.lms.repository.QuestionAcceptedAnswerRepository;
import com.mysms.learning.lms.repository.QuestionBlankRepository;
import com.mysms.learning.lms.repository.QuestionHotspotRepository;
import com.mysms.learning.lms.repository.QuestionMatchPairRepository;
import com.mysms.learning.lms.repository.QuestionOptionRepository;
import com.mysms.learning.lms.repository.QuestionPatternRepository;
import com.mysms.learning.lms.repository.QuestionBankSearch;
import com.mysms.learning.lms.repository.QuestionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of lms.question and of the records that exist only as part of a Question (QuestionAcceptedAnswer,
 * QuestionBlank, QuestionHotspot, QuestionMatchPair, QuestionOption, QuestionPattern): audit and domain event commit in
 * the same transaction as every change. Rows outside the caller's institutions are invisible; tenant-level rows change
 * only through a tenant-wide grant.
 */
@Service
public class QuestionService extends AppTransactionManager {

    public static final String AGGREGATE = QuestionEntity.TABLE;
    static final String PERMISSION_LIST = "lms.question.read";
    static final int MAX_SEARCH_TEXT = 200;
    static final List<String> QUESTION_TYPES = List.of("choices", "user_input", "open_ended", "true_false",
            "matching", "fill_blank", "numeric", "hotspot", "pattern");
    static final String PERMISSION_GET = "lms.question.read";
    static final String PERMISSION_CREATE = "lms.question.create";
    static final String PERMISSION_UPDATE = "lms.question.update";
    static final String PERMISSION_DELETE = "lms.question.delete";

    public static final String QUESTION_ACCEPTED_ANSWER_AGGREGATE = QuestionAcceptedAnswerEntity.TABLE;
    static final String QUESTION_ACCEPTED_ANSWER_PERMISSION_LIST = "lms.question_accepted_answer.read";
    static final String QUESTION_ACCEPTED_ANSWER_PERMISSION_GET = "lms.question_accepted_answer.read";
    static final String QUESTION_ACCEPTED_ANSWER_PERMISSION_CREATE = "lms.question_accepted_answer.create";
    static final String QUESTION_ACCEPTED_ANSWER_PERMISSION_UPDATE = "lms.question_accepted_answer.update";
    static final String QUESTION_ACCEPTED_ANSWER_PERMISSION_DELETE = "lms.question_accepted_answer.delete";

    public static final String QUESTION_BLANK_AGGREGATE = QuestionBlankEntity.TABLE;
    static final String QUESTION_BLANK_PERMISSION_LIST = "lms.question_blank.read";
    static final String QUESTION_BLANK_PERMISSION_GET = "lms.question_blank.read";
    static final String QUESTION_BLANK_PERMISSION_CREATE = "lms.question_blank.create";
    static final String QUESTION_BLANK_PERMISSION_UPDATE = "lms.question_blank.update";
    static final String QUESTION_BLANK_PERMISSION_DELETE = "lms.question_blank.delete";

    public static final String QUESTION_HOTSPOT_AGGREGATE = QuestionHotspotEntity.TABLE;
    static final String QUESTION_HOTSPOT_PERMISSION_LIST = "lms.question_hotspot.read";
    static final String QUESTION_HOTSPOT_PERMISSION_GET = "lms.question_hotspot.read";
    static final String QUESTION_HOTSPOT_PERMISSION_CREATE = "lms.question_hotspot.create";
    static final String QUESTION_HOTSPOT_PERMISSION_UPDATE = "lms.question_hotspot.update";
    static final String QUESTION_HOTSPOT_PERMISSION_DELETE = "lms.question_hotspot.delete";

    public static final String QUESTION_MATCH_PAIR_AGGREGATE = QuestionMatchPairEntity.TABLE;
    static final String QUESTION_MATCH_PAIR_PERMISSION_LIST = "lms.question_match_pair.read";
    static final String QUESTION_MATCH_PAIR_PERMISSION_GET = "lms.question_match_pair.read";
    static final String QUESTION_MATCH_PAIR_PERMISSION_CREATE = "lms.question_match_pair.create";
    static final String QUESTION_MATCH_PAIR_PERMISSION_UPDATE = "lms.question_match_pair.update";
    static final String QUESTION_MATCH_PAIR_PERMISSION_DELETE = "lms.question_match_pair.delete";

    public static final String QUESTION_OPTION_AGGREGATE = QuestionOptionEntity.TABLE;
    static final String QUESTION_OPTION_PERMISSION_LIST = "lms.question_option.read";
    static final String QUESTION_OPTION_PERMISSION_GET = "lms.question_option.read";
    static final String QUESTION_OPTION_PERMISSION_CREATE = "lms.question_option.create";
    static final String QUESTION_OPTION_PERMISSION_UPDATE = "lms.question_option.update";
    static final String QUESTION_OPTION_PERMISSION_DELETE = "lms.question_option.delete";

    public static final String QUESTION_PATTERN_AGGREGATE = QuestionPatternEntity.TABLE;
    static final String QUESTION_PATTERN_PERMISSION_LIST = "lms.question_pattern.read";
    static final String QUESTION_PATTERN_PERMISSION_GET = "lms.question_pattern.read";
    static final String QUESTION_PATTERN_PERMISSION_CREATE = "lms.question_pattern.create";
    static final String QUESTION_PATTERN_PERMISSION_UPDATE = "lms.question_pattern.update";
    static final String QUESTION_PATTERN_PERMISSION_DELETE = "lms.question_pattern.delete";

    private final QuestionRepository repository;
    private final QuestionMapper mapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final QuestionAcceptedAnswerRepository questionAcceptedAnswerRepository;
    private final QuestionAcceptedAnswerMapper questionAcceptedAnswerMapper;
    private final QuestionBlankRepository questionBlankRepository;
    private final QuestionBlankMapper questionBlankMapper;
    private final QuestionHotspotRepository questionHotspotRepository;
    private final QuestionHotspotMapper questionHotspotMapper;
    private final QuestionMatchPairRepository questionMatchPairRepository;
    private final QuestionMatchPairMapper questionMatchPairMapper;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionOptionMapper questionOptionMapper;
    private final QuestionPatternRepository questionPatternRepository;
    private final QuestionPatternMapper questionPatternMapper;

    public QuestionService(QuestionRepository repository,
            QuestionMapper mapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit,
            DtoValidator validator,
            Clock clock,
            QuestionAcceptedAnswerRepository questionAcceptedAnswerRepository,
            QuestionAcceptedAnswerMapper questionAcceptedAnswerMapper,
            QuestionBlankRepository questionBlankRepository,
            QuestionBlankMapper questionBlankMapper,
            QuestionHotspotRepository questionHotspotRepository,
            QuestionHotspotMapper questionHotspotMapper,
            QuestionMatchPairRepository questionMatchPairRepository,
            QuestionMatchPairMapper questionMatchPairMapper,
            QuestionOptionRepository questionOptionRepository,
            QuestionOptionMapper questionOptionMapper,
            QuestionPatternRepository questionPatternRepository,
            QuestionPatternMapper questionPatternMapper) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.questionAcceptedAnswerRepository = questionAcceptedAnswerRepository;
        this.questionAcceptedAnswerMapper = questionAcceptedAnswerMapper;
        this.questionBlankRepository = questionBlankRepository;
        this.questionBlankMapper = questionBlankMapper;
        this.questionHotspotRepository = questionHotspotRepository;
        this.questionHotspotMapper = questionHotspotMapper;
        this.questionMatchPairRepository = questionMatchPairRepository;
        this.questionMatchPairMapper = questionMatchPairMapper;
        this.questionOptionRepository = questionOptionRepository;
        this.questionOptionMapper = questionOptionMapper;
        this.questionPatternRepository = questionPatternRepository;
        this.questionPatternMapper = questionPatternMapper;
    }

    public Question getQuestion(UUID id) {
        return doInReadTransaction(() -> {
            return mapper.toDto(loadVisible(DataScope.current(PERMISSION_GET), id));
        });
    }

    public Page<Question> listQuestions(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        SortSpec sort = SortSpec.parse(sortBy, QuestionRepository.SORT_FIELDS, QuestionRepository.DEFAULT_ORDER,
                QuestionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuestionRepository.FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> {
            Page<QuestionEntity> rows = repository.list(page, where);
            return rows.map(mapper::toDto);
        });
    }

    /**
     * Finds questions to add to a quiz: live and unretired, matching the text (title, text or code) and the types,
     * leaving out the questions already in {@code excludeQuizId}; newest first. Text of at most 200 characters, known
     * types only (422).
     */
    public Page<Question> searchQuestionBank(String query, List<String> questionTypes, UUID excludeQuizId,
            Integer pageNumber, Integer pageSize, String cursor) {
        List<FieldError> errors = new ArrayList<>();
        if (query != null && query.length() > MAX_SEARCH_TEXT) {
            errors.add(new FieldError("query", "must be at most " + MAX_SEARCH_TEXT + " characters", "Size"));
        }
        List<String> types = questionTypes == null ? List.of() : questionTypes;
        if (!QUESTION_TYPES.containsAll(types)) {
            errors.add(new FieldError("questionTypes", "must be among " + String.join(", ", QUESTION_TYPES),
                    "AllowedValues"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        SortSpec sort = SortSpec.parse(List.of("createTime,desc"), QuestionRepository.SORT_FIELDS,
                QuestionRepository.DEFAULT_ORDER, QuestionRepository.ID_COLUMN);
        SqlFragment where = QuestionBankSearch.of(query, types, excludeQuizId);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort,
                query + "|" + types + "|" + excludeQuizId, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public Question createQuestion(Question request, boolean validateOnly) {
        validator.validate(request);
        QuestionEntity entity = mapper.toEntity(request);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            assertValid(entity, true);
            scope.requireWritable(institutionOf(entity));
            assertUnique(entity);
            if (validateOnly) {
                return mapper.toDto(entity);
            }
            QuestionEntity stored = repository.insert(entity);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + LmsEvents.CREATED, AGGREGATE, stored.getId(), LmsEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public Question updateQuestion(UUID id, String ifMatch, QuestionUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_UPDATE);
            QuestionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = apply(entity, changes);
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
    public void deleteQuestion(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(PERMISSION_DELETE);
            QuestionEntity entity = loadVisible(scope, id);
            scope.requireWritable(institutionOf(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (repository.referencedByContentAuthorQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.content_author rows; remove those first.");
            }
            if (repository.referencedByQuestionSupersededById(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.question rows; remove those first.");
            }
            if (repository.referencedByQuestionAcceptedAnswerQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.question_accepted_answer rows; remove those first.");
            }
            if (repository.referencedByQuestionBlankQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.question_blank rows; remove those first.");
            }
            if (repository.referencedByQuestionHotspotQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.question_hotspot rows; remove those first.");
            }
            if (repository.referencedByQuestionMatchPairQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.question_match_pair rows; remove those first.");
            }
            if (repository.referencedByQuestionOptionQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.question_option rows; remove those first.");
            }
            if (repository.referencedByQuestionPatternQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.question_pattern rows; remove those first.");
            }
            if (repository.referencedByQuestionPoolItemQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.question_pool_item rows; remove those first.");
            }
            if (repository.referencedByQuizAnswerQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.quiz_answer rows; remove those first.");
            }
            if (repository.referencedByQuizQuestionQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.quiz_question rows; remove those first.");
            }
            if (repository.referencedByQuizQuestionHistoryQuestionId(id)) {
                throw new InvalidStateException(
                        "The question is still referenced by lms.quiz_question_history rows; remove those first.");
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
    private QuestionEntity loadVisible(DataScope scope, UUID id) {
        QuestionEntity entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(institutionOf(entity))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOf(QuestionEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> apply(QuestionEntity entity, QuestionUpdate changes) {
        Patches patch = new Patches();
        patch.apply("questionCode", changes.getQuestionCode(), entity::setQuestionCode);
        patch.apply("questionText", changes.getQuestionText(), entity::setQuestionText);
        patch.apply("questionType", changes.getQuestionType(), v -> entity.setQuestionType(v == null ? null : v.getValue()));
        patch.apply("allowsMultiple", changes.getAllowsMultiple(), entity::setAllowsMultiple);
        patch.apply("marks", changes.getMarks(), v -> entity.setMarks(WireValues.decimal(v)));
        patch.apply("title", changes.getTitle(), entity::setTitle);
        patch.apply("shuffleOptions", changes.getShuffleOptions(), entity::setShuffleOptions);
        patch.apply("optionLabelStyle", changes.getOptionLabelStyle(), entity::setOptionLabelStyle);
        patch.apply("maxSelections", changes.getMaxSelections(), entity::setMaxSelections);
        patch.apply("difficulty", changes.getDifficulty(), entity::setDifficulty);
        patch.apply("generalFeedback", changes.getGeneralFeedback(), entity::setGeneralFeedback);
        patch.apply("correctFeedback", changes.getCorrectFeedback(), entity::setCorrectFeedback);
        patch.apply("incorrectFeedback", changes.getIncorrectFeedback(), entity::setIncorrectFeedback);
        patch.apply("modelAnswer", changes.getModelAnswer(), entity::setModelAnswer);
        patch.apply("trueFalseAnswer", changes.getTrueFalseAnswer(), entity::setTrueFalseAnswer);
        patch.apply("numericAnswer", changes.getNumericAnswer(), entity::setNumericAnswer);
        patch.apply("numericTolerance", changes.getNumericTolerance(), entity::setNumericTolerance);
        patch.apply("numericIsRange", changes.getNumericIsRange(), entity::setNumericIsRange);
        patch.apply("caseSensitive", changes.getCaseSensitive(), entity::setCaseSensitive);
        patch.apply("allowSuperscript", changes.getAllowSuperscript(), entity::setAllowSuperscript);
        patch.apply("allowSubscript", changes.getAllowSubscript(), entity::setAllowSubscript);
        patch.apply("isRetired", changes.getIsRetired(), entity::setIsRetired);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValid(QuestionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (!creating && entity.getQuestionType() == null) {
            errors.add(new FieldError("questionType", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowsMultiple() == null) {
            errors.add(new FieldError("allowsMultiple", "must not be null", "NotNull"));
        }
        WireValues.checkDecimal("marks", WireValues.decimal(entity.getMarks()), 8, 2, errors);
        if (!creating && entity.getShuffleOptions() == null) {
            errors.add(new FieldError("shuffleOptions", "must not be null", "NotNull"));
        }
        if (!creating && entity.getNumericIsRange() == null) {
            errors.add(new FieldError("numericIsRange", "must not be null", "NotNull"));
        }
        if (!creating && entity.getCaseSensitive() == null) {
            errors.add(new FieldError("caseSensitive", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowSuperscript() == null) {
            errors.add(new FieldError("allowSuperscript", "must not be null", "NotNull"));
        }
        if (!creating && entity.getAllowSubscript() == null) {
            errors.add(new FieldError("allowSubscript", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsRetired() == null) {
            errors.add(new FieldError("isRetired", "must not be null", "NotNull"));
        }
        if (entity.getSupersededById() != null && !repository.existsSupersededById(entity.getSupersededById())) {
            errors.add(new FieldError("supersededById", "question does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUnique(QuestionEntity entity) {

    }

    // ---- QuestionAcceptedAnswer: a record of the Question aggregate ----

    public QuestionAcceptedAnswer getQuestionAcceptedAnswer(UUID questionId, UUID id) {
        return doInReadTransaction(() -> {
            return questionAcceptedAnswerMapper.toDto(loadVisibleQuestionAcceptedAnswer(DataScope.current(
                    QUESTION_ACCEPTED_ANSWER_PERMISSION_GET), questionId, id));
        });
    }

    public Page<QuestionAcceptedAnswer> listQuestionAcceptedAnswers(UUID questionId, Integer pageNumber,
            Integer pageSize, String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuestionAcceptedAnswerRepository.SORT_FIELDS,
                        QuestionAcceptedAnswerRepository.DEFAULT_ORDER,
                QuestionAcceptedAnswerRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuestionAcceptedAnswerRepository.FILTER_FIELDS)
                .and(QuestionAcceptedAnswerRepository.ofParent(questionId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + questionId, clock);
        return doInReadTransaction(() -> {
            Page<QuestionAcceptedAnswerEntity> rows = questionAcceptedAnswerRepository.list(page, where);
            return rows.map(questionAcceptedAnswerMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public QuestionAcceptedAnswer createQuestionAcceptedAnswer(UUID questionId, QuestionAcceptedAnswer request,
            boolean validateOnly) {
        validator.validate(request);
        QuestionAcceptedAnswerEntity entity = questionAcceptedAnswerMapper.toEntity(request);
        entity.setQuestionId(questionId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(QUESTION_ACCEPTED_ANSWER_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!questionAcceptedAnswerRepository.existsQuestionId(entity.getQuestionId())) {
                throw new ResourceNotFoundException("lms.question", entity.getQuestionId());
            }
            assertValidQuestionAcceptedAnswer(entity, true);
            scope.requireWritable(institutionOfQuestionAcceptedAnswer(entity));
            assertUniqueQuestionAcceptedAnswer(entity);
            if (validateOnly) {
                return questionAcceptedAnswerMapper.toDto(entity);
            }
            QuestionAcceptedAnswerEntity stored = questionAcceptedAnswerRepository.insert(entity);
            audit.recordCreate(QUESTION_ACCEPTED_ANSWER_AGGREGATE, stored.getId());
            outbox.publish(QUESTION_ACCEPTED_ANSWER_AGGREGATE + LmsEvents.CREATED, QUESTION_ACCEPTED_ANSWER_AGGREGATE,
                    stored.getId(), LmsEvents.payload(stored.getId()));
            return questionAcceptedAnswerMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public QuestionAcceptedAnswer updateQuestionAcceptedAnswer(UUID questionId, UUID id, String ifMatch,
            QuestionAcceptedAnswerUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_ACCEPTED_ANSWER_PERMISSION_UPDATE);
            QuestionAcceptedAnswerEntity entity = loadVisibleQuestionAcceptedAnswer(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionAcceptedAnswer(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyQuestionAcceptedAnswer(entity, changes);
            validator.validate(questionAcceptedAnswerMapper.toDto(entity));
            assertValidQuestionAcceptedAnswer(entity, false);
            scope.requireWritable(institutionOfQuestionAcceptedAnswer(entity));
            assertUniqueQuestionAcceptedAnswer(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return questionAcceptedAnswerMapper.toDto(entity);
            }
            if (questionAcceptedAnswerRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(QUESTION_ACCEPTED_ANSWER_AGGREGATE, id, fields);
            outbox.publish(QUESTION_ACCEPTED_ANSWER_AGGREGATE + LmsEvents.UPDATED, QUESTION_ACCEPTED_ANSWER_AGGREGATE, id,
                    LmsEvents.payload(id));
            return questionAcceptedAnswerMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteQuestionAcceptedAnswer(UUID questionId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_ACCEPTED_ANSWER_PERMISSION_DELETE);
            QuestionAcceptedAnswerEntity entity = loadVisibleQuestionAcceptedAnswer(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionAcceptedAnswer(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (questionAcceptedAnswerRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(QUESTION_ACCEPTED_ANSWER_AGGREGATE, id);
            outbox.publish(QUESTION_ACCEPTED_ANSWER_AGGREGATE + LmsEvents.DELETED, QUESTION_ACCEPTED_ANSWER_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuestionAcceptedAnswerEntity loadVisibleQuestionAcceptedAnswer(DataScope scope, UUID questionId, UUID id) {
        QuestionAcceptedAnswerEntity entity = questionAcceptedAnswerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUESTION_ACCEPTED_ANSWER_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuestionAcceptedAnswer(entity)) || !questionId.equals(entity.getQuestionId())) {
            throw new ResourceNotFoundException(QUESTION_ACCEPTED_ANSWER_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfQuestionAcceptedAnswer(QuestionAcceptedAnswerEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyQuestionAcceptedAnswer(QuestionAcceptedAnswerEntity entity,
            QuestionAcceptedAnswerUpdate changes) {
        Patches patch = new Patches();
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("answerText", changes.getAnswerText(), entity::setAnswerText);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuestionAcceptedAnswer(QuestionAcceptedAnswerEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuestionId() == null) {
            errors.add(new FieldError("questionId", "must not be null", "NotNull"));
        }
        if (entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getAnswerText() == null) {
            errors.add(new FieldError("answerText", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() != null && !questionAcceptedAnswerRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuestionAcceptedAnswer(QuestionAcceptedAnswerEntity entity) {
        if (questionAcceptedAnswerRepository.takenQuestionIdSortOrder1(entity)) {
            throw new DuplicateResourceException(
                    "A question accepted answer with the same question id and sort order already exists.");
        }
    }

    // ---- QuestionBlank: a record of the Question aggregate ----

    public QuestionBlank getQuestionBlank(UUID questionId, UUID id) {
        return doInReadTransaction(() -> {
            return questionBlankMapper.toDto(loadVisibleQuestionBlank(DataScope.current(QUESTION_BLANK_PERMISSION_GET),
                    questionId, id));
        });
    }

    public Page<QuestionBlank> listQuestionBlanks(UUID questionId, Integer pageNumber, Integer pageSize, String cursor,
            String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuestionBlankRepository.SORT_FIELDS, QuestionBlankRepository.DEFAULT_ORDER,
                QuestionBlankRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuestionBlankRepository.FILTER_FIELDS)
                .and(QuestionBlankRepository.ofParent(questionId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + questionId, clock);
        return doInReadTransaction(() -> {
            Page<QuestionBlankEntity> rows = questionBlankRepository.list(page, where);
            return rows.map(questionBlankMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public QuestionBlank createQuestionBlank(UUID questionId, QuestionBlank request, boolean validateOnly) {
        validator.validate(request);
        QuestionBlankEntity entity = questionBlankMapper.toEntity(request);
        entity.setQuestionId(questionId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(QUESTION_BLANK_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!questionBlankRepository.existsQuestionId(entity.getQuestionId())) {
                throw new ResourceNotFoundException("lms.question", entity.getQuestionId());
            }
            assertValidQuestionBlank(entity, true);
            scope.requireWritable(institutionOfQuestionBlank(entity));
            assertUniqueQuestionBlank(entity);
            if (validateOnly) {
                return questionBlankMapper.toDto(entity);
            }
            QuestionBlankEntity stored = questionBlankRepository.insert(entity);
            audit.recordCreate(QUESTION_BLANK_AGGREGATE, stored.getId());
            outbox.publish(QUESTION_BLANK_AGGREGATE + LmsEvents.CREATED, QUESTION_BLANK_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return questionBlankMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public QuestionBlank updateQuestionBlank(UUID questionId, UUID id, String ifMatch, QuestionBlankUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_BLANK_PERMISSION_UPDATE);
            QuestionBlankEntity entity = loadVisibleQuestionBlank(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionBlank(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyQuestionBlank(entity, changes);
            validator.validate(questionBlankMapper.toDto(entity));
            assertValidQuestionBlank(entity, false);
            scope.requireWritable(institutionOfQuestionBlank(entity));
            assertUniqueQuestionBlank(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return questionBlankMapper.toDto(entity);
            }
            if (questionBlankRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(QUESTION_BLANK_AGGREGATE, id, fields);
            outbox.publish(QUESTION_BLANK_AGGREGATE + LmsEvents.UPDATED, QUESTION_BLANK_AGGREGATE, id, LmsEvents.payload(id));
            return questionBlankMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteQuestionBlank(UUID questionId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_BLANK_PERMISSION_DELETE);
            QuestionBlankEntity entity = loadVisibleQuestionBlank(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionBlank(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (questionBlankRepository.referencedByQuestionBlankAnswerQuestionBlankId(id)) {
                throw new InvalidStateException(
                        "The question blank is still referenced by lms.question_blank_answer rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (questionBlankRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(QUESTION_BLANK_AGGREGATE, id);
            outbox.publish(QUESTION_BLANK_AGGREGATE + LmsEvents.DELETED, QUESTION_BLANK_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuestionBlankEntity loadVisibleQuestionBlank(DataScope scope, UUID questionId, UUID id) {
        QuestionBlankEntity entity = questionBlankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUESTION_BLANK_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuestionBlank(entity)) || !questionId.equals(entity.getQuestionId())) {
            throw new ResourceNotFoundException(QUESTION_BLANK_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfQuestionBlank(QuestionBlankEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyQuestionBlank(QuestionBlankEntity entity, QuestionBlankUpdate changes) {
        Patches patch = new Patches();
        patch.apply("blankText", changes.getBlankText(), entity::setBlankText);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuestionBlank(QuestionBlankEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuestionId() == null) {
            errors.add(new FieldError("questionId", "must not be null", "NotNull"));
        }
        if (entity.getBlankText() == null) {
            errors.add(new FieldError("blankText", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() != null && !questionBlankRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuestionBlank(QuestionBlankEntity entity) {

    }

    // ---- QuestionHotspot: a record of the Question aggregate ----

    public QuestionHotspot getQuestionHotspot(UUID questionId, UUID id) {
        return doInReadTransaction(() -> {
            return questionHotspotMapper.toDto(loadVisibleQuestionHotspot(DataScope.current(QUESTION_HOTSPOT_PERMISSION_GET),
                    questionId, id));
        });
    }

    public Page<QuestionHotspot> listQuestionHotspots(UUID questionId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuestionHotspotRepository.SORT_FIELDS, QuestionHotspotRepository.DEFAULT_ORDER,
                QuestionHotspotRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuestionHotspotRepository.FILTER_FIELDS)
                .and(QuestionHotspotRepository.ofParent(questionId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + questionId, clock);
        return doInReadTransaction(() -> {
            Page<QuestionHotspotEntity> rows = questionHotspotRepository.list(page, where);
            return rows.map(questionHotspotMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public QuestionHotspot createQuestionHotspot(UUID questionId, QuestionHotspot request, boolean validateOnly) {
        validator.validate(request);
        QuestionHotspotEntity entity = questionHotspotMapper.toEntity(request);
        entity.setQuestionId(questionId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(QUESTION_HOTSPOT_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!questionHotspotRepository.existsQuestionId(entity.getQuestionId())) {
                throw new ResourceNotFoundException("lms.question", entity.getQuestionId());
            }
            assertValidQuestionHotspot(entity, true);
            scope.requireWritable(institutionOfQuestionHotspot(entity));
            assertUniqueQuestionHotspot(entity);
            if (validateOnly) {
                return questionHotspotMapper.toDto(entity);
            }
            QuestionHotspotEntity stored = questionHotspotRepository.insert(entity);
            audit.recordCreate(QUESTION_HOTSPOT_AGGREGATE, stored.getId());
            outbox.publish(QUESTION_HOTSPOT_AGGREGATE + LmsEvents.CREATED, QUESTION_HOTSPOT_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return questionHotspotMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public QuestionHotspot updateQuestionHotspot(UUID questionId, UUID id, String ifMatch,
            QuestionHotspotUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_HOTSPOT_PERMISSION_UPDATE);
            QuestionHotspotEntity entity = loadVisibleQuestionHotspot(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionHotspot(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyQuestionHotspot(entity, changes);
            validator.validate(questionHotspotMapper.toDto(entity));
            assertValidQuestionHotspot(entity, false);
            scope.requireWritable(institutionOfQuestionHotspot(entity));
            assertUniqueQuestionHotspot(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return questionHotspotMapper.toDto(entity);
            }
            if (questionHotspotRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(QUESTION_HOTSPOT_AGGREGATE, id, fields);
            outbox.publish(QUESTION_HOTSPOT_AGGREGATE + LmsEvents.UPDATED, QUESTION_HOTSPOT_AGGREGATE, id, LmsEvents.payload(id));
            return questionHotspotMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteQuestionHotspot(UUID questionId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_HOTSPOT_PERMISSION_DELETE);
            QuestionHotspotEntity entity = loadVisibleQuestionHotspot(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionHotspot(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (questionHotspotRepository.referencedByQuestionHotspotRegionQuestionHotspotId(id)) {
                throw new InvalidStateException(
                        "The question hotspot is still referenced by lms.question_hotspot_region rows; remove those "
                                + "first.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (questionHotspotRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(QUESTION_HOTSPOT_AGGREGATE, id);
            outbox.publish(QUESTION_HOTSPOT_AGGREGATE + LmsEvents.DELETED, QUESTION_HOTSPOT_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuestionHotspotEntity loadVisibleQuestionHotspot(DataScope scope, UUID questionId, UUID id) {
        QuestionHotspotEntity entity = questionHotspotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUESTION_HOTSPOT_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuestionHotspot(entity)) || !questionId.equals(entity.getQuestionId())) {
            throw new ResourceNotFoundException(QUESTION_HOTSPOT_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfQuestionHotspot(QuestionHotspotEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyQuestionHotspot(QuestionHotspotEntity entity, QuestionHotspotUpdate changes) {
        Patches patch = new Patches();
        patch.apply("hotspotType", changes.getHotspotType(), entity::setHotspotType);
        patch.apply("imageFileId", changes.getImageFileId(), entity::setImageFileId);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuestionHotspot(QuestionHotspotEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuestionId() == null) {
            errors.add(new FieldError("questionId", "must not be null", "NotNull"));
        }
        if (entity.getHotspotType() == null) {
            errors.add(new FieldError("hotspotType", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() != null && !questionHotspotRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuestionHotspot(QuestionHotspotEntity entity) {

    }

    // ---- QuestionMatchPair: a record of the Question aggregate ----

    public QuestionMatchPair getQuestionMatchPair(UUID questionId, UUID id) {
        return doInReadTransaction(() -> {
            return questionMatchPairMapper.toDto(loadVisibleQuestionMatchPair(DataScope.current(
                    QUESTION_MATCH_PAIR_PERMISSION_GET), questionId, id));
        });
    }

    public Page<QuestionMatchPair> listQuestionMatchPairs(UUID questionId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuestionMatchPairRepository.SORT_FIELDS,
                        QuestionMatchPairRepository.DEFAULT_ORDER,
                QuestionMatchPairRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuestionMatchPairRepository.FILTER_FIELDS)
                .and(QuestionMatchPairRepository.ofParent(questionId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + questionId, clock);
        return doInReadTransaction(() -> {
            Page<QuestionMatchPairEntity> rows = questionMatchPairRepository.list(page, where);
            return rows.map(questionMatchPairMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public QuestionMatchPair createQuestionMatchPair(UUID questionId, QuestionMatchPair request, boolean validateOnly) {
        validator.validate(request);
        QuestionMatchPairEntity entity = questionMatchPairMapper.toEntity(request);
        entity.setQuestionId(questionId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(QUESTION_MATCH_PAIR_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!questionMatchPairRepository.existsQuestionId(entity.getQuestionId())) {
                throw new ResourceNotFoundException("lms.question", entity.getQuestionId());
            }
            assertValidQuestionMatchPair(entity, true);
            scope.requireWritable(institutionOfQuestionMatchPair(entity));
            assertUniqueQuestionMatchPair(entity);
            if (validateOnly) {
                return questionMatchPairMapper.toDto(entity);
            }
            QuestionMatchPairEntity stored = questionMatchPairRepository.insert(entity);
            audit.recordCreate(QUESTION_MATCH_PAIR_AGGREGATE, stored.getId());
            outbox.publish(QUESTION_MATCH_PAIR_AGGREGATE + LmsEvents.CREATED, QUESTION_MATCH_PAIR_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return questionMatchPairMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public QuestionMatchPair updateQuestionMatchPair(UUID questionId, UUID id, String ifMatch,
            QuestionMatchPairUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_MATCH_PAIR_PERMISSION_UPDATE);
            QuestionMatchPairEntity entity = loadVisibleQuestionMatchPair(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionMatchPair(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyQuestionMatchPair(entity, changes);
            validator.validate(questionMatchPairMapper.toDto(entity));
            assertValidQuestionMatchPair(entity, false);
            scope.requireWritable(institutionOfQuestionMatchPair(entity));
            assertUniqueQuestionMatchPair(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return questionMatchPairMapper.toDto(entity);
            }
            if (questionMatchPairRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(QUESTION_MATCH_PAIR_AGGREGATE, id, fields);
            outbox.publish(QUESTION_MATCH_PAIR_AGGREGATE + LmsEvents.UPDATED, QUESTION_MATCH_PAIR_AGGREGATE, id,
                    LmsEvents.payload(id));
            return questionMatchPairMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteQuestionMatchPair(UUID questionId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_MATCH_PAIR_PERMISSION_DELETE);
            QuestionMatchPairEntity entity = loadVisibleQuestionMatchPair(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionMatchPair(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (questionMatchPairRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(QUESTION_MATCH_PAIR_AGGREGATE, id);
            outbox.publish(QUESTION_MATCH_PAIR_AGGREGATE + LmsEvents.DELETED, QUESTION_MATCH_PAIR_AGGREGATE, id,
                    LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuestionMatchPairEntity loadVisibleQuestionMatchPair(DataScope scope, UUID questionId, UUID id) {
        QuestionMatchPairEntity entity = questionMatchPairRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUESTION_MATCH_PAIR_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuestionMatchPair(entity)) || !questionId.equals(entity.getQuestionId())) {
            throw new ResourceNotFoundException(QUESTION_MATCH_PAIR_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfQuestionMatchPair(QuestionMatchPairEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyQuestionMatchPair(QuestionMatchPairEntity entity, QuestionMatchPairUpdate changes) {
        Patches patch = new Patches();
        patch.apply("leftText", changes.getLeftText(), entity::setLeftText);
        patch.apply("rightText", changes.getRightText(), entity::setRightText);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuestionMatchPair(QuestionMatchPairEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuestionId() == null) {
            errors.add(new FieldError("questionId", "must not be null", "NotNull"));
        }
        if (entity.getLeftText() == null) {
            errors.add(new FieldError("leftText", "must not be null", "NotNull"));
        }
        if (entity.getRightText() == null) {
            errors.add(new FieldError("rightText", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() != null && !questionMatchPairRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuestionMatchPair(QuestionMatchPairEntity entity) {

    }

    // ---- QuestionOption: a record of the Question aggregate ----

    public QuestionOption getQuestionOption(UUID questionId, UUID id) {
        return doInReadTransaction(() -> {
            return questionOptionMapper.toDto(loadVisibleQuestionOption(DataScope.current(QUESTION_OPTION_PERMISSION_GET),
                    questionId, id));
        });
    }

    public Page<QuestionOption> listQuestionOptions(UUID questionId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuestionOptionRepository.SORT_FIELDS, QuestionOptionRepository.DEFAULT_ORDER,
                QuestionOptionRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuestionOptionRepository.FILTER_FIELDS)
                .and(QuestionOptionRepository.ofParent(questionId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + questionId, clock);
        return doInReadTransaction(() -> {
            Page<QuestionOptionEntity> rows = questionOptionRepository.list(page, where);
            return rows.map(questionOptionMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public QuestionOption createQuestionOption(UUID questionId, QuestionOption request, boolean validateOnly) {
        validator.validate(request);
        QuestionOptionEntity entity = questionOptionMapper.toEntity(request);
        entity.setQuestionId(questionId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(QUESTION_OPTION_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!questionOptionRepository.existsQuestionId(entity.getQuestionId())) {
                throw new ResourceNotFoundException("lms.question", entity.getQuestionId());
            }
            assertValidQuestionOption(entity, true);
            scope.requireWritable(institutionOfQuestionOption(entity));
            assertUniqueQuestionOption(entity);
            if (validateOnly) {
                return questionOptionMapper.toDto(entity);
            }
            QuestionOptionEntity stored = questionOptionRepository.insert(entity);
            audit.recordCreate(QUESTION_OPTION_AGGREGATE, stored.getId());
            outbox.publish(QUESTION_OPTION_AGGREGATE + LmsEvents.CREATED, QUESTION_OPTION_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return questionOptionMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public QuestionOption updateQuestionOption(UUID questionId, UUID id, String ifMatch, QuestionOptionUpdate changes,
            boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_OPTION_PERMISSION_UPDATE);
            QuestionOptionEntity entity = loadVisibleQuestionOption(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionOption(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyQuestionOption(entity, changes);
            validator.validate(questionOptionMapper.toDto(entity));
            assertValidQuestionOption(entity, false);
            scope.requireWritable(institutionOfQuestionOption(entity));
            assertUniqueQuestionOption(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return questionOptionMapper.toDto(entity);
            }
            if (questionOptionRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(QUESTION_OPTION_AGGREGATE, id, fields);
            outbox.publish(QUESTION_OPTION_AGGREGATE + LmsEvents.UPDATED, QUESTION_OPTION_AGGREGATE, id, LmsEvents.payload(id));
            return questionOptionMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteQuestionOption(UUID questionId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_OPTION_PERMISSION_DELETE);
            QuestionOptionEntity entity = loadVisibleQuestionOption(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionOption(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (questionOptionRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(QUESTION_OPTION_AGGREGATE, id);
            outbox.publish(QUESTION_OPTION_AGGREGATE + LmsEvents.DELETED, QUESTION_OPTION_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuestionOptionEntity loadVisibleQuestionOption(DataScope scope, UUID questionId, UUID id) {
        QuestionOptionEntity entity = questionOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUESTION_OPTION_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuestionOption(entity)) || !questionId.equals(entity.getQuestionId())) {
            throw new ResourceNotFoundException(QUESTION_OPTION_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfQuestionOption(QuestionOptionEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyQuestionOption(QuestionOptionEntity entity, QuestionOptionUpdate changes) {
        Patches patch = new Patches();
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        patch.apply("optionText", changes.getOptionText(), entity::setOptionText);
        patch.apply("isCorrect", changes.getIsCorrect(), entity::setIsCorrect);
        patch.apply("explanation", changes.getExplanation(), entity::setExplanation);
        patch.apply("label", changes.getLabel(), entity::setLabel);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuestionOption(QuestionOptionEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuestionId() == null) {
            errors.add(new FieldError("questionId", "must not be null", "NotNull"));
        }
        if (entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getOptionText() == null) {
            errors.add(new FieldError("optionText", "must not be null", "NotNull"));
        }
        if (!creating && entity.getIsCorrect() == null) {
            errors.add(new FieldError("isCorrect", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() != null && !questionOptionRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuestionOption(QuestionOptionEntity entity) {
        if (questionOptionRepository.takenQuestionIdSortOrder1(entity)) {
            throw new DuplicateResourceException(
                    "A question option with the same question id and sort order already exists.");
        }
    }

    // ---- QuestionPattern: a record of the Question aggregate ----

    public QuestionPattern getQuestionPattern(UUID questionId, UUID id) {
        return doInReadTransaction(() -> {
            return questionPatternMapper.toDto(loadVisibleQuestionPattern(DataScope.current(QUESTION_PATTERN_PERMISSION_GET),
                    questionId, id));
        });
    }

    public Page<QuestionPattern> listQuestionPatterns(UUID questionId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        SortSpec sort =
                SortSpec.parse(sortBy, QuestionPatternRepository.SORT_FIELDS, QuestionPatternRepository.DEFAULT_ORDER,
                QuestionPatternRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, QuestionPatternRepository.FILTER_FIELDS)
                .and(QuestionPatternRepository.ofParent(questionId));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + questionId, clock);
        return doInReadTransaction(() -> {
            Page<QuestionPatternEntity> rows = questionPatternRepository.list(page, where);
            return rows.map(questionPatternMapper::toDto);
        });
    }

    /** Creates the row; with validateOnly every check runs and nothing is written. */
    public QuestionPattern createQuestionPattern(UUID questionId, QuestionPattern request, boolean validateOnly) {
        validator.validate(request);
        QuestionPatternEntity entity = questionPatternMapper.toEntity(request);
        entity.setQuestionId(questionId);
        entity.onCreatingData(Actor.current(), clock);
        entity.setTenantId(CallContextHolder.require().tenantId().orElseThrow());
        DataScope scope = DataScope.current(QUESTION_PATTERN_PERMISSION_CREATE);
        return doInWriteTransaction(() -> {
            if (!questionPatternRepository.existsQuestionId(entity.getQuestionId())) {
                throw new ResourceNotFoundException("lms.question", entity.getQuestionId());
            }
            assertValidQuestionPattern(entity, true);
            scope.requireWritable(institutionOfQuestionPattern(entity));
            assertUniqueQuestionPattern(entity);
            if (validateOnly) {
                return questionPatternMapper.toDto(entity);
            }
            QuestionPatternEntity stored = questionPatternRepository.insert(entity);
            audit.recordCreate(QUESTION_PATTERN_AGGREGATE, stored.getId());
            outbox.publish(QUESTION_PATTERN_AGGREGATE + LmsEvents.CREATED, QUESTION_PATTERN_AGGREGATE, stored.getId(),
                    LmsEvents.payload(stored.getId()));
            return questionPatternMapper.toDto(stored);
        });
    }

    /**
     * Partial update under If-Match (§7.2): a field the request leaves out is kept, a null one is cleared and a value
     * is set; the merged row must validate and stay in the caller's scope.
     */
    public QuestionPattern updateQuestionPattern(UUID questionId, UUID id, String ifMatch,
            QuestionPatternUpdate changes, boolean validateOnly) {
        int expected = Etags.expectedVersion(ifMatch);
        return doInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_PATTERN_PERMISSION_UPDATE);
            QuestionPatternEntity entity = loadVisibleQuestionPattern(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionPattern(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            List<String> fields = applyQuestionPattern(entity, changes);
            validator.validate(questionPatternMapper.toDto(entity));
            assertValidQuestionPattern(entity, false);
            scope.requireWritable(institutionOfQuestionPattern(entity));
            assertUniqueQuestionPattern(entity);
            entity.onUpdatingData(Actor.current(), clock);
            if (validateOnly) {
                return questionPatternMapper.toDto(entity);
            }
            if (questionPatternRepository.update(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.setRowVersion(expected + 1);
            audit.recordUpdate(QUESTION_PATTERN_AGGREGATE, id, fields);
            outbox.publish(QUESTION_PATTERN_AGGREGATE + LmsEvents.UPDATED, QUESTION_PATTERN_AGGREGATE, id, LmsEvents.payload(id));
            return questionPatternMapper.toDto(entity);
        });
    }

    /** Soft delete under If-Match; refused while live rows of this module still reference the row. */
    public void deleteQuestionPattern(UUID questionId, UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        runInWriteTransaction(() -> {
            DataScope scope = DataScope.current(QUESTION_PATTERN_PERMISSION_DELETE);
            QuestionPatternEntity entity = loadVisibleQuestionPattern(scope, questionId, id);
            scope.requireWritable(institutionOfQuestionPattern(entity));
            if (entity.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            entity.onDeletingData(Actor.current(), clock);
            if (questionPatternRepository.softDelete(entity, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            audit.recordDelete(QUESTION_PATTERN_AGGREGATE, id);
            outbox.publish(QUESTION_PATTERN_AGGREGATE + LmsEvents.DELETED, QUESTION_PATTERN_AGGREGATE, id, LmsEvents.payload(id));
        });
    }

    /** The live row, if the caller may see it: another parent's row, or a row outside the caller's scope, is 404. */
    private QuestionPatternEntity loadVisibleQuestionPattern(DataScope scope, UUID questionId, UUID id) {
        QuestionPatternEntity entity = questionPatternRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(QUESTION_PATTERN_AGGREGATE, id));
        if (!scope.canRead(institutionOfQuestionPattern(entity)) || !questionId.equals(entity.getQuestionId())) {
            throw new ResourceNotFoundException(QUESTION_PATTERN_AGGREGATE, id);
        }
        return entity;
    }

    /** No institution: the rows are tenant-level. */
    private Optional<UUID> institutionOfQuestionPattern(QuestionPatternEntity entity) {
        return Optional.empty();
    }

    /** Applies the fields the request carries (§7.2); answers their names for the audit record. */
    private static List<String> applyQuestionPattern(QuestionPatternEntity entity, QuestionPatternUpdate changes) {
        Patches patch = new Patches();
        patch.apply("modelAnswer", changes.getModelAnswer(), entity::setModelAnswer);
        patch.apply("pattern", changes.getPattern(), entity::setPattern);
        patch.apply("sortOrder", changes.getSortOrder(), entity::setSortOrder);
        return patch.applied();
    }

    /** Business invariants beyond the field shapes the contract validates (422 with every failing field). */
    private void assertValidQuestionPattern(QuestionPatternEntity entity, boolean creating) {
        List<FieldError> errors = new ArrayList<>();
        if (entity.getQuestionId() == null) {
            errors.add(new FieldError("questionId", "must not be null", "NotNull"));
        }
        if (entity.getModelAnswer() == null) {
            errors.add(new FieldError("modelAnswer", "must not be null", "NotNull"));
        }
        if (entity.getPattern() == null) {
            errors.add(new FieldError("pattern", "must not be null", "NotNull"));
        }
        if (!creating && entity.getSortOrder() == null) {
            errors.add(new FieldError("sortOrder", "must not be null", "NotNull"));
        }
        if (entity.getQuestionId() != null && !questionPatternRepository.existsQuestionId(entity.getQuestionId())) {
            errors.add(new FieldError("questionId", "question does not exist", "NOT_FOUND"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    private void assertUniqueQuestionPattern(QuestionPatternEntity entity) {

    }
}
