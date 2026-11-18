package com.mysms.authclient;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Caching decorator of a {@link TenantCodeResolver}: a malformed code names no school without a call, a school found
 * is kept for {@link #TTL} so a page of guest requests costs one iam call, and a code naming no school for
 * {@link #NEGATIVE_TTL} only, so a school just opened is found soon. At most {@link #MAX_ENTRIES} codes are kept, the
 * least recently used leaving first, so random codes cannot grow the cache or push the schools in use out at once;
 * codes compare case-insensitively.
 */
public final class TenantCodes {

    static final Duration TTL = Duration.ofSeconds(60);
    static final Duration NEGATIVE_TTL = Duration.ofSeconds(10);
    static final int MAX_ENTRIES = 10_000;
    /** iam.tenant_directory.tenant_code is varchar(60). */
    static final int MAX_LENGTH = 60;

    private record Answer(Optional<UUID> tenant, Instant expires) {
    }

    private final TenantCodeResolver resolver;
    private final Clock clock;
    private final Map<String, Answer> answers = new Lru();

    public TenantCodes(TenantCodeResolver resolver, Clock clock) {
        this.resolver = resolver;
        this.clock = clock;
    }

    /** The active tenant the code names; empty for no such active school (or a code no school can have). */
    public Optional<UUID> activeTenant(String rawCode) {
        String code = rawCode.trim();
        if (code.isEmpty() || code.length() > MAX_LENGTH || !code.chars().allMatch(c -> c > 0x20 && c < 0x7f)) {
            return Optional.empty();
        }
        String key = code.toLowerCase(Locale.ROOT);
        Instant now = clock.instant();
        Answer cached;
        synchronized (answers) {
            cached = answers.get(key);
        }
        if (cached != null && now.isBefore(cached.expires())) {
            return cached.tenant();
        }
        Optional<UUID> tenant = resolver.activeTenant(code);
        Answer answer = new Answer(tenant, now.plus(tenant.isPresent() ? TTL : NEGATIVE_TTL));
        synchronized (answers) {
            answers.put(key, answer);
        }
        return tenant;
    }

    /** The kept answers in access order, the least recently used dropped beyond {@link #MAX_ENTRIES}. */
    private static final class Lru extends LinkedHashMap<String, Answer> {

        private static final long serialVersionUID = 1L;

        Lru() {
            super(16, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Answer> eldest) {
            return size() > MAX_ENTRIES;
        }
    }
}
