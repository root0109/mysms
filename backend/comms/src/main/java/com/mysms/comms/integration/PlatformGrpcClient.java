package com.mysms.comms.integration;

import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.platform.v1.CompleteUploadRequest;
import com.mysms.platform.v1.CompleteUploadResponse;
import com.mysms.platform.v1.CreateUploadUrlRequest;
import com.mysms.platform.v1.CreateUploadUrlResponse;
import com.mysms.platform.v1.FileObjectServiceGrpc;
import com.mysms.platform.v1.GetDownloadUrlRequest;
import com.mysms.platform.v1.GetDownloadUrlResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * comms's client of the platform service: the file store an export is written to. Every call carries the caller's
 * own identity, so platform applies the caller's own file permissions and quota; a failed call is an
 * {@link IntegrationGrpcClientException}.
 */
@Service
public class PlatformGrpcClient {

    /** The starter's client of the platform service. */
    public static final String PLATFORM_SERVICE_KEY = "platform-service";
    private static final String SERVICE = "platform";

    private FileObjectServiceGrpc.FileObjectServiceBlockingStub files;

    @GrpcClient(PLATFORM_SERVICE_KEY)
    public void initFileObjectStub(FileObjectServiceGrpc.FileObjectServiceBlockingStub stub) {
        this.files = stub;
    }

    public CreateUploadUrlResponse createUploadUrl(CreateUploadUrlRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> files.createUploadUrl(request));
    }

    public CompleteUploadResponse completeUpload(CompleteUploadRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> files.completeUpload(request));
    }

    public GetDownloadUrlResponse getDownloadUrl(GetDownloadUrlRequest request) {
        return IntegrationGrpcClientException.call(SERVICE, () -> files.getDownloadUrl(request));
    }
}
