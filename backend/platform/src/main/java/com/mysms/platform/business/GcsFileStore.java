package com.mysms.platform.business;

import com.google.auth.ServiceAccountSigner;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.mysms.core.error.UnavailableException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * {@link FileStore} on Google Cloud Storage: V4 signed URLs for the client's upload and download, the JSON API for the
 * rest; {@code host} points signed URLs at an emulator. An upload URL signs the exact size and "create only" as headers
 * the client must send; every read is of one generation. A store failure is 503 without its message.
 */
public final class GcsFileStore implements FileStore {

    private static final String UNREACHABLE = "The file store is not reachable.";
    /** Signed upload headers: the body must be exactly the declared size, and only a new object is created. */
    static final String LENGTH_RANGE = "x-goog-content-length-range";
    static final String IF_GENERATION_MATCH = "x-goog-if-generation-match";

    private final Storage storage;
    private final String bucket;
    private final ServiceAccountSigner signer;
    private final String host;
    private final Clock clock;

    /**
     * @param host the scheme and authority signed URLs point at, e.g. {@code http://localhost:4443}; null for Google
     */
    public GcsFileStore(Storage storage, String bucket, ServiceAccountSigner signer, String host, Clock clock) {
        this.storage = storage;
        this.bucket = bucket;
        this.signer = signer;
        this.host = host;
        this.clock = clock;
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public String disk() {
        return bucket;
    }

    @Override
    public SignedUpload uploadUrl(String key, String contentType, long sizeBytes, Duration ttl) {
        Map<String, String> headers = Map.of("Content-Type", contentType,
                LENGTH_RANGE, sizeBytes + "," + sizeBytes, IF_GENERATION_MATCH, "0");
        BlobInfo blob = BlobInfo.newBuilder(BlobId.of(bucket, key)).setContentType(contentType).build();
        SignedUrl url = sign(blob, ttl, Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withExtHeaders(headers));
        return new SignedUpload(url.url(), url.expiresAt(), headers);
    }

    @Override
    public SignedUrl downloadUrl(String key, Long generation, String fileName, Duration ttl) {
        Map<String, String> query = new HashMap<>();
        query.put("response-content-disposition", disposition(fileName));
        if (generation != null) {
            query.put("generation", Long.toString(generation));
        }
        BlobInfo blob = BlobInfo.newBuilder(BlobId.of(bucket, key)).build();
        return sign(blob, ttl, Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                Storage.SignUrlOption.withQueryParams(query));
    }

    @Override
    public Optional<StoredObject> describe(String key) {
        Blob blob = call(() -> storage.get(BlobId.of(bucket, key)));
        if (blob == null || blob.getSize() == null || blob.getGeneration() == null) {
            return Optional.empty();
        }
        return Optional.of(new StoredObject(blob.getSize(), blob.getContentType(), blob.getGeneration()));
    }

    @Override
    public String sha256(String key, long generation) {
        return call(() -> {
            MessageDigest digest = sha256Digest();
            try (ReadChannel reader = storage.reader(BlobId.of(bucket, key, generation));
                 InputStream in = new DigestInputStream(Channels.newInputStream(reader), digest)) {
                in.transferTo(OutputStream.nullOutputStream());
                return HexFormat.of().formatHex(digest.digest());
            } catch (IOException unreadable) {
                throw new UnavailableException(UNREACHABLE);
            }
        });
    }

    @Override
    public byte[] read(String key, Long generation) {
        return call(() -> storage.readAllBytes(BlobId.of(bucket, key, generation)));
    }

    @Override
    public long copy(String fromKey, Long fromGeneration, String toKey) {
        return call(() -> storage.copy(Storage.CopyRequest.newBuilder()
                .setSource(BlobId.of(bucket, fromKey, fromGeneration)).setTarget(BlobId.of(bucket, toKey)).build())
                .getResult().getGeneration());
    }

    @Override
    public long write(String key, byte[] content, String contentType) {
        return call(() -> storage.create(BlobInfo.newBuilder(BlobId.of(bucket, key)).setContentType(contentType)
                .build(), content).getGeneration());
    }

    @Override
    public void delete(String key) {
        call(() -> storage.delete(BlobId.of(bucket, key)));
    }

    private SignedUrl sign(BlobInfo blob, Duration ttl, Storage.SignUrlOption... method) {
        List<Storage.SignUrlOption> options = new ArrayList<>(List.of(method));
        options.add(Storage.SignUrlOption.withV4Signature());
        options.add(Storage.SignUrlOption.signWith(signer));
        if (host != null) {
            options.add(Storage.SignUrlOption.withHostName(host));
            options.add(Storage.SignUrlOption.withPathStyle());
        }
        OffsetDateTime expires = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.SECONDS).plus(ttl);
        String url = call(() -> storage.signUrl(blob, ttl.toSeconds(), TimeUnit.SECONDS,
                options.toArray(Storage.SignUrlOption[]::new)).toString());
        return new SignedUrl(url, expires);
    }

    /** Saves under the file's own name, ASCII fallback first, then the exact UTF-8 name (RFC 6266 / RFC 5987). */
    static String disposition(String fileName) {
        String ascii = fileName.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_").replace("\\", "_");
        String exact = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + exact;
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException missing) {
            throw new IllegalStateException("SHA-256 is not available in this JVM.", missing);
        }
    }

    private static <T> T call(Supplier<T> work) {
        try {
            return work.get();
        } catch (StorageException unreachable) {
            throw new UnavailableException(UNREACHABLE);
        }
    }
}
