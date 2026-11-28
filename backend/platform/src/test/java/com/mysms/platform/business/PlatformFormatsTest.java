package com.mysms.platform.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The text formats the platform writes and reads: report CSV, the download's Content-Disposition, record types. */
class PlatformFormatsTest {

    @Test
    void csvQuotesWhatNeedsItAndDisarmsFormulas() {
        String csv = new String(ReportService.csv(List.of(List.of("name", "note"),
                List.of("Rao, A.", "=HYPERLINK(\"x\")"), List.of("-5", "line\nbreak"))), StandardCharsets.UTF_8);
        assertThat(csv).isEqualTo("﻿name,note\r\n\"Rao, A.\",\"'=HYPERLINK(\"\"x\"\")\"\r\n'-5,\"line\nbreak\"\r\n");
    }

    @Test
    void aDownloadKeepsItsExactNameWithAnAsciiFallback() {
        assertThat(GcsFileStore.disposition("प्रगति \"report\".pdf"))
                .isEqualTo("attachment; filename=\"______ _report_.pdf\"; filename*=UTF-8''"
                        + "%E0%A4%AA%E0%A5%8D%E0%A4%B0%E0%A4%97%E0%A4%A4%E0%A4%BF%20%22report%22.pdf");
    }

    @Test
    void aRecordTypeIsSchemaDotTableAndASchemaMayHoldAnUnderscore() {
        assertThat(FileObjectService.isRecordType("lms.course")).isTrue();
        assertThat(FileObjectService.isRecordType("time_attendance.staff_attendance")).isTrue();
        assertThat(FileObjectService.isRecordType("_lms.course")).isFalse();
        assertThat(FileObjectService.isRecordType("lms.Course")).isFalse();
        assertThat(FileObjectService.isRecordType("lms")).isFalse();
        assertThat(FileObjectService.isRecordType("lms." + "x".repeat(77))).isFalse();
    }
}
