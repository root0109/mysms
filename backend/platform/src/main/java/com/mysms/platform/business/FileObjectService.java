package com.mysms.platform.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.ApiException;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.InvalidStateException;
import com.mysms.core.error.PermissionDeniedException;
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
import com.mysms.platform.api.model.CopyFileRequest;
import com.mysms.platform.api.model.CreateUploadUrlRequest;
import com.mysms.platform.api.model.DownloadLink;
import com.mysms.platform.api.model.FileObject;
import com.mysms.platform.api.model.UploadTarget;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.config.PlatformEventsConfig;
import com.mysms.platform.domain.FileObjectEntity;
import com.mysms.platform.domain.UploadPolicyEntity;
import com.mysms.platform.events.PlatformEvents;
import com.mysms.platform.mapper.FileObjectMapper;
import com.mysms.platform.repository.FileObjectRepository;
import com.mysms.platform.validation.FileKinds;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Business service of platform.file_object: a client gets a signed upload URL (the file is created pending), PUTs the
 * bytes straight to the store and completes the upload, which checks the stored object against what was declared and
 * pins its generation; downloads are signed URLs of that generation. No store call runs inside a transaction.
 *
 * <p>Who may do what: the owning module's permission decides. A file may belong to one record
 * of another module ({@code ownerEntityType} = its table, {@code schema.table}); the permission codes of that module
 * are {@code <schema>.<table>.<action>}, so the platform checks the owner's permission from the token: attaching a file
 * to a record needs the owner's create or update permission, reading it the owner's read permission. A file attached
 * through platform.entity_attachment is readable with the read permission of any record type it is attached to; a
 * file a theme shows is readable by every user of the tenant; the uploader always reads their own file, and a
 * tenant-wide {@code platform.file_object.read} reads every file. The platform cannot see the owner's rows, so the
 * check is by permission, not by row (a known limit, reported).
 */
@Service
public class FileObjectService extends AppTransactionManager {

    public static final String AGGREGATE = FileObjectEntity.TABLE;
    static final String PERMISSION_READ = "platform.file_object.read";
    static final String PERMISSION_DELETE = "platform.file_object.delete";
    /** 409 code: the tenant's plan allows no more files. */
    static final String PLAN_LIMIT_REACHED = "PLAN_LIMIT_REACHED";
    /** 409 code: CompleteUpload before the bytes reached the store. */
    static final String UPLOAD_MISSING = "UPLOAD_MISSING";
    /** Signed URLs are short-lived: long enough to start a transfer, too short to share usefully. */
    static final Duration UPLOAD_TTL = Duration.ofMinutes(15);
    static final Duration DOWNLOAD_TTL = Duration.ofMinutes(10);
    /** A record type as stored in owner/entity type columns: {@code schema.table}. */
    private static final Pattern RECORD_TYPE = Pattern.compile("[a-z]+\\.[a-z][a-z0-9_]*");
    private static final Pattern CONTENT_TYPE = Pattern.compile("[a-z]+/[a-z0-9][a-z0-9.+-]*");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String PENDING = "pending";
    private static final String AVAILABLE = "available";
    private static final Logger LOG = LoggerFactory.getLogger(FileObjectService.class);

    private final FileObjectRepository repository;
    private final FileObjectMapper mapper;
    private final FileStore store;
    private final StorageQuotaService quotas;
    private final UploadPolicyService policies;
    private final TenantSubscriptionService subscriptions;
    private final OutboxWriter outbox;
    private final AuditWriter audit;
    private final DtoValidator validator;
    private final Clock clock;
    private final long defaultMaxBytes;

    public FileObjectService(FileObjectRepository repository, FileObjectMapper mapper, FileStore store,
            StorageQuotaService quotas, UploadPolicyService policies, TenantSubscriptionService subscriptions,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(PlatformEventsConfig.OUTBOX) OutboxWriter outbox,
            @Qualifier(PlatformEventsConfig.AUDIT) AuditWriter audit, DtoValidator validator, Clock clock,
            @Value("${mysms.platform.files.max-size-bytes}")
            long defaultMaxBytes) {
        super(database, transactions);
        this.repository = repository;
        this.mapper = mapper;
        this.store = store;
        this.quotas = quotas;
        this.policies = policies;
        this.subscriptions = subscriptions;
        this.outbox = outbox;
        this.audit = audit;
        this.validator = validator;
        this.clock = clock;
        this.defaultMaxBytes = defaultMaxBytes;
    }

