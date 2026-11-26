package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.BookmarkApi;
import com.mysms.learning.lms.api.model.Bookmark;
import com.mysms.learning.lms.api.model.BookmarkUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.BookmarkService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Bookmark operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class BookmarkController implements BookmarkApi {

    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @Override
    public ResponseEntity<Bookmark> createBookmark(Bookmark bookmark, UUID idempotencyKey,
            Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Bookmark created = bookmarkService.createBookmark(bookmark, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/bookmarks/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteBookmark(UUID bookmarkId, String ifMatch) {
        bookmarkService.deleteBookmark(bookmarkId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Bookmark> getBookmark(UUID bookmarkId, String fields) {
        Bookmark found = bookmarkService.getBookmark(bookmarkId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Bookmark>> listBookmarks(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Bookmark> list =
                PagingUtil.pagingInfo(bookmarkService.listBookmarks(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Bookmark> updateBookmark(UUID bookmarkId, String ifMatch,
            BookmarkUpdate bookmarkUpdate, Boolean validateOnly) {
        Bookmark updated = bookmarkService.updateBookmark(bookmarkId, ifMatch, bookmarkUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
