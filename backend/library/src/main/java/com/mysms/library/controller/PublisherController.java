package com.mysms.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.PublisherApi;
import com.mysms.library.api.model.Publisher;
import com.mysms.library.api.model.PublisherUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.library.business.PublisherService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Publisher operations: implements the generated contract interface, so paths, parameters and payloads
 * cannot drift from api/openapi/library.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
 */
@RestController
public class PublisherController implements PublisherApi {

    private final PublisherService publisherService;

    public PublisherController(PublisherService publisherService) {
        this.publisherService = publisherService;
    }

    @Override
    public ResponseEntity<Publisher> createPublisher(Publisher publisher, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Publisher created = publisherService.createPublisher(publisher, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/publishers/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deletePublisher(UUID publisherId, String ifMatch) {
        publisherService.deletePublisher(publisherId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Publisher> getPublisher(UUID publisherId, String fields) {
        Publisher found = publisherService.getPublisher(publisherId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Publisher>> listPublishers(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<Publisher> list =
                PagingUtil.pagingInfo(publisherService.listPublishers(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Publisher> updatePublisher(UUID publisherId, String ifMatch,
            PublisherUpdate publisherUpdate, String updateMask, Boolean validateOnly) {
        Publisher updated = publisherService.updatePublisher(publisherId, ifMatch, publisherUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
