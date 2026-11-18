package com.mysms.platform.grpc;

import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import com.mysms.platform.api.model.Theme;
import com.mysms.platform.api.model.ThemeUpdate;
import com.mysms.platform.business.ThemeService;
import com.mysms.platform.v1.CreateThemeRequest;
import com.mysms.platform.v1.CreateThemeResponse;
import com.mysms.platform.v1.DeleteThemeRequest;
import com.mysms.platform.v1.DeleteThemeResponse;
import com.mysms.platform.v1.GetThemeRequest;
import com.mysms.platform.v1.GetThemeResponse;
import com.mysms.platform.v1.ListThemesRequest;
import com.mysms.platform.v1.ListThemesResponse;
import com.mysms.platform.v1.ThemeServiceGrpc;
import com.mysms.platform.v1.UpdateThemeRequest;
import com.mysms.platform.v1.UpdateThemeResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of platform.ThemeService: delegates every RPC to the business service REST uses, so the rules exist
 * once. Authentication, permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class ThemeResolver extends ThemeServiceGrpc.ThemeServiceImplBase {

    private final ThemeService service;
    private final ProtoBridge bridge;

    public ThemeResolver(ThemeService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listThemes(ListThemesRequest request, StreamObserver<ListThemesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Theme> rows = service.listThemes(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListThemesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getTheme(GetThemeRequest request, StreamObserver<GetThemeResponse> out) {
        Theme found = service.getTheme(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetThemeResponse.newBuilder(), "theme", found).build());
    }

    @Override
    public void createTheme(CreateThemeRequest request, StreamObserver<CreateThemeResponse> out) {
        Theme created = service.createTheme(
                bridge.body(request.getTheme(), Theme.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateThemeResponse.newBuilder(), "theme", created).build());
    }

    @Override
    public void updateTheme(UpdateThemeRequest request, StreamObserver<UpdateThemeResponse> out) {
        Theme updated = service.updateTheme(
                bridge.id(request.getTheme().getId(), "id"),
                bridge.requiredIfMatch(request.getTheme().getEtag()),
                bridge.changes(request.getTheme(), request.getUpdateMask(), ThemeUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateThemeResponse.newBuilder(), "theme", updated).build());
    }

    @Override
    public void deleteTheme(DeleteThemeRequest request, StreamObserver<DeleteThemeResponse> out) {
        service.deleteTheme(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteThemeResponse.getDefaultInstance());
    }
}
