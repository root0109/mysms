package com.mysms.comms.grpc;

import com.mysms.comms.api.model.Event;
import com.mysms.comms.api.model.EventAttendee;
import com.mysms.comms.api.model.EventAttendeeUpdate;
import com.mysms.comms.api.model.EventGalleryItem;
import com.mysms.comms.api.model.EventGalleryItemUpdate;
import com.mysms.comms.api.model.EventInstitution;
import com.mysms.comms.api.model.EventInstitutionUpdate;
import com.mysms.comms.api.model.EventOccurrence;
import com.mysms.comms.api.model.EventOccurrenceUpdate;
import com.mysms.comms.api.model.EventUpdate;
import com.mysms.comms.business.EventService;
import com.mysms.comms.v1.ActivateEventRequest;
import com.mysms.comms.v1.ActivateEventResponse;
import com.mysms.comms.v1.CancelEventOccurrenceRequest;
import com.mysms.comms.v1.CancelEventOccurrenceResponse;
import com.mysms.comms.v1.CompletePersonalEventRequest;
import com.mysms.comms.v1.CompletePersonalEventResponse;
import com.mysms.comms.v1.CreateEventAttendeeRequest;
import com.mysms.comms.v1.CreateEventAttendeeResponse;
import com.mysms.comms.v1.CreateEventGalleryItemRequest;
import com.mysms.comms.v1.CreateEventGalleryItemResponse;
import com.mysms.comms.v1.CreateEventInstitutionRequest;
import com.mysms.comms.v1.CreateEventInstitutionResponse;
import com.mysms.comms.v1.CreateEventOccurrenceRequest;
import com.mysms.comms.v1.CreateEventOccurrenceResponse;
import com.mysms.comms.v1.CreateEventRequest;
import com.mysms.comms.v1.CreateEventResponse;
import com.mysms.comms.v1.DeactivateEventRequest;
import com.mysms.comms.v1.DeactivateEventResponse;
import com.mysms.comms.v1.DeleteEventAttendeeRequest;
import com.mysms.comms.v1.DeleteEventAttendeeResponse;
import com.mysms.comms.v1.DeleteEventGalleryItemRequest;
import com.mysms.comms.v1.DeleteEventGalleryItemResponse;
import com.mysms.comms.v1.DeleteEventInstitutionRequest;
import com.mysms.comms.v1.DeleteEventInstitutionResponse;
import com.mysms.comms.v1.DeleteEventOccurrenceRequest;
import com.mysms.comms.v1.DeleteEventOccurrenceResponse;
import com.mysms.comms.v1.DeleteEventRequest;
import com.mysms.comms.v1.DeleteEventResponse;
import com.mysms.comms.v1.EventServiceGrpc;
import com.mysms.comms.v1.GetEventAttendeeRequest;
import com.mysms.comms.v1.GetEventAttendeeResponse;
import com.mysms.comms.v1.GetEventGalleryItemRequest;
import com.mysms.comms.v1.GetEventGalleryItemResponse;
import com.mysms.comms.v1.GetEventInstitutionRequest;
import com.mysms.comms.v1.GetEventInstitutionResponse;
import com.mysms.comms.v1.GetEventOccurrenceRequest;
import com.mysms.comms.v1.GetEventOccurrenceResponse;
import com.mysms.comms.v1.GetEventRequest;
import com.mysms.comms.v1.GetEventResponse;
import com.mysms.comms.v1.GetMyCalendarRequest;
import com.mysms.comms.v1.GetMyCalendarResponse;
import com.mysms.comms.v1.ListEventAttendeesRequest;
import com.mysms.comms.v1.ListEventAttendeesResponse;
import com.mysms.comms.v1.ListEventGalleryItemsRequest;
import com.mysms.comms.v1.ListEventGalleryItemsResponse;
import com.mysms.comms.v1.ListEventInstitutionsRequest;
import com.mysms.comms.v1.ListEventInstitutionsResponse;
import com.mysms.comms.v1.ListEventOccurrencesRequest;
import com.mysms.comms.v1.ListEventOccurrencesResponse;
import com.mysms.comms.v1.ListEventsRequest;
import com.mysms.comms.v1.ListEventsResponse;
import com.mysms.comms.v1.UpdateEventAttendeeRequest;
import com.mysms.comms.v1.UpdateEventAttendeeResponse;
import com.mysms.comms.v1.UpdateEventGalleryItemRequest;
import com.mysms.comms.v1.UpdateEventGalleryItemResponse;
import com.mysms.comms.v1.UpdateEventInstitutionRequest;
import com.mysms.comms.v1.UpdateEventInstitutionResponse;
import com.mysms.comms.v1.UpdateEventOccurrenceRequest;
import com.mysms.comms.v1.UpdateEventOccurrenceResponse;
import com.mysms.comms.v1.UpdateEventRequest;
import com.mysms.comms.v1.UpdateEventResponse;
import com.mysms.core.error.NotImplementedException;
import com.mysms.core.grpc.ProtoBridge;
import com.mysms.core.paging.Page;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC resolver of comms.EventService: delegates every RPC to the same business service as REST. Authentication,
 * permission, idempotency and error mapping happen at the gRPC edge.
 */