    // ---------------------------------------------------------------------------------------------- standard reads

    /** The caller's own uploads, or every file of the tenant with a tenant-wide read grant. */
    public Page<FileObject> listFileObjects(Integer pageNumber, Integer pageSize, String cursor, String filter,
            List<String> sortBy) {
        AuthContext caller = AuthContext.require();
        SortSpec sort = SortSpec.parse(sortBy, FileObjectRepository.SORT_FIELDS, FileObjectRepository.DEFAULT_ORDER,
                FileObjectRepository.ID_COLUMN);
        SqlFragment where = FilterParser.parse(filter, FileObjectRepository.FILTER_FIELDS).and(FileObjectRepository
                .visibleTo(caller.userId(), caller.hasTenantWidePermission(PERMISSION_READ)));
        PageRequest page = PageRequest.of(pageNumber, pageSize, cursor, sort, filter, clock);
        return doInReadTransaction(() -> repository.list(page, where).map(mapper::toDto));
    }

    /** One file the caller may read (the download rules); 404 otherwise. */
    public FileObject getFileObject(UUID id) {
        AuthContext caller = AuthContext.require();
        return doInReadTransaction(() -> mapper.toDto(readable(caller, id)));
    }

    // ---------------------------------------------------------------------------------------------- the upload

    /**
     * platform.CreateUploadUrl: checks the file against the tenant's upload policy for the owner's module (size,
     * extension), the uploader's quota and the plan's file limit, creates the pending file and signs a PUT URL for
     * it. The upload must send the declared Content-Type.
     */
    public UploadTarget createUploadUrl(CreateUploadUrlRequest request) {
        validator.validate(request);
        AuthContext caller = AuthContext.require();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        String fileName = baseName(request.getFileName());
        String mimeType = request.getMimeType().trim().toLowerCase(Locale.ROOT);
        List<FieldError> errors = new ArrayList<>();
        WireValues.checkInt64("sizeBytes", request.getSizeBytes(), errors);
        Long size = errors.isEmpty() ? WireValues.int64(request.getSizeBytes()) : null;
        if (size != null && size <= 0) {
            errors.add(new FieldError("sizeBytes", "must be greater than 0", "Min"));
        }
        if (fileName.isEmpty() || fileName.length() > 255) {
            errors.add(new FieldError("fileName", "a file name of 1 to 255 characters", "Size"));
        }
        if (!CONTENT_TYPE.matcher(mimeType).matches() || mimeType.length() > 150) {
            errors.add(new FieldError("mimeType", "a media type such as application/pdf", "Pattern"));
        }
        String checksum = request.getChecksumSha256();
        if (checksum != null && !SHA256.matcher(checksum).matches()) {
            errors.add(new FieldError("checksumSha256", "64 lower-case hex digits", "Pattern"));
        }
        String collection = request.getCollectionName();
        if (collection != null && collection.length() > 80) {
            errors.add(new FieldError("collectionName", "at most 80 characters", "Size"));
        }
        checkOwner(request.getOwnerEntityType(), request.getOwnerEntityId(), errors);
        String extension = FileKinds.extension(fileName);
        String kind = FileKinds.kindOf(extension);
        if (errors.isEmpty() && !FileKinds.contentTypeFits(kind, mimeType)) {
            errors.add(new FieldError("mimeType", "does not match the file's extension", "MISMATCH"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        requireOwnerWrite(caller, request.getOwnerEntityType());
        String disk = store.disk();
        String module = Optional.ofNullable(request.getOwnerEntityType()).map(t -> t.substring(0, t.indexOf('.')))
                .orElse("platform");
        assertAllowed(module, request.getCollectionName(), extension, size);
        assertRoom(caller.userId(), size);
        FileObjectEntity entity = FileObjectEntity.builder()
                .tenantId(tenant)
                .fileName(fileName)
                .originalName(fileName)
                .mimeType(mimeType)
                .sizeBytes(size)
                .checksumSha256(request.getChecksumSha256())
                .ownerEntityType(request.getOwnerEntityType())
                .ownerEntityId(request.getOwnerEntityId())
                .collectionName(request.getCollectionName())
                .fileKind(kind)
                .status(PENDING)
                .uploadedByUserId(caller.userId())
                .storageDisk(disk)
                .build();
        entity.onCreatingData(Actor.current(), clock);
        entity.setStorageKey(keyOf(tenant, entity.getId()));
        FileStore.SignedUpload url = store.uploadUrl(entity.getStorageKey(), mimeType, size, UPLOAD_TTL);
        FileObjectEntity stored = doInWriteTransaction(() -> {
            FileObjectEntity inserted = repository.insert(entity);
            audit.recordCreate(AGGREGATE, inserted.getId());
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, inserted.getId(),
                    PlatformEvents.payload(inserted.getId()));
            return inserted;
        });
        return new UploadTarget(stored.getId(), url.url(), url.expiresAt()).uploadHeaders(url.headers());
    }

    /**
     * platform.CompleteUpload, by the uploader: the stored object must exist (409 UPLOAD_MISSING) with the declared
     * size, content type and SHA-256 (422, and the object is removed so the upload can be repeated). The file becomes
     * available at the generation checked, so bytes written to the key later are never served; quota 409.
     */
    public FileObject completeUpload(UUID id) {
        AuthContext caller = AuthContext.require();
        FileObjectEntity file = doInPrimaryReadTransaction(() -> pendingUpload(caller, id));
        FileStore.StoredObject stored = store.describe(file.getStorageKey())
                .orElseThrow(() -> new ConflictException(UPLOAD_MISSING, "No bytes were uploaded for this file yet."));
        List<FieldError> errors = new ArrayList<>();
        if (stored.sizeBytes() != file.getSizeBytes()) {
            errors.add(new FieldError("sizeBytes", "the uploaded file has a different size", "MISMATCH"));
        }
        if (stored.contentType() == null || !file.getMimeType().equalsIgnoreCase(stored.contentType().trim())) {
            errors.add(new FieldError("mimeType", "the upload was sent with a different content type", "MISMATCH"));
        }
        String declared = file.getChecksumSha256();
        if (errors.isEmpty() && declared != null
                && !declared.equals(store.sha256(file.getStorageKey(), stored.generation()))) {
            errors.add(new FieldError("checksumSha256", "the uploaded bytes have a different SHA-256", "MISMATCH"));
        }
        if (!errors.isEmpty()) {
            store.delete(file.getStorageKey());
            throw new ValidationFailedException(errors);
        }
        return doInWriteTransaction(() -> {
            FileObjectEntity current = pendingUpload(caller, id);
            if (current.getRowVersion() != file.getRowVersion()) {
                throw new PreconditionFailedException("The file changed while its upload was checked.");
            }
            quotas.spend(current.getUploadedByUserId(), current.getSizeBytes());
            if (repository.completeUpload(id, current.getRowVersion(), stored.generation(), now(),
                    caller.userId()) == 0) {
                throw new PreconditionFailedException("The file changed while its upload was checked.");
            }
            audit.recordTransition(AGGREGATE, id, "completeUpload", List.of("status"), null);
            outbox.publish(AGGREGATE + ".completeUpload", AGGREGATE, id, PlatformEvents.payload(id));
            return mapper.toDto(repository.findById(id).orElseThrow());
        });
    }

    // ---------------------------------------------------------------------------------------------- the download

    /**
     * platform.GetDownloadUrl: a short-lived signed URL for an available file the caller may read. Without a token
     * no file is found: the platform cannot tell which files belong to published catalogue content (the owning
     * module knows; reported).
     */
    public DownloadLink getDownloadUrl(UUID id) {
        Optional<AuthContext> caller = AuthContext.current();
        if (caller.isEmpty() || CallContextHolder.current().flatMap(CallContext::tenantId).isEmpty()) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        FileObjectEntity file = doInReadTransaction(() -> readable(caller.get(), id));
        if (!AVAILABLE.equals(file.getStatus())) {
            throw new InvalidStateException("The file is not available for download (its upload is not complete).");
        }
        FileStore.SignedUrl url = store.downloadUrl(file.getStorageKey(), file.getStorageGeneration(),
                file.getOriginalName() == null ? file.getFileName() : file.getOriginalName(), DOWNLOAD_TTL);
        return new DownloadLink(url.url(), url.expiresAt());
    }

    /**
     * A signed download URL of the bound tenant's file for a public page (the logo of the tenant's branding), or
     * empty when the store is not configured or the file is not available.
     */
    public Optional<String> publicUrl(UUID id) {
        if (!store.configured()) {
            return Optional.empty();
        }
        Optional<FileObjectEntity> file = doInReadTransaction(() -> repository.findById(id)
                .filter(f -> AVAILABLE.equals(f.getStatus())));
        return file.map(f -> store.downloadUrl(f.getStorageKey(), f.getStorageGeneration(), f.getFileName(),
                DOWNLOAD_TTL).url());
    }

    // ---------------------------------------------------------------------------------------------- delete and copy

    /**
     * Soft delete under If-Match, by the uploader or a tenant-wide delete grant; refused (409) while a live attachment
     * or a theme still uses the file. The bytes leave the store after the commit and the uploader's quota gets them
     * back.
     */
    public void deleteFileObject(UUID id, String ifMatch) {
        int expected = Etags.expectedVersion(ifMatch);
        AuthContext caller = AuthContext.require();
        FileObjectEntity deleted = doInWriteTransaction(() -> {
            FileObjectEntity file = readable(caller, id);
            if (!caller.userId().equals(file.getUploadedByUserId())
                    && !caller.hasTenantWidePermission(PERMISSION_DELETE)) {
                throw new PermissionDeniedException("Only the uploader or a tenant-wide grant may delete this file.");
            }
            if (file.getRowVersion() != expected) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (!repository.attachedTo(id).isEmpty() || repository.shownByTheme(id)) {
                throw new InvalidStateException(
                        "The file is still attached to a record or shown by a theme; remove it there first.");
            }
            file.onDeletingData(Actor.current(), clock);
            if (repository.softDelete(file, expected) == 0) {
                throw new PreconditionFailedException("If-Match does not match the current ETag.");
            }
            if (AVAILABLE.equals(file.getStatus()) && file.getUploadedByUserId() != null) {
                quotas.release(file.getUploadedByUserId(), file.getSizeBytes());
            }
            audit.recordDelete(AGGREGATE, id);
            outbox.publish(AGGREGATE + PlatformEvents.DELETED, AGGREGATE, id, PlatformEvents.payload(id));
            return file;
        });
        removeQuietly(deleted.getStorageKey());
    }

    /**
     * platform.CopyFile: a server-side copy of an available file the caller may read into a new available file of
     * the caller, optionally for another owner record (the owner's create or update permission is needed). The copy
     * counts against the caller's quota and the plan's file limit.
     */
    public FileObject copyFile(UUID id, CopyFileRequest request) {
        AuthContext caller = AuthContext.require();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        List<FieldError> errors = new ArrayList<>();
        checkOwner(request.getOwnerEntityType(), request.getOwnerEntityId(), errors);
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
        requireOwnerWrite(caller, request.getOwnerEntityType());
        FileObjectEntity source = doInReadTransaction(() -> readable(caller, id));
        if (!AVAILABLE.equals(source.getStatus())) {
            throw new InvalidStateException("Only an available file can be copied.");
        }
        assertRoom(caller.userId(), source.getSizeBytes());
        FileObjectEntity copy = FileObjectEntity.builder()
                .tenantId(tenant)
                .fileName(source.getFileName())
                .originalName(source.getOriginalName())
                .mimeType(source.getMimeType())
                .sizeBytes(source.getSizeBytes())
                .checksumSha256(source.getChecksumSha256())
                .ownerEntityType(request.getOwnerEntityType())
                .ownerEntityId(request.getOwnerEntityId())
                .fileKind(source.getFileKind())
                .status(AVAILABLE)
                .uploadedByUserId(caller.userId())
                .storageDisk(store.disk())
                .build();
        copy.onCreatingData(Actor.current(), clock);
        copy.setStorageKey(keyOf(tenant, copy.getId()));
        copy.setStorageGeneration(store.copy(source.getStorageKey(), source.getStorageGeneration(),
                copy.getStorageKey()));
        try {
            return doInWriteTransaction(() -> {
                quotas.spend(caller.userId(), copy.getSizeBytes());
                FileObjectEntity stored = repository.insert(copy);
                audit.recordCreate(AGGREGATE, stored.getId());
                outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, stored.getId(),
                        PlatformEvents.payload(stored.getId()));
                return mapper.toDto(stored);
            });
        } catch (RuntimeException refused) {
            removeQuietly(copy.getStorageKey());
            throw refused;
        }
    }

    /**
     * Stores generated output (a report) as an available file of the caller, owned by the record that produced it.
     * Output does not count against the caller's quota. Call outside any transaction: the bytes go to the store first.
     */
    public FileObject saveOutput(String fileName, String mimeType, byte[] content, String ownerType, UUID ownerId) {
        AuthContext caller = AuthContext.require();
        UUID tenant = CallContextHolder.require().tenantId().orElseThrow();
        FileObjectEntity output = FileObjectEntity.builder()
                .tenantId(tenant)
                .fileName(fileName)
                .originalName(fileName)
                .mimeType(mimeType)
                .sizeBytes((long) content.length)
                .checksumSha256(sha256(content))
                .ownerEntityType(ownerType)
                .ownerEntityId(ownerId)
                .fileKind(FileKinds.kindOf(FileKinds.extension(fileName)))
                .status(AVAILABLE)
                .uploadedByUserId(caller.userId())
                .storageDisk(store.disk())
                .build();
        output.onCreatingData(Actor.current(), clock);
        output.setStorageKey(keyOf(tenant, output.getId()));
        output.setStorageGeneration(store.write(output.getStorageKey(), content, mimeType));
        return doInWriteTransaction(() -> {
            FileObjectEntity stored = repository.insert(output);
            audit.recordCreate(AGGREGATE, stored.getId());
            outbox.publish(AGGREGATE + PlatformEvents.CREATED, AGGREGATE, stored.getId(),
                    PlatformEvents.payload(stored.getId()));
            return mapper.toDto(stored);
        });
    }

    /**
     * Generated output the caller may already have (an import template, the same for everyone): their earlier file of
     * the same name and bytes is answered instead of storing another copy each time. Call outside any transaction.
     */
    public FileObject saveOutputOnce(String fileName, String mimeType, byte[] content) {
        UUID caller = AuthContext.require().userId();
        Optional<FileObjectEntity> earlier = doInReadTransaction(() -> repository.findOutput(caller, fileName,
                sha256(content)));
        return earlier.map(mapper::toDto).orElseGet(() -> saveOutput(fileName, mimeType, content, null, null));
    }

    /**
     * The bytes of an uploaded file the caller may read, for the platform to process itself (an import): the file must
     * be available (409), a .csv file (422) and at most {@code maxBytes} long (422). The store is read outside any
     * transaction.
     */
    public byte[] readCsv(UUID id, long maxBytes) {
        AuthContext caller = AuthContext.require();
        FileObjectEntity file = doInReadTransaction(() -> readable(caller, id));
        if (!AVAILABLE.equals(file.getStatus())) {
            throw new InvalidStateException("The file is not available (its upload is not complete).");
        }
        if (!"csv".equals(FileKinds.extension(file.getFileName()))) {
            throw new ValidationFailedException(List.of(new FieldError("fileId", "a .csv file", "FILE_TYPE")));
        }
        if (file.getSizeBytes() > maxBytes) {
            throw new ValidationFailedException(List.of(new FieldError("fileId",
                    "at most " + maxBytes / (1024 * 1024) + " MB", "TOO_LARGE")));
        }
        return store.read(file.getStorageKey(), file.getStorageGeneration());
    }

    // ---------------------------------------------------------------------------------------------- rules

    /** The live file, if the caller may read it (see the class comment); 404 otherwise, never a hint. */
    private FileObjectEntity readable(AuthContext caller, UUID id) {
        FileObjectEntity file = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (caller.userId().equals(file.getUploadedByUserId()) || caller.hasTenantWidePermission(PERMISSION_READ)) {
            return file;
        }
        if (file.getOwnerEntityType() != null && caller.hasPermission(file.getOwnerEntityType() + ".read")) {
            return file;
        }
        if (repository.attachedTo(id).stream().anyMatch(type -> caller.hasPermission(type + ".read"))
                || repository.shownByTheme(id)) {
            return file;
        }
        throw new ResourceNotFoundException(AGGREGATE, id);
    }

    /** The caller's own pending upload; another user's file is 404, a completed one 409. */
    private FileObjectEntity pendingUpload(AuthContext caller, UUID id) {
        FileObjectEntity file = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(AGGREGATE, id));
        if (!caller.userId().equals(file.getUploadedByUserId())) {
            throw new ResourceNotFoundException(AGGREGATE, id);
        }
        if (!PENDING.equals(file.getStatus())) {
            throw new InvalidStateException("The upload of this file is already complete.");
        }
        return file;
    }

