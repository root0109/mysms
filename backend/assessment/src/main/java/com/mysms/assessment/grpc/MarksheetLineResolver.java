package com.mysms.assessment.grpc;

import com.mysms.assessment.api.model.MarksheetLine;
import com.mysms.assessment.api.model.ResultCard;
import com.mysms.assessment.business.MarksheetLineService;
import com.mysms.assessment.v1.GetMarksheetLineRequest;
import com.mysms.assessment.v1.GetMarksheetLineResponse;
import com.mysms.assessment.v1.GetStudentResultsRequest;
import com.mysms.assessment.v1.GetStudentResultsResponse;
import com.mysms.assessment.v1.ListMarksheetLinesRequest;
import com.mysms.assessment.v1.ListMarksheetLinesResponse;
import com.mysms.assessment.v1.MarksheetLineServiceGrpc;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of assessment.MarksheetLineService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class MarksheetLineResolver extends MarksheetLineServiceGrpc.MarksheetLineServiceImplBase {

    private final MarksheetLineService marksheetLineService;
    private final ProtoBridge bridge;

    public MarksheetLineResolver(MarksheetLineService marksheetLineService, ProtoBridge bridge) {
        this.marksheetLineService = marksheetLineService;
        this.bridge = bridge;
    }

    @Override
    public void listMarksheetLines(ListMarksheetLinesRequest request,
            StreamObserver<ListMarksheetLinesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<MarksheetLine> rows = marksheetLineService.listMarksheetLines(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListMarksheetLinesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getMarksheetLine(GetMarksheetLineRequest request, StreamObserver<GetMarksheetLineResponse> out) {
        MarksheetLine found = marksheetLineService.getMarksheetLine(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetMarksheetLineResponse.newBuilder(), "marksheet_line", found).build());
    }

    @Override
    public void getStudentResults(GetStudentResultsRequest request, StreamObserver<GetStudentResultsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ResultCard> rows = marksheetLineService.getStudentResults(
                bridge.id(request.getStudentId(), "studentId"),
                request.hasAcademicYearId() ? bridge.id(request.getAcademicYearId(), "academicYearId") : null,
                request.hasExamSessionId() ? bridge.id(request.getExamSessionId(), "examSessionId") : null,
                page.page(), page.size(), page.cursor());
        ProtoBridge.send(out, bridge.page(GetStudentResultsResponse.newBuilder(), rows).build());
    }
}
