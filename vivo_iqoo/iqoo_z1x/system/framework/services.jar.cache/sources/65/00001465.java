package com.android.server.people.data;

/* loaded from: classes.dex */
public class AppUsageStatsData {
    private int mChosenCount;
    private int mLaunchCount;

    public AppUsageStatsData(int chosenCount, int launchCount) {
        this.mChosenCount = chosenCount;
        this.mLaunchCount = launchCount;
    }

    public AppUsageStatsData() {
    }

    public int getLaunchCount() {
        return this.mLaunchCount;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void incrementLaunchCountBy(int launchCount) {
        this.mLaunchCount += launchCount;
    }

    public int getChosenCount() {
        return this.mChosenCount;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void incrementChosenCountBy(int chosenCount) {
        this.mChosenCount += chosenCount;
    }
}