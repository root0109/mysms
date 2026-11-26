package com.mysms.platform.org.events;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.imports.ImportBatch;
import com.mysms.core.imports.ImportBatchApplier;
import com.mysms.core.imports.ImportIdentity;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.platform.org.api.model.Holiday;
import com.mysms.platform.org.business.HolidayService;
import com.mysms.platform.org.config.OrgDatabaseConfig;
import com.mysms.platform.org.config.OrgEventsConfig;
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
 * Creates the rows the platform imports for this module (holidays) through its own business services as the importing
 * user, with exactly the create permission the platform checked, and answers with {@code org.import.batch_applied};
 * other modules' batches are left alone. A batch that is not this tenant's is rejected, never retried.
 */
@Component
public class OrgImportHandler extends ImportBatchApplier implements EventSubscriptions {

    private final Map<String, ImportTarget<?>> targets;

    public OrgImportHandler(HolidayService holidays,
            @Qualifier(OrgDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(OrgDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(OrgEventsConfig.OUTBOX) OutboxWriter outbox, ImportIdentity identity, FieldCipher cipher,
            JsonMapper json) {
        super("org", database, transactions, outbox, identity, cipher, json);
        this.targets = Map.of(
                "holidays", new ImportTarget<>(HolidayService.PERMISSION_CREATE, Holiday.class,
                        Holiday::setInstitutionId, holiday -> holidays.createHoliday(holiday, false)));
    }

    @Override
    public String module() {
        return "org";
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
            throw new RejectedEventException("import batch " + event.id() + " is not one org can apply");
        }
    }
}
