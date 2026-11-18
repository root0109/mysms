package com.mysms.platform.config;

import com.google.auth.ServiceAccountSigner;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.StorageOptions;
import com.mysms.platform.business.FileStore;
import com.mysms.platform.business.GcsFileStore;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/** Builds the Google Cloud Storage file store; loaded only when {@link PlatformFilesConfig} has a bucket. */
final class GcsFileStores {

    private GcsFileStores() {
    }

    static FileStore create(String bucket, String endpoint, String credentialsFile, String project, Clock clock) {
        GoogleCredentials credentials = credentials(credentialsFile);
        if (!(credentials instanceof ServiceAccountSigner signer)) {
            throw new IllegalStateException("The file store's credentials cannot sign URLs: configure a service "
                    + "account (mysms.platform.files.credentials-file, or the workload's attached service account).");
        }
        StorageOptions.Builder options = StorageOptions.newBuilder();
        if (!project.isBlank()) {
            options.setProjectId(project);
        }
        if (endpoint.isBlank()) {
            options.setCredentials(credentials);
            return new GcsFileStore(options.build().getService(), bucket, signer, null, clock);
        }
        options.setHost(endpoint).setCredentials(NoCredentials.getInstance());
        if (project.isBlank()) {
            options.setProjectId("emulator");
        }
        return new GcsFileStore(options.build().getService(), bucket, signer, endpoint, clock);
    }

    private static GoogleCredentials credentials(String file) {
        try {
            if (file.isBlank()) {
                return GoogleCredentials.getApplicationDefault();
            }
            try (InputStream key = Files.newInputStream(Path.of(file))) {
                return ServiceAccountCredentials.fromStream(key);
            }
        } catch (IOException unreadable) {
            throw new UncheckedIOException("The file store's credentials cannot be read.", unreadable);
        }
    }
}
