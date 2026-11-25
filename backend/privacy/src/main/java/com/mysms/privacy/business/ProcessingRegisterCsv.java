package com.mysms.privacy.business;

import com.mysms.privacy.model.AccessSummarySnapshot;
import com.mysms.privacy.repository.RegisterEntryView;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Writes the processing register as CSV (RFC 4180): a header row, then one row per activity, CRLF line ends, a cell
 * quoted when it holds a comma, quote or line break, and one opening with a formula character defused with a leading
 * apostrophe, so a spreadsheet never runs a cell. Lists inside a cell are separated by "; ".
 */
final class ProcessingRegisterCsv {

    private static final List<String> HEADER = List.of("activity_code", "module", "purpose", "legal_basis",
            "education_exemption_clause", "targets_minors", "audiences", "data_categories", "recipients",
            "cross_border", "retention_policy");
    private static final String LIST_SEPARATOR = "; ";
    private static final String ROW_END = "\r\n";

    private ProcessingRegisterCsv() {
    }

    static String write(List<RegisterEntryView> register) {
        return Stream.concat(Stream.of(HEADER), register.stream().map(ProcessingRegisterCsv::row))
                .map(cells -> cells.stream().map(ProcessingRegisterCsv::cell).collect(Collectors.joining(",")))
                .collect(Collectors.joining(ROW_END, "", ROW_END));
    }

    private static List<String> row(RegisterEntryView entry) {
        return List.of(entry.code(), entry.module(), entry.purposeCode(), entry.legalBasis(),
                Objects.toString(entry.educationExemptionClause(), ""), String.valueOf(entry.targetsMinors()),
                String.join(LIST_SEPARATOR, entry.audiences()), String.join(LIST_SEPARATOR, entry.categories()),
                entry.recipients().stream().map(ProcessingRegisterCsv::recipient)
                        .collect(Collectors.joining(LIST_SEPARATOR)),
                String.valueOf(entry.crossBorder()), Objects.toString(entry.retentionPolicyCode(), ""));
    }

    /** A recipient as "name (kind, country)", the country left out when the register does not say. */
    private static String recipient(AccessSummarySnapshot.Recipient recipient) {
        String country = recipient.country();
        return recipient.name() + " (" + recipient.kind() + (country == null ? "" : ", " + country) + ")";
    }

    private static String cell(String value) {
        String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