@GrpcService
public class EventResolver extends EventServiceGrpc.EventServiceImplBase {

    private final EventService service;
    private final ProtoBridge bridge;

    public EventResolver(EventService service, ProtoBridge bridge) {
        this.service = service;
        this.bridge = bridge;
    }

    @Override
    public void listEvents(ListEventsRequest request, StreamObserver<ListEventsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<Event> rows = service.listEvents(
                page.page(), page.size(), page.cursor(), bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEventsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEvent(GetEventRequest request, StreamObserver<GetEventResponse> out) {
        Event found = service.getEvent(bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEventResponse.newBuilder(), "event", found).build());
    }

    @Override
    public void createEvent(CreateEventRequest request, StreamObserver<CreateEventResponse> out) {
        Event created = service.createEvent(bridge.body(request.getEvent(), Event.class), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEventResponse.newBuilder(), "event", created).build());
    }

    @Override
    public void updateEvent(UpdateEventRequest request, StreamObserver<UpdateEventResponse> out) {
        Event updated = service.updateEvent(
                bridge.id(request.getEvent().getId(), "id"), bridge.requiredIfMatch(request.getEvent().getEtag()),
                bridge.changes(request.getEvent(), request.getUpdateMask(), EventUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEventResponse.newBuilder(), "event", updated).build());
    }

    @Override
    public void deleteEvent(DeleteEventRequest request, StreamObserver<DeleteEventResponse> out) {
        service.deleteEvent(bridge.id(request.getId(), "id"), bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEventResponse.getDefaultInstance());
    }

    @Override
    public void listEventGalleryItems(ListEventGalleryItemsRequest request, StreamObserver<ListEventGalleryItemsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EventGalleryItem> rows = service.listEventGalleryItems(
                bridge.id(request.getEventId(), "eventId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEventGalleryItemsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEventGalleryItem(GetEventGalleryItemRequest request, StreamObserver<GetEventGalleryItemResponse> out) {
        EventGalleryItem found = service.getEventGalleryItem(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEventGalleryItemResponse.newBuilder(), "event_gallery_item", found).build());
    }

    @Override
    public void createEventGalleryItem(CreateEventGalleryItemRequest request,
            StreamObserver<CreateEventGalleryItemResponse> out) {
        EventGalleryItem created = service.createEventGalleryItem(
                bridge.id(request.getEventId(), "eventId"), bridge.body(request.getEventGalleryItem(), EventGalleryItem.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEventGalleryItemResponse.newBuilder(), "event_gallery_item", created).build());
    }

    @Override
    public void updateEventGalleryItem(UpdateEventGalleryItemRequest request,
            StreamObserver<UpdateEventGalleryItemResponse> out) {
        EventGalleryItem updated = service.updateEventGalleryItem(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getEventGalleryItem().getId(), "id"),
                bridge.requiredIfMatch(request.getEventGalleryItem().getEtag()),
                bridge.changes(request.getEventGalleryItem(), request.getUpdateMask(), EventGalleryItemUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEventGalleryItemResponse.newBuilder(), "event_gallery_item", updated).build());
    }

