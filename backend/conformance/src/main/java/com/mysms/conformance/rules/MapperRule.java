package com.mysms.conformance.rules;

import com.mysms.conformance.ConformanceRule;
import com.mysms.conformance.SourceTree;
import com.mysms.conformance.Violation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * M1: one {@code @MapperConfig} per area, named {@code <Area>MapperConfig}, with
 * {@code unmappedTargetPolicy = ERROR}, used by every {@code @Mapper}; mappers are stateless interfaces (no
 * componentModel, null-value strategies, {@code @MappingTarget} or injected fields); a DTO → entity method ignores the
 * id, the tenant and the audit set, so a client can never set them.
 */
public final class MapperRule implements ConformanceRule {

    private static final Pattern MAPPER_POLICY = Pattern.compile(
            "componentModel|NullValue(Property|Check)|@(?:[\\w.]+\\.)?(MappingTarget|Autowired)\\b"
                    + "|abstract class \\w+Mapper");
    private static final Pattern TO_ENTITY = Pattern.compile("(?m)^    (\\w+Entity) \\w+\\(\\w+ \\w+\\);");
    private static final Pattern IGNORED = Pattern.compile("@Mapping\\(target = \"(\\w+)\", ignore = true\\)");

    @Override
    public List<Violation> check(SourceTree tree) {
        List<Violation> found = new ArrayList<>();
        for (SourceTree.Area area : tree.areas()) {
            String module = area.name();
            Path base = tree.base(area, "main");
            String config = Character.toUpperCase(module.charAt(0)) + module.substring(1) + "MapperConfig";
            List<Path> mappers = tree.files(base.resolve("mapper"), ".java");
            List<Path> configs = mappers.stream().filter(f -> tree.read(f).contains("@MapperConfig")).toList();
            if (configs.size() != 1 || !SourceTree.fileName(configs.get(0)).equals(config + ".java")) {
                found.add(new Violation("M1", tree.relative(base.resolve("mapper")),
                        "needs exactly one @MapperConfig, " + config));
            }
            for (Path file : configs) {
                if (!tree.read(file).contains("unmappedTargetPolicy = ReportingPolicy.ERROR")) {
                    found.add(new Violation("M1", tree.relative(file), "unmappedTargetPolicy must be ERROR"));
                }
            }
            Map<String, Boolean> entityHasTenant = new HashMap<>();
            for (Path entity : tree.files(base.resolve("domain"), "Entity.java")) {
                String name = SourceTree.fileName(entity);
                entityHasTenant.put(name.substring(0, name.length() - ".java".length()),
                        tree.read(entity).contains("private UUID tenantId;"));
            }
            for (Path file : mappers) {
                String source = tree.read(file);
                if (source.contains("@Mapper") && !configs.contains(file)) {
                    mapper(tree, file, source, config, entityHasTenant, found);
                }
            }
        }
        return found;
    }

    private static void mapper(SourceTree tree, Path file, String source, String config,
                               Map<String, Boolean> entityHasTenant, List<Violation> found) {
        String path = tree.relative(file);
        if (!source.contains("@Mapper(config = " + config + ".class")) {
            found.add(new Violation("M1", path, "must use config = " + config + ".class"));
        }
        if (MAPPER_POLICY.matcher(source).find()) {
            found.add(new Violation("M1", path, "mapper policy, null strategy, @MappingTarget or state (§8)"));
        }
        Matcher method = TO_ENTITY.matcher(source);
        while (method.find()) {
            String entity = method.group(1);
            // the annotations of this method: everything since the previous declaration ended
            int start = Math.max(source.lastIndexOf(';', method.start()), source.lastIndexOf('{', method.start()));
            Set<String> ignored = new TreeSet<>();
            IGNORED.matcher(source.substring(start + 1, method.start())).results().forEach(m -> ignored.add(m.group(1)));
            Set<String> needed = new LinkedHashSet<>(PersistenceRule.AUDIT_SET);
            if (entityHasTenant.getOrDefault(entity, false)) {
                needed.add("tenantId");
            }
            needed.removeAll(ignored);
            if (!needed.isEmpty()) {
                found.add(new Violation("M1", path,
                        entity + " from a DTO must ignore " + String.join(", ", new TreeSet<>(needed))));
            }
        }
    }
}
