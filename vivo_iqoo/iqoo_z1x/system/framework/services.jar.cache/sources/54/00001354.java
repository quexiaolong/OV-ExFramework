package com.android.server.notification;

import java.util.ArrayList;
import java.util.Comparator;

/* loaded from: classes.dex */
public interface IVivoRankingHelper {

    /* loaded from: classes.dex */
    public interface IVivoGroupHelperExport {
        IVivoRankingHelper getVivoInjectInstance();
    }

    void sort(ArrayList<NotificationRecord> arrayList, Comparator<NotificationRecord> comparator, GlobalSortKeyComparator globalSortKeyComparator);
}