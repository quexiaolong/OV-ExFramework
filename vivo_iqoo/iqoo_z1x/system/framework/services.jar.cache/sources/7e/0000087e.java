package com.android.server.audio;

import android.util.Log;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;

/* loaded from: classes.dex */
public class AudioEventLogger {
    private final LinkedList<Event> mEvents = new LinkedList<>();
    private final int mMemSize;
    private final String mTitle;

    /* loaded from: classes.dex */
    public static abstract class Event {
        private static final SimpleDateFormat sFormat = new SimpleDateFormat("MM-dd HH:mm:ss:SSS");
        private final long mTimestamp = System.currentTimeMillis();

        public abstract String eventToString();

        public String toString() {
            return sFormat.format(new Date(this.mTimestamp)) + " " + eventToString();
        }

        public Event printLog(String tag) {
            Log.i(tag, eventToString());
            return this;
        }
    }

    /* loaded from: classes.dex */
    public static class StringEvent extends Event {
        private final String mMsg;

        public StringEvent(String msg) {
            this.mMsg = msg;
        }

        @Override // com.android.server.audio.AudioEventLogger.Event
        public String eventToString() {
            return this.mMsg;
        }
    }

    public AudioEventLogger(int size, String title) {
        this.mMemSize = size;
        this.mTitle = title;
    }

    public synchronized void log(Event evt) {
        if (this.mEvents.size() >= this.mMemSize) {
            this.mEvents.removeFirst();
        }
        this.mEvents.add(evt);
    }

    public synchronized void dump(PrintWriter pw) {
        pw.println("Audio event log: " + this.mTitle);
        Iterator<Event> it = this.mEvents.iterator();
        while (it.hasNext()) {
            Event evt = it.next();
            pw.println(evt.toString());
        }
    }
}