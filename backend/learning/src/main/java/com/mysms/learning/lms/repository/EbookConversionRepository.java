package com.mysms.learning.lms.repository;

import com.mysms.core.db.ModuleDatabase;
import com.mysms.learning.lms.config.LmsDatabaseConfig;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Plain-JDBC reads and writes of an e-book's conversion: the job sent, the callback's outcome, the stored result. */
@Repository
public class EbookConversionRepository {

    /** What a conversion needs to know about the e-book. */
    public record Head(UUID id, UUID institutionId, String title, UUID sourceFileId, UUID fileId, String jobRef,
                       String outputUrl, String error, int rowVersion) {
    }

    private static final String COLUMNS = """
            SELECT id, institution_id, title, source_file_id, file_id, conversion_job_ref, conversion_output_url,
                   conversion_error, row_version
              FROM lms.ebook WHERE tenant_id = :tenantId AND id = :id AND deleted_at IS NULL
            """;
    private static final String SENT = """
            UPDATE lms.ebook SET conversion_job_ref = :jobRef, conversion_output_url = NULL, conversion_error = NULL,
                   updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String FINISHED = """
            UPDATE lms.ebook SET file_id = :fileId, conversion_job_ref = NULL, conversion_output_url = NULL,
                   conversion_error = NULL, updated_at = :now, updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND row_version = :expected AND deleted_at IS NULL
            """;
    private static final String OUTCOME = """
            UPDATE lms.ebook SET conversion_output_url = :outputUrl, conversion_error = :error, updated_at = :now,
                   updated_by = :actor, row_version = row_version + 1
             WHERE tenant_id = :tenantId AND id = :id AND conversion_job_ref = :jobRef AND deleted_at IS NULL
            """;

    private final JdbcClient jdbc;

    public EbookConversionRepository(@Qualifier(LmsDatabaseConfig.DATABASE) ModuleDatabase database) {
        this.jdbc = database.jdbc();
    }

    public Optional<Head> head(UUID id) {
        return read(COLUMNS, id);
    }

    public Optional<Head> lock(UUID id) {
        return read(COLUMNS + " FOR UPDATE", id);
    }

    private Optional<Head> read(String sql, UUID id) {
        return jdbc.sql(sql).param("id", id).query((rs, n) -> new Head(rs.getObject("id", UUID.class),
                rs.getObject("institution_id", UUID.class), rs.getString("title"),
                rs.getObject("source_file_id", UUID.class), rs.getObject("file_id", UUID.class),
                rs.getString("conversion_job_ref"), rs.getString("conversion_output_url"),
                rs.getString("conversion_error"), rs.getInt("row_version"))).optional();
    }

    public int sent(UUID id, int expected, String jobRef, OffsetDateTime now, UUID actor) {
        return jdbc.sql(SENT).param("id", id).param("expected", expected).param("jobRef", jobRef).param("now", now)
                .param("actor", actor).update();
    }

    public int finished(UUID id, int expected, UUID fileId, OffsetDateTime now, UUID actor) {
        return jdbc.sql(FINISHED).param("id", id).param("expected", expected).param("fileId", fileId)
                .param("now", now).param("actor", actor).update();
    }

    /** Records the callback's outcome on the e-book whose sent job it names. */
    public int outcome(UUID id, String jobRef, String outputUrl, String error, OffsetDateTime now, UUID actor) {
        return jdbc.sql(OUTCOME).param("id", id).param("jobRef", jobRef).param("outputUrl", outputUrl)
                .param("error", error).param("now", now).param("actor", actor).update();
    }
}
