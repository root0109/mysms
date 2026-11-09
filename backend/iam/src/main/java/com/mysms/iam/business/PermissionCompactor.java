package com.mysms.iam.business;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Compacts an effective permission set for the token's {@code perms} claim: a complete catalogue becomes {@code *},
 * every permission of a module {@code <module>.*}, every action on a resource {@code <module>.<resource>.*}. The
 * verifier expands the same wildcards (AuthContext.hasPermission), so a broad role still fits in a request header.
 * Wildcards are only ever produced from complete sets of the current catalogue.
 */
public final class PermissionCompactor {

    private PermissionCompactor() {
    }

    /**
     * @param granted   the caller's effective permission codes
     * @param catalogue every permission code that exists
     */
    public static Set<String> compact(Collection<String> granted, Collection<String> catalogue) {
        Set<String> grantedSet = new TreeSet<>(granted);
        Set<String> all = new TreeSet<>(catalogue);
        if (!all.isEmpty() && grantedSet.containsAll(all)) {
            return Set.of("*");
        }
        Map<String, Integer> catalogueCount = counts(all);
        Map<String, Integer> grantedCount = counts(grantedSet);
        Set<String> out = new TreeSet<>();
        for (String code : grantedSet) {
            String module = prefix(code, 1);
            String resource = prefix(code, 2);
            if (module != null && complete(module, grantedCount, catalogueCount)) {
                out.add(module + "*");
            } else if (resource != null && complete(resource, grantedCount, catalogueCount)) {
                out.add(resource + "*");
            } else {
                out.add(code);
            }
        }
        return out;
    }

    private static boolean complete(String prefix, Map<String, Integer> granted, Map<String, Integer> catalogue) {
        Integer total = catalogue.get(prefix);
        return total != null && total.equals(granted.get(prefix));
    }

    /** How many codes share each module prefix ("lms.") and resource prefix ("lms.course."). */
    private static Map<String, Integer> counts(Set<String> codes) {
        Map<String, Integer> counts = new HashMap<>();
        for (String code : codes) {
            for (int level = 1; level <= 2; level++) {
                String prefix = prefix(code, level);
                if (prefix != null) {
                    counts.merge(prefix, 1, Integer::sum);
                }
            }
        }
        return counts;
    }

    /** The first {@code level} dot-separated segments plus the dot, when the code has more segments than that. */
    private static String prefix(String code, int level) {
        int end = -1;
        for (int i = 0; i < level; i++) {
            end = code.indexOf('.', end + 1);
            if (end < 0) {
                return null;
            }
        }
        return code.substring(0, end + 1);
    }
}
