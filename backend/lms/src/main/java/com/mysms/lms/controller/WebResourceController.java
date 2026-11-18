package com.mysms.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.lms.api.WebResourceApi;
import com.mysms.lms.api.model.WebResource;
import com.mysms.lms.api.model.WebResourceUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.lms.business.WebResourceService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the WebResource operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class WebResourceController implements WebResourceApi {

    private final WebResourceService webResourceService;

    public WebResourceController(WebResourceService webResourceService) {
        this.webResourceService = webResourceService;
    }

    @Override
    public ResponseEntity<WebResource> createWebResource(WebResource webResource, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        WebResource created = webResourceService.createWebResource(webResource, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/web-resources/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteWebResource(UUID webResourceId, String ifMatch) {
        webResourceService.deleteWebResource(webResourceId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<WebResource> getWebResource(UUID webResourceId, String fields) {
        WebResource found = webResourceService.getWebResource(webResourceId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<WebResource>> listWebResources(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<WebResource> list =
                PagingUtil.pagingInfo(webResourceService.listWebResources(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<WebResource> updateWebResource(UUID webResourceId, String ifMatch,
            WebResourceUpdate webResourceUpdate, String updateMask, Boolean validateOnly) {
        WebResource updated =
                webResourceService.updateWebResource(webResourceId, ifMatch, webResourceUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
