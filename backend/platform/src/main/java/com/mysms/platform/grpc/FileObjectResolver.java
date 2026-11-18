package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.DownloadLink;
import com.mysms.platform.api.model.FileObject;
import com.mysms.platform.api.model.UploadTarget;
import com.mysms.platform.business.FileObjectService;
import com.mysms.platform.v1.CompleteUploadRequest;
import com.mysms.platform.v1.CompleteUploadResponse;
import com.mysms.platform.v1.CopyFileRequest;
import com.mysms.platform.v1.CopyFileResponse;
import com.mysms.platform.v1.CreateUploadUrlRequest;
import com.mysms.platform.v1.CreateUploadUrlResponse;
import com.mysms.platform.v1.DeleteFileObjectRequest;
import com.mysms.platform.v1.DeleteFileObjectResponse;
import com.mysms.platform.v1.FileObjectServiceGrpc;
import com.mysms.platform.v1.GetDownloadUrlRequest;
import com.mysms.platform.v1.GetDownloadUrlResponse;
import com.mysms.platform.v1.GetFileObjectRequest;
import com.mysms.platform.v1.GetFileObjectResponse;
import com.mysms.platform.v1.ListFileObjectsRequest;
import com.mysms.platform.v1.ListFileObjectsResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.FileObjectService, delegating to the business service REST uses. The bytes never travel
 * over gRPC: a client uploads to and downloads from the store through the signed URL the reply carries.
 */
@GrpcService
public class FileObjectResolver extends FileObjectServiceGrpc.FileObjectServiceImplBase {

    private final FileObjectService service;
    private final ProtoBridge bridge;

    public FileObjectResolver(FileObjectService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listFileObjects(ListFileObjectsRequest request, StreamObserver<ListFileObjectsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<FileObject> rows = service.listFileObjects(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListFileObjectsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getFileObject(GetFileObjectRequest request, StreamObserver<GetFileObjectResponse> out) {
        FileObject found = service.getFileObject(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetFileObjectResponse.newBuilder(), "file_object", found).build());
    }

    @Override
    public void deleteFileObject(DeleteFileObjectRequest request, StreamObserver<DeleteFileObjectResponse> out) {
        service.deleteFileObject(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteFileObjectResponse.getDefaultInstance());
    }

    @Override
    public void createUploadUrl(CreateUploadUrlRequest request, StreamObserver<CreateUploadUrlResponse> out) {
        UploadTarget target = service.createUploadUrl(
                bridge.body(request, com.mysms.platform.api.model.CreateUploadUrlRequest.class, "request_id"));
        ProtoBridge.send(out, bridge.reply(CreateUploadUrlResponse.newBuilder(), "result", target).build());
    }

    @Override
    public void completeUpload(CompleteUploadRequest request, StreamObserver<CompleteUploadResponse> out) {
        FileObject completed = service.completeUpload(bridge.id(request.getFileObjectId(), "fileObjectId"));
        ProtoBridge.send(out, bridge.reply(CompleteUploadResponse.newBuilder(), "file_object", completed).build());
    }

    @Override
    public void getDownloadUrl(GetDownloadUrlRequest request, StreamObserver<GetDownloadUrlResponse> out) {
        DownloadLink link = service.getDownloadUrl(bridge.id(request.getFileObjectId(), "fileObjectId"));
        ProtoBridge.send(out, bridge.reply(GetDownloadUrlResponse.newBuilder(), "result", link).build());
    }

    @Override
    public void copyFile(CopyFileRequest request, StreamObserver<CopyFileResponse> out) {
        FileObject copy = service.copyFile(bridge.id(request.getFileObjectId(), "fileObjectId"),
                bridge.body(request, com.mysms.platform.api.model.CopyFileRequest.class, "file_object_id",
                        "request_id"));
        ProtoBridge.send(out, bridge.reply(CopyFileResponse.newBuilder(), "file_object", copy).build());
    }
}
