package com.mysms.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.LibraryCardApi;
import com.mysms.library.api.model.LibraryCard;
import com.mysms.library.api.model.LibraryCardUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.library.business.LibraryCardService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LibraryCard operations: implements the generated contract interface, so paths, parameters and
 * payloads
 * cannot drift from api/openapi/library.yaml (JAVA-CODING-RULES §9). Thin: delegate, then shape the HTTP response.
 * Authentication and the x-permission check already happened in the auth-client filter; operations not served yet
 * answer 501 NOT_IMPLEMENTED.
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
            LibraryCardUpdate libraryCardUpdate, String updateMask, Boolean validateOnly) {
        LibraryCard updated =
                libraryCardService.updateLibraryCard(libraryCardId, ifMatch, libraryCardUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
