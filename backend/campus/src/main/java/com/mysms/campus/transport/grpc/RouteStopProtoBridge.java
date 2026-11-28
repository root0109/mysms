package com.mysms.campus.transport.grpc;

import com.mysms.campus.transport.api.model.RouteStopUpdate;
import com.mysms.core.grpc.ProtoBridge;

/**
 * UpdateRouteStop over gRPC (JAVA-CODING-RULES §7.2): the {@code transport.v1.RouteStopUpdate} message as REST's {@link
 * RouteStopUpdate} body, one explicit line per field: unset keeps the value, null clears it, a value sets it.
 */
final class RouteStopProtoBridge {

    private RouteStopProtoBridge() {
    }

    /** The changes the message carries, validated like REST's body. */
    static RouteStopUpdate changes(ProtoBridge bridge, com.mysms.transport.v1.RouteStopUpdate request) {
        RouteStopUpdate changes = new RouteStopUpdate();
        changes.setSequence(ProtoBridge.number(request.hasSequence(), request.getSequence()));
        changes.setStopName(ProtoBridge.text(request.hasStopName(), request.getStopName()));
        changes.setPickupTime(ProtoBridge.text(request.hasPickupTime(), request.getPickupTime()));
        changes.setDropTime(ProtoBridge.text(request.hasDropTime(), request.getDropTime()));
        return bridge.patch(changes);
    }
}
