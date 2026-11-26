package com.mysms.learning.lms.business;

import com.mysms.common.v1.UploadTarget;
import com.mysms.core.error.ConflictException;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.UnavailableException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.grpc.IntegrationGrpcClientException;
import com.mysms.learning.lms.api.model.DownloadLink;
import com.mysms.learning.lms.integration.PlatformGrpcClient;
import com.mysms.platform.v1.CompleteUploadRequest;
import com.mysms.platform.v1.CreateUploadUrlRequest;
import com.mysms.platform.v1.EntityAttachment;
import com.mysms.platform.v1.FileObject;
import com.mysms.platform.v1.GetDownloadUrlRequest;
import com.mysms.platform.v1.GetFileObjectRequest;
import com.mysms.platform.v1.ListEntityAttachmentsRequest;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * The files lms reads and writes, kept in platform's file store (the Adapter between lms's workflows and platform's
 * FileObjectService). Every call runs as the caller — platform applies the caller's own read permission and upload
 * quota — and moves the bytes through platform's short-lived signed URLs, never through platform itself. Never
 * called inside a database transaction (the gRPC client interceptor refuses it).
 *
 * <p>Refusals: a file the caller cannot read, or that does not exist, is 422 on the request's field; a file still
 * being uploaded is 409 {@code FILE_NOT_READY}; a file of the wrong kind or too large is 422; platform or the store
 * unreachable is 503.
 */
@Component
public class PlatformFiles {

    /** A file read in full, with its stored name. */
    public record Read(UUID id, String fileName, byte[] content) {
    }

    /** The record a file is attached to, and the file. */
    public record Attachment(UUID id, String entityType, UUID entityId, UUID fileId) {
    }

    private final PlatformGrpcClient platform;
    private final Duration timeout;
    private final HttpClient http;

    public PlatformFiles(PlatformGrpcClient platform, @Value("${mysms.lms.files.timeout}") Duration timeout) {
        this.platform = platform;
        this.timeout = timeout;
        this.http = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Reads the file the request names in {@code field}: one of the given extensions (lower case, with the dot; an
     * empty list takes any kind), at most {@code maxBytes}.
     */
    public Read read(UUID fileId, String field, List<String> extensions, long maxBytes) {
        FileObject file = describe(fileId, field);
        String name = file.getOriginalName().isEmpty() ? file.getFileName() : file.getOriginalName();
        String lower = name.toLowerCase(Locale.ROOT);
        if (!extensions.isEmpty() && extensions.stream().noneMatch(lower::endsWith)) {
            throw invalid(field, "must be a " + String.join(" or ", extensions) + " file", "FILE_KIND");
        }
        if (file.getSizeBytes() > maxBytes) {
            throw invalid(field, "must be at most " + maxBytes / (1024 * 1024) + " MB", "FILE_SIZE");
        }
        String url = downloadLink(fileId, field).getUrl();
        return new Read(fileId, name, fetch(url, maxBytes));
    }

    /** The attachment the request names in {@code field}, as the caller may read it. */
    public Attachment attachment(UUID id, String field) {
        List<EntityAttachment> found = platform(() -> platform.listEntityAttachments(ListEntityAttachmentsRequest
                .newBuilder().setFilter("id = \"" + id + "\"").build()).getItemsList(), field);
        if (found.isEmpty()) {
            throw invalid(field, "names no attachment the caller may read", "NOT_FOUND");
        }
        EntityAttachment attachment = found.get(0);
        return new Attachment(id, attachment.getEntityType(), UUID.fromString(attachment.getEntityId()),
                UUID.fromString(attachment.getFileId()));
    }

    /** A signed link the caller downloads the file with (platform checks the caller may read it). */
    public DownloadLink link(UUID fileId, String field) {
        com.mysms.common.v1.DownloadLink link = downloadLink(fileId, field);
        return new DownloadLink(link.getUrl(), OffsetDateTime.ofInstant(Instant.ofEpochSecond(
                link.getExpiresAt().getSeconds(), link.getExpiresAt().getNanos()), ZoneOffset.UTC));
    }

    /** Stores generated content as a new file of the caller (their quota applies); answers its id. */
    public UUID write(String fileName, String mimeType, byte[] content) {
        return write(fileName, mimeType, content, null, null);
    }

    /**
     * Stores generated content as a new file of the caller, owned by the given lms record (both null: no owner);
     * platform verifies the SHA-256 the upload declares. Answers the new file's id.
     */
    public UUID write(String fileName, String mimeType, byte[] content, @Nullable String ownerEntityType,
            @Nullable UUID ownerEntityId) {
        CreateUploadUrlRequest.Builder request = CreateUploadUrlRequest.newBuilder().setFileName(fileName)
                .setMimeType(mimeType).setSizeBytes(content.length).setChecksumSha256(sha256(content));
        if (ownerEntityType != null && ownerEntityId != null) {
            request.setOwnerEntityType(ownerEntityType).setOwnerEntityId(ownerEntityId.toString());
        }
        UploadTarget target = platform(() -> platform.createUploadUrl(request.build()).getResult(), "fileName");
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
                .setFileObjectId(target.getFileId()).build()), "fileName");
        return UUID.fromString(target.getFileId());
    }

    private FileObject describe(UUID fileId, String field) {
        return platform(() -> platform.getFileObject(GetFileObjectRequest.newBuilder().setId(fileId.toString())
                .build()).getFileObject(), field);
    }

    private com.mysms.common.v1.DownloadLink downloadLink(UUID fileId, String field) {
        return platform(() -> platform.getDownloadUrl(GetDownloadUrlRequest.newBuilder()
                .setFileObjectId(fileId.toString()).build()).getResult(), field);
    }

    /** The bytes behind a signed URL, refused beyond {@code maxBytes} however the store describes them. */
    byte[] fetch(String url, long maxBytes) {
        HttpRequest get = HttpRequest.newBuilder(URI.create(url)).timeout(timeout).GET().build();
        try {
            HttpResponse<InputStream> response = http.send(get, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() / 100 != 2) {
                    throw new UnavailableException("The file store could not provide the file.");
                }
                byte[] bytes = body.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, maxBytes + 1));
                if (bytes.length > maxBytes) {
                    throw new UnavailableException("The stored file is larger than it was declared.");
                }
                return bytes;
            }
        } catch (IOException e) {
            throw new UnavailableException("The file store could not be reached.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnavailableException("The download was interrupted.");
        }
    }

    /** Maps platform's refusal onto lms's own: never platform's sentence, never its fields. */
    private static <T> T platform(Supplier<T> call, String field) {
        try {
            return call.get();
        } catch (IntegrationGrpcClientException e) {
            switch (e.httpStatus()) {
                case 400, 404, 422 -> throw invalid(field, "names no file the caller may read", "NOT_FOUND");
                case 403 -> throw new PermissionDeniedException("The file store refused this file to the caller.");
                case 409 -> throw new ConflictException("FILE_NOT_READY",
                        "The file is not available (its upload is unfinished, or the quota is used up).");
                default -> throw new UnavailableException("The platform file store could not answer.");
            }
        }
    }

    private static ValidationFailedException invalid(String field, String message, String code) {
        return new ValidationFailedException(List.of(new FieldError(field, message, code)));
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
