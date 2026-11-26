package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.PlagiarismAccountApi;
import com.mysms.learning.lms.api.model.PlagiarismAccount;
import com.mysms.learning.lms.api.model.PlagiarismAccountUpdate;
import com.mysms.learning.lms.api.model.PlagiarismUserLink;
import com.mysms.learning.lms.api.model.PlagiarismUserLinkUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.PlagiarismAccountService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the PlagiarismAccount operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class PlagiarismAccountController implements PlagiarismAccountApi {

    private final PlagiarismAccountService plagiarismAccountService;

    public PlagiarismAccountController(PlagiarismAccountService plagiarismAccountService) {
        this.plagiarismAccountService = plagiarismAccountService;
    }

    @Override
    public ResponseEntity<PlagiarismAccount> createPlagiarismAccount(PlagiarismAccount plagiarismAccount,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PlagiarismAccount created = plagiarismAccountService.createPlagiarismAccount(plagiarismAccount, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/plagiarism-accounts/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<PlagiarismUserLink> createPlagiarismUserLink(UUID plagiarismAccountId,
            PlagiarismUserLink plagiarismUserLink, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        PlagiarismUserLink created =
                plagiarismAccountService.createPlagiarismUserLink(plagiarismAccountId, plagiarismUserLink, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/lms/plagiarism-accounts/" + plagiarismAccountId + "/user-links/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deletePlagiarismAccount(UUID plagiarismAccountId, String ifMatch) {
        plagiarismAccountService.deletePlagiarismAccount(plagiarismAccountId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deletePlagiarismUserLink(UUID plagiarismAccountId,
            UUID plagiarismUserLinkId, String ifMatch) {
        plagiarismAccountService.deletePlagiarismUserLink(plagiarismAccountId, plagiarismUserLinkId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<PlagiarismAccount> getPlagiarismAccount(UUID plagiarismAccountId,
            String fields) {
        PlagiarismAccount found = plagiarismAccountService.getPlagiarismAccount(plagiarismAccountId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PlagiarismUserLink> getPlagiarismUserLink(UUID plagiarismAccountId,
            UUID plagiarismUserLinkId, String fields) {
        PlagiarismUserLink found =
                plagiarismAccountService.getPlagiarismUserLink(plagiarismAccountId, plagiarismUserLinkId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<PlagiarismAccount>> listPlagiarismAccounts(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PlagiarismAccount> list =
                PagingUtil.pagingInfo(plagiarismAccountService.listPlagiarismAccounts(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<PlagiarismUserLink>> listPlagiarismUserLinks(UUID plagiarismAccountId,
            Integer page, Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<PlagiarismUserLink> list =
                PagingUtil.pagingInfo(plagiarismAccountService.listPlagiarismUserLinks(plagiarismAccountId, page, size,
                        cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PlagiarismAccount> updatePlagiarismAccount(UUID plagiarismAccountId,
            String ifMatch, PlagiarismAccountUpdate plagiarismAccountUpdate, Boolean validateOnly) {
        PlagiarismAccount updated =
                plagiarismAccountService.updatePlagiarismAccount(plagiarismAccountId, ifMatch, plagiarismAccountUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<PlagiarismUserLink> updatePlagiarismUserLink(UUID plagiarismAccountId,
            UUID plagiarismUserLinkId, String ifMatch, PlagiarismUserLinkUpdate plagiarismUserLinkUpdate,
            Boolean validateOnly) {
        PlagiarismUserLink updated =
                plagiarismAccountService.updatePlagiarismUserLink(plagiarismAccountId, plagiarismUserLinkId, ifMatch,
                        plagiarismUserLinkUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
