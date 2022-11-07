package com.vivo.services.rms.sp;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.ArrayMap;
import com.vivo.services.rms.sdk.ObjectCache;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class ComponentLifeCycleMgr {
    private static final ObjectCache<SpComponent> CACHE = new ObjectCache<>(SpComponent.class, 128);
    private static final long MAX_CACHED_COMPONENT_SIZE = 512;
    private static final long MAX_CACHED_COMPONTENT_INTERVAL = 300000;
    private static final long MIN_CARE_OCCUPIED_INTERVAL = 1000;
    private static final int POINT_SEC_RANGE = 100;
    private static final String TAG = "SpManager";
    private final ArrayList<SpComponent> mCareComponents;
    private final Object mLock;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final ComponentLifeCycleMgr INSTANCE = new ComponentLifeCycleMgr();

        private Instance() {
        }
    }

    private ComponentLifeCycleMgr() {
        this.mLock = new Object();
        this.mCareComponents = new ArrayList<>();
    }

    public static ComponentLifeCycleMgr getInstance() {
        return Instance.INSTANCE;
    }

    /* loaded from: classes.dex */
    public static class SpComponent {
        String className;
        long endPoint;
        int pid;
        String pkgName;
        long startPoint;
        int type;

        public SpComponent fill(int pid, int type, String pkgName, String className) {
            this.pid = pid;
            this.pkgName = pkgName;
            this.className = className;
            this.type = type;
            this.startPoint = SystemClock.uptimeMillis();
            this.endPoint = -1L;
            return this;
        }

        public int hashCode() {
            return this.pid;
        }

        public boolean equals(Object obj) {
            if (obj instanceof SpComponent) {
                SpComponent s = (SpComponent) obj;
                return s.pid == this.pid && s.type == this.type && s.pkgName.equals(this.pkgName) && s.className.equals(this.className);
            }
            return false;
        }

        public String toString() {
            return String.format(Locale.getDefault(), "%d|%d|%s|%s", Integer.valueOf(this.pid), Integer.valueOf(this.type), this.pkgName, this.className);
        }
    }

    public void componentStartPoint(int pid, int type, String pkgName, String className) {
        synchronized (this.mLock) {
            this.mCareComponents.add(pop().fill(pid, type, pkgName, className));
            checkAndRemoveOldComponentLocked();
        }
    }

    private void checkAndRemoveOldComponentLocked() {
        int size = this.mCareComponents.size();
        for (int i = 0; i < size - 512; i++) {
            push(this.mCareComponents.remove(0));
        }
        int size2 = this.mCareComponents.size();
        long now = SystemClock.uptimeMillis();
        for (int i2 = 0; i2 < size2; i2++) {
            if (now - this.mCareComponents.get(0).startPoint > MAX_CACHED_COMPONTENT_INTERVAL) {
                push(this.mCareComponents.remove(0));
            }
        }
    }

    public void componentFinishPoint(int pid, int type, String pkgName, String className) {
        synchronized (this.mLock) {
            long now = SystemClock.uptimeMillis();
            int index = firstNotFinishIndex(this.mCareComponents, pid, type, pkgName, className);
            if (index < 0) {
                VSlog.e("SpManager", String.format(Locale.getDefault(), "Can not find start point for %%d|%d|%s|%s", Integer.valueOf(pid), Integer.valueOf(type), pkgName, className));
                return;
            }
            SpComponent start = this.mCareComponents.get(index);
            if (now - start.startPoint < 1000) {
                this.mCareComponents.remove(index);
                push(start);
            } else {
                start.endPoint = now;
            }
        }
    }

    public String whoShouldBlame(int pid, int type, String pkgName, String className) {
        synchronized (this.mLock) {
            dumpTest(this.mCareComponents, "super process component life cycle map");
            int index = firstNotFinishIndex(this.mCareComponents, pid, type, pkgName, className);
            if (index < 0) {
                VSlog.e("SpManager", String.format(Locale.getDefault(), "ANR, can not find start point for %d|%d|%s|%s", Integer.valueOf(pid), Integer.valueOf(type), pkgName, className));
                return null;
            }
            SpComponent start = this.mCareComponents.get(index);
            return whoShouldBlameLocked(pid, start.startPoint, SystemClock.uptimeMillis());
        }
    }

    public void onSuperProcessDied(int oldPid) {
        synchronized (this.mLock) {
            Iterator<SpComponent> it = this.mCareComponents.iterator();
            while (it.hasNext()) {
                SpComponent tmp = it.next();
                if (tmp.pid == oldPid) {
                    it.remove();
                    push(tmp);
                }
            }
        }
    }

    private String whoShouldBlameLocked(int pid, long start, long end) {
        int size = this.mCareComponents.size();
        ArrayList<SpComponent> finishSuspicions = new ArrayList<>(size);
        ArrayList<SpComponent> notFinishSuspicions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            SpComponent tmp = this.mCareComponents.get(i);
            if (tmp.pid == pid && ((tmp.endPoint <= 0 || tmp.endPoint > start) && tmp.startPoint / 100 <= start / 100)) {
                if (tmp.endPoint > 0 && tmp.endPoint / 100 <= end / 100) {
                    finishSuspicions.add(tmp);
                } else if (tmp.endPoint <= 0) {
                    notFinishSuspicions.add(tmp);
                } else {
                    VSlog.e("SpManager", "Should not happen, component finish later than us ?");
                }
            }
        }
        int finishSize = finishSuspicions.size();
        int notFinishSize = notFinishSuspicions.size();
        if (finishSize <= 0 && notFinishSize <= 0) {
            return null;
        }
        if (finishSize > 0) {
            String suspicion = pickFinishSuspicions(start, end, finishSuspicions);
            if (!TextUtils.isEmpty(suspicion)) {
                VSlog.d("SpManager", "find suspicion on finish list, index:" + suspicion);
                return suspicion;
            }
        }
        if (notFinishSize <= 0) {
            return null;
        }
        VSlog.d("SpManager", "find suspicion on not finish list " + notFinishSuspicions.get(0));
        return notFinishSuspicions.get(0).pkgName;
    }

    private static String pickFinishSuspicions(long start, long end, ArrayList<SpComponent> suspicions) {
        int size = suspicions.size();
        ArrayList<SpComponent> sortedList = new ArrayList<>(size);
        ArrayList<SpComponent> source = new ArrayList<>(suspicions);
        for (int i = 0; i < size; i++) {
            int firstFinishIdx = firstFinishIndex(source);
            sortedList.add(source.remove(firstFinishIdx));
        }
        long[] occupied = new long[size];
        for (int i2 = 0; i2 < size; i2++) {
            if (i2 == 0) {
                occupied[i2] = sortedList.get(i2).endPoint - start;
            } else {
                occupied[i2] = sortedList.get(i2).endPoint - sortedList.get(i2 - 1).endPoint;
            }
        }
        ArrayMap<String, Long> timeMerge = new ArrayMap<>(size);
        for (int i3 = 0; i3 < size; i3++) {
            Long time = timeMerge.remove(sortedList.get(i3).pkgName);
            timeMerge.put(sortedList.get(i3).pkgName, Long.valueOf((time != null ? time.longValue() : 0L) + occupied[i3]));
        }
        String longestPkg = timeMerge.keyAt(0);
        long longestTime = timeMerge.valueAt(0).longValue();
        int mergeSize = timeMerge.size();
        for (int i4 = 1; i4 < mergeSize; i4++) {
            if (timeMerge.valueAt(i4).longValue() > longestTime) {
                longestTime = timeMerge.valueAt(i4).longValue();
                String longestPkg2 = timeMerge.keyAt(i4);
                longestPkg = longestPkg2;
            }
        }
        if (longestTime < (end - start) / 2) {
            VSlog.i("SpManager", "Find blame occupied component but it is less than " + ((end - start) / 2) + "ms");
            return null;
        }
        VSlog.i("SpManager", "Find blame occupied component " + longestPkg + " with " + longestTime + "ms");
        return longestPkg;
    }

    private static int firstFinishIndex(ArrayList<SpComponent> source) {
        int size = source.size();
        if (size <= 1) {
            return 0;
        }
        int firstIdx = 0;
        for (int i = 1; i < size; i++) {
            if (source.get(i).endPoint < source.get(firstIdx).endPoint) {
                firstIdx = i;
            }
        }
        return firstIdx;
    }

    private static int firstNotFinishIndex(ArrayList<SpComponent> source, int pid, int type, String pkgName, String className) {
        int size = source.size();
        for (int i = 0; i < size; i++) {
            SpComponent tmp = source.get(i);
            if (tmp.endPoint <= 0 && tmp.pid == pid && tmp.type == type && tmp.pkgName.equals(pkgName) && tmp.className.equals(className)) {
                return i;
            }
        }
        return -1;
    }

    private static int round(double d) {
        if (d <= 0.0d || d >= 1.0d) {
            return (int) d;
        }
        return 1;
    }

    private static SpComponent pop() {
        SpComponent item = CACHE.pop();
        return item == null ? new SpComponent() : item;
    }

    private static void push(SpComponent item) {
        if (item == null) {
            return;
        }
        CACHE.put((ObjectCache<SpComponent>) item);
    }

    private static String timeLine(int max, long s1, long start, long end, long e1) {
        long total = e1 - s1;
        long step = total / max;
        if (step <= 0) {
            return "***";
        }
        int pref = round((start - s1) / step);
        int posix = round((e1 - end) / step);
        if (pref < 0) {
            pref = 0;
        }
        if (posix < 0) {
            posix = 0;
        }
        int occupy = (max - pref) - posix;
        StringBuilder sb = new StringBuilder(max);
        for (int i = 0; i < pref; i++) {
            sb.append("-");
        }
        for (int i2 = 0; i2 < occupy; i2++) {
            sb.append("*");
        }
        for (int i3 = 0; i3 < posix; i3++) {
            sb.append("=");
        }
        return sb.toString();
    }

    public void dump(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.append((CharSequence) dumpTest(this.mCareComponents));
        }
    }

    private static String dumpTest(ArrayList<SpComponent> source) {
        StringBuilder sb = new StringBuilder();
        int size = source.size();
        sb.append("TOTAL SIZE : ");
        sb.append(size);
        sb.append("\n");
        if (size <= 0) {
            return sb.toString();
        }
        long now = SystemClock.uptimeMillis();
        long start = source.get(0).startPoint;
        int i = 0;
        while (i < size) {
            long end = source.get(i).endPoint <= 0 ? now : source.get(i).endPoint;
            sb.append(source.get(i) + ":");
            sb.append("\t");
            sb.append(source.get(i).startPoint - start);
            sb.append("|");
            sb.append(end - source.get(i).startPoint);
            sb.append("|");
            sb.append(now - end);
            sb.append("\n");
            sb.append(timeLine(100, start, source.get(i).startPoint, end, now));
            sb.append("\n");
            i++;
            start = start;
        }
        return sb.toString();
    }

    private static void dumpTest(ArrayList<SpComponent> source, String tag) {
        VSlog.i("SpManager", tag + ":");
        VSlog.i("SpManager", dumpTest(source));
    }
}