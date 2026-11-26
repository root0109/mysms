package com.mysms.finance.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.finance.api.CouponApi;
import com.mysms.finance.api.model.Coupon;
import com.mysms.finance.api.model.CouponScope;
import com.mysms.finance.api.model.CouponScopeUpdate;
import com.mysms.finance.api.model.CouponUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.finance.business.CouponService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Coupon operations; authentication and the permission check already happened in the auth-client
 * filter; operations not served yet answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class CouponController implements CouponApi {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @Override
    public ResponseEntity<Coupon> createCoupon(Coupon coupon, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Coupon created = couponService.createCoupon(coupon, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/finance/coupons/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<CouponScope> createCouponScope(UUID couponId, CouponScope couponScope,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        CouponScope created = couponService.createCouponScope(couponId, couponScope, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/finance/coupons/" + couponId + "/scopes/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteCoupon(UUID couponId, String ifMatch) {
        couponService.deleteCoupon(couponId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteCouponScope(UUID couponId, UUID couponScopeId, String ifMatch) {
        couponService.deleteCouponScope(couponId, couponScopeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Coupon> getCoupon(UUID couponId, String fields) {
        Coupon found = couponService.getCoupon(couponId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<CouponScope> getCouponScope(UUID couponId, UUID couponScopeId, String fields) {
        CouponScope found = couponService.getCouponScope(couponId, couponScopeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<CouponScope>> listCouponScopes(UUID couponId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<CouponScope> list =
                PagingUtil.pagingInfo(couponService.listCouponScopes(couponId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<Coupon>> listCoupons(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Coupon> list = PagingUtil.pagingInfo(couponService.listCoupons(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Coupon> updateCoupon(UUID couponId, String ifMatch, CouponUpdate couponUpdate,
            Boolean validateOnly) {
        Coupon updated = couponService.updateCoupon(couponId, ifMatch, couponUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<CouponScope> updateCouponScope(UUID couponId, UUID couponScopeId,
            String ifMatch, CouponScopeUpdate couponScopeUpdate, Boolean validateOnly) {
        CouponScope updated =
                couponService.updateCouponScope(couponId, couponScopeId, ifMatch, couponScopeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
