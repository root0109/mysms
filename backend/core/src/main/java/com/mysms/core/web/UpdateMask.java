package com.mysms.core.web;

import com.mysms.core.error.InvalidArgumentException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Which fields a partial update changes: with a mask, a listed field absent from the body is cleared; without one,
 * only the non-null body fields apply. Names are checked against the writable fields (400 otherwise).
 */
public final class UpdateMask {

    private UpdateMask() {
    }

    /** The fields to apply, in the order of {@code writable}. */
    public static List<String> fields(String updateMask, Object body, List<String> writable) {
        if (updateMask == null || updateMask.isBlank()) {
            BeanWrapperImpl bean = new BeanWrapperImpl(body);
            return writable.stream().filter(f -> bean.getPropertyValue(f) != null).toList();
        }
        Set<String> listed = new LinkedHashSet<>();
        for (String part : updateMask.split(",")) {
            String field = part.trim();
            if (!writable.contains(field)) {
                throw new InvalidArgumentException("updateMask: '" + field + "' is not an updatable field.");
            }
            listed.add(field);
        }
        return writable.stream().filter(listed::contains).toList();
    }

    /** For callers that keep the writable fields as an array constant. */
    public static List<String> fields(String updateMask, Object body, String... writable) {
        return fields(updateMask, body, Arrays.asList(writable));
    }
}
