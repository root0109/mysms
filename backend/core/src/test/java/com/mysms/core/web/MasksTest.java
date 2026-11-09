package com.mysms.core.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mysms.core.context.Actor;
import com.mysms.core.context.CallContext;
import com.mysms.core.context.CallContextHolder;
import com.mysms.core.error.InvalidArgumentException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MasksTest {

    /** Shaped like a generated DTO: bean properties with setters. */
    public static final class Dto {
        private String name;
        private String code;
        private Integer sortOrder;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Integer getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(Integer sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    private static Dto dto() {
        Dto dto = new Dto();
        dto.setName("India");
        dto.setCode("IN");
        return dto;
    }

    @Test
    void readMaskKeepsOnlyListedFields() {
        assertThat(ReadMask.apply(dto(), null).getCode()).isEqualTo("IN");
        Dto masked = ReadMask.apply(dto(), "name, sortOrder");
        assertThat(masked.getName()).isEqualTo("India");
        assertThat(masked.getCode()).isNull();
        assertThatThrownBy(() -> ReadMask.apply(dto(), "secret")).isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void updateMaskSelectsFieldsToApply() {
        assertThat(UpdateMask.fields(null, dto(), "name", "code", "sortOrder")).containsExactly("name", "code");
        assertThat(UpdateMask.fields("sortOrder,name", dto(), "name", "code", "sortOrder")).containsExactly("name", "sortOrder");
        assertThatThrownBy(() -> UpdateMask.fields("id", dto(), "name")).isInstanceOf(InvalidArgumentException.class);
    }

    @Test
    void actorIsTheCaller() {
        assertThat(Actor.current()).isEmpty();
        UUID user = UUID.randomUUID();
        CallContextHolder.set(CallContext.anonymous("t").withIdentity(null, user));
        try {
            assertThat(Actor.current()).contains(user);
        } finally {
            CallContextHolder.clear();
        }
    }
}
