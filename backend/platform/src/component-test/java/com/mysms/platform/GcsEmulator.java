package com.mysms.platform;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * A real Google Cloud Storage emulator (fsouza/fake-gcs-server in Testcontainers) for the platform's file store, shared
 * by the component tests that move bytes: the platform is pointed at it, and the tests act as the client — their own
 * PUT to a signed upload URL and GET from a signed download URL.
 */
final class GcsEmulator implements AutoCloseable {

    private static final HttpClient STORE = HttpClient.newHttpClient();

    private final String bucket;
    private final int port;
    private final GenericContainer<?> container;
    private final String keyFile;

    private GcsEmulator(String bucket) {
        this.bucket = bucket;
        this.port = freePort();
        this.container = start(bucket, port);
        this.keyFile = serviceAccountKey();
    }

    /** An emulator holding one empty bucket. */
    static GcsEmulator withBucket(String bucket) {
        return new GcsEmulator(bucket);
    }

    /** Points the platform's file store at the emulator. */
    void register(DynamicPropertyRegistry registry) {
        registry.add("mysms.platform.files.bucket", () -> bucket);
        registry.add("mysms.platform.files.endpoint", () -> "http://localhost:" + port);
        registry.add("mysms.platform.files.credentials-file", () -> keyFile);
    }

    /** The client's own upload: a PUT of the bytes to the signed URL with the headers the platform named. */
    int put(JsonNode target, byte[] bytes) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(target.path("uploadUrl").asString()))
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes));
        target.path("uploadHeaders").properties().forEach(h -> request.header(h.getKey(), h.getValue().asString()));
        return STORE.send(request.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    /** The client's own download through a signed URL; null when the store has no such object. */
    byte[] get(String url) throws Exception {
        HttpResponse<byte[]> response = STORE.send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        return response.statusCode() == 200 ? response.body() : null;
    }

    @Override
    public void close() {
        container.stop();
    }

    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The emulator, on a fixed host port: signed URLs are path-style for {@code localhost:<port>}, and the emulator
     * serves them only for the public host it was started with.
     */
    @SuppressWarnings("resource")
    private static GenericContainer<?> start(String bucket, int port) {
        GenericContainer<?> container = new GenericContainer<>("fsouza/fake-gcs-server:1.52.2")
                .withCommand("-scheme", "http", "-port", "4443", "-public-host", "localhost:" + port)
                .withExposedPorts(4443)
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPortBindings(
                        new PortBinding(Ports.Binding.bindPort(port), new ExposedPort(4443))))
                .waitingFor(Wait.forHttp("/storage/v1/b").forStatusCode(200));
        container.start();
        try {
            HttpRequest create = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/storage/v1/b"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"" + bucket + "\"}")).build();
            STORE.send(create, HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return container;
    }

    /** A throwaway service-account key the platform signs URLs with (the emulator does not check signatures). */
    private static String serviceAccountKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            String pem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                            .encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                    + "\n-----END PRIVATE KEY-----\n";
            ObjectNode key = PlatformCalls.JSON.createObjectNode().put("type", "service_account")
                    .put("project_id", "mysms-test").put("private_key_id", "test-key")
                    .put("private_key", pem).put("client_email", "files@mysms-test.iam.gserviceaccount.com")
                    .put("client_id", "1").put("token_uri", "https://oauth2.googleapis.com/token");
            Path file = Files.createTempFile("mysms-files-key", ".json");
            file.toFile().deleteOnExit();
            Files.writeString(file, key.toString());
            return file.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
