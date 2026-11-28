package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.RelatedMaterialUpdate;

/**
 * UpdateRelatedMaterial over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.RelatedMaterialUpdate} message as REST's
 * {@link RelatedMaterialUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets
 * it.
 */
final class RelatedMaterialProtoBridge {

    private RelatedMaterialProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RelatedMaterialUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.RelatedMaterialUpdate request) {
        RelatedMaterialUpdate changes = new RelatedMaterialUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setUrl(ProtoBridge.text(request.hasUrl(), request.getUrl()));
        changes.setSummary(ProtoBridge.text(request.hasSummary(), request.getSummary()));
        changes.setSource(ProtoBridge.text(request.hasSource(), request.getSource()));
        changes.setMediaType(ProtoBridge.text(request.hasMediaType(), request.getMediaType()));
        changes.setContentLength(ProtoBridge.text(request.hasContentLength(), request.getContentLength()));
        changes.setContentLengthBand(ProtoBridge.text(request.hasContentLengthBand(), request.getContentLengthBand()));
        changes.setVideoLengthSeconds(ProtoBridge.text(
                request.hasVideoLengthSeconds(), request.getVideoLengthSeconds()));
        changes.setVideoLengthBand(ProtoBridge.text(request.hasVideoLengthBand(), request.getVideoLengthBand()));
        changes.setInitialRank(ProtoBridge.number(request.hasInitialRank(), request.getInitialRank()));
        changes.setInteractivityLevel(ProtoBridge.number(
                request.hasInteractivityLevel(), request.getInteractivityLevel()));
        changes.setInteractivityBand(ProtoBridge.text(request.hasInteractivityBand(), request.getInteractivityBand()));
        changes.setConceptualLevel(ProtoBridge.text(request.hasConceptualLevel(), request.getConceptualLevel()));
        changes.setLanguageComplexity(ProtoBridge.text(
                request.hasLanguageComplexity(), request.getLanguageComplexity()));
        changes.setLanguageComplexityBand(ProtoBridge.text(
                request.hasLanguageComplexityBand(), request.getLanguageComplexityBand()));
        return bridge.patch(changes);
    }
}
