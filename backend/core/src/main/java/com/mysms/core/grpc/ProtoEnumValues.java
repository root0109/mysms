package com.mysms.core.grpc;

import java.util.Map;

/**
 * Proto enum values whose REST value does not follow the rule {@link ProtoBridge} applies (the value name without the
 * enum's prefix, lower-cased: {@code STATUS_PRESENT} ↔ {@code "present"}), for example blood groups
 * ({@code BLOOD_GROUP_A_POSITIVE} ↔ {@code "A+"}). A module declares one bean of this type in its configuration for
 * the values of its own contract; the bridge collects them all.
 *
 * @param restValues the REST value by the full name of the proto enum value
 *                   ({@code mysms.people.v1.Person.BloodGroup.BLOOD_GROUP_A_POSITIVE})
 */
public record ProtoEnumValues(Map<String, String> restValues) {

    public ProtoEnumValues {
        restValues = Map.copyOf(restValues);
    }
}