    @Override
    public void deleteEventGalleryItem(DeleteEventGalleryItemRequest request,
            StreamObserver<DeleteEventGalleryItemResponse> out) {
        service.deleteEventGalleryItem(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEventGalleryItemResponse.getDefaultInstance());
    }

    @Override
    public void listEventInstitutions(ListEventInstitutionsRequest request, StreamObserver<ListEventInstitutionsResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EventInstitution> rows = service.listEventInstitutions(
                bridge.id(request.getEventId(), "eventId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEventInstitutionsResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEventInstitution(GetEventInstitutionRequest request, StreamObserver<GetEventInstitutionResponse> out) {
        EventInstitution found = service.getEventInstitution(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEventInstitutionResponse.newBuilder(), "event_institution", found).build());
    }

    @Override
    public void createEventInstitution(CreateEventInstitutionRequest request,
            StreamObserver<CreateEventInstitutionResponse> out) {
        EventInstitution created = service.createEventInstitution(
                bridge.id(request.getEventId(), "eventId"), bridge.body(request.getEventInstitution(), EventInstitution.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEventInstitutionResponse.newBuilder(), "event_institution", created).build());
    }

    @Override
    public void updateEventInstitution(UpdateEventInstitutionRequest request,
            StreamObserver<UpdateEventInstitutionResponse> out) {
        EventInstitution updated = service.updateEventInstitution(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getEventInstitution().getId(), "id"),
                bridge.requiredIfMatch(request.getEventInstitution().getEtag()),
                bridge.changes(request.getEventInstitution(), request.getUpdateMask(), EventInstitutionUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEventInstitutionResponse.newBuilder(), "event_institution", updated).build());
    }

    @Override
    public void deleteEventInstitution(DeleteEventInstitutionRequest request,
            StreamObserver<DeleteEventInstitutionResponse> out) {
        service.deleteEventInstitution(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEventInstitutionResponse.getDefaultInstance());
    }

    @Override
    public void listEventOccurrences(ListEventOccurrencesRequest request, StreamObserver<ListEventOccurrencesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EventOccurrence> rows = service.listEventOccurrences(
                bridge.id(request.getEventId(), "eventId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEventOccurrencesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEventOccurrence(GetEventOccurrenceRequest request, StreamObserver<GetEventOccurrenceResponse> out) {
        EventOccurrence found = service.getEventOccurrence(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEventOccurrenceResponse.newBuilder(), "event_occurrence", found).build());
    }

    @Override
    public void createEventOccurrence(CreateEventOccurrenceRequest request, StreamObserver<CreateEventOccurrenceResponse> out) {
        EventOccurrence created = service.createEventOccurrence(
                bridge.id(request.getEventId(), "eventId"), bridge.body(request.getEventOccurrence(), EventOccurrence.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEventOccurrenceResponse.newBuilder(), "event_occurrence", created).build());
    }

    @Override
    public void updateEventOccurrence(UpdateEventOccurrenceRequest request, StreamObserver<UpdateEventOccurrenceResponse> out) {
        EventOccurrence updated = service.updateEventOccurrence(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getEventOccurrence().getId(), "id"),
                bridge.requiredIfMatch(request.getEventOccurrence().getEtag()),
                bridge.changes(request.getEventOccurrence(), request.getUpdateMask(), EventOccurrenceUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEventOccurrenceResponse.newBuilder(), "event_occurrence", updated).build());
    }

    @Override
    public void deleteEventOccurrence(DeleteEventOccurrenceRequest request, StreamObserver<DeleteEventOccurrenceResponse> out) {
        service.deleteEventOccurrence(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEventOccurrenceResponse.getDefaultInstance());
    }

