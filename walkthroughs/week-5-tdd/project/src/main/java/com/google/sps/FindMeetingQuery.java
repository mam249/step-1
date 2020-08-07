// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FindMeetingQuery {
  /*
   * Returns available slots for a meeting. If one or more time slots exist so that both mandatory
   * and optional attendees can attend, returns those time slots. Otherwise, returns the time slots
   * that fit mandatory attendees.
   *
   * @param events All existing events in the calendar
   * @param request {@link MeetingRequest} object with the request details
   */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    Collection<String> mandatoryAttendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();
    long meetingDuration = request.getDuration();

    if (optionalAttendees.isEmpty()) {
      return getAvailableTimes(events, mandatoryAttendees, meetingDuration);
    }

    Collection<String> allAttendees = Stream.
            concat(mandatoryAttendees.stream(), optionalAttendees.stream()).
            collect(Collectors.toList());

    Collection<TimeRange> availableTimesForAllAttendees = getAvailableTimes(events, allAttendees, meetingDuration);

    if (availableTimesForAllAttendees.isEmpty()) {
      return getAvailableTimes(events, mandatoryAttendees, meetingDuration);
    }
    return availableTimesForAllAttendees;
  }

  private Collection<TimeRange> getAvailableTimes(Collection<Event> events,
         Collection<String> attendees, long duration) {
    if (attendees.isEmpty()) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    // Get all unavailable time ranges based on the attendees
    List<TimeRange> unavailableTimes = new ArrayList<>();
    for (Event event: events) {
      if (containsAtLeastOneAttendee(event, attendees)) {
        unavailableTimes.add(event.getWhen());
      }
    }

    if (unavailableTimes.isEmpty()) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    // Combine all overlapping unavailable time ranges
    Collections.sort(unavailableTimes, TimeRange.ORDER_BY_START);
    int index = 1;
    TimeRange current, next;
    while (index < unavailableTimes.size()) {
      current = unavailableTimes.get(index - 1);
      next = unavailableTimes.get(index);
      if (current.overlaps(next)) {
        int start = Math.min(current.start(), next.start());
        int end = Math.max(current.end(), next.end());
        TimeRange overlapped = TimeRange.fromStartEnd(start, end, /* inclusive */false);
        unavailableTimes.set(index - 1, overlapped);
        unavailableTimes.remove(next);
      } else {
        index++;
      }
    }

    // Create slots between unavailable time ranges
    Collection<TimeRange> availableTimes = new ArrayList<>();
    for (int i = 0; i < unavailableTimes.size(); i++) {
      if (i == 0) {
        addSlotToAvailableTimesIfSlotValid(TimeRange.START_OF_DAY,
                unavailableTimes.get(i).start(), duration, availableTimes);
      } else  {
        addSlotToAvailableTimesIfSlotValid(unavailableTimes.get(i - 1).end(),
                unavailableTimes.get(i).start(), duration, availableTimes);
      }
      if (i == unavailableTimes.size() - 1) {
        addSlotToAvailableTimesIfSlotValid(unavailableTimes.get(i).end(),
                TimeRange.END_OF_DAY, duration, availableTimes);
      }
    }

    return availableTimes;
  }

  private boolean containsAtLeastOneAttendee(Event event, Collection<String> attendees) {
    for (String attendee: attendees) {
      if (event.getAttendees().contains(attendee)) {
        return true;
      }
    }
    return false;
  }

  private void addSlotToAvailableTimesIfSlotValid(int start, int end, long duration,
                                                  Collection<TimeRange> availableTimes) {
    if (start < end && end - start >= duration) {
      boolean inclusive = end == TimeRange.END_OF_DAY;
      availableTimes.add(TimeRange.fromStartEnd(start, end, inclusive));
    }
  }
}
