package com.mysms.finance.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.finance.api.model.Coupon;
import com.mysms.finance.api.model.CouponScope;
import com.mysms.finance.api.model.CouponScopeUpdate;
import com.mysms.finance.api.model.CouponUpdate;
import com.mysms.finance.business.CouponService;
import com.mysms.finance.v1.CouponServiceGrpc;
import com.mysms.finance.v1.CreateCouponRequest;
import com.mysms.finance.v1.CreateCouponResponse;
import com.mysms.finance.v1.CreateCouponScopeRequest;
import com.mysms.finance.v1.CreateCouponScopeResponse;
import com.mysms.finance.v1.DeleteCouponRequest;
import com.mysms.finance.v1.DeleteCouponResponse;
import com.mysms.finance.v1.DeleteCouponScopeRequest;
import com.mysms.finance.v1.DeleteCouponScopeResponse;
import com.mysms.finance.v1.GetCouponRequest;
import com.mysms.finance.v1.GetCouponResponse;
import com.mysms.finance.v1.GetCouponScopeRequest;
import com.mysms.finance.v1.GetCouponScopeResponse;
import com.mysms.finance.v1.ListCouponScopesRequest;
import com.mysms.finance.v1.ListCouponScopesResponse;
import com.mysms.finance.v1.ListCouponsRequest;
import com.mysms.finance.v1.ListCouponsResponse;
import com.mysms.finance.v1.UpdateCouponRequest;
import com.mysms.finance.v1.UpdateCouponResponse;
import com.mysms.finance.v1.UpdateCouponScopeRequest;
import com.mysms.finance.v1.UpdateCouponScopeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of finance.CouponService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class CouponResolver extends CouponServiceGrpc.CouponServiceImplBase {

    private final CouponService service;
    private final ProtoBridge bridge;

    public CouponResolver(CouponService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listCoupons(ListCouponsRequest request, StreamObserver<ListCouponsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Coupon> rows = service.listCoupons(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCouponsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCoupon(GetCouponRequest request, StreamObserver<GetCouponResponse> out) {
        Coupon found = service.getCoupon(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCouponResponse.newBuilder(), "coupon", found).build());
    }

    @Override
    public void createCoupon(CreateCouponRequest request, StreamObserver<CreateCouponResponse> out) {
        Coupon created = service.createCoupon(
                bridge.body(request.getCoupon(), Coupon.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCouponResponse.newBuilder(), "coupon", created).build());
    }

    @Override
    public void updateCoupon(UpdateCouponRequest request, StreamObserver<UpdateCouponResponse> out) {
        Coupon updated = service.updateCoupon(
                bridge.id(request.getCoupon().getId(), "id"),
                bridge.requiredIfMatch(request.getCoupon().getEtag()),
                bridge.changes(request.getCoupon(), request.getUpdateMask(), CouponUpdate.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCouponResponse.newBuilder(), "coupon", updated).build());
    }

    @Override
    public void deleteCoupon(DeleteCouponRequest request, StreamObserver<DeleteCouponResponse> out) {
        service.deleteCoupon(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCouponResponse.getDefaultInstance());
    }

    @Override
    public void listCouponScopes(ListCouponScopesRequest request, StreamObserver<ListCouponScopesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<CouponScope> rows = service.listCouponScopes(bridge.id(request.getCouponId(), "couponId"),
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListCouponScopesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getCouponScope(GetCouponScopeRequest request, StreamObserver<GetCouponScopeResponse> out) {
        CouponScope found = service.getCouponScope(bridge.id(request.getCouponId(), "couponId"),
                bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetCouponScopeResponse.newBuilder(), "coupon_scope", found).build());
    }

    @Override
    public void createCouponScope(CreateCouponScopeRequest request, StreamObserver<CreateCouponScopeResponse> out) {
        CouponScope created = service.createCouponScope(bridge.id(request.getCouponId(), "couponId"),
                bridge.body(request.getCouponScope(), CouponScope.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateCouponScopeResponse.newBuilder(), "coupon_scope", created).build());
    }

    @Override
    public void updateCouponScope(UpdateCouponScopeRequest request, StreamObserver<UpdateCouponScopeResponse> out) {
        CouponScope updated = service.updateCouponScope(
                bridge.id(request.getCouponId(), "couponId"),
                bridge.id(request.getCouponScope().getId(), "id"),
                bridge.requiredIfMatch(request.getCouponScope().getEtag()),
                bridge.changes(request.getCouponScope(), request.getUpdateMask(), CouponScopeUpdate.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateCouponScopeResponse.newBuilder(), "coupon_scope", updated).build());
    }

    @Override
    public void deleteCouponScope(DeleteCouponScopeRequest request, StreamObserver<DeleteCouponScopeResponse> out) {
        service.deleteCouponScope(bridge.id(request.getCouponId(), "couponId"),
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteCouponScopeResponse.getDefaultInstance());
    }
}
