package com.mysms.lms.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
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
import com.mysms.core.validation.DtoValidator;
import com.mysms.core.web.Etags;
import com.mysms.lms.api.model.PublishEbookCurationRequest;
import com.mysms.lms.api.model.RelatedMaterial;
import com.mysms.lms.api.model.RelatedMaterialList;
import com.mysms.lms.api.model.RelatedMaterialRank;
import com.mysms.lms.config.LmsDatabaseConfig;
import com.mysms.lms.config.LmsEventsConfig;
import com.mysms.lms.domain.EbookEntity;
import com.mysms.lms.events.LmsEvents;
import com.mysms.lms.mapper.RelatedMaterialMapper;
import com.mysms.lms.repository.EbookCurationRepository;
import com.mysms.lms.repository.EbookCurationRepository.EbookHead;
import com.mysms.lms.repository.EbookCurationRepository.RankRow;
import com.mysms.lms.repository.RelatedMaterialRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The workflows of an e-book's related materials: a teacher's curation
 * (PublishEbookCuration), the materials a student is shown (GetMyRelatedMaterials) and the personal rankings kept for
 * students (ListRelatedMaterialRanks, GetRelatedMaterialRank).
 *
 * <p>Curation orders the chosen materials 1..n (their initial rank), leaves the others out of what students are shown,
 * keeps the order as the e-book's seed list and switches the related materials on. It applies to the e-book wherever
 * it is placed: a named cohort is checked (it exists and is the e-book's school's, 422) but keeps no curation of its
 * own. A student is shown their personal ranking for the course and document when one exists, else the curation
 * (every material when nothing is curated). A personal ranking is personal data: it is read by its student, or by
 * whoever may edit the e-book in its school; anyone else reads none (404 for one).
 */
@Service
public class EbookWorkflowService extends AppTransactionManager {

    static final String AGGREGATE = EbookEntity.TABLE;
    static final String RANK_AGGREGATE = "lms.related_material_rank";
    static final String PERMISSION_CURATE = "lms.ebook.publish_ebook_curation";
    static final String PERMISSION_MY_MATERIALS = "lms.ebook.get_my_related_materials";
    static final String PERMISSION_RANKS = "lms.related_material_rank.read";
    /** Who may edit the e-book also reads its students' rankings. */
    static final String PERMISSION_EBOOK_OFFICER = "lms.ebook.update";
    static final int MAX_CURATED = 200;

    private final EbookCurationRepository repository;
    private final RelatedMaterialRepository materials;
    private final RelatedMaterialMapper materialMapper;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;

    public EbookWorkflowService(EbookCurationRepository repository, RelatedMaterialRepository materials,
            RelatedMaterialMapper materialMapper,
            @Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LmsDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LmsEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(LmsEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.materials = materials;
        this.materialMapper = materialMapper;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
    }

    public RelatedMaterialList publishEbookCuration(UUID id, PublishEbookCurationRequest request) {
        validator.validate(request);
        DataScope scope = DataScope.current(PERMISSION_CURATE);
        UUID actor = AuthContext.require().userId();
        return doInWriteTransaction(() -> {
            EbookHead ebook = visible(scope, repository.lock(id), id);
            scope.requireWritable(Optional.ofNullable(ebook.institutionId()));
            List<FieldError> errors = new ArrayList<>();
            List<UUID> ordered = new ArrayList<>(new LinkedHashSet<>(request.getRelatedMaterialIds()));
            if (ordered.size() != request.getRelatedMaterialIds().size()) {
                errors.add(new FieldError("relatedMaterialIds", "a material appears more than once", "DUPLICATE"));
            }
            if (ordered.size() > MAX_CURATED) {
                errors.add(new FieldError("relatedMaterialIds", "at most " + MAX_CURATED + " materials", "Size"));
            }
            Set<UUID> own = new HashSet<>(repository.materials(id));
            ordered.stream().filter(m -> !own.contains(m)).forEach(m -> errors.add(new FieldError(
                    "relatedMaterialIds", "material " + m + " is not a related material of this e-book",
                    "NOT_FOUND")));
            if (request.getCohortId() != null) {
                if (!repository.cohortExists(request.getCohortId())) {
                    errors.add(new FieldError("cohortId", "cohort does not exist", "NOT_FOUND"));
                } else if (ebook.institutionId() != null && !ebook.institutionId()
                        .equals(repository.cohortInstitution(request.getCohortId()).orElse(null))) {
                    errors.add(new FieldError("cohortId", "is not a cohort of the e-book's school", "INVALID"));
                }
            }
            if (!errors.isEmpty()) {
                throw new ValidationFailedException(errors);
            }
            OffsetDateTime now = now();
            repository.curate(id, ordered, now, actor);
            String seed = ordered.stream().map(UUID::toString).collect(Collectors.joining(","));
            if (repository.setSeed(id, ebook.rowVersion(), seed.isEmpty() ? null : seed, !ordered.isEmpty(), now,
                    actor) == 0) {
                throw new PreconditionFailedException("The e-book changed meanwhile; read it again.");
            }
            audit.recordTransition(AGGREGATE, id, "publishCuration", List.of("relatedMaterialSeed",
                    "showRelatedMaterials"), null);
            outbox.publish(AGGREGATE + ".publishCuration", AGGREGATE, id, LmsEvents.payload(id));
            Map<UUID, RelatedMaterial> found = materials(ordered);
            RelatedMaterialList list = new RelatedMaterialList();
            list.setItems(ordered.stream().map(found::get).toList());
            return list;
        });
    }

    public Page<RelatedMaterial> getMyRelatedMaterials(UUID id, UUID courseId, UUID fileId, Integer pageNumber,
            Integer pageSize, String cursor) {
        DataScope scope = DataScope.current(PERMISSION_MY_MATERIALS);
        UUID student = AuthContext.require().userId();
        SortSpec sort = SortSpec.parse(null, EbookCurationRepository.SHOWN_SORT_FIELDS, "position", "r.id");
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, id + "|" + courseId + "|" + fileId,
                clock);
        return doInReadTransaction(() -> {
            EbookHead ebook = visible(scope, repository.head(id), id);
            if (!ebook.showRelatedMaterials()) {
                return new Page<RelatedMaterial>(List.of(), page.page(), page.size(), "");
            }
            Optional<UUID> rank = repository.myRank(student, id, courseId, fileId);
            Page<EbookCurationRepository.Shown> shown = repository.shown(page, id, rank);
            Map<UUID, RelatedMaterial> found = materials(shown.items().stream()
                    .map(EbookCurationRepository.Shown::id).toList());
            return shown.map(row -> found.get(row.id()));
        });
    }

