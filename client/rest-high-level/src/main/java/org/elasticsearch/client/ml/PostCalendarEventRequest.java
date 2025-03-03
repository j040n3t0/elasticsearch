/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */
package org.elasticsearch.client.ml;

import org.elasticsearch.client.Validatable;
import org.elasticsearch.client.ml.calendars.Calendar;
import org.elasticsearch.client.ml.calendars.ScheduledEvent;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.ConstructingObjectParser;
import org.elasticsearch.xcontent.ToXContentObject;
import org.elasticsearch.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Request to add a ScheduledEvent to a Machine Learning calendar
 */
public class PostCalendarEventRequest implements Validatable, ToXContentObject {

    private final String calendarId;
    private final List<ScheduledEvent> scheduledEvents;

    public static final String INCLUDE_CALENDAR_ID_KEY = "include_calendar_id";
    public static final ParseField EVENTS = new ParseField("events");

    @SuppressWarnings("unchecked")
    public static final ConstructingObjectParser<PostCalendarEventRequest, Void> PARSER =
        new ConstructingObjectParser<>("post_calendar_event_request",
            a -> new PostCalendarEventRequest((String)a[0], (List<ScheduledEvent>)a[1]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), Calendar.ID);
        PARSER.declareObjectArray(ConstructingObjectParser.constructorArg(),
            (p, c) -> ScheduledEvent.PARSER.apply(p, null), EVENTS);
    }
    public static final MapParams EXCLUDE_CALENDAR_ID_PARAMS =
        new MapParams(Collections.singletonMap(INCLUDE_CALENDAR_ID_KEY, Boolean.toString(false)));

    /**
     * Create a new PostCalendarEventRequest with an existing non-null calendarId and a list of Scheduled events
     *
     * @param calendarId The ID of the calendar, must be non-null
     * @param scheduledEvents The non-null, non-empty, list of {@link ScheduledEvent} objects to add to the calendar
     */
    public PostCalendarEventRequest(String calendarId, List<ScheduledEvent> scheduledEvents) {
        this.calendarId = Objects.requireNonNull(calendarId, "[calendar_id] must not be null.");
        this.scheduledEvents = Objects.requireNonNull(scheduledEvents, "[events] must not be null.");
        if (scheduledEvents.isEmpty()) {
            throw new IllegalArgumentException("At least 1 event is required");
        }
    }

    public String getCalendarId() {
        return calendarId;
    }

    public List<ScheduledEvent> getScheduledEvents() {
        return scheduledEvents;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        if (params.paramAsBoolean(INCLUDE_CALENDAR_ID_KEY, true)) {
            builder.field(Calendar.ID.getPreferredName(), calendarId);
        }
        builder.field(EVENTS.getPreferredName(), scheduledEvents);
        builder.endObject();
        return builder;
    }

    @Override
    public int hashCode() {
        return Objects.hash(calendarId, scheduledEvents);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        PostCalendarEventRequest other = (PostCalendarEventRequest) obj;
        return Objects.equals(calendarId, other.calendarId) && Objects.equals(scheduledEvents, other.scheduledEvents);
    }
}
