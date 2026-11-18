package com.mysms.conformance;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.conformance.rules.GrpcGuidelineRule;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** How the field-number check (G2) reads a proto: top-level messages and their own fields only. */
class GrpcGuidelineRuleTest {

    @Test
    void readsTheFieldsOfTopLevelMessagesOnly() {
        Map<String, Map<String, Integer>> messages = GrpcGuidelineRule.messages("""
                // a comment = 9;
                message Widget {
                  reserved 4, 5;
                  string id = 1;
                  optional string name = 2; // trailing = 7;
                  repeated Status history = 3;
                  map<string, int32> by_key = 6;
                  enum Status {
                    STATUS_UNSPECIFIED = 0;
                    STATUS_ACTIVE = 1;
                  }
                  google.protobuf.Timestamp create_time = 7 [deprecated = true];
                }
                message Empty {
                }
                """);

        assertThat(messages).containsOnlyKeys("Widget", "Empty");
        assertThat(messages.get("Widget")).containsExactlyInAnyOrderEntriesOf(Map.of("id", 1, "name", 2, "history", 3,
                "by_key", 6, "create_time", 7));
        assertThat(messages.get("Empty")).isEmpty();
    }
}
