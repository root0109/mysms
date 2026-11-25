package com.mysms.privacy.mapper;

import com.mysms.privacy.api.model.ProcessingActivity;
import com.mysms.privacy.repository.ProcessingActivityView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = PrivacyMapperConfig.class)
public interface ProcessingActivityMapper {

    @Mapping(target = "legalBasis",
            expression = "java(ProcessingActivity.LegalBasisEnum.fromValue(view.legalBasis()))")
    ProcessingActivity toDto(ProcessingActivityView view);
}
