package com.mysms.privacy.mapper;

import com.mysms.core.web.Etags;
import com.mysms.privacy.api.model.MyRightsRequest;
import com.mysms.privacy.api.model.RightsRequest;
import com.mysms.privacy.api.model.RightsRequestCorrection;
import com.mysms.privacy.api.model.RightsRequestCorrectionField;
import com.mysms.privacy.api.model.RightsRequestTask;
import com.mysms.privacy.domain.RightsRequestEntity;
import com.mysms.privacy.domain.RightsRequestTaskEntity;
import com.mysms.privacy.model.RightsRequestDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps privacy.rights_request, with its modules' tasks, to the staff's and the requester's contract DTOs. There is no
 * way in: a request is built by the rights-request service from a submission, never from a client's copy. Whether a
 * request is overdue depends on the time of the read, and its tasks on its type, which the service sets.
 */
@Mapper(config = PrivacyMapperConfig.class, imports = {Etags.class})
public interface RightsRequestMapper {

    @Mapping(target = "type", expression = "java(RightsRequest.TypeEnum.fromValue(entity.getType()))")
    @Mapping(target = "requesterCapacity",
            expression = "java(RightsRequest.RequesterCapacityEnum.fromValue(entity.getRequesterCapacity()))")
    @Mapping(target = "status", expression = "java(RightsRequest.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "identityCheck", expression = "java(entity.getIdentityCheck() == null ? null"
            + " : RightsRequest.IdentityCheckEnum.fromValue(entity.getIdentityCheck()))")
    @Mapping(target = "description", source = "details.description")
    @Mapping(target = "overdue", ignore = true)
    @Mapping(target = "outcomeCode", expression = "java(entity.getOutcomeCode() == null ? null"
            + " : RightsRequest.OutcomeCodeEnum.fromValue(entity.getOutcomeCode()))")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    @Mapping(target = "correction", source = "details.correction")
    @Mapping(target = "grievanceCategory", expression = "java(entity.getDetails().grievanceCategory() == null ? null"
            + " : RightsRequest.GrievanceCategoryEnum.fromValue(entity.getDetails().grievanceCategory()))")
    @Mapping(target = "response", source = "outcomeResponse")
    @Mapping(target = "tasks", ignore = true)
    RightsRequest toDto(RightsRequestEntity entity);

    @Mapping(target = "type", expression = "java(MyRightsRequest.TypeEnum.fromValue(entity.getType()))")
    @Mapping(target = "requesterCapacity",
            expression = "java(MyRightsRequest.RequesterCapacityEnum.fromValue(entity.getRequesterCapacity()))")
    @Mapping(target = "status", expression = "java(MyRightsRequest.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "description", source = "details.description")
    @Mapping(target = "overdue", ignore = true)
    @Mapping(target = "outcomeCode", expression = "java(entity.getOutcomeCode() == null ? null"
            + " : MyRightsRequest.OutcomeCodeEnum.fromValue(entity.getOutcomeCode()))")
    @Mapping(target = "correction", source = "details.correction")
    @Mapping(target = "grievanceCategory", expression = "java(entity.getDetails().grievanceCategory() == null ? null"
            + " : MyRightsRequest.GrievanceCategoryEnum.fromValue(entity.getDetails().grievanceCategory()))")
    @Mapping(target = "response", source = "outcomeResponse")
    MyRightsRequest toMine(RightsRequestEntity entity);

    /** The record a correction names (details.correction) as the contract shows it. */
    RightsRequestCorrection toDto(RightsRequestDetails.Correction correction);

    RightsRequestCorrectionField toDto(RightsRequestDetails.Field field);

    /** A module's task of an erasure or export request, with its answer in counts. */
    @Mapping(target = "rightsRequestId", source = "requestId")
    @Mapping(target = "action", expression = "java(RightsRequestTask.ActionEnum.fromValue(entity.getAction()))")
    @Mapping(target = "status", expression = "java(RightsRequestTask.StatusEnum.fromValue(entity.getStatus()))")
    @Mapping(target = "affectedRows",
            expression = "java(entity.getResult() == null ? null : entity.getResult().affectedRows())")
    @Mapping(target = "heldItems", source = "result.heldItems")
    @Mapping(target = "policyCodes", source = "result.policyCodes")
    @Mapping(target = "fileId", source = "result.fileId")
    @Mapping(target = "etag", expression = "java(Etags.of(entity.getRowVersion()))")
    RightsRequestTask toDto(RightsRequestTaskEntity entity);
}
