package com.mysms.timetable.controller;

import com.mysms.timetable.api.model.TimetableView;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.timetable.api.SessionApi;
import com.mysms.timetable.api.model.BatchConfirmSessionsRequest;
import com.mysms.timetable.api.model.CancelSessionRequest;
import com.mysms.timetable.api.model.CompleteSessionRequest;
import com.mysms.timetable.api.model.ConfirmSessionRequest;
import com.mysms.timetable.api.model.GenerateSessionsRequest;
import com.mysms.timetable.api.model.Operation;
import com.mysms.timetable.api.model.ResetSessionRequest;
import com.mysms.timetable.api.model.Session;
import com.mysms.timetable.api.model.SessionList;
import com.mysms.timetable.api.model.SessionStudent;
import com.mysms.timetable.api.model.SessionStudentUpdate;
import com.mysms.timetable.api.model.SessionUpdate;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.web.ReadMask;
import com.mysms.timetable.business.SessionService;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Session operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class SessionController implements SessionApi {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public ResponseEntity<SessionList> batchConfirmSessions(
            BatchConfirmSessionsRequest batchConfirmSessionsRequest, UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Session> cancelSession(UUID sessionId,
            CancelSessionRequest cancelSessionRequest, UUID idempotencyKey) {
        Session changed = sessionService.cancelSession(sessionId, cancelSessionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Session> completeSession(UUID sessionId,
            CompleteSessionRequest completeSessionRequest, UUID idempotencyKey) {
        Session changed = sessionService.completeSession(sessionId, completeSessionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Session> confirmSession(UUID sessionId,
            ConfirmSessionRequest confirmSessionRequest, UUID idempotencyKey) {
        Session changed = sessionService.confirmSession(sessionId, confirmSessionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Session> createSession(Session session, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Session created = sessionService.createSession(session, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/timetable/sessions/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<SessionStudent> createSessionStudent(UUID sessionId,
            SessionStudent sessionStudent, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        SessionStudent created = sessionService.createSessionStudent(sessionId, sessionStudent, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/timetable/sessions/" + sessionId + "/students/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteSession(UUID sessionId, String ifMatch) {
        sessionService.deleteSession(sessionId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteSessionStudent(UUID sessionId, UUID sessionStudentId,
            String ifMatch) {
        sessionService.deleteSessionStudent(sessionId, sessionStudentId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Operation> generateSessions(GenerateSessionsRequest generateSessionsRequest,
            UUID idempotencyKey) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Session> getSession(UUID sessionId, String fields) {
        Session found = sessionService.getSession(sessionId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<SessionStudent> getSessionStudent(UUID sessionId, UUID sessionStudentId,
            String fields) {
        SessionStudent found = sessionService.getSessionStudent(sessionId, sessionStudentId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<TimetableView>> getTimetable(LocalDate fromDate, LocalDate toDate,
            UUID classSectionId, UUID batchId, UUID staffId, UUID studentId, UUID roomId, Integer page, Integer size,
            String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(sessionService.getTimetable(fromDate, toDate, classSectionId,
                batchId, staffId, studentId, roomId, page, size, cursor)));
    }

    @Override
    public ResponseEntity<PagingInfo<SessionStudent>> listSessionStudents(UUID sessionId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<SessionStudent> list =
                PagingUtil.pagingInfo(sessionService.listSessionStudents(sessionId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Session>> listSessions(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Session> list = PagingUtil.pagingInfo(sessionService.listSessions(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Session> resetSession(UUID sessionId, ResetSessionRequest resetSessionRequest,
            UUID idempotencyKey) {
        Session changed = sessionService.resetSession(sessionId, resetSessionRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<Session> updateSession(UUID sessionId, String ifMatch,
            SessionUpdate sessionUpdate, String updateMask, Boolean validateOnly) {
        Session updated = sessionService.updateSession(sessionId, ifMatch, sessionUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<SessionStudent> updateSessionStudent(UUID sessionId, UUID sessionStudentId,
            String ifMatch, SessionStudentUpdate sessionStudentUpdate, String updateMask, Boolean validateOnly) {
        SessionStudent updated =
                sessionService.updateSessionStudent(sessionId, sessionStudentId, ifMatch, sessionStudentUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
