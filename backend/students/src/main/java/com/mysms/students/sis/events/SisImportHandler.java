package com.mysms.students.sis.events;

import com.mysms.core.crypto.FieldCipher;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.imports.ImportBatch;
import com.mysms.core.imports.ImportBatchApplier;
import com.mysms.core.imports.ImportIdentity;
import com.mysms.core.outbox.OutboxWriter;
import com.mysms.students.sis.api.model.Guardian;
import com.mysms.students.sis.api.model.Promotion;
import com.mysms.students.sis.api.model.Student;
import com.mysms.students.sis.business.GuardianService;
import com.mysms.students.sis.business.PromotionService;
import com.mysms.students.sis.business.StudentService;
import com.mysms.students.sis.config.SisDatabaseConfig;
import com.mysms.students.sis.config.SisEventsConfig;
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
 * Creates the rows the platform imports for this module (students, guardians and promotions): each
 * {@value ImportBatch#EVENT_TYPE} batch of its entity types is applied through the module's own business services as
 * the importing user, with exactly the create permission the platform checked, and answered with
 * {@code sis.import.batch_applied}; batches of other modules' entity types are left alone. Runs inside the event's
 * tenant transaction (exactly once per event); a batch that is not one of this tenant is rejected, never retried.
 */
@Component
public class SisImportHandler extends ImportBatchApplier implements EventSubscriptions {

    private final Map<String, ImportTarget<?>> targets;

    public SisImportHandler(StudentService students, GuardianService guardians, PromotionService promotions,
            @Qualifier(SisDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(SisDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions,
            @Qualifier(SisEventsConfig.OUTBOX) OutboxWriter outbox, ImportIdentity identity, FieldCipher cipher,
            JsonMapper json) {
        super("sis", database, transactions, outbox, identity, cipher, json);
        this.targets = Map.of(
                "students", new ImportTarget<>(StudentService.PERMISSION_CREATE, Student.class,
                        Student::setInstitutionId, student -> students.createStudent(student, false)),
                "guardians", new ImportTarget<>(GuardianService.PERMISSION_CREATE, Guardian.class, null,
                        guardian -> guardians.createGuardian(guardian, false)),
                "promotions", new ImportTarget<>(PromotionService.PERMISSION_CREATE, Promotion.class,
                        Promotion::setInstitutionId, promotion -> promotions.createPromotion(promotion, false)));
    }

    @Override
    public String module() {
        return "sis";
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
            throw new RejectedEventException("import batch " + event.id() + " is not one sis can apply");
        }
    }
}