    /** An owner is a record type ({@code schema.table}) and an id, both or neither. */
    private static void checkOwner(String type, UUID id, List<FieldError> errors) {
        if (type == null && id == null) {
            return;
        }
        if (type == null || id == null) {
            errors.add(new FieldError(type == null ? "ownerEntityType" : "ownerEntityId",
                    "ownerEntityType and ownerEntityId go together", "Required"));
        } else if (type.length() > 80 || !RECORD_TYPE.matcher(type).matches()) {
            errors.add(new FieldError("ownerEntityType", "a record type such as lms.course", "Pattern"));
        }
    }

    /** Attaching a file to a record needs the owning module's create or update permission on that record type. */
    private static void requireOwnerWrite(AuthContext caller, String ownerType) {
        if (ownerType != null && !caller.hasPermission(ownerType + ".create")
                && !caller.hasPermission(ownerType + ".update")) {
            throw new PermissionDeniedException("Attaching files to " + ownerType + " records is not permitted.");
        }
    }

    /** The tenant's upload policy for the feature, or the platform's defaults (size limit, extension catalogue). */
    private void assertAllowed(String module, String collection, String extension, long size) {
        Optional<UploadPolicyEntity> policy = policies.limitsFor(module, collection);
        long max = policy.map(UploadPolicyEntity::getMaxSizeBytes).orElse(defaultMaxBytes);
        Optional<Set<String>> allowed = policy.flatMap(p -> FileKinds.allowed(p.getAllowedExtensions()));
        List<FieldError> errors = new ArrayList<>();
        if (size > max) {
            errors.add(new FieldError("sizeBytes", "at most " + max + " bytes", "Max"));
        }
        if (allowed.map(set -> !set.contains(extension)).orElse(!FileKinds.acceptedByDefault(extension))) {
            errors.add(new FieldError("fileName", "files of this type are not accepted here", "EXTENSION"));
        }
        if (!errors.isEmpty()) {
            throw new ValidationFailedException(errors);
        }
    }

