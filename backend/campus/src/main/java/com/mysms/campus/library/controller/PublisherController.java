package com.mysms.campus.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.campus.library.api.PublisherApi;
import com.mysms.campus.library.api.model.Publisher;
import com.mysms.campus.library.api.model.PublisherUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.campus.library.business.PublisherService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Publisher operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
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
            PublisherUpdate publisherUpdate, Boolean validateOnly) {
        Publisher updated = publisherService.updatePublisher(publisherId, ifMatch, publisherUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
