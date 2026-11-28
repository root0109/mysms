package com.mysms.identity.iam.mapper;

import com.mysms.identity.iam.api.model.Person;
import org.mapstruct.Mapper;

/**
 * The caller's own person as iam.GetMe embeds it: people.GetMyPerson's answer, read in process from the sibling people
 * area, copied field by field into iam's contract (the two contracts carry the same fields).
 */
@Mapper(config = IamMapperConfig.class)
public interface MePersonMapper {

    Person toMe(com.mysms.identity.people.api.model.Person person);
}
