package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.LessonProgressApi;
import com.mysms.lms.api.model.LessonProgress;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.LessonProgressService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LessonProgress operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/lms.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class LessonProgressController implements LessonProgressApi {

    private final LessonProgressService lessonProgressService;

    public LessonProgressController(LessonProgressService lessonProgressService) {
        this.lessonProgressService = lessonProgressService;
    }

    @Override
    public ResponseEntity<LessonProgress> getLessonProgress(UUID lessonProgressId, String fields) {
        LessonProgress found = lessonProgressService.getLessonProgress(lessonProgressId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LessonProgress>> listLessonProgress(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LessonProgress> list =
                PagingUtil.pagingInfo(lessonProgressService.listLessonProgresss(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }
}
