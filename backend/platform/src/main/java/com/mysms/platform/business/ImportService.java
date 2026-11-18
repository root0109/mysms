package com.mysms.platform.business;

import com.mysms.authclient.AuthContext;
import com.mysms.authclient.DataScope;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ResourceNotFoundException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.id.UuidV7;
import com.mysms.core.imports.ImportBatch;
import com.mysms.core.imports.ImportBatchResult;
import com.mysms.core.outbox.AuditWriter;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.core.validation.DtoValidator;
import com.mysms.platform.api.model.DownloadLink;
import com.mysms.platform.api.model.FileObject;
import com.mysms.platform.api.model.Operation;
import com.mysms.platform.api.model.StartImportRequest;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.ImportJobEntity;
import com.mysms.platform.errors.ImportTypeUnavailableException;
import com.mysms.platform.repository.ImportJobRepository;
import com.mysms.platform.validation.CsvReader;
import com.mysms.platform.validation.ImportTemplates;
import com.mysms.platform.validation.ImportTemplates.Template;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bulk imports (platform.GetImportTemplate, platform.StartImport) and their results. The platform may not write another
 * module's data, so an import is an exchange of events: the platform checks the uploaded CSV file against its template
 * ({@link ImportTemplates}) and sends the valid rows in batches ({@value ImportBatch#EVENT_TYPE}, rows sealed for the
 * tenant) to the module that owns them, which creates each row through its own business service as the importing user
 * and answers every batch ({@code <module>.import.batch_applied}); the platform adds the answers up and the operation
 * succeeds when every batch is answered. A dry run, or a file without a valid row, is finished at once with the
 * template check's row errors. The operation fails only on system errors, which the bus redelivers.
 *
 * <p>Who may import: the caller needs the owner's create permission of the entity type (sis.student.create…) for the
 * target institution — or tenant-wide, for tenant-level records — read from the caller's token, since the platform
 * cannot ask the owner. The batches carry the caller, and the owner grants that one permission in that one scope and
 * nothing else (core's ImportIdentity).
 */
@Service
public class ImportService extends AppTransactionManager {

    public static final String AGGREGATE = ImportJobEntity.TABLE;
    /** The largest import file. */
    static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    /** The most data rows of one import. */
    static final int MAX_ROWS = 10_000;
    /** Rows per batch event. */
    static final int BATCH_SIZE = 200;

    private final ImportJobRepository repository;
    private final OperationService operations;
    private final FileObjectService files;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final FieldCipher cipher;
    private final JsonMapper json;
    private final Clock clock;

    public ImportService(ImportJobRepository repository, OperationService operations, FileObjectService files,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, FieldCipher cipher,
            JsonMapper json, Clock clock) {
        super(database, transactions);
        this.repository = repository;
        this.operations = operations;
        this.files = files;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.cipher = cipher;
        this.json = json;
        this.clock = clock;
    }

    /**
     * platform.GetImportTemplate: the entity type's CSV template — instruction lines describing every column (required
     * or optional, format, meaning), then the header row — as a file of the caller, downloaded through a URL signed
     * now. The caller's identical earlier template file is reused. 404 for an unknown type, 422 for one no module
     * imports yet, 503 without a file store.
     */
    public DownloadLink getImportTemplate(String entityType) {
        Template template = template(entityType);
        FileObject file = files.saveOutputOnce("import-template-" + entityType.replace('_', '-') + ".csv",
                "text/csv", ReportService.csv(template.fileRecords()));
        return files.getDownloadUrl(file.getId());
    }

    /**
     * platform.StartImport: 403 without the entity type's create permission for the institution (tenant-wide for
     * tenant-level records); 422 for a missing or superfluous institution, a file that is not a CSV file of at most
     * 5 MB and {@value #MAX_ROWS} rows, or an entity type no module imports yet; 404 for a file the caller may not
     * read; 409 for an upload that is not complete; 503 without a file store. Answers the operation: finished for a
     * dry run (or nothing to send), running while the owner works through the batches.
     */
    public Operation startImport(StartImportRequest request) {
        validator.validate(request);
        String entityType = request.getEntityType().getValue();
        Template template = template(entityType);
        AuthContext caller = AuthContext.require();
        UUID institution = request.getInstitutionId();
        boolean tenantWide = authorize(caller, template, institution);
        boolean dryRun = Boolean.TRUE.equals(request.getDryRun());
        byte[] content = files.readCsv(request.getFileId(), MAX_FILE_BYTES);
        ImportTemplates.Checked checked = check(template, content);
        if (checked.rowCount() > MAX_ROWS) {
            throw new ValidationFailedException(List.of(new FieldError("fileId",
                    "at most " + MAX_ROWS + " rows; split the file", "TOO_MANY_ROWS")));
        }
        List<List<ImportBatch.Row>> batches = dryRun ? List.of() : batches(checked.valid());
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        ImportJobEntity job = ImportJobEntity.builder()
                .tenantId(tenant)
                .entityType(entityType)
                .institutionId(institution)
                .fileId(request.getFileId())
                .dryRun(dryRun)
                .rowCount(checked.rowCount())
                .batchCount(batches.size())
                .errorCount(checked.errors().size())
                .build();
        job.onCreatingData(Actor.current(), clock);
        return doInWriteTransaction(() -> {
            UUID operation = operations.recordRunning(OperationService.IMPORT, AGGREGATE, job.getId());
            job.setOperationId(operation);
            repository.insert(job);
            repository.insertErrors(job.getId(), null, ids(checked.errors().size()), checked.errors(), now(),
                    caller.userId());
            audit.recordCreate(AGGREGATE, job.getId());
            for (int i = 0; i < batches.size(); i++) {
                ImportBatch batch = new ImportBatch(operation, entityType, Optional.ofNullable(institution), i + 1,
                        caller.userId(), caller.impersonatorId(), tenantWide, batches.get(i));
                outbox.publish(ImportBatch.EVENT_TYPE, ImportBatch.AGGREGATE, operation,
                        batch.toPayload(cipher, json, tenant));
            }
            if (batches.isEmpty()) {
                operations.advance(operation, 100);
            }
            return operations.getOperation(operation);
        });
    }

    /**
     * Adds one batch answer of the owning module to its import, inside the event's transaction; the operation
     * succeeds with the last batch. A repeated answer changes nothing (false).
     *
     * @throws IllegalArgumentException for an answer that does not fit the import (another module, a batch the
     *                                  import never sent, more rows than it has): never retried
     */
    public boolean applyBatchResult(String module, ImportBatchResult result) {
        return doInWriteTransaction(() -> {
            ImportJobEntity job = repository.lockByOperation(result.operationId())
                    .orElseThrow(() -> new IllegalArgumentException("no import " + result.operationId()));
            boolean owner = ImportTemplates.find(job.getEntityType()).map(Template::module).filter(module::equals)
                    .isPresent();
            if (!owner || !job.getEntityType().equals(result.entityType()) || result.batchNumber() > job.getBatchCount()
                    || job.getCreatedCount() + result.createdCount() > job.getRowCount()) {
                throw new IllegalArgumentException("the answer of " + module + " does not fit import "
                        + result.operationId());
            }
            OffsetDateTime now = now();
            if (!repository.insertBatch(UuidV7.next(clock), job.getId(), result.batchNumber(), module,
                    result.createdCount(), result.errors().size(), now, null)) {
                return false;
            }
            repository.insertErrors(job.getId(), result.batchNumber(), ids(result.errors().size()), result.errors(),
                    now, null);
            if (repository.applyBatch(job.getId(), job.getRowVersion(), result.createdCount(), result.errors().size(),
                    now, null) == 0) {
                throw new IllegalStateException("import " + job.getId() + " changed while locked");
            }
            audit.recordUpdate(AGGREGATE, job.getId(), List.of("appliedBatchCount", "createdCount", "errorCount"));
            int applied = job.getAppliedBatchCount() + 1;
            operations.advance(job.getOperationId(), applied == job.getBatchCount() ? 100
                    : Math.min(99, applied * 100 / job.getBatchCount()));
            return true;
        });
    }

    /** The template of an entity type: 422 for one the contract lists but no module imports yet, 404 for others. */
    private static Template template(String entityType) {
        if (ImportTemplates.UNAVAILABLE.contains(entityType)) {
            throw new ImportTypeUnavailableException(entityType);
        }
        return ImportTemplates.find(entityType)
                .orElseThrow(() -> new ResourceNotFoundException("platform.import_template", entityType));
    }

    /**
     * The caller may import the entity type where the records go: the owner's create permission, for the target
     * institution (the data scope of that permission, X-Institution-Id included), or tenant-wide for tenant-level
     * records. Answers whether the caller holds it tenant-wide.
     */
    private static boolean authorize(AuthContext caller, Template template, UUID institution) {
        String permission = template.permission();
        if (!caller.hasPermission(permission)) {
            throw new PermissionDeniedException("Importing " + template.entityType() + " needs " + permission + ".");
        }
        if (institution == null && template.scope() == ImportTemplates.Scope.INSTITUTION) {
            throw new ValidationFailedException(List.of(new FieldError("institutionId",
                    "the institution the records belong to", "NotNull")));
        }
        if (institution != null && template.scope() == ImportTemplates.Scope.TENANT) {
            throw new ValidationFailedException(List.of(new FieldError("institutionId",
                    template.entityType() + " belong to the whole tenant; leave it out", "NOT_APPLICABLE")));
        }
        DataScope.of(caller, permission).requireWritable(Optional.ofNullable(institution));
        return caller.hasTenantWidePermission(permission);
    }

    /** The file checked against the template; a file that is no CSV text is one error of row 1. */
    private static ImportTemplates.Checked check(Template template, byte[] content) {
        try {
            return template.check(CsvReader.read(content));
        } catch (CsvReader.MalformedCsvException malformed) {
            return new ImportTemplates.Checked(0, List.of(),
                    List.of(new ImportBatchResult.RowError(1, null, malformed.getMessage())));
        }
    }

    private static List<List<ImportBatch.Row>> batches(List<ImportBatch.Row> rows) {
        return Stream.iterate(0, from -> from < rows.size(), from -> from + BATCH_SIZE)
                .map(from -> rows.subList(from, Math.min(from + BATCH_SIZE, rows.size()))).toList();
    }

    private List<UUID> ids(int count) {
        return Stream.generate(() -> UuidV7.next(clock)).limit(count).toList();
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
