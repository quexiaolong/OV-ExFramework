package com.android.server.people.data;

import com.android.internal.util.CollectionUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class EventList {
    private final List<Event> mEvents = new ArrayList();

    /* JADX INFO: Access modifiers changed from: package-private */
    public void add(Event event) {
        int index = firstIndexOnOrAfter(event.getTimestamp());
        if (index < this.mEvents.size() && this.mEvents.get(index).getTimestamp() == event.getTimestamp() && isDuplicate(event, index)) {
            return;
        }
        this.mEvents.add(index, event);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void addAll(List<Event> events) {
        for (Event event : events) {
            add(event);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Event> queryEvents(Set<Integer> eventTypes, long fromTimestamp, long toTimestamp) {
        int fromIndex = firstIndexOnOrAfter(fromTimestamp);
        if (fromIndex == this.mEvents.size()) {
            return new ArrayList();
        }
        int toIndex = firstIndexOnOrAfter(toTimestamp);
        if (toIndex < fromIndex) {
            return new ArrayList();
        }
        List<Event> result = new ArrayList<>();
        for (int i = fromIndex; i < toIndex; i++) {
            Event e = this.mEvents.get(i);
            if (eventTypes.contains(Integer.valueOf(e.getType()))) {
                result.add(e);
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void clear() {
        this.mEvents.clear();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public List<Event> getAllEvents() {
        return CollectionUtils.copyOf(this.mEvents);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void removeOldEvents(long cutOffThreshold) {
        int cutOffIndex = firstIndexOnOrAfter(cutOffThreshold);
        if (cutOffIndex == 0) {
            return;
        }
        int eventsSize = this.mEvents.size();
        if (cutOffIndex == eventsSize) {
            this.mEvents.clear();
            return;
        }
        int i = 0;
        while (cutOffIndex < eventsSize) {
            List<Event> list = this.mEvents;
            list.set(i, list.get(cutOffIndex));
            i++;
            cutOffIndex++;
        }
        if (eventsSize > i) {
            this.mEvents.subList(i, eventsSize).clear();
        }
    }

    private int firstIndexOnOrAfter(long timestamp) {
        int result = this.mEvents.size();
        int low = 0;
        int high = this.mEvents.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            if (this.mEvents.get(mid).getTimestamp() >= timestamp) {
                high = mid - 1;
                result = mid;
            } else {
                low = mid + 1;
            }
        }
        return result;
    }

    private boolean isDuplicate(Event event, int startIndex) {
        int size = this.mEvents.size();
        int index = startIndex;
        while (index < size && this.mEvents.get(index).getTimestamp() <= event.getTimestamp()) {
            int index2 = index + 1;
            if (this.mEvents.get(index).getType() != event.getType()) {
                index = index2;
            } else {
                return true;
            }
        }
        return false;
    }
}