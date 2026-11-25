package com.mysms.platform.repository;

/**
 * The statements of platform's privacy tasks (design PRIVACY-AND-CONSENT-CENTRE §9.1), run by its
 * {@code PlatformPrivacyTaskHandler} through privacy-client: per table, the ids of a subject's rows and the statement
 * that erases one row. platform knows a data principal only where its polymorphic references name the person's record
 * ({@code people.person}, the id privacy's tasks carry): the notes, attachments and tags on it and the files it owns or
 * has attached. Soft-deleted rows are included, since soft delete is not erasure. The person's type name is the
 * parameter {@code :personType} ({@link #PERSON_TYPE}): a module's SQL names no other module's schema.
 */
public final class PrivacyErasureRepository {

    /** The polymorphic type of the person's record in platform's references. */
    public static final String PERSON_TYPE = "people.person";

    public static final String ENTITY_NOTE_ROWS = """
            SELECT id FROM platform.entity_note
             WHERE tenant_id = :tenantId AND entity_type = :personType AND entity_id = :subjectId
             ORDER BY id
            """;
    /** The note's text is required: a fixed placeholder takes its place. */
    public static final String ERASE_ENTITY_NOTE = """
            UPDATE platform.entity_note
               SET note_text = 'Erased', deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String ENTITY_ATTACHMENT_ROWS = """
            SELECT id FROM platform.entity_attachment
             WHERE tenant_id = :tenantId AND entity_type = :personType AND entity_id = :subjectId
             ORDER BY id
            """;
    public static final String ERASE_ENTITY_ATTACHMENT = """
            UPDATE platform.entity_attachment
               SET title = NULL, deleted_at = COALESCE(deleted_at, :now), updated_at = :now,
                   row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    public static final String TAG_ASSIGNMENT_ROWS = """
            SELECT id FROM platform.tag_assignment
             WHERE tenant_id = :tenantId AND entity_type = :personType AND entity_id = :subjectId
             ORDER BY id
            """;
    public static final String ERASE_TAG_ASSIGNMENT = """
            UPDATE platform.tag_assignment
               SET deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    /** The files the person's record owns, and the unshared files attached to it. */
    public static final String FILE_OBJECT_ROWS = """
            SELECT f.id FROM platform.file_object f
             WHERE f.tenant_id = :tenantId
               AND ((f.owner_entity_type = :personType AND f.owner_entity_id = :subjectId)
                    OR (f.is_shared = false AND f.id IN (
                        SELECT a.file_id FROM platform.entity_attachment a
                         WHERE a.tenant_id = :tenantId AND a.entity_type = :personType
                           AND a.entity_id = :subjectId)))
             ORDER BY f.id
            """;
    /**
     * The file's names go (the stored name is required: a fixed placeholder), its processing error with them, and it is
     * marked deleted; the storage key carries the tenant and the file id only.
     */
    public static final String ERASE_FILE_OBJECT = """
            UPDATE platform.file_object
               SET file_name = 'erased', original_name = NULL, processing_error = NULL, status = 'deleted',
                   deleted_at = COALESCE(deleted_at, :now), updated_at = :now, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id
            """;

    private PrivacyErasureRepository() {
    }
}
