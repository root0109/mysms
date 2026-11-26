package com.mysms.campus.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.campus.library.api.LibraryCardApi;
import com.mysms.campus.library.api.model.LibraryCard;
import com.mysms.campus.library.api.model.LibraryCardUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.campus.library.business.LibraryCardService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LibraryCard operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class LibraryCardController implements LibraryCardApi {

    private final LibraryCardService libraryCardService;

    public LibraryCardController(LibraryCardService libraryCardService) {
        this.libraryCardService = libraryCardService;
    }

    @Override
    public ResponseEntity<LibraryCard> createLibraryCard(LibraryCard libraryCard, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LibraryCard created = libraryCardService.createLibraryCard(libraryCard, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/library-cards/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLibraryCard(UUID libraryCardId, String ifMatch) {
        libraryCardService.deleteLibraryCard(libraryCardId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LibraryCard> getLibraryCard(UUID libraryCardId, String fields) {
        LibraryCard found = libraryCardService.getLibraryCard(libraryCardId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LibraryCard>> listLibraryCards(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LibraryCard> list =
                PagingUtil.pagingInfo(libraryCardService.listLibraryCards(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LibraryCard> updateLibraryCard(UUID libraryCardId, String ifMatch,
            LibraryCardUpdate libraryCardUpdate, Boolean validateOnly) {
        LibraryCard updated =
                libraryCardService.updateLibraryCard(libraryCardId, ifMatch, libraryCardUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