    @Override
    public void listEventAttendees(ListEventAttendeesRequest request, StreamObserver<ListEventAttendeesResponse> out) {
        ProtoBridge.ListArguments page = bridge.list(request.getPageable());
        Page<EventAttendee> rows = service.listEventAttendees(
                bridge.id(request.getEventId(), "eventId"), page.page(), page.size(), page.cursor(),
                bridge.orNull(request.getFilter()), page.sort());
        ProtoBridge.send(out, bridge.page(ListEventAttendeesResponse.newBuilder(), rows).build());
    }

    @Override
    public void getEventAttendee(GetEventAttendeeRequest request, StreamObserver<GetEventAttendeeResponse> out) {
        EventAttendee found = service.getEventAttendee(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getId(), "id"));
        ProtoBridge.send(out, bridge.reply(GetEventAttendeeResponse.newBuilder(), "event_attendee", found).build());
    }

    @Override
    public void createEventAttendee(CreateEventAttendeeRequest request, StreamObserver<CreateEventAttendeeResponse> out) {
        EventAttendee created = service.createEventAttendee(
                bridge.id(request.getEventId(), "eventId"), bridge.body(request.getEventAttendee(), EventAttendee.class),
                request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(CreateEventAttendeeResponse.newBuilder(), "event_attendee", created).build());
    }

    @Override
    public void updateEventAttendee(UpdateEventAttendeeRequest request, StreamObserver<UpdateEventAttendeeResponse> out) {
        EventAttendee updated = service.updateEventAttendee(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getEventAttendee().getId(), "id"),
                bridge.requiredIfMatch(request.getEventAttendee().getEtag()),
                bridge.changes(request.getEventAttendee(), request.getUpdateMask(), EventAttendeeUpdate.class),
                bridge.updateMask(request.getUpdateMask()), request.getValidateOnly());
        ProtoBridge.send(out, bridge.reply(UpdateEventAttendeeResponse.newBuilder(), "event_attendee", updated).build());
    }

    @Override
    public void deleteEventAttendee(DeleteEventAttendeeRequest request, StreamObserver<DeleteEventAttendeeResponse> out) {
        service.deleteEventAttendee(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getId(), "id"),
                bridge.requiredIfMatch(request.getEtag()));
        ProtoBridge.send(out, DeleteEventAttendeeResponse.getDefaultInstance());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void activateEvent(ActivateEventRequest request, StreamObserver<ActivateEventResponse> out) {
        Event changed = service.activateEvent(
                bridge.id(request.getEventId(), "eventId"),
                bridge.body(request, com.mysms.comms.api.model.ActivateEventRequest.class, "event_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(ActivateEventResponse.newBuilder(), "event", changed).build());
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void deactivateEvent(DeactivateEventRequest request, StreamObserver<DeactivateEventResponse> out) {
        Event changed = service.deactivateEvent(
                bridge.id(request.getEventId(), "eventId"),
                bridge.body(request, com.mysms.comms.api.model.DeactivateEventRequest.class, "event_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(DeactivateEventResponse.newBuilder(), "event", changed).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void getMyCalendar(GetMyCalendarRequest request, StreamObserver<GetMyCalendarResponse> out) {
        throw new NotImplementedException();
    }

    /** The REST body (the workflow etag, its other fields) is the request without its path ids and idempotency key. */
    @Override
    public void cancelEventOccurrence(CancelEventOccurrenceRequest request, StreamObserver<CancelEventOccurrenceResponse> out) {
        EventOccurrence changed = service.cancelEventOccurrence(
                bridge.id(request.getEventId(), "eventId"), bridge.id(request.getEventOccurrenceId(), "eventOccurrenceId"),
                bridge.body(request, com.mysms.comms.api.model.CancelEventOccurrenceRequest.class, "event_id",
                        "event_occurrence_id", "request_id"));
        ProtoBridge.send(out, bridge.reply(CancelEventOccurrenceResponse.newBuilder(), "event_occurrence", changed).build());
    }

    /** Not served yet: UNIMPLEMENTED, like REST's 501. */
    @Override
    public void completePersonalEvent(CompletePersonalEventRequest request, StreamObserver<CompletePersonalEventResponse> out) {
        throw new NotImplementedException();
    }
}
