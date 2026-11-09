package com.mysms.iam.domain;

import com.mysms.core.entity.BaseEntity;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/** A row of iam.setting_definition: the product's setting catalogue (global, maintained by migrations). */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class SettingDefinitionEntity extends BaseEntity {

    public static final String TABLE = "iam.setting_definition";

    private String settingKey;
    private String category;
    private String valueType;
    private List<String> allowedValues;
    private Integer minValue;
    private Integer maxValue;
    private String defaultValue;
    private List<String> allowedScopes;
    private boolean userEditable;
    private String description;
    private int sortOrder;

    public List<String> getAllowedValues() {
        return allowedValues == null ? null : List.copyOf(allowedValues);
    }

    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues == null ? null : List.copyOf(allowedValues);
    }

    public List<String> getAllowedScopes() {
        return allowedScopes == null ? List.of() : List.copyOf(allowedScopes);
    }

    public void setAllowedScopes(List<String> allowedScopes) {
        this.allowedScopes = allowedScopes == null ? List.of() : List.copyOf(allowedScopes);
    }
}
