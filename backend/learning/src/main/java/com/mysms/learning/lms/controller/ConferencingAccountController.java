package com.mysms.learning.lms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.learning.lms.api.ConferencingAccountApi;
import com.mysms.learning.lms.api.model.ConferencingAccount;
import com.mysms.learning.lms.api.model.ConferencingAccountUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.learning.lms.business.ConferencingAccountService;
import com.mysms.learning.lms.business.ConferencingWebhookService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the ConferencingAccount operations; authentication and the permission check already ran in the
 * auth-client filter. Operations not served yet answer 501.
 */
@RestController
public class ConferencingAccountController implements ConferencingAccountApi {

    private final ConferencingAccountService conferencingAccountService;
    private final ConferencingWebhookService conferencingWebhookService;

    public ConferencingAccountController(ConferencingAccountService conferencingAccountService,
            ConferencingWebhookService conferencingWebhookService) {
        this.conferencingWebhookService = conferencingWebhookService;
        this.conferencingAccountService = conferencingAccountService;
    }

    @Override
    public ResponseEntity<ConferencingAccount> createConferencingAccount(
            ConferencingAccount conferencingAccount, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        ConferencingAccount created = conferencingAccountService.createConferencingAccount(conferencingAccount, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/lms/conferencing-accounts/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteConferencingAccount(UUID conferencingAccountId, String ifMatch) {
        conferencingAccountService.deleteConferencingAccount(conferencingAccountId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ConferencingAccount> getConferencingAccount(UUID conferencingAccountId,
            String fields) {
        ConferencingAccount found = conferencingAccountService.getConferencingAccount(conferencingAccountId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<ConferencingAccount>> listConferencingAccounts(Integer page, Integer size,
            String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<ConferencingAccount> list =
                PagingUtil.pagingInfo(conferencingAccountService.listConferencingAccounts(page, size, cursor, filter,
                        sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Void> receiveConferencingWebhook(String provider, String xSignature, String xTimestamp,
            byte[] body) {
        conferencingWebhookService.receiveConferencingWebhook(provider, xSignature, xTimestamp, body);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ConferencingAccount> updateConferencingAccount(UUID conferencingAccountId,
            String ifMatch, ConferencingAccountUpdate conferencingAccountUpdate, Boolean validateOnly) {
        ConferencingAccount updated =
                conferencingAccountService.updateConferencingAccount(conferencingAccountId, ifMatch,
                        conferencingAccountUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
