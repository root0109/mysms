package com.mysms.platform.business;

import com.mysms.authclient.AuthContext;
import com.mysms.core.db.AppTransactionManager;
import com.mysms.core.db.ModuleDatabase;
import com.mysms.core.error.FieldError;
import com.mysms.core.error.PermissionDeniedException;
import com.mysms.core.error.ValidationFailedException;
import com.mysms.core.paging.Page;
import com.mysms.core.paging.PageRequest;
import com.mysms.platform.api.model.FileObject;
import com.mysms.platform.api.model.Operation;
import com.mysms.platform.api.model.ReportDefinition;
import com.mysms.platform.api.model.RunReportRequest;
import com.mysms.platform.config.PlatformDatabaseConfig;
import com.mysms.platform.repository.AuditLogRepository;
import com.mysms.platform.repository.AuditLogRow;
import com.mysms.platform.repository.FileObjectRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The platform's own reports (platform.ListReportDefinitions, platform.RunReport). Each report names its permission
 * and, since it spans the whole tenant, needs it through a tenant-wide grant; the list shows only the reports the
 * caller may run. Reports of other modules are theirs to define and run — the platform may not read their data — so
 * this list holds the platform's reports only. A run produces a CSV file (downloaded through platform.GetDownloadUrl)
 * and answers the finished operation whose result is that file; it is bounded so it finishes within the request.
 */
@Service
public class ReportService extends AppTransactionManager {

    /** The most rows a report file holds; a larger result is refused with a hint to narrow it. */
    static final int MAX_ROWS = 100_000;
    static final String CSV = "csv";

    /** One report: its code, title, the permission it needs, its parameters' JSON Schema and how it is produced. */
    private record Report(String code, String title, String permission, Map<String, Object> parameters,
                          Function<Map<?, ?>, List<List<String>>> rows) {
    }

    private final AuditLogRepository auditLogs;
    private final FileObjectRepository files;
    private final FileObjectService fileService;
    private final OperationService operations;
    private final Clock clock;
    private final Map<String, Report> reports;

    public ReportService(AuditLogRepository auditLogs, FileObjectRepository files, FileObjectService fileService,
            OperationService operations,
            @Qualifier(PlatformDatabaseConfig.DATABASE) ModuleDatabase database,
            @Qualifier(PlatformDatabaseConfig.TRANSACTIONS) PlatformTransactionManager transactions, Clock clock) {
        super(database, transactions);
        this.auditLogs = auditLogs;
        this.files = files;
        this.fileService = fileService;
        this.operations = operations;
        this.clock = clock;
        this.reports = Stream.of(
                new Report("platform.audit_log", "Audit log", AuditLogService.PERMISSION_READ, Map.of(
                        "type", "object", "required", List.of("from", "to"),
                        "properties", Map.of("from", Map.of("type", "string", "format", "date"),
                                "to", Map.of("type", "string", "format", "date"))), this::auditLogRows),
                new Report("platform.file_object", "Stored files", FileObjectService.PERMISSION_READ,
                        Map.of("type", "object", "properties", Map.of()), this::fileRows))
                .collect(Collectors.toMap(Report::code, r -> r, (a, b) -> a, LinkedHashMap::new));
    }

    /** The reports the caller may run, of the platform (or of no module filter); another module's list is empty. */
    public Page<ReportDefinition> listReportDefinitions(String module, Integer pageNumber, Integer pageSize) {
        AuthContext caller = AuthContext.require();
        int page = pageNumber == null ? 1 : pageNumber;
        int size = pageSize == null ? PageRequest.DEFAULT_SIZE : pageSize;
        List<ReportDefinition> mine = module != null && !"platform".equals(module) ? List.of()
                : reports.values().stream().filter(r -> caller.hasTenantWidePermission(r.permission()))
                        .map(r -> new ReportDefinition(r.code(), "platform", r.title(), List.of(CSV))
                                .parameters(r.parameters()))
                        .toList();
        int from = Math.min((page - 1) * size, mine.size());
        int to = Math.min(from + size, mine.size());
        return new Page<>(mine.subList(from, to), page, size, to < mine.size() ? "p" + (page + 1) : "");
    }

