package com.mysms.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import com.mysms.core.error.FieldError;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WireValuesTest {

    @Test
    void convertsBothWaysAndKeepsNulls() {
        assertThat(WireValues.decimal("12.50")).isEqualByComparingTo(new BigDecimal("12.5"));
        assertThat(WireValues.decimal(new BigDecimal("1E+3"))).isEqualTo("1000");
        assertThat(WireValues.int64("9007199254740993")).isEqualTo(9_007_199_254_740_993L);
        assertThat(WireValues.int64(42L)).isEqualTo("42");
        assertThat(WireValues.time("08:30")).isEqualTo(LocalTime.of(8, 30));
        assertThat(WireValues.time(LocalTime.of(8, 30, 15))).isEqualTo("08:30:15");
        assertThat(WireValues.decimal((String) null)).isNull();
        assertThat(WireValues.decimal((BigDecimal) null)).isNull();
        assertThat(WireValues.int64((String) null)).isNull();
        assertThat(WireValues.int64((Long) null)).isNull();
        assertThat(WireValues.time((String) null)).isNull();
        assertThat(WireValues.time((LocalTime) null)).isNull();
    }

    @Test
    void refusesWhatTheColumnWouldRoundOrReject() {
        List<FieldError> errors = new ArrayList<>();
        WireValues.checkDecimal("fee", "123.45", 5, 2, errors);
        WireValues.checkDecimal("fee", "123.4500", 5, 2, errors);
        WireValues.checkDecimal("fee", null, 5, 2, errors);
        assertThat(errors).isEmpty();
        WireValues.checkDecimal("scale", "1.234", 5, 2, errors);
        WireValues.checkDecimal("whole", "1234.5", 5, 2, errors);
        WireValues.checkDecimal("text", "abc", 5, 2, errors);
        assertThat(errors).extracting(FieldError::field).containsExactly("scale", "whole", "text");
        errors.clear();
        WireValues.checkInt64("count", "12", errors);
        WireValues.checkInt64("count", null, errors);
        WireValues.checkTime("at", "23:59:59", errors);
        WireValues.checkTime("at", null, errors);
        assertThat(errors).isEmpty();
        WireValues.checkDuration("d", "5400s", 60, errors);
        WireValues.checkDuration("d", "90s", 1, errors);
        WireValues.checkDuration("d", null, 60, errors);
        assertThat(errors).isEmpty();
        assertThat(WireValues.duration("5400s", 60)).isEqualTo(90);
        assertThat(WireValues.duration(90, 60)).isEqualTo("5400s");
        assertThat(WireValues.duration((String) null, 60)).isNull();
        assertThat(WireValues.duration((Integer) null, 60)).isNull();
        WireValues.checkDuration("partial", "90s", 60, errors);
        WireValues.checkDuration("unit", "90", 1, errors);
        WireValues.checkDuration("huge", "99999999999999999999s", 1, errors);
        assertThat(errors).extracting(FieldError::field).containsExactly("partial", "unit", "huge");
        errors.clear();
        WireValues.checkInt64("big", "99999999999999999999", errors);
        WireValues.checkTime("late", "24:00", errors);
        assertThat(errors).extracting(FieldError::field).containsExactly("big", "late");
    }
}
