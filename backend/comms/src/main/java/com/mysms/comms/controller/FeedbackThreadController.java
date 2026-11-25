package com.mysms.comms.controller;

import java.util.List;
import com.mysms.core.paging.PagingUtil;
import com.mysms.core.paging.PagingInfo;
import com.mysms.comms.business.FeedbackThreadService;
import com.mysms.comms.api.FeedbackThreadApi;
import com.mysms.comms.api.model.CloseFeedbackThreadRequest;
import com.mysms.comms.api.model.FeedbackMessage;
import com.mysms.comms.api.model.FeedbackMessageUpdate;
import com.mysms.comms.api.model.FeedbackThread;
import com.mysms.comms.api.model.FeedbackThreadUpdate;
import com.mysms.comms.api.model.MarkFeedbackMessageRequest;
import com.mysms.comms.api.model.ReopenFeedbackThreadRequest;
import com.mysms.core.web.ReadMask;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST edge of the FeedbackThread operations; authentication and the permission check already ran in the auth-client
 * filter.
 */
@RestController
public class FeedbackThreadController implements FeedbackThreadApi {

    private final FeedbackThreadService feedbackThreadService;

    public FeedbackThreadController(FeedbackThreadService feedbackThreadService) {
        this.feedbackThreadService = feedbackThreadService;
    }

    @Override
    public ResponseEntity<FeedbackThread> closeFeedbackThread(UUID feedbackThreadId,
            CloseFeedbackThreadRequest closeFeedbackThreadRequest, UUID idempotencyKey) {
        FeedbackThread changed =
                feedbackThreadService.closeFeedbackThread(feedbackThreadId, closeFeedbackThreadRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<FeedbackMessage> createFeedbackMessage(UUID feedbackThreadId,
            FeedbackMessage feedbackMessage, UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        FeedbackMessage created =
                feedbackThreadService.createFeedbackMessage(feedbackThreadId, feedbackMessage, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(
                URI.create("/v1/comms/feedback-threads/" + feedbackThreadId + "/messages/" + created.getId()))
                        .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<FeedbackThread> createFeedbackThread(FeedbackThread feedbackThread,
            UUID idempotencyKey, Boolean validateOnly) {
        boolean dryRun = Boolean.TRUE.equals(validateOnly);
        FeedbackThread created = feedbackThreadService.createFeedbackThread(feedbackThread, dryRun);
        if (dryRun) {
            return ResponseEntity.ok(created);
        }
        return ResponseEntity.created(URI.create("/v1/comms/feedback-threads/" + created.getId()))
                .eTag(created.getEtag()).body(created);
    }

    @Override
    public ResponseEntity<Void> deleteFeedbackMessage(UUID feedbackThreadId, UUID feedbackMessageId,
            String ifMatch) {
        feedbackThreadService.deleteFeedbackMessage(feedbackThreadId, feedbackMessageId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteFeedbackThread(UUID feedbackThreadId, String ifMatch) {
        feedbackThreadService.deleteFeedbackThread(feedbackThreadId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FeedbackMessage> getFeedbackMessage(UUID feedbackThreadId,
            UUID feedbackMessageId, String fields) {
        FeedbackMessage found = feedbackThreadService.getFeedbackMessage(feedbackThreadId, feedbackMessageId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<FeedbackThread> getFeedbackThread(UUID feedbackThreadId, String fields) {
        FeedbackThread found = feedbackThreadService.getFeedbackThread(feedbackThreadId);
        return ResponseEntity.ok().eTag(found.getEtag()).body(ReadMask.apply(found, fields));
    }

    @Override
    public ResponseEntity<PagingInfo<FeedbackMessage>> listFeedbackMessages(UUID feedbackThreadId, Integer page,
            Integer size, String cursor, String filter, List<String> sort, String fields) {
        PagingInfo<FeedbackMessage> list =
                PagingUtil.pagingInfo(feedbackThreadService.listFeedbackMessages(feedbackThreadId, page, size, cursor,
                        filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<PagingInfo<FeedbackThread>> listFeedbackThreads(Integer page, Integer size, String cursor,
            String filter, List<String> sort, String fields) {
        PagingInfo<FeedbackThread> list =
                PagingUtil.pagingInfo(feedbackThreadService.listFeedbackThreads(page, size, cursor, filter, sort));
        list.data().forEach(item -> ReadMask.apply(item, fields));
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<FeedbackMessage> markFeedbackMessage(UUID messageId,
            MarkFeedbackMessageRequest markFeedbackMessageRequest, UUID idempotencyKey) {
        FeedbackMessage changed = feedbackThreadService.markFeedbackMessage(messageId, markFeedbackMessageRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<FeedbackThread> reopenFeedbackThread(UUID feedbackThreadId,
            ReopenFeedbackThreadRequest reopenFeedbackThreadRequest, UUID idempotencyKey) {
        FeedbackThread changed =
                feedbackThreadService.reopenFeedbackThread(feedbackThreadId, reopenFeedbackThreadRequest);
        return ResponseEntity.ok().eTag(changed.getEtag()).body(changed);
    }

    @Override
    public ResponseEntity<FeedbackMessage> updateFeedbackMessage(UUID feedbackThreadId,
            UUID feedbackMessageId, String ifMatch, FeedbackMessageUpdate feedbackMessageUpdate, String updateMask,
            Boolean validateOnly) {
        FeedbackMessage updated =
                feedbackThreadService.updateFeedbackMessage(feedbackThreadId, feedbackMessageId, ifMatch,
                        feedbackMessageUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }

    @Override
    public ResponseEntity<FeedbackThread> updateFeedbackThread(UUID feedbackThreadId, String ifMatch,
            FeedbackThreadUpdate feedbackThreadUpdate, String updateMask, Boolean validateOnly) {
        FeedbackThread updated =
                feedbackThreadService.updateFeedbackThread(feedbackThreadId, ifMatch, feedbackThreadUpdate, updateMask,
                Boolean.TRUE.equals(validateOnly));
        return ResponseEntity.ok().eTag(updated.getEtag()).body(updated);
    }
}
