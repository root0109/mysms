package com.mysms.demoseed.seed;

import com.mysms.demoseed.DemoSettings;
import com.mysms.demoseed.client.ApiClient;
import com.mysms.demoseed.client.ApiClient.Ensured;
import com.mysms.demoseed.client.ApiException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * What the pipeline's steps share: the administrator's API client, the fixed-seed random source, the ids earlier
 * steps made (by name), and the tally the manifest is written from.
 */
public final class SeedContext {

    private final DemoSettings settings;
    private final ApiClient api;
    private final Random random;
    private final SchoolCalendar calendar;
    private final Map<String, JsonNode> rows = new HashMap<>();
    private final Map<String, List<JsonNode>> groups = new HashMap<>();
    private final Map<String, Map<String, Integer>> tally = new TreeMap<>();
    private final TreeSet<String> skipped = new TreeSet<>();
    private final Map<String, String> personas = new TreeMap<>();

    public SeedContext(DemoSettings settings, ApiClient api) {
        this.settings = settings;
        this.api = api;
        this.random = new Random(settings.randomSeed());
        this.calendar = SchoolCalendar.around(settings.today());
    }

    public DemoSettings settings() {
        return settings;
    }

    public ApiClient api() {
        return api;
    }

    public Random random() {
        return random;
    }

    public SchoolCalendar calendar() {
        return calendar;
    }

    public ObjectNode object() {
        return api.object();
    }

    /** Finds the row the filter names or creates it, and counts it for the manifest under module and resource. */
    public JsonNode ensure(String module, String resource, String path, String filter, JsonNode body) {
        Ensured ensured = api.ensure(path, filter, body);
        count(module, resource);
        return ensured.row();
    }

    /** Counts a row a workflow operation wrote (or an earlier run did). */
    public void count(String module, String resource) {
        tally.computeIfAbsent(module, m -> new TreeMap<>()).merge(resource, 1, Integer::sum);
    }

    /**
     * Runs one write; when this release answers the operation 501 it is listed as skipped and the step goes on, any
     * other refusal stops the run.
     */
    public boolean attempt(String operation, Runnable write) {
        try {
            write.run();
            return true;
        } catch (ApiException e) {
            if (e.notImplemented()) {
                skipped.add(operation);
                return false;
            }
            throw e;
        }
    }

    public void remember(String key, JsonNode row) {
        rows.put(key, row);
    }

    public JsonNode row(String key) {
        JsonNode row = rows.get(key);
        if (row == null) {
            throw new IllegalStateException("no " + key + " seeded before this step");
        }
        return row;
    }

    public String id(String key) {
        return row(key).path("id").asString();
    }

    public UUID uuid(String key) {
        return UUID.fromString(id(key));
    }

    public void add(String group, JsonNode row) {
        groups.computeIfAbsent(group, g -> new ArrayList<>()).add(row);
    }

    public List<JsonNode> group(String group) {
        return groups.getOrDefault(group, List.of());
    }

    public void persona(String persona, String username) {
        personas.put(persona, username);
    }

    public Map<String, Map<String, Integer>> tally() {
        return tally;
    }

    public List<String> skipped() {
        return List.copyOf(skipped);
    }

    public Map<String, String> personas() {
        return personas;
    }
}
