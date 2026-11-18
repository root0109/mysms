package com.mysms.lms.controller;

import com.mysms.lms.business.EbookConversionService;
import com.mysms.lms.business.EbookWorkflowService;
import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.EbookApi;
import com.mysms.lms.api.model.Ebook;
import com.mysms.lms.api.model.EbookUpdate;
import com.mysms.lms.api.model.PublishEbookCurationRequest;
import com.mysms.lms.api.model.RelatedMaterial;
import com.mysms.lms.api.model.RelatedMaterialList;
import com.mysms.lms.api.model.RelatedMaterialRank;
import com.mysms.lms.api.model.RelatedMaterialUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.EbookService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Ebook operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class EbookController implements EbookApi {

    private final EbookService ebookService;
    private final EbookWorkflowService ebookWorkflowService;
    private final EbookConversionService ebookConversionService;

    public EbookController(EbookService ebookService, EbookWorkflowService ebookWorkflowService,
            EbookConversionService ebookConversionService) {
        this.ebookWorkflowService = ebookWorkflowService;
        this.ebookService = ebookService;
        this.ebookConversionService = ebookConversionService;
    }

    @Override
    public ResponseEntity<Ebook> convertEbook(UUID ebookId, Object body, UUID idempotencyKey) {
        Ebook converted = ebookConversionService.convertEbook(ebookId);
        return ResponseEntity.ok().eTag(converted.getEtag()).body(converted);
    }

    @Override
    public ResponseEntity<Ebook> createEbook(Ebook ebook, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Ebook created = ebookService.createEbook(ebook, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/ebooks/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<RelatedMaterial> createRelatedMaterial(UUID ebookId,
            RelatedMaterial relatedMaterial, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        RelatedMaterial created = ebookService.createRelatedMaterial(ebookId, relatedMaterial, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/ebooks/" + ebookId + "/related-materials/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteEbook(UUID ebookId, String ifMatch) {
        ebookService.deleteEbook(ebookId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteRelatedMaterial(UUID ebookId, UUID relatedMaterialId,
            String ifMatch) {
        ebookService.deleteRelatedMaterial(ebookId, relatedMaterialId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Ebook> getEbook(UUID ebookId, String fields) {
        Ebook found = ebookService.getEbook(ebookId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<RelatedMaterial>> getMyRelatedMaterials(UUID ebookId, UUID courseId, UUID fileId,
            Integer page, Integer size, String cursor) {
        return ResponseEntity.ok(PagingUtil.pagingInfo(ebookWorkflowService.getMyRelatedMaterials(ebookId, courseId,
                fileId, page, size, cursor)));
    }

    @Override
    public ResponseEntity<RelatedMaterial> getRelatedMaterial(UUID ebookId, UUID relatedMaterialId,
            String fields) {
        RelatedMaterial found = ebookService.getRelatedMaterial(ebookId, relatedMaterialId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<RelatedMaterialRank> getRelatedMaterialRank(UUID ebookId,
            UUID relatedMaterialRankId, String fields) {
        RelatedMaterialRank found = ebookWorkflowService.getRelatedMaterialRank(ebookId, relatedMaterialRankId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Ebook>> listEbooks(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Ebook> list = PagingUtil.pagingInfo(ebookService.listEbooks(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<RelatedMaterialRank>> listRelatedMaterialRanks(UUID ebookId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<RelatedMaterialRank> list = PagingUtil.pagingInfo(ebookWorkflowService.listRelatedMaterialRanks(
                ebookId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<RelatedMaterial>> listRelatedMaterials(UUID ebookId, Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<RelatedMaterial> list =
                PagingUtil.pagingInfo(ebookService.listRelatedMaterials(ebookId, page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<RelatedMaterialList> publishEbookCuration(UUID ebookId,
            PublishEbookCurationRequest publishEbookCurationRequest, UUID idempotencyKey) {
        return ResponseEntity.ok(ebookWorkflowService.publishEbookCuration(ebookId, publishEbookCurationRequest));
    }

    @Override
    public ResponseEntity<Void> receiveEbookConversion(String provider, String xSignature,
            String xTimestamp, byte[] body) {
        ebookConversionService.receiveEbookConversion(provider, xSignature, xTimestamp, body);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Ebook> updateEbook(UUID ebookId, String ifMatch, EbookUpdate ebookUpdate,
            String updateMask, Boolean validateOnly) {
        Ebook updated = ebookService.updateEbook(ebookId, ifMatch, ebookUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<RelatedMaterial> updateRelatedMaterial(UUID ebookId, UUID relatedMaterialId,
            String ifMatch, RelatedMaterialUpdate relatedMaterialUpdate, String updateMask, Boolean validateOnly) {
        RelatedMaterial updated =
                ebookService.updateRelatedMaterial(ebookId, relatedMaterialId, ifMatch, relatedMaterialUpdate,
                        updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
