package com.mysms.attendance.grpc;

import com.mysms.attendance.api.model.AttendanceRecord;
import com.mysms.attendance.api.model.AttendanceRecordList;
import com.mysms.attendance.api.model.AttendanceSheet;
import com.mysms.attendance.api.model.AttendanceSheetUpdate;
import com.mysms.attendance.api.model.AttendanceSummary;
import com.mysms.attendance.business.AttendanceSheetService;
import com.mysms.attendance.business.AttendanceSummaryService;
import com.mysms.attendance.business.AttendanceTakingService;
import com.mysms.attendance.v1.AttendanceSheetServiceGrpc;
import com.mysms.attendance.v1.CancelAttendanceSheetRequest;
import com.mysms.attendance.v1.CancelAttendanceSheetResponse;
import com.mysms.attendance.v1.CompleteAttendanceSheetRequest;
import com.mysms.attendance.v1.CompleteAttendanceSheetResponse;
import com.mysms.attendance.v1.CreateAttendanceSheetRequest;
import com.mysms.attendance.v1.CreateAttendanceSheetResponse;
import com.mysms.attendance.v1.DeleteAttendanceSheetRequest;
import com.mysms.attendance.v1.DeleteAttendanceSheetResponse;
import com.mysms.attendance.v1.GetAttendanceRecordRequest;
import com.mysms.attendance.v1.GetAttendanceRecordResponse;
import com.mysms.attendance.v1.GetAttendanceSheetRequest;
import com.mysms.attendance.v1.GetAttendanceSheetResponse;
import com.mysms.attendance.v1.ListAttendanceRecordsRequest;
import com.mysms.attendance.v1.ListAttendanceRecordsResponse;
import com.mysms.attendance.v1.ListAttendanceSheetsRequest;
import com.mysms.attendance.v1.ListAttendanceSheetsResponse;
import com.mysms.attendance.v1.RecordAttendanceRequest;
import com.mysms.attendance.v1.RecordAttendanceResponse;
import com.mysms.attendance.v1.ResetAttendanceSheetRequest;
import com.mysms.attendance.v1.ResetAttendanceSheetResponse;
import com.mysms.attendance.v1.StartAttendanceSheetRequest;
import com.mysms.attendance.v1.StartAttendanceSheetResponse;
import com.mysms.attendance.v1.SummarizeAttendanceRequest;
import com.mysms.attendance.v1.SummarizeAttendanceResponse;
import com.mysms.attendance.v1.TakeAttendanceForSessionRequest;
import com.mysms.attendance.v1.TakeAttendanceForSessionResponse;
import com.mysms.attendance.v1.UpdateAttendanceSheetRequest;
import com.mysms.attendance.v1.UpdateAttendanceSheetResponse;
import com.mysms.core.error.InvalidArgumentException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of attendance.AttendanceSheetService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class AttendanceSheetResolver extends AttendanceSheetServiceGrpc.AttendanceSheetServiceImplBase {

    private final AttendanceSheetService service;
    private final AttendanceTakingService taking;
    private final AttendanceSummaryService summary;
    private final ProtoBridge bridge;

    public AttendanceSheetResolver(AttendanceSheetService service, AttendanceTakingService taking,
            AttendanceSummaryService summary, ProtoBridge bridge) {
        this.service = service;
        this.taking = taking;
        this.summary = summary;
        this.bridge = bridge;
    }

    @Override
    public void listAttendanceSheets(ListAttendanceSheetsRequest request, StreamObserver<ListAttendanceSheetsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AttendanceSheet> rows = service.listAttendanceSheets(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAttendanceSheetsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAttendanceSheet(GetAttendanceSheetRequest request, StreamObserver<GetAttendanceSheetResponse> out) {
        AttendanceSheet found = service.getAttendanceSheet(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAttendanceSheetResponse.newBuilder(), "attendance_sheet", found).build());
    }

    @Override
    public void createAttendanceSheet(CreateAttendanceSheetRequest request, StreamObserver<CreateAttendanceSheetResponse> out) {
        AttendanceSheet created = service.createAttendanceSheet(bridge.body(request.getAttendanceSheet(), AttendanceSheet.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateAttendanceSheetResponse.newBuilder(), "attendance_sheet", created).build());
    }

    @Override
    public void updateAttendanceSheet(UpdateAttendanceSheetRequest request, StreamObserver<UpdateAttendanceSheetResponse> out) {
        AttendanceSheet updated = service.updateAttendanceSheet(bridge.id(request.getAttendanceSheet().getId(), "id"),
                bridge.requiredIfMatch(request.getAttendanceSheet().getEtag()),
                bridge.changes(request.getAttendanceSheet(), request.getUpdateMask(), AttendanceSheetUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateAttendanceSheetResponse.newBuilder(), "attendance_sheet", updated).build());
    }

    @Override
    public void deleteAttendanceSheet(DeleteAttendanceSheetRequest request, StreamObserver<DeleteAttendanceSheetResponse> out) {
        service.deleteAttendanceSheet(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteAttendanceSheetResponse.getDefaultInstance());
    }

    @Override
    public void listAttendanceRecords(ListAttendanceRecordsRequest request, StreamObserver<ListAttendanceRecordsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AttendanceRecord> rows = service.listAttendanceRecords(
                bridge.id(request.getAttendanceSheetId(), "attendanceSheetId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListAttendanceRecordsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getAttendanceRecord(GetAttendanceRecordRequest request, StreamObserver<GetAttendanceRecordResponse> out) {
        AttendanceRecord found = service.getAttendanceRecord(bridge.id(request.getAttendanceSheetId(), "attendanceSheetId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetAttendanceRecordResponse.newBuilder(), "attendance_record", found).build());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void startAttendanceSheet(StartAttendanceSheetRequest request, StreamObserver<StartAttendanceSheetResponse> out) {
        AttendanceSheet changed = service.startAttendanceSheet(bridge.id(request.getAttendanceSheetId(), "attendanceSheetId"),
                bridge.body(request, com.mysms.attendance.api.model.StartAttendanceSheetRequest.class, "attendance_sheet_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(StartAttendanceSheetResponse.newBuilder(), "attendance_sheet", changed).build());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void completeAttendanceSheet(CompleteAttendanceSheetRequest request,
            StreamObserver<CompleteAttendanceSheetResponse> out) {
        AttendanceSheet changed = taking.completeAttendanceSheet(bridge.id(request.getAttendanceSheetId(), "attendanceSheetId"),
                bridge.body(request, com.mysms.attendance.api.model.CompleteAttendanceSheetRequest.class, "attendance_sheet_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CompleteAttendanceSheetResponse.newBuilder(), "attendance_sheet", changed).build());
    }

    /** The etag (optional) and the reason are the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void cancelAttendanceSheet(CancelAttendanceSheetRequest request, StreamObserver<CancelAttendanceSheetResponse> out) {
        AttendanceSheet changed = service.cancelAttendanceSheet(bridge.id(request.getAttendanceSheetId(), "attendanceSheetId"),
                bridge.body(request, com.mysms.attendance.api.model.CancelAttendanceSheetRequest.class, "attendance_sheet_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelAttendanceSheetResponse.newBuilder(), "attendance_sheet", changed).build());
    }

    /** The etag (optional) is the REST body's; the path id and the idempotency key travel apart. */
    @Override
    public void resetAttendanceSheet(ResetAttendanceSheetRequest request, StreamObserver<ResetAttendanceSheetResponse> out) {
        AttendanceSheet changed = service.resetAttendanceSheet(bridge.id(request.getAttendanceSheetId(), "attendanceSheetId"),
                bridge.body(request, com.mysms.attendance.api.model.ResetAttendanceSheetRequest.class, "attendance_sheet_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(ResetAttendanceSheetResponse.newBuilder(), "attendance_sheet", changed).build());
    }

    /**
     * The marks, {@code complete} and the sheet's etag are the REST body's; the path id and the idempotency key travel
     * apart. Every record written comes back in one response — it is not a page.
     */
    @Override
    public void recordAttendance(RecordAttendanceRequest request, StreamObserver<RecordAttendanceResponse> out) {
        AttendanceRecordList written = taking.recordAttendance(
                bridge.id(request.getAttendanceSheetId(), "attendanceSheetId"),
                bridge.body(request, com.mysms.attendance.api.model.RecordAttendanceRequest.class, "attendance_sheet_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(RecordAttendanceResponse.newBuilder(), "items", written.getItems()).build());
    }

    /** The session is the REST body's; the idempotency key travels apart. */
    @Override
    public void takeAttendanceForSession(TakeAttendanceForSessionRequest request,
            StreamObserver<TakeAttendanceForSessionResponse> out) {
        AttendanceSheet sheet = taking.takeAttendanceForSession(bridge.body(request,
                com.mysms.attendance.api.model.TakeAttendanceForSessionRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(TakeAttendanceForSessionResponse.newBuilder(), "attendance_sheet",
                sheet).build());
    }

    /**
     * REST's query string as fields: the institution, the two dates and the grouping are required, the student and the
     * class optional; the page comes from pageable (each grouping has one order, so its sort is not read).
     */
    @Override
    public void summarizeAttendance(SummarizeAttendanceRequest request, StreamObserver<SummarizeAttendanceResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<AttendanceSummary> rows = summary.summarizeAttendance(bridge.id(request.getInstitutionId(), "institutionId"),
                date(request.getFromDate(), "fromDate"), date(request.getToDate(), "toDate"),
                groupBy(request.getGroupBy()), optionalId(request.getStudentId(), "studentId"),
                optionalId(request.getClassSectionId(), "classSectionId"), page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(SummarizeAttendanceResponse.newBuilder(), rows).build());
    }

    /** REST's {@code groupBy} value of the grouping; null when it is not set, which the service refuses like REST. */
    private static String groupBy(SummarizeAttendanceRequest.GroupBy grouping) {
        return switch (grouping) {
            case GROUP_BY_STUDENT -> "student";
            case GROUP_BY_CLASS_SECTION -> "class_section";
            case GROUP_BY_DATE -> "date";
            case GROUP_BY_UNSPECIFIED, UNRECOGNIZED -> null;
        };
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
