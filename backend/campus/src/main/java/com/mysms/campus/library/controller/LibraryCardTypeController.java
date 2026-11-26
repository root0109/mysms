package com.mysms.campus.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.campus.library.api.LibraryCardTypeApi;
import com.mysms.campus.library.api.model.LibraryCardType;
import com.mysms.campus.library.api.model.LibraryCardTypeUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.campus.library.business.LibraryCardTypeService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the LibraryCardType operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class LibraryCardTypeController implements LibraryCardTypeApi {

    private final LibraryCardTypeService libraryCardTypeService;

    public LibraryCardTypeController(LibraryCardTypeService libraryCardTypeService) {
        this.libraryCardTypeService = libraryCardTypeService;
    }

    @Override
    public ResponseEntity<LibraryCardType> createLibraryCardType(LibraryCardType libraryCardType,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        LibraryCardType created = libraryCardTypeService.createLibraryCardType(libraryCardType, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/library-card-types/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteLibraryCardType(UUID libraryCardTypeId, String ifMatch) {
        libraryCardTypeService.deleteLibraryCardType(libraryCardTypeId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LibraryCardType> getLibraryCardType(UUID libraryCardTypeId, String fields) {
        LibraryCardType found = libraryCardTypeService.getLibraryCardType(libraryCardTypeId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<LibraryCardType>> listLibraryCardTypes(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<LibraryCardType> list =
                PagingUtil.pagingInfo(libraryCardTypeService.listLibraryCardTypes(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<LibraryCardType> updateLibraryCardType(UUID libraryCardTypeId, String ifMatch,
            LibraryCardTypeUpdate libraryCardTypeUpdate, Boolean validateOnly) {
        LibraryCardType updated =
                libraryCardTypeService.updateLibraryCardType(libraryCardTypeId, ifMatch, libraryCardTypeUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
