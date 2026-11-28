package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.WebResourceUpdate;

/**
 * UpdateWebResource over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.WebResourceUpdate} message as REST's {@link
 * WebResourceUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class WebResourceProtoBridge {

    private WebResourceProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static WebResourceUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.WebResourceUpdate request) {
        WebResourceUpdate changes = new WebResourceUpdate();
        changes.setInstitutionId(bridge.uuid(request.hasInstitutionId(), request.getInstitutionId(), "institution_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setResourceType(bridge.choice(
                request.hasResourceType(), request.getResourceType(), WebResourceUpdate.ResourceTypeEnum::fromValue));
        changes.setUrl(ProtoBridge.text(request.hasUrl(), request.getUrl()));
        changes.setLaunchFile(ProtoBridge.text(request.hasLaunchFile(), request.getLaunchFile()));
        changes.setWindowFeatures(ProtoBridge.text(request.hasWindowFeatures(), request.getWindowFeatures()));
        changes.setCategoryId(bridge.uuid(request.hasCategoryId(), request.getCategoryId(), "category_id"));
        changes.setIsInRepository(ProtoBridge.flag(request.hasIsInRepository(), request.getIsInRepository()));
        return bridge.patch(changes);
    }
}
