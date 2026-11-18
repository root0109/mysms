package com.mysms.hr.events;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.imports.ImportBatch;
import com.mysms.core.imports.ImportBatchApplier;
import com.mysms.core.imports.ImportIdentity;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.hr.api.model.Staff;
import com.mysms.hr.business.StaffService;
import com.mysms.hr.config.HrDatabaseConfig;
import com.mysms.hr.config.HrEventsConfig;
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
 * Creates the rows the platform imports for this module (staff) through the module's own services, as the importing
 * user with the create permission the platform checked, and answers {@code hr.import.batch_applied}. Other modules'
 * batches are left alone; a batch of another tenant is rejected, never retried.
 */
@Component
public class HrImportHandler extends ImportBatchApplier implements EventSubscriptions {

    private final Map<String, ImportTarget<?>> targets;

    public HrImportHandler(StaffService staff,
            @Qualifier(HrDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(HrDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(HrEventsConfig.OUTBOX) OutboxWriter outbox, ImportIdentity identity, FieldCipher cipher,
            JsonMapper json) {
        super("hr", database, transactions, outbox, identity, cipher, json);
        this.targets = Map.of(
                "staff", new ImportTarget<>(StaffService.PERMISSION_CREATE, Staff.class, Staff::setInstitutionId,
                        member -> staff.createStaff(member, false)));
    }

    @Override
    public String module() {
        return "hr";
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
            throw new RejectedEventException("import batch " + event.id() + " is not one hr can apply");
        }
    }
}
