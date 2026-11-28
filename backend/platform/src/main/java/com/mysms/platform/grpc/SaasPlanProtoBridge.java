package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.platform.api.model.SaasPlanUpdate;

/**
 * UpdateSaasPlan over gRPC (JAVA-CODING-RULES §7.2): the {@code platform.v1.SaasPlanUpdate} message
 * as REST's {@link SaasPlanUpdate} body, one explicit line per field: unset keeps the value, null
 * clears it, a value sets it.
 */
final class SaasPlanProtoBridge {

    private SaasPlanProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SaasPlanUpdate changes(ProtoBridge bridge, com.mysms.platform.v1.SaasPlanUpdate request) {
        SaasPlanUpdate changes = new SaasPlanUpdate();
        changes.setCode(ProtoBridge.text(request.hasCode(), request.getCode()));
        changes.setDisplayName(ProtoBridge.text(request.hasDisplayName(), request.getDisplayName()));
        changes.setBillingCycleMonths(ProtoBridge.number(
                request.hasBillingCycleMonths(), request.getBillingCycleMonths()));
        changes.setPrice(ProtoBridge.text(request.hasPrice(), request.getPrice()));
        changes.setCurrencyId(bridge.uuid(request.hasCurrencyId(), request.getCurrencyId(), "currency_id"));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsActive(ProtoBridge.flag(request.hasIsActive(), request.getIsActive()));
        changes.setMaxMembers(ProtoBridge.number(request.hasMaxMembers(), request.getMaxMembers()));
        changes.setMaxEvents(ProtoBridge.number(request.hasMaxEvents(), request.getMaxEvents()));
        changes.setMaxFolders(ProtoBridge.number(request.hasMaxFolders(), request.getMaxFolders()));
        changes.setMaxFiles(ProtoBridge.number(request.hasMaxFiles(), request.getMaxFiles()));
        changes.setMaxVideos(ProtoBridge.number(request.hasMaxVideos(), request.getMaxVideos()));
        changes.setMaxAudios(ProtoBridge.number(request.hasMaxAudios(), request.getMaxAudios()));
        changes.setMaxBulletins(ProtoBridge.number(request.hasMaxBulletins(), request.getMaxBulletins()));
        changes.setMaxGroups(ProtoBridge.number(request.hasMaxGroups(), request.getMaxGroups()));
        return bridge.patch(changes);
    }
}
