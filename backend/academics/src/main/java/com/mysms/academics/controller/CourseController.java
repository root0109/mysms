package com.mysms.academics.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.academics.business.CourseService;
import com.mysms.academics.api.CourseApi;
import com.mysms.academics.api.model.Course;
import com.mysms.academics.api.model.CourseSubject;
import com.mysms.academics.api.model.CourseSubjectUpdate;
import com.mysms.academics.api.model.CourseUpdate;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Course operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class CourseController implements CourseApi {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Override
    public ResponseEntity<Course> createCourse(Course course, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Course created = courseService.createCourse(course, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/courses/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<CourseSubject> createCourseSubject(UUID courseId, CourseSubject courseSubject,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CourseSubject created = courseService.createCourseSubject(courseId, courseSubject, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/academics/courses/" + courseId + "/subjects/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCourse(UUID courseId, String ifMatch) {
        courseService.deleteCourse(courseId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCourseSubject(UUID courseId, UUID courseSubjectId, String ifMatch) {
        courseService.deleteCourseSubject(courseId, courseSubjectId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Course> getCourse(UUID courseId, String fields) {
        Course found = courseService.getCourse(courseId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CourseSubject> getCourseSubject(UUID courseId, UUID courseSubjectId,
            String fields) {
        CourseSubject found = courseService.getCourseSubject(courseId, courseSubjectId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CourseSubject>> listCourseSubjects(UUID courseId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CourseSubject> list =
                PagingUtil.pagingInfo(courseService.listCourseSubjects(courseId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Course>> listCourses(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Course> list = PagingUtil.pagingInfo(courseService.listCourses(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Course> updateCourse(UUID courseId, String ifMatch, CourseUpdate courseUpdate,
            Boolean validateOnly) {
        Course updated = courseService.updateCourse(courseId, ifMatch, courseUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CourseSubject> updateCourseSubject(UUID courseId, UUID courseSubjectId,
            String ifMatch, CourseSubjectUpdate courseSubjectUpdate, Boolean validateOnly) {
        CourseSubject updated =
                courseService.updateCourseSubject(courseId, courseSubjectId, ifMatch, courseSubjectUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
