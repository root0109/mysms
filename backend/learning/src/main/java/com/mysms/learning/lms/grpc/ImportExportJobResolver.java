package com.mysms.learning.lms.grpc;

import com.mysms.learning.lms.business.CoursePackageService;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.ImportExportJob;
import com.mysms.learning.lms.business.ImportExportJobService;
import com.mysms.lms.v1.CancelImportExportJobRequest;
import com.mysms.lms.v1.CancelImportExportJobResponse;
import com.mysms.lms.v1.CreateImportExportJobRequest;
import com.mysms.lms.v1.CreateImportExportJobResponse;
import com.mysms.lms.v1.GetImportExportJobRequest;
import com.mysms.lms.v1.GetImportExportJobResponse;
import com.mysms.lms.v1.GetJobPackageRequest;
import com.mysms.lms.v1.GetJobPackageResponse;
import com.mysms.lms.v1.ImportExportJobServiceGrpc;
import com.mysms.lms.v1.ListImportExportJobsRequest;
import com.mysms.lms.v1.ListImportExportJobsResponse;
import com.mysms.lms.v1.RestoreCohortArchiveRequest;
import com.mysms.lms.v1.RestoreCohortArchiveResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.ImportExportJobService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ImportExportJobResolver extends ImportExportJobServiceGrpc.ImportExportJobServiceImplBase {

    private final ImportExportJobService service;
    private final ProtoBridge bridge;
    private final CoursePackageService packages;

    public ImportExportJobResolver(ImportExportJobService service, ProtoBridge bridge, CoursePackageService packages) {
        this.packages = packages;
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listImportExportJobs(ListImportExportJobsRequest request,
            StreamObserver<ListImportExportJobsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ImportExportJob> rows = service.listImportExportJobs(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListImportExportJobsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getImportExportJob(GetImportExportJobRequest request, StreamObserver<GetImportExportJobResponse> out) {
        ImportExportJob found = service.getImportExportJob(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(
                GetImportExportJobResponse.newBuilder(), "import_export_job", found).build());
    }

    @Override
    public void createImportExportJob(CreateImportExportJobRequest request,
            StreamObserver<CreateImportExportJobResponse> out) {
        ImportExportJob created = service.createImportExportJob(
                bridge.body(request.getImportExportJob(), ImportExportJob.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(
                CreateImportExportJobResponse.newBuilder(), "import_export_job", created).build());
    }

    @Override
    public void cancelImportExportJob(CancelImportExportJobRequest request,
            StreamObserver<CancelImportExportJobResponse> out) {
        ImportExportJob changed = service.cancelImportExportJob(
                bridge.id(request.getImportExportJobId(), "importExportJobId"),
                bridge.body(request, com.mysms.learning.lms.api.model.CancelImportExportJobRequest.class,
                        "import_export_job_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(
                CancelImportExportJobResponse.newBuilder(), "import_export_job", changed).build());
    }

    @Override
    public void restoreCohortArchive(RestoreCohortArchiveRequest request,
            StreamObserver<RestoreCohortArchiveResponse> out) {
        ImportExportJob restored = packages.restoreCohortArchive(
                bridge.id(request.getImportExportJobId(), "importExportJobId"),
                bridge.body(request, com.mysms.learning.lms.api.model.RestoreCohortArchiveRequest.class,
                        "import_export_job_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(RestoreCohortArchiveResponse.newBuilder(), "import_export_job", restored)
                .build());
    }

    @Override
    public void getJobPackage(GetJobPackageRequest request, StreamObserver<GetJobPackageResponse> out) {
        ProtoBridge.send(out, bridge.reply(GetJobPackageResponse.newBuilder(), "result",
                packages.getJobPackage(bridge.id(request.getImportExportJobId(), "importExportJobId"))).build());
    }
}
