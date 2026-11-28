package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.AnnotationUpdate;

/**
 * UpdateAnnotation over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.AnnotationUpdate} message as REST's {@link
 * AnnotationUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class AnnotationProtoBridge {

    private AnnotationProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static AnnotationUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.AnnotationUpdate request) {
        AnnotationUpdate changes = new AnnotationUpdate();
        changes.setContentItemId(bridge.uuid(
                request.hasContentItemId(), request.getContentItemId(), "content_item_id"));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setEbookId(bridge.uuid(request.hasEbookId(), request.getEbookId(), "ebook_id"));
        changes.setFileId(bridge.uuid(request.hasFileId(), request.getFileId(), "file_id"));
        changes.setAnnotationType(bridge.choice(
                request.hasAnnotationType(), request.getAnnotationType(),
                AnnotationUpdate.AnnotationTypeEnum::fromValue));
        changes.setAnnotationSubtype(ProtoBridge.text(request.hasAnnotationSubtype(), request.getAnnotationSubtype()));
        changes.setPageNumber(ProtoBridge.text(request.hasPageNumber(), request.getPageNumber()));
        changes.setStartCharPos(ProtoBridge.text(request.hasStartCharPos(), request.getStartCharPos()));
        changes.setCharCount(ProtoBridge.text(request.hasCharCount(), request.getCharCount()));
        changes.setLocation(ProtoBridge.text(request.hasLocation(), request.getLocation()));
        changes.setAnnotatedText(ProtoBridge.text(request.hasAnnotatedText(), request.getAnnotatedText()));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setEmbedCode(ProtoBridge.text(request.hasEmbedCode(), request.getEmbedCode()));
        changes.setUrl(ProtoBridge.text(request.hasUrl(), request.getUrl()));
        changes.setMediaFileId(bridge.uuid(request.hasMediaFileId(), request.getMediaFileId(), "media_file_id"));
        changes.setVisibility(bridge.choice(
                request.hasVisibility(), request.getVisibility(), AnnotationUpdate.VisibilityEnum::fromValue));
        changes.setForumTopicId(bridge.uuid(request.hasForumTopicId(), request.getForumTopicId(), "forum_topic_id"));
        return bridge.patch(changes);
    }
}
