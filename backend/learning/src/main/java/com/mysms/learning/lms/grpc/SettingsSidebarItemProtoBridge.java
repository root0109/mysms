package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.SettingsSidebarItemUpdate;

/**
 * UpdateSettingsSidebarItem over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.SettingsSidebarItemUpdate} message as
 * REST's {@link SettingsSidebarItemUpdate} body, one explicit line per field: unset keeps the value, null clears it, a
 * value sets it.
 */
final class SettingsSidebarItemProtoBridge {

    private SettingsSidebarItemProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static SettingsSidebarItemUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.SettingsSidebarItemUpdate request) {
        SettingsSidebarItemUpdate changes = new SettingsSidebarItemUpdate();
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setItemType(bridge.choice(
                request.hasItemType(), request.getItemType(), SettingsSidebarItemUpdate.ItemTypeEnum::fromValue));
        changes.setItemKey(ProtoBridge.text(request.hasItemKey(), request.getItemKey()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setIcon(ProtoBridge.text(request.hasIcon(), request.getIcon()));
        changes.setWebPageRoute(ProtoBridge.text(request.hasWebPageRoute(), request.getWebPageRoute()));
        changes.setRoute(ProtoBridge.text(request.hasRoute(), request.getRoute()));
        changes.setUrl(ProtoBridge.text(request.hasUrl(), request.getUrl()));
        changes.setIsStandard(ProtoBridge.flag(request.hasIsStandard(), request.getIsStandard()));
        changes.setIsHidden(ProtoBridge.flag(request.hasIsHidden(), request.getIsHidden()));
        changes.setOpenInNewWindow(ProtoBridge.flag(request.hasOpenInNewWindow(), request.getOpenInNewWindow()));
        return bridge.patch(changes);
    }
}
