package com.mysms.platform.validation;

import com.mysms.core.imports.ImportBatch;
import com.mysms.core.imports.ImportBatchResult.RowError;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The import templates of platform.GetImportTemplate and platform.StartImport, owned here as source: per entity type,
 * the module that owns its records, the create permission an importer needs, whether the records belong to an
 * institution, and the columns — the owning module's create contract, less what an import must not set (ids of
 * accounts and files, which have operations of their own; the institution, which the import names once). A file is
 * checked against its template for structure, required cells and formats only; the owner's business rules
 * (references, uniqueness, scope) are applied by the owner when it creates each row.
 */
public final class ImportTemplates {

    /** The entity type the contract lists but no module imports yet (a course package is a ZIP, not CSV rows). */
    public static final Set<String> UNAVAILABLE = Set.of("lms_course_package");
    /** The comment marker of the template's instruction lines; such records are skipped on import. */
    public static final String COMMENT = "#";
    /** The longest cell of a column without a limit of its own. */
    static final int MAX_TEXT = 4000;

    /** Where the records of an entity type belong. */
    public enum Scope {
        /** to the institution the import names (required) */
        INSTITUTION,
        /** to the whole tenant: no institution, a tenant-wide grant */
        TENANT,
        /** to the named institution, or to the tenant when none is named */
        EITHER
    }

    /** A cell format: how it is described, checked and typed for the owner's create contract. */
    public enum Format {
        TEXT("text"),
        ID("an id (UUID)"),
        DATE("a date as yyyy-mm-dd"),
        DECIMAL("a number such as 125000.50"),
        WHOLE_NUMBER("a whole number from 0 to 1000"),
        BOOLEAN("true or false"),
        CHOICE("one of the listed values");

        private static final Pattern DECIMAL_TEXT = Pattern.compile("-?\\d{1,15}(\\.\\d{1,4})?");

        private final String description;

        Format(String description) {
            this.description = description;
        }

        /** The typed value of a non-empty cell, or empty when it does not have the format. */
        Optional<?> read(String cell, List<String> choices) {
            try {
                return switch (this) {
                    case TEXT -> Optional.of(cell);
                    case ID -> Optional.of(UUID.fromString(cell).toString()).filter(id -> id.equalsIgnoreCase(cell));
                    case DATE -> Optional.of(LocalDate.parse(cell).toString());
                    case DECIMAL -> Optional.of(cell).filter(c -> DECIMAL_TEXT.matcher(c).matches())
                            .map(c -> new BigDecimal(c).toPlainString());
                    case WHOLE_NUMBER -> Optional.of(Integer.parseInt(cell)).filter(n -> n >= 0 && n <= 1000);
                    case BOOLEAN -> Optional.ofNullable(switch (cell.toLowerCase(Locale.ROOT)) {
                        case "true" -> Boolean.TRUE;
                        case "false" -> Boolean.FALSE;
                        default -> null;
                    });
                    case CHOICE -> choices.stream().filter(c -> c.equalsIgnoreCase(cell)).findFirst();
                };
            } catch (IllegalArgumentException | DateTimeParseException wrongFormat) {
                return Optional.empty();
            }
        }
    }

    /** One column: its contract field name, whether a row needs it, its format, limit, choices and meaning. */
    public record Column(String name, boolean required, Format format, int maxLength, List<String> choices,
                         String meaning) {

        public Column {
            choices = List.copyOf(choices);
        }

        /** The instruction line the template file carries for the column. */
        public String describe() {
            String kind = switch (format) {
                case TEXT -> "text of at most " + maxLength + " characters";
                case CHOICE -> "one of " + String.join(", ", choices);
                default -> format.description;
            };
            return name + ": " + (required ? "required" : "optional") + "; " + kind + ". " + meaning;
        }

        /** Why a non-empty cell is refused, or empty when it is accepted. */
        Optional<String> refusal(String cell) {
            if (format == Format.TEXT) {
                return cell.length() > maxLength ? Optional.of("at most " + maxLength + " characters")
                        : Optional.empty();
            }
            return format.read(cell, choices).isPresent() ? Optional.empty()
                    : Optional.of(format == Format.CHOICE ? "one of " + String.join(", ", choices)
                            : format.description);
        }
    }

    /** What a file came to: its data rows, the rows ready for the owner (typed), and the refused rows' errors. */
    public record Checked(int rowCount, List<ImportBatch.Row> valid, List<RowError> errors) {

        public Checked {
            valid = List.copyOf(valid);
            errors = List.copyOf(errors);
        }
    }

