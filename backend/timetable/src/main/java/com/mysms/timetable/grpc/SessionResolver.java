package com.mysms.timetable.grpc;

import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.timetable.api.model.Session;
import com.mysms.timetable.api.model.SessionStudent;
import com.mysms.timetable.api.model.SessionStudentUpdate;
import com.mysms.timetable.api.model.SessionUpdate;
import com.mysms.timetable.api.model.TimetableView;
import com.mysms.timetable.business.SessionService;
import com.mysms.timetable.v1.BatchConfirmSessionsRequest;
import com.mysms.timetable.v1.BatchConfirmSessionsResponse;
import com.mysms.timetable.v1.CancelSessionRequest;
import com.mysms.timetable.v1.CancelSessionResponse;
import com.mysms.timetable.v1.CompleteSessionRequest;
import com.mysms.timetable.v1.CompleteSessionResponse;
import com.mysms.timetable.v1.ConfirmSessionRequest;
import com.mysms.timetable.v1.ConfirmSessionResponse;
import com.mysms.timetable.v1.CreateSessionRequest;
import com.mysms.timetable.v1.CreateSessionResponse;
import com.mysms.timetable.v1.CreateSessionStudentRequest;
import com.mysms.timetable.v1.CreateSessionStudentResponse;
import com.mysms.timetable.v1.DeleteSessionRequest;
import com.mysms.timetable.v1.DeleteSessionResponse;
import com.mysms.timetable.v1.DeleteSessionStudentRequest;
import com.mysms.timetable.v1.DeleteSessionStudentResponse;
import com.mysms.timetable.v1.GenerateSessionsRequest;
import com.mysms.timetable.v1.GenerateSessionsResponse;
import com.mysms.timetable.v1.GetSessionRequest;
import com.mysms.timetable.v1.GetSessionResponse;
import com.mysms.timetable.v1.GetSessionStudentRequest;
import com.mysms.timetable.v1.GetSessionStudentResponse;
import com.mysms.timetable.v1.GetTimetableRequest;
import com.mysms.timetable.v1.GetTimetableResponse;
import com.mysms.timetable.v1.ListSessionStudentsRequest;
import com.mysms.timetable.v1.ListSessionStudentsResponse;
import com.mysms.timetable.v1.ListSessionsRequest;
import com.mysms.timetable.v1.ListSessionsResponse;
import com.mysms.timetable.v1.ResetSessionRequest;
import com.mysms.timetable.v1.ResetSessionResponse;
import com.mysms.timetable.v1.SessionServiceGrpc;
import com.mysms.timetable.v1.UpdateSessionRequest;
import com.mysms.timetable.v1.UpdateSessionResponse;
import com.mysms.timetable.v1.UpdateSessionStudentRequest;
import com.mysms.timetable.v1.UpdateSessionStudentResponse;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of timetable.SessionService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class SessionResolver extends SessionServiceGrpc.SessionServiceImplBase {

    private final SessionService service;
    private final ProtoBridge bridge;

    public SessionResolver(SessionService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listSessions(ListSessionsRequest request, StreamObserver<ListSessionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Session> rows = service.listSessions(page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()),
                page.sort());
        ProtoBridge.send(out, bridge.page(ListSessionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSession(GetSessionRequest request, StreamObserver<GetSessionResponse> out) {
        Session found = service.getSession(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSessionResponse.newBuilder(), "session", found).build());
    }

    @Override
    public void createSession(CreateSessionRequest request, StreamObserver<CreateSessionResponse> out) {
        Session created = service.createSession(bridge.body(request.getSession(), Session.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSessionResponse.newBuilder(), "session", created).build());
    }

    @Override
    public void updateSession(UpdateSessionRequest request, StreamObserver<UpdateSessionResponse> out) {
        Session updated = service.updateSession(bridge.id(request.getSession().getId(), "id"),
                bridge.requiredIfMatch(request.getSession().getEtag()),
                bridge.changes(request.getSession(), request.getUpdateMask(), SessionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSessionResponse.newBuilder(), "session", updated).build());
    }

    @Override
    public void deleteSession(DeleteSessionRequest request, StreamObserver<DeleteSessionResponse> out) {
        service.deleteSession(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSessionResponse.getDefaultInstance());
    }

    @Override
    public void listSessionStudents(ListSessionStudentsRequest request, StreamObserver<ListSessionStudentsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<SessionStudent> rows = service.listSessionStudents(bridge.id(request.getSessionId(), "sessionId"), page.page(),
                page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListSessionStudentsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getSessionStudent(GetSessionStudentRequest request, StreamObserver<GetSessionStudentResponse> out) {
        SessionStudent found = service.getSessionStudent(bridge.id(request.getSessionId(), "sessionId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetSessionStudentResponse.newBuilder(), "session_student", found).build());
    }

    @Override
    public void createSessionStudent(CreateSessionStudentRequest request, StreamObserver<CreateSessionStudentResponse> out) {
        SessionStudent created = service.createSessionStudent(bridge.id(request.getSessionId(), "sessionId"),
                bridge.body(request.getSessionStudent(), SessionStudent.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateSessionStudentResponse.newBuilder(), "session_student", created).build());
    }

    @Override
    public void updateSessionStudent(UpdateSessionStudentRequest request, StreamObserver<UpdateSessionStudentResponse> out) {
        SessionStudent updated = service.updateSessionStudent(bridge.id(request.getSessionId(), "sessionId"),
                bridge.id(request.getSessionStudent().getId(), "id"),
                bridge.requiredIfMatch(request.getSessionStudent().getEtag()),
                bridge.changes(request.getSessionStudent(), request.getUpdateMask(), SessionStudentUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateSessionStudentResponse.newBuilder(), "session_student", updated).build());
    }

    @Override
    public void deleteSessionStudent(DeleteSessionStudentRequest request, StreamObserver<DeleteSessionStudentResponse> out) {
        service.deleteSessionStudent(bridge.id(request.getSessionId(), "sessionId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteSessionStudentResponse.getDefaultInstance());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void confirmSession(ConfirmSessionRequest request, StreamObserver<ConfirmSessionResponse> out) {
        Session changed = service.confirmSession(bridge.id(request.getSessionId(), "sessionId"),
                bridge.body(request, com.mysms.timetable.api.model.ConfirmSessionRequest.class, "session_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ConfirmSessionResponse.newBuilder(), "session", changed).build());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void completeSession(CompleteSessionRequest request, StreamObserver<CompleteSessionResponse> out) {
        Session changed = service.completeSession(bridge.id(request.getSessionId(), "sessionId"),
                bridge.body(request, com.mysms.timetable.api.model.CompleteSessionRequest.class, "session_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CompleteSessionResponse.newBuilder(), "session", changed).build());
    }

    /** The etag (optional) and the reason are the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void cancelSession(CancelSessionRequest request, StreamObserver<CancelSessionResponse> out) {
        Session changed = service.cancelSession(bridge.id(request.getSessionId(), "sessionId"),
                bridge.body(request, com.mysms.timetable.api.model.CancelSessionRequest.class, "session_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelSessionResponse.newBuilder(), "session", changed).build());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void resetSession(ResetSessionRequest request, StreamObserver<ResetSessionResponse> out) {
        Session changed = service.resetSession(bridge.id(request.getSessionId(), "sessionId"),
                bridge.body(request, com.mysms.timetable.api.model.ResetSessionRequest.class, "session_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ResetSessionResponse.newBuilder(), "session", changed).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void batchConfirmSessions(BatchConfirmSessionsRequest request, StreamObserver<BatchConfirmSessionsResponse> out) {
        throw new NotImplementedException();
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void generateSessions(GenerateSessionsRequest request, StreamObserver<GenerateSessionsResponse> out) {
        throw new NotImplementedException();
    }

    /**
     * REST's query string as fields: the two dates are required, the ids of whose timetable is asked for optional; the
     * page comes from pageable (the view has one order, so its sort is not read — REST has no sort parameter either).
     */
    @Override
    public void getTimetable(GetTimetableRequest request, StreamObserver<GetTimetableResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<TimetableView> rows = service.getTimetable(date(request.getFromDate(), "fromDate"),
                date(request.getToDate(), "toDate"), optionalId(request.getClassSectionId(), "classSectionId"),
                optionalId(request.getBatchId(), "batchId"), optionalId(request.getStaffId(), "staffId"),
                optionalId(request.getStudentId(), "studentId"), optionalId(request.getRoomId(), "roomId"),
                page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(GetTimetableResponse.newBuilder(), rows).build());
    }

    /** A required ISO date of REST's query string: 400 when absent or malformed, like REST's parameter binding. */
    private static LocalDate date(String value, String name) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidArgumentException("Invalid value for " + name + ".");
        }
    }

    /** An optional id of REST's query string: empty is "not sent". */
    private UUID optionalId(String value, String name) {
        return value.isEmpty() ? null : bridge.id(value, name);
    }
}
