package com.mysms.core.testkit;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A fresh, empty PostgreSQL database for one test class (JAVA-CODING-RULES §12: real PostgreSQL and the real Flyway
 * migrations, never H2). Two fresh login roles, neither superuser nor BYPASSRLS, as in production
 * (database/README "Roles"): the owner, which Flyway migrates as, and the application role, which only receives
 * DML grants — so row-level security is enforced and cannot be switched off by the application.
 *
 * <p>The server is a Testcontainers PostgreSQL 16 by default. Set {@code MYSMS_TEST_JDBC_URL} (an admin connection
 * such as {@code jdbc:postgresql://localhost:5432/postgres}), {@code MYSMS_TEST_JDBC_USER} and
 * {@code MYSMS_TEST_JDBC_PASSWORD} to use an existing server instead (CI without Docker).
 */
public final class TestDatabase implements AutoCloseable {

    private static final String TEST_SECRET_KEY = randomKey();

    private static final String IMAGE = "postgres:16-alpine";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static Server server;

    private final String name;
    private final String password;
    private final String appRole;
    private final String appPassword;
    private final String jdbcUrl;

    private record Server(String adminUrl, String user, String password) {
    }

    private TestDatabase(String name, String password, String appPassword, String jdbcUrl) {
        this.name = name;
        this.password = password;
        this.appRole = name + "_app";
        this.appPassword = appPassword;
        this.jdbcUrl = jdbcUrl;
    }

    /** Creates a new database and its owner role. */
    public static TestDatabase create() {
        Server admin = server();
        String name = "mysms_t_" + HexFormat.of().formatHex(randomBytes(6));
        String password = HexFormat.of().formatHex(randomBytes(16));
        String appPassword = HexFormat.of().formatHex(randomBytes(16));
        try (Connection connection = DriverManager.getConnection(admin.adminUrl(), admin.user(), admin.password());
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE ROLE " + name + " LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEROLE PASSWORD '"
                    + password + "'");
            statement.execute("CREATE ROLE " + name + "_app LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEROLE PASSWORD '"
                    + appPassword + "'");
            // the outbox relay's role (database/README "Roles": mysms_platform_admin): reads every tenant's outbox
            statement.execute("CREATE ROLE " + name + "_relay LOGIN NOSUPERUSER BYPASSRLS NOCREATEROLE PASSWORD '"
                    + appPassword + "'");
            statement.execute("CREATE DATABASE " + name + " OWNER " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("cannot create test database", e);
        }
        return new TestDatabase(name, password, appPassword, replaceDatabase(admin.adminUrl(), name));
    }

    /**
     * Creates a database that is dropped when the test JVM exits — for Spring tests, whose cached application context
     * keeps connections open until then.
     */
    public static TestDatabase createDropOnExit() {
        TestDatabase database = create();
        Runtime.getRuntime().addShutdownHook(new Thread(database::closeQuietly, "drop-" + database.name));
        return database;
    }

    private void closeQuietly() {
        try {
            close();
        } catch (IllegalStateException e) {
            // the server may already be gone (container stopped first); nothing is left to clean up then
            return;
        }
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    /** The application role: DML only, owns nothing. */
    public String username() {
        return appRole;
    }

    public String password() {
        return appPassword;
    }

    /** The schema owner the migrations run as. */
    public String migrateUsername() {
        return name;
    }

    public String migratePassword() {
        return password;
    }

    /**
     * Statements as the server's administrator on this test database, for assertions on rows no application role may
     * read (records without a tenant, which row-level security hides from every role but a bypassing one). Tests only;
     * never a way to arrange data the code under test reads.
     */
    public JdbcClient adminJdbc() {
        Server admin = server();
        return JdbcClient.create(new DriverManagerDataSource(jdbcUrl(), admin.user(), admin.password()));
    }

    /** The outbox relay's role: BYPASSRLS, granted only the outbox (ApplicationRoleGrants). */
    public String relayUsername() {
        return name + "_relay";
    }

    public String relayPassword() {
        return appPassword;
    }

    /**
     * Points `mysms.<module>.datasource.*` of every given module at this database. Events run on the in-process bus
     * (a real implementation of the bus interface; the RabbitMQ one is tested in messaging-rabbitmq and the
     * monolith): each module's relay publishes and its subscriptions consume.
     */
    public void register(DynamicPropertyRegistry registry, String... modules) {
        for (String module : modules) {
            String prefix = "mysms." + module + ".datasource.";
            registry.add(prefix + "url", this::jdbcUrl);
            registry.add(prefix + "username", this::username);
            registry.add(prefix + "password", this::password);
            registry.add(prefix + "migrate-username", this::migrateUsername);
            registry.add(prefix + "migrate-password", this::migratePassword);
            registry.add(prefix + "maximum-pool-size", () -> "2");
            registry.add(prefix + "relay-username", this::relayUsername);
            registry.add(prefix + "relay-password", this::relayPassword);
        }
        registry.add("mysms.events.bus", () -> "in-process");
        registry.add("mysms.events.relay.interval", () -> "100ms");
        // a master key for the secret columns, random per test JVM: never a fixed key in code or configuration
        registry.add("mysms.secrets.keys", () -> "test:" + TEST_SECRET_KEY);
        registry.add("mysms.secrets.active-key", () -> "test");
    }

    private static String randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    @Override
    public void close() {
        Server admin = server();
        try (Connection connection = DriverManager.getConnection(admin.adminUrl(), admin.user(), admin.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS " + name + " WITH (FORCE)");
            statement.execute("DROP ROLE IF EXISTS " + appRole);
            statement.execute("DROP ROLE IF EXISTS " + name + "_relay");
            statement.execute("DROP ROLE IF EXISTS " + name);
        } catch (SQLException e) {
            throw new IllegalStateException("cannot drop test database " + name, e);
        }
    }

    private static synchronized Server server() {
        if (server == null) {
            String url = System.getenv("MYSMS_TEST_JDBC_URL");
            if (url != null && !url.isBlank()) {
                server = new Server(url, System.getenv("MYSMS_TEST_JDBC_USER"), System.getenv("MYSMS_TEST_JDBC_PASSWORD"));
            } else {
                PostgreSQLContainer container = new PostgreSQLContainer(IMAGE);
                container.start();
                // one container per test JVM; Testcontainers' Ryuk removes it when the JVM exits
                server = new Server(container.getJdbcUrl(), container.getUsername(), container.getPassword());
            }
        }
        return server;
    }

    private static String replaceDatabase(String url, String database) {
        int slash = url.indexOf('/', "jdbc:postgresql://".length());
        int query = url.indexOf('?', slash);
        String tail = query < 0 ? "" : url.substring(query);
        return url.substring(0, slash + 1) + database.toLowerCase(Locale.ROOT) + tail;
    }

    private static byte[] randomBytes(int count) {
        byte[] bytes = new byte[count];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
