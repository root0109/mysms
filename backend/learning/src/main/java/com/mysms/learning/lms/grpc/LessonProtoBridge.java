package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.LessonUpdate;

/**
 * UpdateLesson over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.LessonUpdate} message as REST's {@link
 * LessonUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class LessonProtoBridge {

    private LessonProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static LessonUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.LessonUpdate request) {
        LessonUpdate changes = new LessonUpdate();
        changes.setChapterId(bridge.uuid(request.hasChapterId(), request.getChapterId(), "chapter_id"));
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIncludeInPreview(ProtoBridge.flag(request.hasIncludeInPreview(), request.getIncludeInPreview()));
        changes.setBody(ProtoBridge.text(request.hasBody(), request.getBody()));
        changes.setContent(bridge.json(request.hasContent(), request.getContent()));
        changes.setTeacherNotes(ProtoBridge.text(request.hasTeacherNotes(), request.getTeacherNotes()));
        changes.setTeacherContent(bridge.json(request.hasTeacherContent(), request.getTeacherContent()));
        changes.setYoutubeVideoId(ProtoBridge.text(request.hasYoutubeVideoId(), request.getYoutubeVideoId()));
        changes.setEmbeddedQuestion(ProtoBridge.text(request.hasEmbeddedQuestion(), request.getEmbeddedQuestion()));
        changes.setFileType(bridge.choice(
                request.hasFileType(), request.getFileType(), LessonUpdate.FileTypeEnum::fromValue));
        changes.setIsScormPackage(ProtoBridge.flag(request.hasIsScormPackage(), request.getIsScormPackage()));
        return bridge.patch(changes);
    }
}
