package com.mysms.platform.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.imports.ImportBatchResult.RowError;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The CSV reader and the template check: records as spreadsheets number them, cells typed for the owner. */
class ImportTemplatesTest {

    private static List<List<String>> csv(String text) {
        return CsvReader.read(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void theReaderFollowsRfc4180() {
        assertThat(csv("﻿a,b\r\n\"x, \"\"y\"\"\",\"line\nbreak\"\nlast,\r\n"))
                .containsExactly(List.of("a", "b"), List.of("x, \"y\"", "line\nbreak"), List.of("last", ""));
        assertThat(csv("one")).containsExactly(List.of("one"));
        assertThat(csv("")).isEmpty();
        assertThat(csv("a\rb\n")).containsExactly(List.of("a\rb"));
        assertThatThrownBy(() -> csv("\"open")).isInstanceOf(CsvReader.MalformedCsvException.class);
        assertThatThrownBy(() -> CsvReader.read(new byte[] {(byte) 0xFF}))
                .isInstanceOf(CsvReader.MalformedCsvException.class).hasMessageContaining("UTF-8");
    }

    @Test
    void cellsAreCheckedAndTypedForTheOwnersContract() {
        ImportTemplates.Template staff = ImportTemplates.find("staff").orElseThrow();
        ImportTemplates.Checked checked = staff.check(csv("""
                # instructions
                personId,employmentType,maxPeriodsPerWeek,joiningDate
                0190B0A0-0000-7000-8000-000000000001,Full_Time,12,2026-06-01
                0190b0a0-0000-7000-8000-000000000002,weekly,1001,2026-02-30
                0190b0a0-0000-7000-8000-000000000003,,,,extra
                """));
        assertThat(checked.rowCount()).isEqualTo(3);
        assertThat(checked.valid()).singleElement().satisfies(row -> {
            assertThat(row.rowNumber()).isEqualTo(3);
            assertThat(row.values()).containsEntry("personId", "0190b0a0-0000-7000-8000-000000000001")
                    .containsEntry("employmentType", "full_time").containsEntry("maxPeriodsPerWeek", 12)
                    .containsEntry("joiningDate", "2026-06-01");
        });
        assertThat(checked.errors()).containsExactly(
                new RowError(4, "employmentType", "one of full_time, part_time, contract, visiting"),
                new RowError(4, "maxPeriodsPerWeek", "a whole number from 0 to 1000"),
                new RowError(4, "joiningDate", "a date as yyyy-mm-dd"),
                new RowError(5, null, "the row has more cells than the header has columns"));

        ImportTemplates.Template media = ImportTemplates.find("library_media").orElseThrow();
        assertThat(media.check(csv("title,isActive,isbn\nAtlas,TRUE,\nGlobe,maybe,\n")).errors())
                .containsExactly(new RowError(3, "isActive", "true or false"));
        assertThat(media.check(csv("title,isActive\nAtlas,false\n")).valid().get(0).values())
                .containsEntry("isActive", false);
        ImportTemplates.Template guardians = ImportTemplates.find("guardians").orElseThrow();
        assertThat(guardians.check(csv("personId,annualIncome\n" + "0190b0a0-0000-7000-8000-000000000001,1e5\n"
                + "0190b0a0-0000-7000-8000-000000000001,125000.50\n")).errors())
                .containsExactly(new RowError(2, "annualIncome", "a number such as 125000.50"));
    }

    @Test
    void theHeaderMustFitTheTemplate() {
        ImportTemplates.Template holidays = ImportTemplates.find("holidays").orElseThrow();
        assertThat(holidays.check(csv("title,title,,start\nDiwali,Diwali,,x\n")).errors()).containsExactly(
                new RowError(1, "title", "the column appears twice"),
                new RowError(1, null, "a header cell is empty"),
                new RowError(1, "start", "not a column of the holidays template"),
                new RowError(1, "startDate", "a required column is missing"),
                new RowError(1, "endDate", "a required column is missing"));
        assertThat(holidays.check(csv("\n# only notes\n")).errors())
                .containsExactly(new RowError(1, null, "the file has no header row"));
        assertThat(holidays.check(csv(String.join(",", "title", "startDate", "endDate") + "\n"
                + "x".repeat(201) + ",2026-11-01,2026-11-02\n")).errors())
                .containsExactly(new RowError(2, "title", "at most 200 characters"));
    }

    @Test
    void everyTemplateFileReadsBackAsAnEmptyImport() {
        for (ImportTemplates.Template template : ImportTemplates.all()) {
            ImportTemplates.Checked checked = template.check(template.fileRecords());
            assertThat(checked.rowCount()).as(template.entityType()).isZero();
            assertThat(checked.errors()).as(template.entityType()).isEmpty();
            assertThat(template.fileRecords().get(template.columns().size() + 2))
                    .isEqualTo(template.columns().stream().map(ImportTemplates.Column::name).toList());
        }
        assertThat(ImportTemplates.find("lms_course_package")).isEmpty();
        assertThat(ImportTemplates.UNAVAILABLE).containsExactly("lms_course_package");
    }
}