    /**
     * platform.RunReport: 422 for an unknown report, a format it does not produce or invalid parameters; 403 without
     * its permission held tenant-wide; 503 when the file store is not configured.
     */
    public Operation runReport(RunReportRequest request) {
        AuthContext caller = AuthContext.require();
        Report report = Optional.ofNullable(reports.get(request.getReportCode())).orElseThrow(() ->
                new ValidationFailedException(List.of(new FieldError("reportCode", "not a report", "NOT_FOUND"))));
        if (!caller.hasTenantWidePermission(report.permission())) {
            throw new PermissionDeniedException("This report needs " + report.permission() + " for the tenant.");
        }
        if (!CSV.equals(request.getFormat().getValue())) {
            throw new ValidationFailedException(List.of(new FieldError("format", "this report is made as csv",
                    "AllowedValues")));
        }
        Map<?, ?> parameters = request.getParameters() instanceof Map<?, ?> given ? given : Map.of();
        List<List<String>> rows = doInReadTransaction(() -> report.rows().apply(parameters));
        if (rows.size() > MAX_ROWS + 1) {
            throw new ValidationFailedException(List.of(new FieldError("parameters",
                    "the report would exceed " + MAX_ROWS + " rows; narrow it", "TOO_LARGE")));
        }
        String name = report.code().replace('.', '-') + "-" + LocalDate.now(clock.withZone(ZoneOffset.UTC)) + ".csv";
        FileObject output = fileService.saveOutput(name, "text/csv", csv(rows), null, null);
        return operations.recordFinished("platform.RunReport", report.code(), null, "file", output.getId());
    }

    /** The tenant's audit records of [from, to] (dates, UTC, at most 366 days), oldest first. */
    private List<List<String>> auditLogRows(Map<?, ?> parameters) {
        LocalDate from = date(parameters, "from");
        LocalDate to = date(parameters, "to");
        if (to.isBefore(from) || ChronoUnit.DAYS.between(from, to) > 366) {
            throw new ValidationFailedException(List.of(new FieldError("parameters.to",
                    "on or after from, at most 366 days later", "RANGE")));
        }
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("occurredAt", "event", "subjectType", "subjectId", "causerType", "causerId", "description"));
        for (AuditLogRow row : auditLogs.export(from.atStartOfDay().atOffset(ZoneOffset.UTC),
                to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC), MAX_ROWS + 1)) {
            rows.add(cells(row.occurredAt(), row.event(), row.subjectType(), row.subjectId(), row.causerType(),
                    row.causerId(), row.description()));
        }
        return rows;
    }

    /** Every live file of the tenant, oldest first. */
    private List<List<String>> fileRows(Map<?, ?> parameters) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("id", "fileName", "mimeType", "sizeBytes", "status", "ownerEntityType", "ownerEntityId",
                "uploadedByUserId", "createTime"));
        files.export(MAX_ROWS + 1).forEach(f -> rows.add(cells(f.getId(), f.getFileName(), f.getMimeType(),
                f.getSizeBytes(), f.getStatus(), f.getOwnerEntityType(), f.getOwnerEntityId(), f.getUploadedByUserId(),
                f.getCreatedAt())));
        return rows;
    }

    private static LocalDate date(Map<?, ?> parameters, String name) {
        FieldError invalid = new FieldError("parameters." + name, "a date (yyyy-mm-dd)", "Pattern");
        if (!(parameters.get(name) instanceof String value)) {
            throw new ValidationFailedException(List.of(invalid));
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException notADate) {
            throw new ValidationFailedException(List.of(invalid));
        }
    }

    private static List<String> cells(Object... values) {
        return Stream.of(values).map(v -> v == null ? "" : v.toString()).toList();
    }

    /**
     * RFC 4180 CSV in UTF-8 with a byte-order mark (spreadsheets then read names correctly). A cell a spreadsheet would
     * run as a formula (starting =, +, -, @, tab or carriage return) is prefixed with an apostrophe.
     */
    static byte[] csv(List<List<String>> rows) {
        StringBuilder out = new StringBuilder("﻿");
        for (List<String> row : rows) {
            out.append(row.stream().map(ReportService::cell).collect(Collectors.joining(","))).append("\r\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String cell(String value) {
        String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }
}
