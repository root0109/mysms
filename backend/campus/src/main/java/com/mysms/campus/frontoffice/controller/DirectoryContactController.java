package com.mysms.campus.frontoffice.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.campus.frontoffice.api.DirectoryContactApi;
import com.mysms.campus.frontoffice.api.model.DirectoryContact;
import com.mysms.campus.frontoffice.api.model.DirectoryContactUpdate;
import com.mysms.core.web.ReadMask;
import com.mysms.campus.frontoffice.business.DirectoryContactService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the DirectoryContact operations; authentication and the permission check already ran in the auth-client
 * filter. Operations not served yet answer 501.
 */
@RestController
public class DirectoryContactController implements DirectoryContactApi {

    private final DirectoryContactService directoryContactService;

    public DirectoryContactController(DirectoryContactService directoryContactService) {
        this.directoryContactService = directoryContactService;
    }

    @Override
    public ResponseEntity<DirectoryContact> createDirectoryContact(DirectoryContact directoryContact,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        DirectoryContact created = directoryContactService.createDirectoryContact(directoryContact, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/frontoffice/directory-contacts/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteDirectoryContact(UUID directoryContactId, String ifMatch) {
        directoryContactService.deleteDirectoryContact(directoryContactId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<DirectoryContact> getDirectoryContact(UUID directoryContactId, String fields) {
        DirectoryContact found = directoryContactService.getDirectoryContact(directoryContactId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<DirectoryContact>> listDirectoryContacts(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<DirectoryContact> list =
                PagingUtil.pagingInfo(directoryContactService.listDirectoryContacts(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<DirectoryContact> updateDirectoryContact(UUID directoryContactId,
            String ifMatch, DirectoryContactUpdate directoryContactUpdate, Boolean validateOnly) {
        DirectoryContact updated =
                directoryContactService.updateDirectoryContact(directoryContactId, ifMatch, directoryContactUpdate,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
