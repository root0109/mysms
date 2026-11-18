package com.mysms.lms.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.platform.v1.CompleteUploadRequest;
import com.mysms.platform.v1.CompleteUploadResponse;
import com.mysms.platform.v1.CreateUploadUrlRequest;
import com.mysms.platform.v1.CreateUploadUrlResponse;
import com.mysms.platform.v1.EntityAttachmentServiceGrpc;
import com.mysms.platform.v1.FileObjectServiceGrpc;
import com.mysms.platform.v1.GetDownloadUrlRequest;
import com.mysms.platform.v1.GetDownloadUrlResponse;
import com.mysms.platform.v1.GetFileObjectRequest;
import com.mysms.platform.v1.GetFileObjectResponse;
import com.mysms.platform.v1.ListEntityAttachmentsRequest;
import com.mysms.platform.v1.ListEntityAttachmentsResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * lms's client of the platform service: the file store, through which every file lms reads (a course package, a
 * question file, a submission sent for a plagiarism check) and writes (an export package) passes. Every call carries
 * the caller's own identity, so platform applies the caller's own file permissions and quota. A failed call is an
 * {@link IntegrationGrpcClientException}.
 */
@Service
public class PlatformGrpcClient {

    /** The starter's client of the platform service. */
    public static final String PLATFORM_SERVICE_KEY = "platform-service";
    private static final String SERVICE = "platform";

    private FileObjectServiceGrpc.FileObjectServiceBlockingStub files;
    private EntityAttachmentServiceGrpc.EntityAttachmentServiceBlockingStub attachments;

    @GrpcClient(PLATFORM_SERVICE_KEY)
    public void initFileObjectStub(FileObjectServiceGrpc.FileObjectServiceBlockingStub stub) {
        this.files = stub;
    }

    @GrpcClient(PLATFORM_SERVICE_KEY)
    public void initEntityAttachmentStub(EntityAttachmentServiceGrpc.EntityAttachmentServiceBlockingStub stub) {
        this.attachments = stub;
    }

    public ListEntityAttachmentsResponse listEntityAttachments(ListEntityAttachmentsRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> attachments.listEntityAttachments(request));
    }

    public GetFileObjectResponse getFileObject(GetFileObjectRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> files.getFileObject(request));
    }

    public GetDownloadUrlResponse getDownloadUrl(GetDownloadUrlRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> files.getDownloadUrl(request));
    }

    public CreateUploadUrlResponse createUploadUrl(CreateUploadUrlRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> files.createUploadUrl(request));
    }

    public CompleteUploadResponse completeUpload(CompleteUploadRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> files.completeUpload(request));
    }
}
