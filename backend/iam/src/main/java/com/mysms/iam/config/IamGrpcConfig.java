package com.mysms.iam.config;

import com.mysms.core.grpc.ProtoEnumValues;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The proto enum values of iam's contract whose REST value does not follow the bridge's rule:
 * iam.GetMe and iam.SwitchRole answer the caller's linked person as a {@code mysms.people.v1.Person}, whose blood
 * group is {@code "A+"} on REST and {@code BLOOD_GROUP_A_POSITIVE} in the proto. People declares the same values for
 * its own contract; iam declares them too because an iam service runs without people (in the monolith both beans
 * carry identical entries, which the bridge merges).
 */
@Configuration(proxyBeanMethods = false)
public class IamGrpcConfig {

    private static final String BLOOD_GROUP = "mysms.people.v1.Person.BloodGroup.BLOOD_GROUP_";

    @Bean
    public ProtoEnumValues iamProtoEnumValues() {
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
