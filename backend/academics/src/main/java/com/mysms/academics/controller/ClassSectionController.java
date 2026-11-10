package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.ClassSectionService;
import com.mysms.academics.api.ClassSectionApi;
import com.mysms.academics.api.model.ClassSection;
import com.mysms.academics.api.model.ClassSectionUpdate;
import com.mysms.academics.api.model.ClassSubject;
import com.mysms.academics.api.model.ClassSubjectUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ClassSection operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/academics.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class ClassSectionController implements ClassSectionApi {

    private final ClassSectionService classSectionService;

    public ClassSectionController(ClassSectionService classSectionService) {
        this.classSectionService = classSectionService;
    }

    @Override
    public ResponseEntity<ClassSection> createClassSection(ClassSection classSection, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ClassSection created = classSectionService.createClassSection(classSection, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/class-sections/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<ClassSubject> createClassSubject(UUID classSectionId, ClassSubject classSubject,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ClassSubject created = classSectionService.createClassSubject(classSectionId, classSubject, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/academics/class-sections/" + classSectionId + "/subjects/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteClassSection(UUID classSectionId, String ifMatch) {
        classSectionService.deleteClassSection(classSectionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteClassSubject(UUID classSectionId, UUID classSubjectId,
            String ifMatch) {
        classSectionService.deleteClassSubject(classSectionId, classSubjectId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ClassSection> getClassSection(UUID classSectionId, String fields) {
        ClassSection found = classSectionService.getClassSection(classSectionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<ClassSubject> getClassSubject(UUID classSectionId, UUID classSubjectId,
            String fields) {
        ClassSubject found = classSectionService.getClassSubject(classSectionId, classSubjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ClassSection>> listClassSections(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<ClassSection> list =
                PagingUtil.pagingInfo(classSectionService.listClassSections(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<ClassSubject>> listClassSubjects(UUID classSectionId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ClassSubject> list =
                PagingUtil.pagingInfo(classSectionService.listClassSubjects(classSectionId, page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ClassSection> updateClassSection(UUID classSectionId, String ifMatch,
            ClassSectionUpdate classSectionUpdate, String updateMask, Boolean validateOnly) {
        ClassSection updated =
                classSectionService.updateClassSection(classSectionId, ifMatch, classSectionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<ClassSubject> updateClassSubject(UUID classSectionId, UUID classSubjectId,
            String ifMatch, ClassSubjectUpdate classSubjectUpdate, String updateMask, Boolean validateOnly) {
        ClassSubject updated =
                classSectionService.updateClassSubject(classSectionId, classSubjectId, ifMatch, classSubjectUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
