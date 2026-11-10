package com.mysms.iam.mapper;

import com.mysms.iam.api.model.Person;
import org.mapstruct.Mapper;

/** The people service's Person (SDK) → the Person embedded in iam's Me (same contract schema, generated twice). */
@Mapper(config = IamMapperConfig.class)
public interface PersonMapper {

    Person toIam(com.mysms.sdk.people.model.Person person);
}
