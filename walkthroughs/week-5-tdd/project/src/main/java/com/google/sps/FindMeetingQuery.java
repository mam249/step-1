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
import java.util.LinkedList;
import java.util.List;

public final class FindMeetingQuery {
  private final Collection<TimeRange> mAvailableTimes = new ArrayList<>();
  private long mMeetingDuration;

  /* Returns all available slots for a meeting to happen
   * Parameters: events (all events that are in the calendar, some of them could not relate
   *                      to the attendees in the meeting request) */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    if (request.getAttendees().isEmpty()) {
      return Arrays.asList(TimeRange.WHOLE_DAY);
    }

    if (request.getDuration() > TimeRange.WHOLE_DAY.duration()) {
      return Arrays.asList();
    }

    // Get all unavailable time ranges based on the attendees
    List<TimeRange> unavailableTimes = new LinkedList<>();
    for (Event event: events) {
      if (containsAtLeastOneAttendee(event, request.getAttendees())) {
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
    mMeetingDuration = request.getDuration();
    for (int j = 0; j < unavailableTimes.size(); j++) {
      if (j == 0) {
        createSlot(TimeRange.START_OF_DAY, unavailableTimes.get(j).start());
      } else  {
        createSlot(unavailableTimes.get(j - 1).end(), unavailableTimes.get(j).start());
      }
      if (j == unavailableTimes.size() - 1) {
        createSlot(unavailableTimes.get(j).end(), TimeRange.END_OF_DAY);
      }
    }

    return mAvailableTimes;
  }

  private boolean containsAtLeastOneAttendee(Event event, Collection<String> attendees) {
    for (String attendee: attendees) {
      if (event.getAttendees().contains(attendee)) {
        return true;
      }
    }
    return false;
  }

  /* mMeetingDuration should be set before calling this method */
  private void createSlot(int start, int end) {
    if (start < end && end - start >= mMeetingDuration) {
      boolean inclusive = end == TimeRange.END_OF_DAY;
      mAvailableTimes.add(TimeRange.fromStartEnd(start, end, inclusive));
    }
  }
}
