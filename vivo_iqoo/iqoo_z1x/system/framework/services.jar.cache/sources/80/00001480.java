package com.android.server.people.data;

import java.util.List;
import java.util.Set;

/* loaded from: classes.dex */
public interface EventHistory {
    EventIndex getEventIndex(int i);

    EventIndex getEventIndex(Set<Integer> set);

    List<Event> queryEvents(Set<Integer> set, long j, long j2);
}