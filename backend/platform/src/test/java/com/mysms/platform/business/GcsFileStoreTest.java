package com.mysms.platform.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.storage.StorageOptions;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** The signed URLs, made offline with a throwaway key: what an upload may write and which bytes a download reads. */
class GcsFileStoreTest {

    @Test
    void anUploadUrlSignsTheExactSizeAndCreateOnlyAndADownloadReadsOneGeneration() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        ServiceAccountCredentials signer = ServiceAccountCredentials.newBuilder()
                .setClientEmail("files@mysms-test.iam.gserviceaccount.com").setPrivateKeyId("k")
                .setPrivateKey(generator.generateKeyPair().getPrivate()).build();
        GcsFileStore store = new GcsFileStore(StorageOptions.newBuilder().setProjectId("mysms-test")
                .setCredentials(NoCredentials.getInstance()).build().getService(), "files", signer, null,
                Clock.systemUTC());

        FileStore.SignedUpload upload = store.uploadUrl("tenants/t/files/f", "application/pdf", 1234,
                Duration.ofMinutes(15));
        assertThat(upload.headers()).containsEntry("Content-Type", "application/pdf")
                .containsEntry(GcsFileStore.LENGTH_RANGE, "1234,1234")
                .containsEntry(GcsFileStore.IF_GENERATION_MATCH, "0");
        assertThat(signedHeaders(upload.url())).contains("content-type", GcsFileStore.LENGTH_RANGE,
                GcsFileStore.IF_GENERATION_MATCH);

        String download = store.downloadUrl("tenants/t/files/f", 1700000000123456L, "report.pdf",
                Duration.ofMinutes(10)).url();
        assertThat(query(download)).contains("generation=1700000000123456").contains("X-Goog-Signature=");
        assertThat(query(store.downloadUrl("tenants/t/files/f", null, "report.pdf", Duration.ofMinutes(10)).url()))
                .doesNotContain("generation=");
    }

    private static String signedHeaders(String url) {
        for (String parameter : URI.create(url).getRawQuery().split("&")) {
            if (parameter.startsWith("X-Goog-SignedHeaders=")) {
                return URLDecoder.decode(parameter.substring("X-Goog-SignedHeaders=".length()), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private static String query(String url) {
        return URLDecoder.decode(URI.create(url).getRawQuery(), StandardCharsets.UTF_8);
    }
}
