package com.mysms.learning.lms.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** The upload-header struct mapping and the checksum platform verifies. */
class PlatformFilesTest {

    @Test
    void readsOnlyStringHeadersFromTheStruct() {
        Value headers = Value.newBuilder().setStructValue(Struct.newBuilder()
                .putFields("x-goog-meta-a", Value.newBuilder().setStringValue("1").build())
                .putFields("ignored", Value.newBuilder().setListValue(ListValue.getDefaultInstance()).build()))
                .build();
        assertThat(PlatformFiles.headers(headers)).containsExactly(java.util.Map.entry("x-goog-meta-a", "1"));
        assertThat(PlatformFiles.headers(Value.getDefaultInstance())).isEmpty();
    }

    @Test
    void checksumIsLowercaseHexSha256() {
        assertThat(PlatformFiles.sha256("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