    /**
     * One entity type's template.
     *
     * @param module     the module that owns and creates the records
     * @param permission the owner's create permission the importer needs (for the institution, or tenant-wide)
     */
    public record Template(String entityType, String module, String permission, Scope scope, List<Column> columns) {

        public Template {
            columns = List.copyOf(columns);
        }

        /** The template file's records: the instruction lines (one cell each), then the header row. */
        public List<List<String>> fileRecords() {
            List<List<String>> records = new ArrayList<>();
            records.add(List.of(COMMENT + " MySMS import template: " + entityType + ". Lines starting with "
                    + COMMENT + " are instructions; they are ignored on import and may stay."));
            records.add(List.of(COMMENT + " One row per record under the header row; save as CSV UTF-8. "
                    + switch (scope) {
                        case INSTITUTION -> "The institution is chosen when the import starts.";
                        case TENANT -> "The records belong to the whole tenant.";
                        case EITHER -> "Choose an institution when the import starts, or none for the whole tenant.";
                    }));
            columns.forEach(column -> records.add(List.of(COMMENT + " " + column.describe())));
            records.add(columns.stream().map(Column::name).toList());
            return records;
        }

        /** Checks a file's records against the template: the header first, then every data row. */
        public Checked check(List<List<String>> records) {
            List<RowError> errors = new ArrayList<>();
            int header = -1;
            for (int i = 0; i < records.size() && header < 0; i++) {
                if (!skipped(records.get(i))) {
                    header = i;
                }
            }
            if (header < 0) {
                return new Checked(0, List.of(), List.of(new RowError(1, null, "the file has no header row")));
            }
            List<String> names = records.get(header).stream().map(String::strip).toList();
            List<Column> order = headerColumns(header + 1, names, errors);
            List<ImportBatch.Row> valid = new ArrayList<>();
            int rows = 0;
            for (int i = header + 1; i < records.size(); i++) {
                List<String> record = records.get(i);
                if (skipped(record)) {
                    continue;
                }
                rows++;
                if (order != null) {
                    checkRow(i + 1, record, order, errors).ifPresent(valid::add);
                }
            }
            return new Checked(rows, valid, errors);
        }

        /** The column of each header cell, or null (with the errors) when the header does not fit the template. */
        private List<Column> headerColumns(int rowNumber, List<String> names, List<RowError> errors) {
            Map<String, Column> known = columns.stream().collect(Collectors.toMap(Column::name, Function.identity()));
            List<Column> order = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            int before = errors.size();
            for (String name : names) {
                Column column = known.get(name);
                if (column == null) {
                    errors.add(new RowError(rowNumber, name.isEmpty() ? null : name, name.isEmpty()
                            ? "a header cell is empty" : "not a column of the " + entityType + " template"));
                } else if (!seen.add(name)) {
                    errors.add(new RowError(rowNumber, name, "the column appears twice"));
                }
                order.add(column);
            }
            columns.stream().filter(c -> c.required() && !seen.contains(c.name())).forEach(missing ->
                    errors.add(new RowError(rowNumber, missing.name(), "a required column is missing")));
            return errors.size() == before ? order : null;
        }

        private static Optional<ImportBatch.Row> checkRow(int rowNumber, List<String> record, List<Column> order,
                List<RowError> errors) {
            int before = errors.size();
            if (record.size() > order.size()) {
                errors.add(new RowError(rowNumber, null, "the row has more cells than the header has columns"));
            }
            Map<String, Object> values = new LinkedHashMap<>();
            for (int c = 0; c < order.size(); c++) {
                Column column = order.get(c);
                String cell = c < record.size() ? record.get(c).strip() : "";
                if (cell.isEmpty()) {
                    if (column.required()) {
                        errors.add(new RowError(rowNumber, column.name(), "required"));
                    }
                    continue;
                }
                Optional<String> refusal = column.refusal(cell);
                if (refusal.isPresent()) {
                    errors.add(new RowError(rowNumber, column.name(), refusal.get()));
                } else {
                    values.put(column.name(), column.format().read(cell, column.choices()).orElseThrow());
                }
            }
            return errors.size() == before ? Optional.of(new ImportBatch.Row(rowNumber, values)) : Optional.empty();
        }

        /** A record that is no row: blank, or an instruction line. */
        private static boolean skipped(List<String> record) {
            return record.stream().allMatch(String::isBlank) || record.get(0).strip().startsWith(COMMENT);
        }
    }

