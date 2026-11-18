package com.mysms.platform.events;

import com.mysms.core.imports.ImportBatchResult;
import com.mysms.messaging.events.EventEnvelope;
import com.mysms.messaging.events.EventHandler;
import com.mysms.messaging.events.EventSubscriptions;
import com.mysms.messaging.events.RejectedEventException;
import com.mysms.platform.business.ImportService;
import com.mysms.platform.validation.ImportTemplates;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Receives the owning modules' answers to an import's batches ({@code <module>.import.batch_applied}, one handler per
 * module that owns an import template) and adds them to the import (ImportService), inside the event's tenant
 * transaction (exactly once per event). An answer that does not fit its import is rejected, never retried.
 */
@Component
public class PlatformImportHandler implements EventSubscriptions {

    private final ImportService imports;
    private final JsonMapper json;

    public PlatformImportHandler(ImportService imports, JsonMapper json) {
        this.imports = imports;
        this.json = json;
    }

    @Override
    public String module() {
        return "platform";
    }

    @Override
    public Map<String, EventHandler> handlers() {
        return ImportTemplates.all().stream().map(ImportTemplates.Template::module).distinct()
                .collect(Collectors.toUnmodifiableMap(ImportBatchResult::eventType,
                        module -> (EventHandler) this::apply));
    }

    /** Adds one batch answer; whether it changed the import (false: answered before). */
    public boolean apply(EventEnvelope event) {
        try {
            return imports.applyBatchResult(event.module(), ImportBatchResult.fromPayload(event.data(), json));
        } catch (IllegalArgumentException misfit) {
            throw new RejectedEventException("import answer " + event.id() + " of " + event.module()
                    + " does not fit its import");
        }
    }
}
