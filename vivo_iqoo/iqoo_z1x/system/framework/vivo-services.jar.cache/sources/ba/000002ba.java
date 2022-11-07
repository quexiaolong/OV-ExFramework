package com.android.server.location;

import com.android.server.location.VivoLocConf;
import com.vivo.common.utils.VLog;
import java.util.HashSet;
import vivo.app.configuration.ContentValuesList;

/* loaded from: classes.dex */
public class VivoLocationAppFilter {
    private static final String TAG = "VivoLocationAppFilter";
    private final String PACK_NAME = "pack_name";
    private final String ID = "id";
    private final String VB = "bl--";
    private final Object mLock = new Object();
    private HashSet<String> mBlackSet = new HashSet<>();

    public static /* synthetic */ void lambda$vjHmZduz81Nf51FhfYTp30pyZrk(VivoLocationAppFilter vivoLocationAppFilter, ContentValuesList contentValuesList) {
        vivoLocationAppFilter.updateSetByJson(contentValuesList);
    }

    public VivoLocationAppFilter() {
        VivoLocConf config = VivoLocConf.getInstance();
        config.registerListener(VivoLocConf.APP_FILTER, new VivoLocConf.ContentValuesListChangedListener() { // from class: com.android.server.location.-$$Lambda$VivoLocationAppFilter$vjHmZduz81Nf51FhfYTp30pyZrk
            @Override // com.android.server.location.VivoLocConf.ContentValuesListChangedListener
            public final void onConfigChanged(ContentValuesList contentValuesList) {
                VivoLocationAppFilter.lambda$vjHmZduz81Nf51FhfYTp30pyZrk(VivoLocationAppFilter.this, contentValuesList);
            }
        });
    }

    public boolean isInBlacklist(String packName) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mBlackSet.contains(packName);
        }
        return contains;
    }

    public boolean addToBlacklist(String packName) {
        boolean add;
        synchronized (this.mLock) {
            add = this.mBlackSet.add(packName);
        }
        return add;
    }

    public boolean removeFromBlacklist(String packName) {
        boolean remove;
        synchronized (this.mLock) {
            remove = this.mBlackSet.remove(packName);
        }
        return remove;
    }

    public int size() {
        int size;
        synchronized (this.mLock) {
            size = this.mBlackSet.size();
        }
        return size;
    }

    public void updateSetByJson(ContentValuesList list) {
        synchronized (this.mLock) {
            this.mBlackSet.clear();
            int index = 1;
            while (true) {
                StringBuilder sb = new StringBuilder();
                sb.append("pkg");
                int index2 = index + 1;
                sb.append(index);
                String packageName = list.getValue(sb.toString());
                if (packageName != null) {
                    this.mBlackSet.add(packageName);
                }
                if (packageName != null) {
                    index = index2;
                } else {
                    VLog.d(TAG, "bl--Update configuration done. size:" + String.valueOf(size()));
                }
            }
        }
    }
}