    /** Room for one more file of {@code size}: the uploader's quota and the plan's file limit (409 each). */
    private void assertRoom(UUID user, long size) {
        if (!quotas.fits(user, size)) {
            throw new ConflictException(StorageQuotaService.QUOTA_EXCEEDED, "The file would exceed your storage quota.");
        }
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        Optional<Integer> limit = subscriptions.fileLimit(today);
        if (limit.isPresent() && doInReadTransaction(repository::liveCount) >= limit.get()) {
            throw new ConflictException(PLAN_LIMIT_REACHED, "The tenant's plan allows no more files.");
        }
    }

    /** The name without any directory part a browser may send, and without control characters. */
    private static String baseName(String given) {
        String name = given.substring(Math.max(given.lastIndexOf('/'), given.lastIndexOf('\\')) + 1);
        return name.replaceAll("\\p{Cntrl}", "").trim();
    }

    /** Object keys carry the tenant and the file id only — never a name or other personal data. */
    private static String keyOf(UUID tenant, UUID file) {
        return "tenants/" + tenant + "/files/" + file;
    }

    /** Removes bytes nothing references any more; a failure leaves an orphan object behind, logged, never an error. */
    private void removeQuietly(String key) {
        try {
            store.delete(key);
        } catch (ApiException unreachable) {
            LOG.warn("file store object {} could not be removed ({}); it is orphaned", key, unreachable.code());
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("SHA-256 is not available in this JVM.", missing);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
