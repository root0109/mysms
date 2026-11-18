package com.mysms.platform.config;

import com.mysms.platform.business.FileStore;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The platform's file store (business/FileStore): Google Cloud Storage when a bucket is configured
 * ({@code mysms.platform.files.bucket}, else {@code MYSMS_FILES_BUCKET}), otherwise none — every file operation then
 * answers 503 NOT_CONFIGURED and nothing else is affected.
 *
 * <p>Credentials are Google's application default credentials, or the service-account key file named by
 * {@code mysms.platform.files.credentials-file} / {@code MYSMS_FILES_CREDENTIALS_FILE}; they must be able to sign
 * (a service account key, or the attached service account on Google Cloud, which signs through the IAM credentials
 * API), since every upload and download is a V4 signed URL. {@code mysms.platform.files.endpoint} /
 * {@code MYSMS_FILES_ENDPOINT} points the store at an emulator (local development, tests): its API is then called
 * without credentials, and the URLs are signed for that host.
 *
 * <p>The Google client is touched only when a bucket is configured: its classes live in {@link GcsFileStores}, which
 * this class does not load otherwise.
 */
@Configuration(proxyBeanMethods = false)
public class PlatformFilesConfig {

    @Bean
    public FileStore fileStore(@Value("${mysms.platform.files.bucket}") String bucket,
            @Value("${mysms.platform.files.endpoint}") String endpoint,
            @Value("${mysms.platform.files.credentials-file}") String credentialsFile,
            @Value("${mysms.platform.files.project}") String project, Clock clock) {
        if (bucket.isBlank()) {
            return FileStore.unconfigured();
        }
        return GcsFileStores.create(bucket, endpoint, credentialsFile, project, clock);
    }
}
