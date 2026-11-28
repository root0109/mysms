package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.TimetableLegendUpdate;

/**
 * UpdateTimetableLegend over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.TimetableLegendUpdate} message as REST's
 * {@link TimetableLegendUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class TimetableLegendProtoBridge {

    private TimetableLegendProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static TimetableLegendUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.TimetableLegendUpdate request) {
        TimetableLegendUpdate changes = new TimetableLegendUpdate();
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setTimetableTemplateId(bridge.uuid(
                request.hasTimetableTemplateId(), request.getTimetableTemplateId(), "timetable_template_id"));
        changes.setItemKind(bridge.choice(
                request.hasItemKind(), request.getItemKind(), TimetableLegendUpdate.ItemKindEnum::fromValue));
        changes.setColor(ProtoBridge.text(request.hasColor(), request.getColor()));
        changes.setLabel(ProtoBridge.text(request.hasLabel(), request.getLabel()));
        return bridge.patch(changes);
    }
}
