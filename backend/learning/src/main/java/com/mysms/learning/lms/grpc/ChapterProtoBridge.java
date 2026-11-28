package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.learning.lms.api.model.ChapterUpdate;

/**
 * UpdateChapter over gRPC (JAVA-CODING-RULES §7.2): the {@code lms.v1.ChapterUpdate} message as REST's {@link
 * ChapterUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class ChapterProtoBridge {

    private ChapterProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static ChapterUpdate changes(ProtoBridge bridge, com.mysms.lms.v1.ChapterUpdate request) {
        ChapterUpdate changes = new ChapterUpdate();
        changes.setTitle(ProtoBridge.text(request.hasTitle(), request.getTitle()));
        changes.setSortOrder(ProtoBridge.number(request.hasSortOrder(), request.getSortOrder()));
        changes.setIsScormPackage(ProtoBridge.flag(request.hasIsScormPackage(), request.getIsScormPackage()));
        changes.setScormPackageFileId(bridge.uuid(
                request.hasScormPackageFileId(), request.getScormPackageFileId(), "scorm_package_file_id"));
        changes.setScormPackagePath(ProtoBridge.text(request.hasScormPackagePath(), request.getScormPackagePath()));
        changes.setScormManifestFile(ProtoBridge.text(request.hasScormManifestFile(), request.getScormManifestFile()));
        changes.setScormLaunchFile(ProtoBridge.text(request.hasScormLaunchFile(), request.getScormLaunchFile()));
        changes.setCohortId(bridge.uuid(request.hasCohortId(), request.getCohortId(), "cohort_id"));
        changes.setParentChapterId(bridge.uuid(
                request.hasParentChapterId(), request.getParentChapterId(), "parent_chapter_id"));
        changes.setDescription(ProtoBridge.text(request.hasDescription(), request.getDescription()));
        changes.setChapterNumber(ProtoBridge.number(request.hasChapterNumber(), request.getChapterNumber()));
        changes.setStartDay(ProtoBridge.number(request.hasStartDay(), request.getStartDay()));
        changes.setStartTime(ProtoBridge.text(request.hasStartTime(), request.getStartTime()));
        changes.setDueDay(ProtoBridge.number(request.hasDueDay(), request.getDueDay()));
        changes.setDueTime(ProtoBridge.text(request.hasDueTime(), request.getDueTime()));
        changes.setStartsAt(ProtoBridge.time(request.hasStartsAt(), request.getStartsAt()));
        changes.setEndsAt(ProtoBridge.time(request.hasEndsAt(), request.getEndsAt()));
        changes.setAllowDelete(ProtoBridge.flag(request.hasAllowDelete(), request.getAllowDelete()));
        changes.setAllowEditDetails(ProtoBridge.flag(request.hasAllowEditDetails(), request.getAllowEditDetails()));
        changes.setAllowEditDates(ProtoBridge.flag(request.hasAllowEditDates(), request.getAllowEditDates()));
        changes.setCopiedFromId(bridge.uuid(request.hasCopiedFromId(), request.getCopiedFromId(), "copied_from_id"));
        changes.setIsOriginal(ProtoBridge.flag(request.hasIsOriginal(), request.getIsOriginal()));
        return bridge.patch(changes);
    }
}
