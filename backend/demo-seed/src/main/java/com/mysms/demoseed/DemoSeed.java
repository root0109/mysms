package com.mysms.demoseed;

import com.mysms.demoseed.client.ApiClient;
import com.mysms.demoseed.client.ApiException;
import com.mysms.demoseed.client.Bearer;
import com.mysms.demoseed.client.SignIn;
import com.mysms.demoseed.seed.AcademicStructureSeeder;
import com.mysms.demoseed.seed.AttendanceSeeder;
import com.mysms.demoseed.seed.ModuleSeeder;
import com.mysms.demoseed.seed.OrgSeeder;
import com.mysms.demoseed.seed.PersonaSeeder;
import com.mysms.demoseed.seed.SeedContext;
import com.mysms.demoseed.seed.StaffSeeder;
import com.mysms.demoseed.seed.StudentSeeder;
import com.mysms.demoseed.seed.TeachingSeeder;
import com.mysms.demoseed.seed.TimetableSeeder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Seeds the demo tenant through the ordered pipeline of module seeders, or removes it. Both refuse any tenant whose
 * code does not start with {@code demo-}; {@code remove} deletes only the tenant its manifest names, and only when the
 * platform still knows it under that code.
 */
public final class DemoSeed {

    /** The steps in dependency order: a step reads only what the steps before it made. */
    static final List<ModuleSeeder> PIPELINE = List.of(new OrgSeeder(), new AcademicStructureSeeder(),
            new StaffSeeder(), new StudentSeeder(), new PersonaSeeder(), new TeachingSeeder(), new TimetableSeeder(),
            new AttendanceSeeder());

    private static final Logger LOG = LoggerFactory.getLogger(DemoSeed.class);
    private static final int MIN_PASSWORD = 12;

    private final DemoSettings settings;
    private final ApiClient anonymous;

    public DemoSeed(DemoSettings settings, JsonMapper json) {
        this.settings = settings;
        this.anonymous = ApiClient.anonymous(settings.baseUrl(), json);
    }

    public void run() {
        if (settings.action() == DemoSettings.Action.SEED) {
            seed();
        } else {
            remove();
        }
    }

    /** Fills the demo tenant (made by iam's installation bootstrap) and writes its manifest. */
    public Manifest seed() {
        require(settings.adminUsername(), "MYSMS_DEMO_ADMIN_USERNAME");
        require(settings.adminPassword(), "MYSMS_DEMO_ADMIN_PASSWORD");
        if (settings.personaPassword().length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("MYSMS_DEMO_PERSONA_PASSWORD must be at least " + MIN_PASSWORD
                    + " characters");
        }
        UUID tenant = tenantId();
        ApiClient admin = anonymous.as(SignIn.of(anonymous, settings.tenantCode(), settings.adminUsername(),
                settings.adminPassword()));
        SeedContext context = new SeedContext(settings, admin);
        for (ModuleSeeder seeder : PIPELINE) {
            LOG.info("seeding {} in {}", seeder.module(), settings.tenantCode());
            seeder.seed(context);
        }
        Manifest manifest = new Manifest(settings.tenantCode(), tenant, context.uuid(OrgSeeder.INSTITUTION),
                context.tally(), context.skipped(), context.personas());
        manifest.write(settings.manifestDirectory(), anonymous.json());
        LOG.info("demo tenant {} ready: {} (manifest {})", settings.tenantCode(), manifest.rows(),
                Manifest.file(settings.manifestDirectory(), settings.tenantCode()));
        if (!manifest.skippedOperations().isEmpty()) {
            LOG.info("not served by this release, skipped: {}", manifest.skippedOperations());
        }
        manifest.personas().forEach((persona, username) -> LOG.info("sign in as {}: tenant {}, username {}",
                persona, settings.tenantCode(), username));
        LOG.info("every demo sign-in uses the password given in MYSMS_DEMO_PERSONA_PASSWORD");
        return manifest;
    }

    /** Deletes the tenant the manifest names (platform.DeleteTenant), then the manifest. */
    public void remove() {
        require(settings.platformToken(), "MYSMS_DEMO_PLATFORM_TOKEN");
        Manifest manifest = Manifest.read(settings.manifestDirectory(), settings.tenantCode(), anonymous.json());
        if (!settings.tenantCode().equals(manifest.tenantCode())
                || !manifest.tenantCode().startsWith(DemoSettings.DEMO_PREFIX)) {
            throw new IllegalStateException("the manifest names tenant " + manifest.tenantCode() + ", not "
                    + settings.tenantCode());
        }
        ApiClient platform = anonymous.as(Bearer.fixed(settings.platformToken()));
        String path = "/v1/platform/tenants/" + manifest.tenantId();
        JsonNode tenant = platform.get(path);
        if (!manifest.tenantCode().equals(tenant.path("code").asString())) {
            throw new IllegalStateException("tenant " + manifest.tenantId() + " is no longer "
                    + manifest.tenantCode() + "; nothing removed");
        }
        platform.delete(path, tenant.path("etag").asString());
        try {
            Files.delete(Manifest.file(settings.manifestDirectory(), settings.tenantCode()));
        } catch (IOException e) {
            throw new UncheckedIOException("tenant removed, but its manifest could not be deleted", e);
        }
        LOG.info("demo tenant {} removed: nobody can sign in to it any more", manifest.tenantCode());
    }

    /** The tenant's id from iam's directory; a tenant the bootstrap has not made yet is a clear refusal. */
    private UUID tenantId() {
        try {
            return UUID.fromString(anonymous.get("/v1/iam/tenant-codes/" + settings.tenantCode())
                    .path("tenantId").asString());
        } catch (ApiException e) {
            if (e.status() == 404) {
                throw new IllegalStateException("tenant " + settings.tenantCode() + " does not exist: start MySMS "
                        + "once with MYSMS_IAM_BOOTSTRAP_TENANT_CODE=" + settings.tenantCode() + " (DEPLOYMENT.md)", e);
            }
            throw e;
        }
    }

    private static void require(String value, String variable) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(variable + " is required");
        }
    }
}
