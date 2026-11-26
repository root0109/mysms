package com.mysms.comms.grpc;

import com.mysms.comms.api.model.NotificationCohortSetting;
import com.mysms.comms.api.model.NotificationCohortSettingUpdate;
import com.mysms.comms.api.model.NotificationRule;
import com.mysms.comms.api.model.NotificationRuleUpdate;
import com.mysms.comms.api.model.NotificationType;
import com.mysms.comms.api.model.NotificationTypeProperty;
import com.mysms.comms.api.model.NotificationTypePropertyUpdate;
import com.mysms.comms.api.model.NotificationTypeUpdate;
import com.mysms.comms.business.NotificationTypeService;
import com.mysms.comms.v1.CreateNotificationCohortSettingRequest;
import com.mysms.comms.v1.CreateNotificationCohortSettingResponse;
import com.mysms.comms.v1.CreateNotificationRuleRequest;
import com.mysms.comms.v1.CreateNotificationRuleResponse;
import com.mysms.comms.v1.CreateNotificationTypePropertyRequest;
import com.mysms.comms.v1.CreateNotificationTypePropertyResponse;
import com.mysms.comms.v1.CreateNotificationTypeRequest;
import com.mysms.comms.v1.CreateNotificationTypeResponse;
import com.mysms.comms.v1.DeleteNotificationCohortSettingRequest;
import com.mysms.comms.v1.DeleteNotificationCohortSettingResponse;
import com.mysms.comms.v1.DeleteNotificationRuleRequest;
import com.mysms.comms.v1.DeleteNotificationRuleResponse;
import com.mysms.comms.v1.DeleteNotificationTypePropertyRequest;
import com.mysms.comms.v1.DeleteNotificationTypePropertyResponse;
import com.mysms.comms.v1.DeleteNotificationTypeRequest;
import com.mysms.comms.v1.DeleteNotificationTypeResponse;
import com.mysms.comms.v1.GetNotificationCohortSettingRequest;
import com.mysms.comms.v1.GetNotificationCohortSettingResponse;
import com.mysms.comms.v1.GetNotificationRuleRequest;
import com.mysms.comms.v1.GetNotificationRuleResponse;
import com.mysms.comms.v1.GetNotificationTypePropertyRequest;
import com.mysms.comms.v1.GetNotificationTypePropertyResponse;
import com.mysms.comms.v1.GetNotificationTypeRequest;
import com.mysms.comms.v1.GetNotificationTypeResponse;
import com.mysms.comms.v1.ListNotificationCohortSettingsRequest;
import com.mysms.comms.v1.ListNotificationCohortSettingsResponse;
import com.mysms.comms.v1.ListNotificationRulesRequest;
import com.mysms.comms.v1.ListNotificationRulesResponse;
import com.mysms.comms.v1.ListNotificationTypePropertiesRequest;
import com.mysms.comms.v1.ListNotificationTypePropertiesResponse;
import com.mysms.comms.v1.ListNotificationTypesRequest;
import com.mysms.comms.v1.ListNotificationTypesResponse;
import com.mysms.comms.v1.NotificationTypeServiceGrpc;
import com.mysms.comms.v1.UpdateNotificationCohortSettingRequest;
import com.mysms.comms.v1.UpdateNotificationCohortSettingResponse;
import com.mysms.comms.v1.UpdateNotificationRuleRequest;
import com.mysms.comms.v1.UpdateNotificationRuleResponse;
import com.mysms.comms.v1.UpdateNotificationTypePropertyRequest;
import com.mysms.comms.v1.UpdateNotificationTypePropertyResponse;
import com.mysms.comms.v1.UpdateNotificationTypeRequest;
import com.mysms.comms.v1.UpdateNotificationTypeResponse;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.NotificationTypeService: delegates every RPC to the same business service as REST.
 * Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class NotificationTypeResolver extends NotificationTypeServiceGrpc.NotificationTypeServiceImplBase {

    private final NotificationTypeService service;
    private final ProtoBridge bridge;

    public NotificationTypeResolver(NotificationTypeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listNotificationTypes(ListNotificationTypesRequest request, StreamObserver<ListNotificationTypesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NotificationType> rows = service.listNotificationTypes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNotificationTypesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotificationType(GetNotificationTypeRequest request, StreamObserver<GetNotificationTypeResponse> out) {
        NotificationType found = service.getNotificationType(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNotificationTypeResponse.newBuilder(), "notification_type", found).build());
    }

    @Override
    public void createNotificationType(CreateNotificationTypeRequest request,
            StreamObserver<CreateNotificationTypeResponse> out) {
        NotificationType created = service.createNotificationType(
                bridge.body(request.getNotificationType(), NotificationType.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateNotificationTypeResponse.newBuilder(), "notification_type", created).build());
    }

    @Override
    public void updateNotificationType(UpdateNotificationTypeRequest request,
            StreamObserver<UpdateNotificationTypeResponse> out) {
        NotificationType updated = service.updateNotificationType(
                bridge.id(request.getNotificationType().getId(), "id"),
                bridge.requiredIfMatch(request.getNotificationType().getEtag()),
                bridge.changes(request.getNotificationType(), request.getUpdateMask(), NotificationTypeUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateNotificationTypeResponse.newBuilder(), "notification_type", updated).build());
    }

    @Override
    public void deleteNotificationType(DeleteNotificationTypeRequest request,
            StreamObserver<DeleteNotificationTypeResponse> out) {
        service.deleteNotificationType(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNotificationTypeResponse.getDefaultInstance());
    }

    @Override
    public void listNotificationTypeProperties(ListNotificationTypePropertiesRequest request,
            StreamObserver<ListNotificationTypePropertiesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NotificationTypeProperty> rows = service.listNotificationTypePropertys(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNotificationTypePropertiesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotificationTypeProperty(GetNotificationTypePropertyRequest request,
            StreamObserver<GetNotificationTypePropertyResponse> out) {
        NotificationTypeProperty found = service.getNotificationTypeProperty(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetNotificationTypePropertyResponse.newBuilder(), "notification_type_property", found).build());
    }

    @Override
    public void createNotificationTypeProperty(CreateNotificationTypePropertyRequest request,
            StreamObserver<CreateNotificationTypePropertyResponse> out) {
        NotificationTypeProperty created = service.createNotificationTypeProperty(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"),
                bridge.body(request.getNotificationTypeProperty(), NotificationTypeProperty.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateNotificationTypePropertyResponse.newBuilder(), "notification_type_property", created).build());
    }

    @Override
    public void updateNotificationTypeProperty(UpdateNotificationTypePropertyRequest request,
            StreamObserver<UpdateNotificationTypePropertyResponse> out) {
        NotificationTypeProperty updated = service.updateNotificationTypeProperty(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"),
                bridge.id(request.getNotificationTypeProperty().getId(), "id"),
                bridge.requiredIfMatch(request.getNotificationTypeProperty().getEtag()),
                bridge.changes(request.getNotificationTypeProperty(), request.getUpdateMask(),
                NotificationTypePropertyUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateNotificationTypePropertyResponse.newBuilder(), "notification_type_property", updated).build());
    }

    @Override
    public void deleteNotificationTypeProperty(DeleteNotificationTypePropertyRequest request,
            StreamObserver<DeleteNotificationTypePropertyResponse> out) {
        service.deleteNotificationTypeProperty(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNotificationTypePropertyResponse.getDefaultInstance());
    }

    @Override
    public void listNotificationRules(ListNotificationRulesRequest request, StreamObserver<ListNotificationRulesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NotificationRule> rows = service.listNotificationRules(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNotificationRulesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotificationRule(GetNotificationRuleRequest request, StreamObserver<GetNotificationRuleResponse> out) {
        NotificationRule found = service.getNotificationRule(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetNotificationRuleResponse.newBuilder(), "notification_rule", found).build());
    }

    @Override
    public void createNotificationRule(CreateNotificationRuleRequest request,
            StreamObserver<CreateNotificationRuleResponse> out) {
        NotificationRule created = service.createNotificationRule(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"),
                bridge.body(request.getNotificationRule(), NotificationRule.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateNotificationRuleResponse.newBuilder(), "notification_rule", created).build());
    }

    @Override
    public void updateNotificationRule(UpdateNotificationRuleRequest request,
            StreamObserver<UpdateNotificationRuleResponse> out) {
        NotificationRule updated = service.updateNotificationRule(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"),
                bridge.id(request.getNotificationRule().getId(), "id"),
                bridge.requiredIfMatch(request.getNotificationRule().getEtag()),
                bridge.changes(request.getNotificationRule(), request.getUpdateMask(), NotificationRuleUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateNotificationRuleResponse.newBuilder(), "notification_rule", updated).build());
    }

    @Override
    public void deleteNotificationRule(DeleteNotificationRuleRequest request,
            StreamObserver<DeleteNotificationRuleResponse> out) {
        service.deleteNotificationRule(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNotificationRuleResponse.getDefaultInstance());
    }

    @Override
    public void listNotificationCohortSettings(ListNotificationCohortSettingsRequest request,
            StreamObserver<ListNotificationCohortSettingsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<NotificationCohortSetting> rows = service.listNotificationCohortSettings(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListNotificationCohortSettingsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getNotificationCohortSetting(GetNotificationCohortSettingRequest request,
            StreamObserver<GetNotificationCohortSettingResponse> out) {
        NotificationCohortSetting found = service.getNotificationCohortSetting(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out,
                bridge.reply(GetNotificationCohortSettingResponse.newBuilder(), "notification_cohort_setting", found).build());
    }

    @Override
    public void createNotificationCohortSetting(CreateNotificationCohortSettingRequest request,
            StreamObserver<CreateNotificationCohortSettingResponse> out) {
        NotificationCohortSetting created = service.createNotificationCohortSetting(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"),
                bridge.body(request.getNotificationCohortSetting(), NotificationCohortSetting.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(CreateNotificationCohortSettingResponse.newBuilder(), "notification_cohort_setting",
                created).build());
    }

    @Override
    public void updateNotificationCohortSetting(UpdateNotificationCohortSettingRequest request,
            StreamObserver<UpdateNotificationCohortSettingResponse> out) {
        NotificationCohortSetting updated = service.updateNotificationCohortSetting(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"),
                bridge.id(request.getNotificationCohortSetting().getId(), "id"),
                bridge.requiredIfMatch(request.getNotificationCohortSetting().getEtag()),
                bridge.changes(request.getNotificationCohortSetting(), request.getUpdateMask(),
                NotificationCohortSettingUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out,
                bridge.reply(UpdateNotificationCohortSettingResponse.newBuilder(), "notification_cohort_setting",
                updated).build());
    }

    @Override
    public void deleteNotificationCohortSetting(DeleteNotificationCohortSettingRequest request,
            StreamObserver<DeleteNotificationCohortSettingResponse> out) {
        service.deleteNotificationCohortSetting(
                bridge.id(request.getNotificationTypeId(), "notificationTypeId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteNotificationCohortSettingResponse.getDefaultInstance());
    }
}