    private static final Map<String, Template> CATALOGUE = Stream.of(
            new Template("students", "sis", "sis.student.create", Scope.INSTITUTION, List.of(
                    id("personId", true, "The student's person record (people)."),
                    text("admissionNumber", false, 30, "Admission number, unique in the institution."),
                    column("admissionDate", false, Format.DATE, "Date of admission."),
                    column("joiningDate", false, Format.DATE, "Date the student joined."),
                    id("studentCategoryId", false, "Student category (sis)."),
                    text("governmentStudentId", false, 40, "Government-issued student id."),
                    text("bonafideCertificateNumber", false, 40, "Bonafide certificate number."))),
            new Template("guardians", "sis", "sis.guardian.create", Scope.TENANT, List.of(
                    id("personId", true, "The guardian's person record (people)."),
                    choice("profession", List.of("admin", "business", "central_government", "state_government",
                            "private_sector", "home_maker", "teacher", "librarian", "other"), "Profession."),
                    text("subOccupation", false, 120, "Occupation detail."),
                    text("designation", false, 120, "Designation at work."),
                    text("organizationName", false, 200, "Employer."),
                    column("annualIncome", false, Format.DECIMAL, "Annual income."),
                    id("qualificationId", false, "Highest qualification (academics)."),
                    text("mobile", false, 20, "Mobile number."),
                    text("email", false, 320, "E-mail address."))),
            new Template("promotions", "sis", "sis.promotion.create", Scope.INSTITUTION, List.of(
                    id("studentId", true, "The student (sis)."),
                    id("fromAcademicYearId", true, "Academic year promoted from."),
                    id("fromGradeLevelId", true, "Grade level promoted from."),
                    id("fromSectionId", false, "Section promoted from."),
                    id("toAcademicYearId", false, "Academic year promoted to."),
                    id("toGradeLevelId", false, "Grade level promoted to."),
                    id("toSectionId", false, "Section promoted to."),
                    id("basedOnExamSessionId", false, "Exam session the promotion is based on."),
                    text("remarks", false, MAX_TEXT, "Remarks."))),
            new Template("staff", "hr", "hr.staff.create", Scope.INSTITUTION, List.of(
                    id("personId", true, "The staff member's person record (people)."),
                    text("employeeCode", false, 30, "Employee code, unique in the institution."),
                    id("designationId", false, "Designation (hr)."),
                    text("subDesignation", false, 120, "Designation detail."),
                    choice("employmentType", List.of("full_time", "part_time", "contract", "visiting"),
                            "Employment type."),
                    column("joiningDate", false, Format.DATE, "Date of joining."),
                    id("reportingToStaffId", false, "The staff member reported to."),
                    id("primaryDepartmentId", false, "Primary department (org)."),
                    id("highestQualificationId", false, "Highest qualification (academics)."),
                    text("qualificationDetail", false, 150, "Qualification detail."),
                    text("specialization", false, 150, "Specialization."),
                    text("interests", false, MAX_TEXT, "Interests."),
                    column("maxPeriodsPerWeek", false, Format.WHOLE_NUMBER, "Most periods a week."))),
            new Template("library_media", "library", "library.media.create", Scope.INSTITUTION, List.of(
                    text("title", true, 300, "Title."),
                    text("isbn", false, 20, "ISBN."),
                    text("internalCode", false, 40, "Accession / call code."),
                    text("edition", false, 60, "Edition."),
                    text("description", false, MAX_TEXT, "Description."),
                    id("mediaTypeId", false, "Media type (library)."),
                    id("mediaCategoryId", false, "Media category (library)."),
                    id("acquiredAcademicYearId", false, "Academic year the title was acquired."),
                    column("isActive", false, Format.BOOLEAN, "In circulation."))),
            new Template("holidays", "org", "org.holiday.create", Scope.EITHER, List.of(
                    text("title", true, 200, "Name of the holiday."),
                    column("startDate", true, Format.DATE, "First day."),
                    column("endDate", true, Format.DATE, "Last day."))))
            .collect(Collectors.toMap(Template::entityType, Function.identity(), (a, b) -> a, LinkedHashMap::new));

    private ImportTemplates() {
    }

    /** The template of an entity type, when the platform has one. */
    public static Optional<Template> find(String entityType) {
        return Optional.ofNullable(CATALOGUE.get(entityType));
    }

    /** Every template, in catalogue order. */
    public static List<Template> all() {
        return List.copyOf(CATALOGUE.values());
    }

    private static Column column(String name, boolean required, Format format, String meaning) {
        return new Column(name, required, format, 0, List.of(), meaning);
    }

    private static Column id(String name, boolean required, String meaning) {
        return column(name, required, Format.ID, meaning);
    }

    private static Column text(String name, boolean required, int maxLength, String meaning) {
        return new Column(name, required, Format.TEXT, maxLength, List.of(), meaning);
    }

    private static Column choice(String name, List<String> choices, String meaning) {
        return new Column(name, false, Format.CHOICE, 0, choices, meaning);
    }
}