    public Page<RelatedMaterialRank> listRelatedMaterialRanks(UUID ebookId, Integer pageNumber, Integer pageSize,
            String cursor, String filter, List<String> sortBy) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.of(caller, PERMISSION_RANKS);
        SortSpec sort = SortSpec.parse(sortBy, EbookCurationRepository.RANK_SORT_FIELDS, "createTime desc", "r.id");
        var where = FilterParser.parse(filter, EbookCurationRepository.RANK_FILTER_FIELDS);
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter + "|" + ebookId, clock);
        return doInReadTransaction(() -> {
            EbookHead ebook = visible(scope, repository.head(ebookId), ebookId);
            return repository.ranks(page, ebookId, where, officer(caller, ebook), caller.userId())
                    .map(EbookWorkflowService::rankDto);
        });
    }

    public RelatedMaterialRank getRelatedMaterialRank(UUID ebookId, UUID id) {
        AuthContext caller = AuthContext.require();
        DataScope scope = DataScope.of(caller, PERMISSION_RANKS);
        return doInReadTransaction(() -> {
            EbookHead ebook = visible(scope, repository.head(ebookId), ebookId);
            RankRow row = repository.rank(ebookId, id)
                    .filter(r -> r.userId().equals(caller.userId()) || officer(caller, ebook))
                    .orElseThrow(() -> new ResourceNotFoundException(RANK_AGGREGATE, id));
            return rankDto(row);
        });
    }

    /** Whether the caller may edit the e-book in its school (and so reads its students' rankings). */
    private static boolean officer(AuthContext caller, EbookHead ebook) {
        if (!caller.hasPermission(PERMISSION_EBOOK_OFFICER)) {
            return false;
        }
        DataScope scope = DataScope.of(caller, PERMISSION_EBOOK_OFFICER);
        return ebook.institutionId() == null ? scope.tenantWide() : scope.canRead(Optional.of(ebook.institutionId()));
    }

    /**
     * The related materials by id, read in one statement rather than one per id; an id with no live material is 404,
     * as reading it alone would be.
     */
    private Map<UUID, RelatedMaterial> materials(Collection<UUID> ids) {
        Map<UUID, RelatedMaterial> found = new HashMap<>();
        materials.findByIds(ids).forEach(row -> found.put(row.getId(), materialMapper.toDto(row)));
        for (UUID id : ids) {
            if (!found.containsKey(id)) {
                throw new ResourceNotFoundException(EbookService.RELATED_MATERIAL_AGGREGATE, id);
            }
        }
        return found;
    }

    private static RelatedMaterialRank rankDto(RankRow row) {
        RelatedMaterialRank rank = new RelatedMaterialRank().userId(row.userId()).rankedMaterialIds(
                row.rankedMaterialIds());
        rank.setId(row.id());
        rank.setEbookId(row.ebookId());
        rank.setCourseId(row.courseId());
        rank.setFileId(row.fileId());
        rank.setSubjectTags(row.subjectTags());
        rank.setCreateTime(row.createdAt());
        rank.setCreatedBy(row.createdBy());
        rank.setUpdateTime(row.updatedAt());
        rank.setUpdatedBy(row.updatedBy());
        rank.setEtag(Etags.of(row.rowVersion()));
        return rank;
    }

    private static EbookHead visible(DataScope scope, Optional<EbookHead> found, UUID id) {
        EbookHead ebook = found.orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!scope.canRead(Optional.ofNullable(ebook.institutionId()))) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        return ebook;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
