package com.mysms.core.web;

import com.mysms.core.error.InvalidArgumentException;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * Partial responses (`fields`): keeps only the listed top-level fields of a response DTO and clears the rest, which
 * non-null JSON inclusion then omits. Unknown names → 400.
 */
public final class ReadMask {

    private ReadMask() {
    }

    public static <T> T apply(T dto, String fields) {
        if (fields == null || fields.isBlank()) {
            return dto;
        }
        Set<String> wanted = Arrays.stream(fields.split(",")).map(String::trim).filter(f -> !f.isEmpty())
                .collect(Collectors.toSet());
        BeanWrapper bean = new BeanWrapperImpl(dto);
        Set<String> known = Arrays.stream(bean.getPropertyDescriptors())
                .filter(p -> p.getWriteMethod() != null && p.getReadMethod() != null)
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toSet());
        for (String field : wanted) {
            if (!known.contains(field)) {
                throw new InvalidArgumentException("fields: unknown field '" + field + "'.");
            }
        }
        for (String property : known) {
            if (!wanted.contains(property)) {
                bean.setPropertyValue(property, null);
            }
        }
        return dto;
    }
}
