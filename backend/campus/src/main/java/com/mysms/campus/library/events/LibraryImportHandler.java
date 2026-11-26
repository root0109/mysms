package com.mysms.campus.library.events;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.imports.ImportBatch;
import com.mysms.core.imports.ImportBatchApplier;
import com.mysms.core.imports.ImportIdentity;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.campus.library.api.model.Media;
import com.mysms.campus.library.business.MediaService;
import com.mysms.campus.library.config.LibraryDatabaseConfig;
import com.mysms.campus.library.config.LibraryEventsConfig;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Creates the rows the platform imports for this module (library media) through the module's own services, as the
 * importing user with the create permission the platform checked, and answers {@code library.import.batch_applied}.
 * Other modules' batches are left alone; a batch of another tenant is rejected, never retried.
 */
@Component
public class LibraryImportHandler extends ImportBatchApplier implements EventSubscriptions {

    private final Map<String, ImportTarget<?>> targets;

    public LibraryImportHandler(MediaService media,
            @Qualifier(LibraryDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(LibraryDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(LibraryEventsConfig.OUTBOX) OutboxWriter outbox, ImportIdentity identity, FieldCipher cipher,
            JsonMapper json) {
        super("library", database, transactions, outbox, identity, cipher, json);
        this.targets = Map.of(
                "library_media", new ImportTarget<>(MediaService.PERMISSION_CREATE, Media.class,
                        Media::setInstitutionId, title -> media.createMedia(title, false)));
    }

    @Override
    public String module() {
        return "library";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return Map.of(ImportBatch.EVENT_TYPE, this::handle);
    }

    @Override
    protected Map<String, ImportTarget<?>> targets() {
        return targets;
    }

    /** Applies one batch event of the platform. */
    public void handle(EventEnvelope event) {
        try {
            apply(event.tenantId().orElseThrow(() -> new IllegalArgumentException("an import batch has a tenant")),
                    event.data());
        } catch (IllegalArgumentException unfit) {
            throw new RejectedEventException("import batch " + event.id() + " is not one library can apply");
        }
    }
}
