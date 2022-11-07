package com.vivo.services.rms.appmng;

import android.os.SystemClock;
import java.io.PrintWriter;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class RecentTask {
    private static final int MAX_SIZE = 13;
    private int mSize = 13;
    private ArrayList<RecentItem> mRecent = new ArrayList<>(this.mSize);

    public void put(String procName, int uid) {
        synchronized (this) {
            if (this.mSize == 0) {
                return;
            }
            int index = indexOf(procName, uid);
            RecentItem item = null;
            if (index != -1) {
                item = this.mRecent.remove(index);
            }
            if (this.mRecent.size() >= this.mSize) {
                item = this.mRecent.remove(0);
            }
            if (item != null) {
                item.fill(procName, uid);
            } else {
                item = new RecentItem(procName, uid);
            }
            this.mRecent.add(item);
        }
    }

    public void remove(String procName, int uid) {
        synchronized (this) {
            int index = indexOf(procName, uid);
            if (index != -1) {
                this.mRecent.remove(index);
            }
        }
    }

    public boolean contains(String procName, int uid) {
        boolean z;
        synchronized (this) {
            z = indexOf(procName, uid) != -1;
        }
        return z;
    }

    public long getRecentTime(String procName, int uid) {
        synchronized (this) {
            int index = indexOf(procName, uid);
            if (index != -1 && index < 3) {
                return this.mRecent.get(index).time;
            }
            return -1L;
        }
    }

    private int indexOf(String procName, int uid) {
        for (int i = 0; i < this.mRecent.size(); i++) {
            RecentItem item = this.mRecent.get(i);
            if (uid == item.uid && procName.equals(item.procName)) {
                int index = i;
                return index;
            }
        }
        return -1;
    }

    public int taskId(String procName, int uid) {
        synchronized (this) {
            int index = indexOf(procName, uid);
            if (index == -1) {
                return -1;
            }
            return (this.mRecent.size() - 1) - index;
        }
    }

    public void setSize(int size) {
        synchronized (this) {
            this.mSize = Math.min(Math.max(size, 1), 13);
        }
    }

    public void dump(PrintWriter pw) {
        synchronized (this) {
            if (this.mRecent.size() > 0) {
                pw.print("*recent:");
            }
            for (int i = this.mRecent.size() - 1; i >= 0; i--) {
                pw.print(" ");
                pw.print(this.mRecent.get(i));
            }
            if (this.mRecent.size() > 0) {
                pw.println();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class RecentItem {
        String procName;
        long time;
        int uid;

        RecentItem(String pkg, int uid) {
            fill(pkg, uid);
        }

        void fill(String pkg, int uid) {
            this.procName = pkg;
            this.uid = uid;
            this.time = SystemClock.uptimeMillis();
        }

        public String toString() {
            return String.format("%s(%d)", this.procName, Integer.valueOf(this.uid));
        }
    }
}