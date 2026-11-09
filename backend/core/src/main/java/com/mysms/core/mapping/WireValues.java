package com.mysms.core.mapping;

import com.mysms.core.error.FieldError;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Conversions between the contract's string-encoded values (decimals, 64-bit integers and times of day travel as
 * strings, api/README "Data types") and the column types, plus the checks that keep a value the database would reject
 * or silently round out of every write (422 with the field, never a 500 or a changed amount).
 */
public final class WireValues {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ISO_LOCAL_TIME;

    private WireValues() {
    }

    public static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    public static String decimal(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    public static Long int64(String value) {
        return value == null ? null : Long.valueOf(value);
    }

    public static String int64(Long value) {
        return value == null ? null : value.toString();
    }

    public static LocalTime time(String value) {
        return value == null ? null : LocalTime.parse(value, TIME);
    }

    public static String time(LocalTime value) {
        return value == null ? null : value.format(TIME);
    }

    /**
     * A duration as the contract carries it ("5400s", the JSON form of a protobuf Duration) stored as a whole number of
     * {@code unitSeconds} (60 for a minutes column, 1 for a seconds column). {@link #checkDuration} refuses values that
     * do not convert exactly, so this never rounds.
     */
    public static Integer duration(String value, int unitSeconds) {
        return value == null ? null : Math.toIntExact(new BigDecimal(value.substring(0, value.length() - 1))
                .divide(BigDecimal.valueOf(unitSeconds)).intValueExact());
    }

    public static String duration(Integer value, int unitSeconds) {
        return value == null ? null : ((long) value * unitSeconds) + "s";
    }

    public static void checkDuration(String field, String value, int unitSeconds, List<FieldError> errors) {
        if (value == null) {
            return;
        }
        try {
            if (!value.endsWith("s")) {
                throw new NumberFormatException(value);
            }
            BigDecimal seconds = new BigDecimal(value.substring(0, value.length() - 1));
            BigDecimal[] units = seconds.divideAndRemainder(BigDecimal.valueOf(unitSeconds));
            if (units[1].signum() != 0) {
                errors.add(new FieldError(field, "must be a whole number of " + (unitSeconds == 60 ? "minutes" : "seconds"),
                        "Duration"));
                return;
            }
            units[0].intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            errors.add(new FieldError(field, "must be a duration such as 90s", "Pattern"));
        }
    }

    /** A decimal that fits numeric(precision, scale) exactly: no rounding, no overflow. */
    public static void checkDecimal(String field, String value, int precision, int scale, List<FieldError> errors) {
        if (value == null) {
            return;
        }
        BigDecimal number;
        try {
            number = new BigDecimal(value);
        } catch (NumberFormatException e) {
            errors.add(new FieldError(field, "must be a decimal number", "Pattern"));
            return;
        }
        BigDecimal plain = number.stripTrailingZeros();
        int fraction = Math.max(plain.scale(), 0);
        int whole = plain.precision() - plain.scale();
        if (fraction > scale) {
            errors.add(new FieldError(field, "must have at most " + scale + " decimal places", "Digits"));
        } else if (whole > precision - scale) {
            errors.add(new FieldError(field, "must have at most " + (precision - scale) + " digits before the point",
                    "Digits"));
        }
    }

    public static void checkInt64(String field, String value, List<FieldError> errors) {
        if (value == null) {
            return;
        }
        try {
            Long.parseLong(value);
        } catch (NumberFormatException e) {
            errors.add(new FieldError(field, "must be a 64-bit integer", "Pattern"));
        }
    }

    public static void checkTime(String field, String value, List<FieldError> errors) {
        if (value == null) {
            return;
        }
        try {
            LocalTime.parse(value, TIME);
        } catch (DateTimeParseException e) {
            errors.add(new FieldError(field, "must be a time of day (HH:mm or HH:mm:ss)", "Pattern"));
        }
    }
}
