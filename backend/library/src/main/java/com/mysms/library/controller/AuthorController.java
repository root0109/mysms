package com.mysms.library.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.library.api.AuthorApi;
import com.mysms.library.api.model.Author;
import com.mysms.library.api.model.AuthorUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.library.business.AuthorService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the Author operations; authentication and the permission check already ran in the auth-client filter.
 * Operations not served yet answer 501.
 */
@RestController
public class AuthorController implements AuthorApi {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @Override
    public ResponseEntity<Author> createAuthor(Author author, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        Author created = authorService.createAuthor(author, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/library/authors/" + created.getId())).eTag(created.getEtag())
                .body(created);
    }

    @Override
    public ResponseEntity<Void> deleteAuthor(UUID authorId, String ifMatch) {
        authorService.deleteAuthor(authorId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Author> getAuthor(UUID authorId, String fields) {
        Author found = authorService.getAuthor(authorId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<Author>> listAuthors(Integer page, Integer size, String cursor, String filter,
            List<String> sort, String fields) {
        PagingInfo<Author> list = PagingUtil.pagingInfo(authorService.listAuthors(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Author> updateAuthor(UUID authorId, String ifMatch, AuthorUpdate authorUpdate,
            String updateMask, Boolean validateOnly) {
        Author updated = authorService.updateAuthor(authorId, ifMatch, authorUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
