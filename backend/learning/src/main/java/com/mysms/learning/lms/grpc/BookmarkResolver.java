package com.mysms.learning.lms.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.learning.lms.api.model.Bookmark;
import com.mysms.learning.lms.business.BookmarkService;
import com.mysms.lms.v1.BookmarkServiceGrpc;
import com.mysms.lms.v1.BookmarkUpdate;
import com.mysms.lms.v1.CreateBookmarkRequest;
import com.mysms.lms.v1.CreateBookmarkResponse;
import com.mysms.lms.v1.DeleteBookmarkRequest;
import com.mysms.lms.v1.DeleteBookmarkResponse;
import com.mysms.lms.v1.GetBookmarkRequest;
import com.mysms.lms.v1.GetBookmarkResponse;
import com.mysms.lms.v1.ListBookmarksRequest;
import com.mysms.lms.v1.ListBookmarksResponse;
import com.mysms.lms.v1.UpdateBookmarkResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of lms.BookmarkService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class BookmarkResolver extends BookmarkServiceGrpc.BookmarkServiceImplBase {

    private final BookmarkService service;
    private final ProtoBridge bridge;

    public BookmarkResolver(BookmarkService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listBookmarks(ListBookmarksRequest request, StreamObserver<ListBookmarksResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Bookmark> rows = service.listBookmarks(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListBookmarksResponse.newBuilder(), rows).build());
    }

    @Override
    public void getBookmark(GetBookmarkRequest request, StreamObserver<GetBookmarkResponse> out) {
        Bookmark found = service.getBookmark(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetBookmarkResponse.newBuilder(), "bookmark", found).build());
    }

    @Override
    public void createBookmark(CreateBookmarkRequest request, StreamObserver<CreateBookmarkResponse> out) {
        Bookmark created = service.createBookmark(
                bridge.body(request.getBookmark(), Bookmark.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateBookmarkResponse.newBuilder(), "bookmark", created).build());
    }

    @Override
    public void updateBookmark(BookmarkUpdate request, StreamObserver<UpdateBookmarkResponse> out) {
        Bookmark updated = service.updateBookmark(
                bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()),
                BookmarkProtoBridge.changes(bridge, request), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateBookmarkResponse.newBuilder(), "bookmark", updated).build());
    }

    @Override
    public void deleteBookmark(DeleteBookmarkRequest request, StreamObserver<DeleteBookmarkResponse> out) {
        service.deleteBookmark(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteBookmarkResponse.getDefaultInstance());
    }
}
