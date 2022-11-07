package com.google.android.startop.iorap;

import android.content.Intent;
import android.util.Log;
import com.android.server.wm.ActivityMetricsLaunchObserver;
import java.io.PrintWriter;
import java.io.StringWriter;

/* loaded from: classes2.dex */
public class EventSequenceValidator implements ActivityMetricsLaunchObserver {
    static final String TAG = "EventSequenceValidator";
    private State state = State.INIT;
    private long accIntentStartedEvents = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public enum State {
        INIT,
        INTENT_STARTED,
        INTENT_FAILED,
        ACTIVITY_LAUNCHED,
        ACTIVITY_CANCELLED,
        ACTIVITY_FINISHED,
        REPORT_FULLY_DRAWN,
        UNKNOWN
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onIntentStarted(Intent intent, long timestampNs) {
        if (this.state == State.UNKNOWN) {
            logWarningWithStackTrace("IntentStarted during UNKNOWN. " + intent);
            incAccIntentStartedEvents();
        } else if (this.state != State.INIT && this.state != State.INTENT_FAILED && this.state != State.ACTIVITY_CANCELLED && this.state != State.ACTIVITY_FINISHED && this.state != State.REPORT_FULLY_DRAWN) {
            logWarningWithStackTrace(String.format("Cannot transition from %s to %s", this.state, State.INTENT_STARTED));
            incAccIntentStartedEvents();
            incAccIntentStartedEvents();
        } else {
            Log.d(TAG, String.format("Transition from %s to %s", this.state, State.INTENT_STARTED));
            this.state = State.INTENT_STARTED;
        }
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onIntentFailed() {
        if (this.state == State.UNKNOWN) {
            logWarningWithStackTrace("onIntentFailed during UNKNOWN.");
            decAccIntentStartedEvents();
        } else if (this.state != State.INTENT_STARTED) {
            logWarningWithStackTrace(String.format("Cannot transition from %s to %s", this.state, State.INTENT_FAILED));
            incAccIntentStartedEvents();
        } else {
            Log.d(TAG, String.format("Transition from %s to %s", this.state, State.INTENT_FAILED));
            this.state = State.INTENT_FAILED;
        }
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onActivityLaunched(byte[] activity, int temperature) {
        if (this.state == State.UNKNOWN) {
            logWarningWithStackTrace("onActivityLaunched during UNKNOWN.");
        } else if (this.state != State.INTENT_STARTED) {
            logWarningWithStackTrace(String.format("Cannot transition from %s to %s", this.state, State.ACTIVITY_LAUNCHED));
            incAccIntentStartedEvents();
        } else {
            Log.d(TAG, String.format("Transition from %s to %s", this.state, State.ACTIVITY_LAUNCHED));
            this.state = State.ACTIVITY_LAUNCHED;
        }
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onActivityLaunchCancelled(byte[] activity) {
        if (this.state == State.UNKNOWN) {
            logWarningWithStackTrace("onActivityLaunchCancelled during UNKNOWN.");
            decAccIntentStartedEvents();
        } else if (this.state != State.ACTIVITY_LAUNCHED) {
            logWarningWithStackTrace(String.format("Cannot transition from %s to %s", this.state, State.ACTIVITY_CANCELLED));
            incAccIntentStartedEvents();
        } else {
            Log.d(TAG, String.format("Transition from %s to %s", this.state, State.ACTIVITY_CANCELLED));
            this.state = State.ACTIVITY_CANCELLED;
        }
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onActivityLaunchFinished(byte[] activity, long timestampNs) {
        if (this.state == State.UNKNOWN) {
            logWarningWithStackTrace("onActivityLaunchFinished during UNKNOWN.");
            decAccIntentStartedEvents();
        } else if (this.state != State.ACTIVITY_LAUNCHED) {
            logWarningWithStackTrace(String.format("Cannot transition from %s to %s", this.state, State.ACTIVITY_FINISHED));
            incAccIntentStartedEvents();
        } else {
            Log.d(TAG, String.format("Transition from %s to %s", this.state, State.ACTIVITY_FINISHED));
            this.state = State.ACTIVITY_FINISHED;
        }
    }

    @Override // com.android.server.wm.ActivityMetricsLaunchObserver
    public void onReportFullyDrawn(byte[] activity, long timestampNs) {
        if (this.state == State.UNKNOWN) {
            logWarningWithStackTrace("onReportFullyDrawn during UNKNOWN.");
        } else if (this.state == State.INIT) {
        } else {
            if (this.state != State.ACTIVITY_FINISHED) {
                logWarningWithStackTrace(String.format("Cannot transition from %s to %s", this.state, State.REPORT_FULLY_DRAWN));
                return;
            }
            Log.d(TAG, String.format("Transition from %s to %s", this.state, State.REPORT_FULLY_DRAWN));
            this.state = State.REPORT_FULLY_DRAWN;
        }
    }

    private void incAccIntentStartedEvents() {
        long j = this.accIntentStartedEvents;
        if (j < 0) {
            throw new AssertionError("The number of unknowns cannot be negative");
        }
        if (j == 0) {
            this.state = State.UNKNOWN;
        }
        long j2 = this.accIntentStartedEvents + 1;
        this.accIntentStartedEvents = j2;
        Log.d(TAG, String.format("inc AccIntentStartedEvents to %d", Long.valueOf(j2)));
    }

    private void decAccIntentStartedEvents() {
        long j = this.accIntentStartedEvents;
        if (j <= 0) {
            throw new AssertionError("The number of unknowns cannot be negative");
        }
        if (j == 1) {
            this.state = State.INIT;
        }
        long j2 = this.accIntentStartedEvents - 1;
        this.accIntentStartedEvents = j2;
        Log.d(TAG, String.format("dec AccIntentStartedEvents to %d", Long.valueOf(j2)));
    }

    private void logWarningWithStackTrace(String log) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        new Throwable("EventSequenceValidator#getStackTrace").printStackTrace(pw);
        Log.d(TAG, String.format("%s\n%s", log, sw));
    }
}