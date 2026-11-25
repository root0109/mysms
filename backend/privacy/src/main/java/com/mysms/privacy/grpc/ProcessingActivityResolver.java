package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.privacy.api.model.ProcessingActivity;
import com.mysms.privacy.api.model.ProcessingRegisterExport;
import com.mysms.privacy.business.ProcessingActivityService;
import com.mysms.privacy.v1.GetProcessingRegisterExportRequest;
import com.mysms.privacy.v1.GetProcessingRegisterExportResponse;
import com.mysms.privacy.v1.ListProcessingActivitiesRequest;
import com.mysms.privacy.v1.ListProcessingActivitiesResponse;
import com.mysms.privacy.v1.ProcessingActivityServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.ProcessingActivityService: delegates to the same business service as REST.
 * Authentication, permission and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ProcessingActivityResolver extends ProcessingActivityServiceGrpc.ProcessingActivityServiceImplBase {

    private final ProcessingActivityService service;
    private final ProtoBridge bridge;

    public ProcessingActivityResolver(ProcessingActivityService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listProcessingActivities(ListProcessingActivitiesRequest request,
            StreamObserver<ListProcessingActivitiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ProcessingActivity> rows = service.listProcessingActivities(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListProcessingActivitiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getProcessingRegisterExport(GetProcessingRegisterExportRequest request,
            StreamObserver<GetProcessingRegisterExportResponse> out) {
        ProcessingRegisterExport export = service.getProcessingRegisterExport();
        ProtoBridge.send(out, bridge.reply(GetProcessingRegisterExportResponse.newBuilder(),
                "processing_register_export", export).build());
    }
}
