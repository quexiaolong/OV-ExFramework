package android.os;

/* loaded from: classes.dex */
public abstract class BatteryStatsInternal {
    public abstract String[] getMobileIfaces();

    public abstract String[] getWifiIfaces();

    public abstract void noteJobsDeferred(int i, int i2, long j);
}