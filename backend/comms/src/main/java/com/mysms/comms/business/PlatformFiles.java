package com.mysms.comms.business;

import com.mysms.common.v1.UploadTarget;
import com.mysms.comms.integration.PlatformGrpcClient;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.platform.v1.CompleteUploadRequest;
import com.mysms.platform.v1.CreateUploadUrlRequest;
import com.mysms.platform.v1.GetDownloadUrlRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The files comms writes (a calendar export), kept in platform's file store (an Adapter over platform's
 * FileObjectService). Every call runs as the caller, so platform applies the caller's upload quota and read
 * permission, and the bytes move through platform's short-lived signed URLs. Never called inside a database
 * transaction. The store refusing the caller is 403, a quota used up 409 {@code FILE_NOT_READY}, the store
 * unreachable 503.
 */
@Component
public class PlatformFiles {

    /** A signed download link and when it stops working. */
    public record Link(String url, OffsetDateTime expiresAt) {
    }

    private final PlatformGrpcClient platform;
    private final Duration timeout;
    private final HttpClient http;

    public PlatformFiles(PlatformGrpcClient platform, @Value("${mysms.comms.files.timeout}") Duration timeout) {
        this.platform = platform;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Stores generated content as a new file of the caller; platform verifies its SHA-256. Answers its id. */
    public UUID write(String fileName, String mimeType, byte[] content) {
        UploadTarget target = platform(() -> platform.createUploadUrl(CreateUploadUrlRequest.newBuilder()
                .setFileName(fileName).setMimeType(mimeType).setSizeBytes(content.length)
                .setChecksumSha256(sha256(content)).build()).getResult());
        HttpRequest.Builder put = HttpRequest.newBuilder(URI.create(target.getUploadUrl())).timeout(timeout)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content));
        Map<String, String> headers = headers(target.getUploadHeaders());
        headers.forEach(put::header);
        if (headers.keySet().stream().noneMatch("Content-Type"::equalsIgnoreCase)) {
            put.header("Content-Type", mimeType);
        }
        try {
            HttpResponse<Void> response = http.send(put.build(), HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() / 100 != 2) {
                throw new UnavailableException("The file store refused the upload.");
            }
        } catch (IOException e) {
            throw new UnavailableException("The file store could not be reached.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnavailableException("The upload was interrupted.");
        }
        platform(() -> platform.completeUpload(CompleteUploadRequest.newBuilder()
                .setFileObjectId(target.getFileId()).build()));
        return UUID.fromString(target.getFileId());
    }

    /** A signed link the caller downloads the file with. */
    public Link link(UUID fileId) {
        com.mysms.common.v1.DownloadLink link = platform(() -> platform.getDownloadUrl(GetDownloadUrlRequest
                .newBuilder().setFileObjectId(fileId.toString()).build()).getResult());
        return new Link(link.getUrl(), OffsetDateTime.ofInstant(Instant.ofEpochSecond(
                link.getExpiresAt().getSeconds(), link.getExpiresAt().getNanos()), ZoneOffset.UTC));
    }

    /** Maps platform's refusal onto comms's own: never platform's sentence, never its fields. */
    private static <T> T platform(Supplier<T> call) {
        try {
            return call.get();
        } catch (IntegrationGrpcClientException e) {
            switch (e.httpStatus()) {
                case 403 -> throw new PermissionDeniedException("The file store refused the caller this file.");
                case 409 -> throw new ConflictException("FILE_NOT_READY",
                        "The file store cannot take the file now (the upload quota may be used up).");
                default -> throw new UnavailableException("The platform file store could not answer.");
            }
        }
    }

    /** The PUT headers platform's store demands, as the struct the upload target carries (strings only). */
    static Map<String, String> headers(com.google.protobuf.Value value) {
        if (!value.hasStructValue()) {
            return Map.of();
        }
        Map<String, String> named = new LinkedHashMap<>();
        value.getStructValue().getFieldsMap().forEach((name, header) -> {
            if (header.hasStringValue()) {
                named.put(name, header.getStringValue());
            }
        });
        return named;
    }

    /** The lower-case hex SHA-256 platform verifies the uploaded bytes against. */
    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
