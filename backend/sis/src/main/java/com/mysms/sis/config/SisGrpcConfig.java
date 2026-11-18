package com.mysms.sis.config;

import com.mysms.core.grpc.ProtoEnumValues;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Proto enum values whose REST value does not follow the bridge's rule: a people {@code Person}'s blood group is
 * {@code "A+"} on REST and {@code BLOOD_GROUP_A_POSITIVE} in the proto. sis declares them itself because it runs
 * without people; in the monolith the identical entries merge.
 */
@Configuration(proxyBeanMethods = false)
public class SisGrpcConfig {

    private static final String BLOOD_GROUP = "mysms.people.v1.Person.BloodGroup.BLOOD_GROUP_";

    @Bean
    public ProtoEnumValues sisProtoEnumValues() {
        return new ProtoEnumValues(Map.ofEntries(
                Map.entry(BLOOD_GROUP + "A_POSITIVE", "A+"), Map.entry(BLOOD_GROUP + "A_NEGATIVE", "A-"),
                Map.entry(BLOOD_GROUP + "B_POSITIVE", "B+"), Map.entry(BLOOD_GROUP + "B_NEGATIVE", "B-"),
                Map.entry(BLOOD_GROUP + "O_POSITIVE", "O+"), Map.entry(BLOOD_GROUP + "O_NEGATIVE", "O-"),
                Map.entry(BLOOD_GROUP + "AB_POSITIVE", "AB+"), Map.entry(BLOOD_GROUP + "AB_NEGATIVE", "AB-"),
                Map.entry(BLOOD_GROUP + "A1_POSITIVE", "A1+"), Map.entry(BLOOD_GROUP + "A1_NEGATIVE", "A1-"),
                Map.entry(BLOOD_GROUP + "B1_POSITIVE", "B1+"), Map.entry(BLOOD_GROUP + "B1_NEGATIVE", "B1-"),
                Map.entry(BLOOD_GROUP + "A1B_POSITIVE", "A1B+"), Map.entry(BLOOD_GROUP + "A1B_NEGATIVE", "A1B-")));
    }
}
