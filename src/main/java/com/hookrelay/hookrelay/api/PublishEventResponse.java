package com.hookrelay.hookrelay.api;

public class PublishEventResponse {

    private final Long eventId;
    private final String status;

    public PublishEventResponse(Long eventId, String status) {
        this.eventId = eventId;
        this.status = status;
    }

    public Long getEventId() { return eventId; }
    public String getStatus() { return status; }
}