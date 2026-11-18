package com.mysms.platform.business;

import com.mysms.platform.errors.NotConfiguredException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * The object store file bytes live in (the Port of the Ports-and-Adapters pair; {@link GcsFileStore} is the adapter).
 * Clients move bytes directly to and from the store through short-lived signed URLs; the platform itself only signs,
 * inspects, copies, writes generated output and deletes. Keys are chosen by the platform and carry no file names.
 * Every call may be remote: callers never make one inside a database transaction.
 *
 * <p>An upload URL creates the object once, at the declared size; a completed file is read, copied and served at the
 * generation that was checked (null: the object as it is now).
 */
public interface FileStore {

    /** A signed URL and the moment it stops working. */
    record SignedUrl(String url, OffsetDateTime expiresAt) {
    }

    /** A signed upload URL, the moment it stops working, and the headers the upload must send exactly. */
    record SignedUpload(String url, OffsetDateTime expiresAt, Map<String, String> headers) {
        public SignedUpload {
            headers = Map.copyOf(headers);
        }
    }

    /** What the store holds under a key: its size, the content type it was uploaded with and its generation. */
    record StoredObject(long sizeBytes, String contentType, long generation) {
    }

    /** Whether this deployment has a store; every other method of an unconfigured store answers 503. */
    boolean configured();

    /** The bucket the keys live in (kept on each file row as its storage disk). */
    String disk();

    /**
     * A URL the client PUTs the bytes to, valid for {@code ttl}: it creates the object only while none exists under the
     * key, only with this Content-Type and exactly {@code sizeBytes} bytes; the upload sends the answered headers.
     */
    SignedUpload uploadUrl(String key, String contentType, long sizeBytes, Duration ttl);

    /**
     * A URL the client GETs the object's generation from, valid for {@code ttl}; the browser saves the bytes as
     * {@code fileName}.
     */
    SignedUrl downloadUrl(String key, Long generation, String fileName, Duration ttl);

    /** The object under the key, when one was uploaded. */
    Optional<StoredObject> describe(String key);

    /**
     * The SHA-256 of the generation's bytes in lower-case hex (the bytes are streamed through, never held whole).
     */
    String sha256(String key, long generation);

    /**
     * The generation's bytes, for the few files the platform reads itself (an import file, bounded by the caller before
     * the read).
     */
    byte[] read(String key, Long generation);

    /** A server-side copy of the generation (no bytes pass through the platform); answers the copy's generation. */
    long copy(String fromKey, Long fromGeneration, String toKey);

    /** Stores generated content (report output) under the key; answers its generation. */
    long write(String key, byte[] content, String contentType);

    /** Removes the object; removing an absent one is not an error. */
    void delete(String key);

    /** The store of a deployment that configures none. */
    static FileStore unconfigured() {
        return new FileStore() {
            private static final String DETAIL = "The file store is not configured for this deployment.";

            @Override
            public boolean configured() {
                return false;
            }

            @Override
            public String disk() {
                throw new NotConfiguredException(DETAIL);
            }

            @Override
            public SignedUpload uploadUrl(String key, String contentType, long sizeBytes, Duration ttl) {
                throw new NotConfiguredException(DETAIL);
            }

            @Override
            public SignedUrl downloadUrl(String key, Long generation, String fileName, Duration ttl) {
                throw new NotConfiguredException(DETAIL);
            }

            @Override
            public Optional<StoredObject> describe(String key) {
                throw new NotConfiguredException(DETAIL);
            }

            @Override
            public String sha256(String key, long generation) {
                throw new NotConfiguredException(DETAIL);
            }

            @Override
            public byte[] read(String key, Long generation) {
                throw new NotConfiguredException(DETAIL);
            }

            @Override
            public long copy(String fromKey, Long fromGeneration, String toKey) {
                throw new NotConfiguredException(DETAIL);
            }

            @Override
            public long write(String key, byte[] content, String contentType) {
                throw new NotConfiguredException(DETAIL);
            }

            @Override
            public void delete(String key) {
                throw new NotConfiguredException(DETAIL);
            }
        };
    }
}
