package com.mysms.privacy.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.privacy.api.model.ConsentRecord;
import com.mysms.privacy.api.model.MyConsent;
import com.mysms.privacy.api.model.MyNotice;
import com.mysms.privacy.business.ConsentService;
import com.mysms.privacy.business.NoticeService;
import com.mysms.privacy.model.RequestOrigin;
import com.mysms.privacy.v1.ConsentServiceGrpc;
import com.mysms.privacy.v1.GetConsentRecordRequest;
import com.mysms.privacy.v1.GetConsentRecordResponse;
import com.mysms.privacy.v1.GetMyNoticeRequest;
import com.mysms.privacy.v1.GetMyNoticeResponse;
import com.mysms.privacy.v1.ListConsentRecordsRequest;
import com.mysms.privacy.v1.ListConsentRecordsResponse;
import com.mysms.privacy.v1.ListMyConsentsRequest;
import com.mysms.privacy.v1.ListMyConsentsResponse;
import com.mysms.privacy.v1.RecordConsentOnBehalfRequest;
import com.mysms.privacy.v1.RecordConsentOnBehalfResponse;
import com.mysms.privacy.v1.RecordMyConsentRequest;
import com.mysms.privacy.v1.RecordMyConsentResponse;
import com.mysms.privacy.v1.VerifyConsentRecordRequest;
import com.mysms.privacy.v1.VerifyConsentRecordResponse;
import com.mysms.privacy.v1.WithdrawMyConsentRequest;
import com.mysms.privacy.v1.WithdrawMyConsentResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of privacy.ConsentService: delegates every RPC to the same business services as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge. A gRPC call carries no client of its own, so its
 * consent evidence records no address or User-Agent.
 */
@GrpcService
public class ConsentResolver extends ConsentServiceGrpc.ConsentServiceImplBase {

    private static final String MY_CONSENT = "my_consent";
    private static final String CONSENT_RECORD = "consent_record";

    private final ConsentService service;
    private final NoticeService noticeService;
    private final ProtoBridge bridge;

    public ConsentResolver(ConsentService service, NoticeService noticeService, ProtoBridge bridge) {
        this.service = service;
        this.noticeService = noticeService;
        this.bridge = bridge;
    }

    @Override
    public void listMyConsents(ListMyConsentsRequest request, StreamObserver<ListMyConsentsResponse> out) {
        Page<MyConsent> rows = service.listMyConsents();
        ProtoBridge.send(out, bridge.page(ListMyConsentsResponse.newBuilder(), rows).build());
    }

    /** The REST body is the request without its idempotency key. */
    @Override
    public void recordMyConsent(RecordMyConsentRequest request, StreamObserver<RecordMyConsentResponse> out) {
        MyConsent recorded = service.recordMyConsent(bridge.body(request,
                com.mysms.privacy.api.model.RecordMyConsentRequest.class, "request_id"), RequestOrigin.UNKNOWN);
        ProtoBridge.send(out, bridge.reply(RecordMyConsentResponse.newBuilder(), MY_CONSENT, recorded).build());
    }

    /** The REST body (the workflow etag) is the request without its path id and idempotency key. */
    @Override
    public void withdrawMyConsent(WithdrawMyConsentRequest request, StreamObserver<WithdrawMyConsentResponse> out) {
        MyConsent withdrawn = service.withdrawMyConsent(bridge.id(request.getConsentRecordId(), "consentRecordId"),
                bridge.body(request, com.mysms.privacy.api.model.WithdrawMyConsentRequest.class, "consent_record_id",
                        "request_id"), RequestOrigin.UNKNOWN);
        ProtoBridge.send(out, bridge.reply(WithdrawMyConsentResponse.newBuilder(), MY_CONSENT, withdrawn).build());
    }

    @Override
    public void getMyNotice(GetMyNoticeRequest request, StreamObserver<GetMyNoticeResponse> out) {
        MyNotice found = noticeService.getMyNotice(request.getNoticeCode(), request.getLanguage());
        ProtoBridge.send(out, bridge.reply(GetMyNoticeResponse.newBuilder(), "my_notice", found).build());
    }

    @Override
    public void listConsentRecords(ListConsentRecordsRequest request, StreamObserver<ListConsentRecordsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<ConsentRecord> rows = service.listConsentRecords(page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListConsentRecordsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getConsentRecord(GetConsentRecordRequest request, StreamObserver<GetConsentRecordResponse> out) {
        ConsentRecord found = service.getConsentRecord(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetConsentRecordResponse.newBuilder(), CONSENT_RECORD, found).build());
    }

    /** The REST body is the request without its idempotency key. */
    @Override
    public void recordConsentOnBehalf(RecordConsentOnBehalfRequest request,
            StreamObserver<RecordConsentOnBehalfResponse> out) {
        ConsentRecord recorded = service.recordConsentOnBehalf(bridge.body(request,
                com.mysms.privacy.api.model.RecordConsentOnBehalfRequest.class, "request_id"), RequestOrigin.UNKNOWN);
        ProtoBridge.send(out, bridge.reply(RecordConsentOnBehalfResponse.newBuilder(), CONSENT_RECORD, recorded)
                .build());
    }

    /** The REST body (the workflow etag) is the request without its path id and idempotency key. */
    @Override
    public void verifyConsentRecord(VerifyConsentRecordRequest request,
            StreamObserver<VerifyConsentRecordResponse> out) {
        ConsentRecord verified = service.verifyConsentRecord(bridge.id(request.getConsentRecordId(), "consentRecordId"),
                bridge.body(request, com.mysms.privacy.api.model.VerifyConsentRecordRequest.class,
                        "consent_record_id", "request_id"), RequestOrigin.UNKNOWN);
        ProtoBridge.send(out, bridge.reply(VerifyConsentRecordResponse.newBuilder(), CONSENT_RECORD, verified)
                .build());
    }
}
