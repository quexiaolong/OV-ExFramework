package com.vivo.services.rms.sp;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.am.EmergencyBroadcastManager;
import com.vivo.services.rms.sdk.Consts;
import com.vivo.services.rms.sp.config.Helpers;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class BadPackageManager {
    private static final String ATTR_ERROR_FLAGS = "flag";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_VERSION = "version";
    private static final String BAD_PACKAGE_XML_FILE = "bad_system_packages.xml";
    private static final long CONSIDER_REAL_BAD_INTERVAL_ALLDAY = 86400000;
    private static final long CONSIDER_REAL_BAD_INTERVAL_RECENT = 1800000;
    private static final int CONSIDER_REAL_BAD_TIMES_ALLDAY = 10;
    private static final int CONSIDER_REAL_BAD_TIMES_RECENT = 3;
    private static final int DB_VERSION = 1;
    private static final int FILLIN_VERSION_CODE_FOR_BAD = 2;
    private static final int FILLIN_VERSION_CODE_FOR_ERROR = 1;
    private static final String FILLIN_VERSION_CODE_KEY_FLAG = "flag";
    private static final String FILLIN_VERSION_CODE_KEY_PKG = "pkg";
    private static final String TAG_BAD_PACKAGES = "bad_packages";
    private static final String TAG_PACKAGE = "package";
    private final BadPackageList mBadPackageListSync;
    private final BadPackageQuickAccess mBadPackagesList;
    private PackageStateChange mChangeListener;
    private final ErrorRecordList mErrorRecordList;
    private final Object mLock;
    private AtomicFile mRecordFile;
    private final Handler mSaverHandler;
    private final Object mSyncLock;
    private Context mSystemContext;

    /* loaded from: classes.dex */
    public interface PackageStateChange {
        void onPackageTurnBad(String str);
    }

    public void initialize(Context context) {
        synchronized (BadPackageManager.class) {
            if (this.mSystemContext == null && context != null) {
                this.mSystemContext = context;
                this.mRecordFile = new AtomicFile(new File(new File(Environment.getDataDirectory(), "system"), BAD_PACKAGE_XML_FILE));
                long start = System.nanoTime();
                loadBadPackages();
                VSlog.i("SpManager", "loadBadPackages took " + ((System.nanoTime() - start) / 1000) + "us");
            }
        }
    }

    public void checkPackageChangeSync() {
        checkPackageChange();
    }

    public boolean isBadPackage(String pkgName) {
        return hasSamePackage(pkgName);
    }

    private boolean hasSamePackage(String pkgName) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mBadPackagesList.contains(pkgName);
        }
        return contains;
    }

    private boolean hasPackageWithDiffVersion(String pkgName, long versionCode) {
        boolean z;
        synchronized (this.mLock) {
            BadPackageItem item = this.mBadPackagesList.get(pkgName);
            z = (item == null || item.version == versionCode) ? false : true;
        }
        return z;
    }

    private boolean hasSamePackageAndVersion(String pkgName, long versionCode) {
        boolean z;
        synchronized (this.mLock) {
            BadPackageItem item = this.mBadPackagesList.get(pkgName);
            z = item != null && item.version == versionCode;
        }
        return z;
    }

    public void reportErrorPackage(String pkgName, long versionCode, int flag) {
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        VSlog.i("SpManager", "report error package:" + pkgName + " versionCode:" + versionCode);
        long newVersionCode = fillVersionCode(pkgName, versionCode);
        if (newVersionCode < 0) {
            scheduleFillInVersionCode(1, pkgName, flag);
        } else if (!hasSamePackageAndVersion(pkgName, newVersionCode)) {
            this.mErrorRecordList.record(pkgName, newVersionCode, flag);
        }
    }

    public void onPackageUpdate(String pkgName, long versionCode) {
    }

    public void addBadPackage(String pkgName, long versionCode, int flag) {
        if (TextUtils.isEmpty(pkgName)) {
            return;
        }
        long newVersionCode = fillVersionCode(pkgName, versionCode);
        if (newVersionCode < 0) {
            scheduleFillInVersionCode(2, pkgName, flag);
            return;
        }
        synchronized (this.mLock) {
            this.mBadPackagesList.add(pkgName, newVersionCode, flag);
        }
        synchronized (this.mSyncLock) {
            BadPackageItem item = this.mBadPackageListSync.add(pkgName, newVersionCode, flag);
            if (item != null) {
                scheduleSaveBadPackages();
            }
        }
    }

    public void removeBadPackage(String pkgName) {
        synchronized (this.mLock) {
            this.mBadPackagesList.remove(pkgName);
        }
        synchronized (this.mSyncLock) {
            BadPackageItem item = this.mBadPackageListSync.remove(pkgName);
            if (item != null) {
                scheduleSaveBadPackages();
            }
        }
    }

    public void dumpBadPackages(PrintWriter pw) {
        pw.append("Current Bad packages:\n");
        synchronized (this.mLock) {
            Iterator<BadPackageItem> it = this.mBadPackagesList.iterator();
            while (it.hasNext()) {
                BadPackageItem i = it.next();
                pw.append((CharSequence) i.toString()).append("\n");
            }
        }
        pw.append("Current BAD packages synced:\n");
        synchronized (this.mSyncLock) {
            Iterator<BadPackageItem> it2 = this.mBadPackageListSync.iterator();
            while (it2.hasNext()) {
                BadPackageItem i2 = it2.next();
                pw.append((CharSequence) i2.toString()).append("\n");
            }
        }
    }

    private void scheduleFillInVersionCode(int forWhat, String packageName, int flag) {
        scheduleFillInVersionCode(forWhat, packageName, flag, 1000L, 0);
    }

    private void scheduleFillInVersionCode(int forWhat, String packageName, int flag, long delay, int count) {
        VSlog.d("SpManager", "scheduleFillInVersionCode(), packageName:" + packageName + ", count:" + count);
        Bundle b = new Bundle();
        b.putString(FILLIN_VERSION_CODE_KEY_PKG, packageName);
        b.putInt("flag", flag);
        Message msg = this.mSaverHandler.obtainMessage(4, forWhat, count);
        msg.setData(b);
        this.mSaverHandler.sendMessageDelayed(msg, delay);
    }

    private long fillVersionCode(String packageName, long old) {
        return old >= 0 ? old : Helpers.getVersion(packageName, this.mSystemContext);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void handleFillInVersionCode(Message msg) {
        Bundle b = msg.getData();
        if (b == null) {
            return;
        }
        String pkg = b.getString(FILLIN_VERSION_CODE_KEY_PKG);
        int flag = b.getInt("flag");
        int forWhat = msg.arg1;
        int count = msg.arg2;
        long code = fillVersionCode(pkg, -1L);
        if (code < 0 && count < 3) {
            scheduleFillInVersionCode(forWhat, pkg, flag, 1000L, count + 1);
        } else if (code >= 0) {
            VSlog.d("SpManager", "finally find, packageName:" + pkg + ", versionCode:" + code);
            if (forWhat == 1) {
                reportErrorPackage(pkg, code, flag);
            } else if (forWhat == 2) {
                addBadPackage(pkg, code, flag);
            }
        } else {
            VSlog.w("SpManager", "Can not find version for packageName:" + pkg + ", versionCode:" + code);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void loadBadPackages() {
        synchronized (this.mSyncLock) {
            InputStream infile = null;
            try {
                infile = this.mRecordFile.openRead();
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(infile, StandardCharsets.UTF_8.name());
                XmlUtils.beginDocument(parser, TAG_BAD_PACKAGES);
                int outerDepth = parser.getDepth();
                while (XmlUtils.nextElementWithin(parser, outerDepth)) {
                    readPackageItemLocked(parser);
                }
                IoUtils.closeQuietly(infile);
            } catch (FileNotFoundException e) {
                IoUtils.closeQuietly(infile);
            } catch (IOException | NumberFormatException | XmlPullParserException e2) {
                VSlog.wtf("SpManager", "Unable to read bad packages, deleting file", e2);
                this.mRecordFile.delete();
                IoUtils.closeQuietly(infile);
            }
            synchronized (this.mLock) {
                int size = this.mBadPackageListSync.size();
                for (int i = 0; i < size; i++) {
                    this.mBadPackagesList.add(this.mBadPackageListSync.get(i).pkg, this.mBadPackageListSync.get(i).version, this.mBadPackageListSync.get(i).errorFlags);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean saveBadPackages() {
        synchronized (this.mSyncLock) {
            try {
                try {
                    FileOutputStream stream = this.mRecordFile.startWrite();
                    try {
                        XmlSerializer out = new FastXmlSerializer();
                        out.setOutput(stream, StandardCharsets.UTF_8.name());
                        out.startDocument(null, true);
                        out.startTag(null, TAG_BAD_PACKAGES);
                        out.attribute(null, ATTR_VERSION, String.valueOf(1));
                        writePackageItemLocked(out);
                        out.endTag(null, TAG_BAD_PACKAGES);
                        out.endDocument();
                        this.mRecordFile.finishWrite(stream);
                        IoUtils.closeQuietly(stream);
                    } catch (IOException e) {
                        VSlog.w("SpManager", "Failed to save bad packages, restoring backup", e);
                        this.mRecordFile.failWrite(stream);
                        IoUtils.closeQuietly(stream);
                        return false;
                    }
                } catch (Throwable th) {
                    throw th;
                }
            } catch (IOException e2) {
                VSlog.w("SpManager", "Cannot update bad packages", e2);
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void checkPackageChange() {
        ArrayList<BadPackageItem> copiedList;
        HashSet<String> changed;
        HashSet<String> changed2 = null;
        synchronized (this.mLock) {
            copiedList = this.mBadPackagesList.copyList();
        }
        if (copiedList != null && copiedList.size() > 0) {
            Iterator<BadPackageItem> it = copiedList.iterator();
            while (it.hasNext()) {
                BadPackageItem item = it.next();
                long newVersion = Helpers.getVersion(item.pkg, this.mSystemContext);
                if (item.errorFlags == 8192 || (newVersion > 0 && newVersion != item.version)) {
                    if (changed2 == null) {
                        changed2 = new HashSet<>();
                    }
                    changed2.add(item.pkg);
                }
            }
            changed = changed2;
        } else {
            changed = null;
        }
        synchronized (this.mLock) {
            if (changed != null) {
                Iterator<String> it2 = changed.iterator();
                while (it2.hasNext()) {
                    String pkg = it2.next();
                    this.mBadPackagesList.remove(pkg);
                }
                synchronized (this.mSyncLock) {
                    Iterator<String> it3 = changed.iterator();
                    while (it3.hasNext()) {
                        String pkg2 = it3.next();
                        this.mBadPackageListSync.remove(pkg2);
                    }
                }
                scheduleSaveBadPackages();
            }
        }
    }

    private void readPackageItemLocked(XmlPullParser parser) {
        if (TAG_PACKAGE.equals(parser.getName())) {
            try {
                String packageName = parser.getAttributeValue(null, ATTR_NAME);
                long version = Long.parseLong(parser.getAttributeValue(null, ATTR_VERSION));
                int errorFlags = Integer.parseInt(parser.getAttributeValue(null, "flag"));
                this.mBadPackageListSync.add(packageName, version, errorFlags);
            } catch (NumberFormatException e) {
                VSlog.wtf("SpManager", "Skipping package", e);
            }
        }
    }

    private boolean writePackageItemLocked(XmlSerializer out) {
        try {
            int size = this.mBadPackageListSync.size();
            for (int i = 0; i < size; i++) {
                out.startTag(null, TAG_PACKAGE);
                out.attribute(null, ATTR_NAME, this.mBadPackageListSync.get(i).pkg);
                out.attribute(null, ATTR_VERSION, String.valueOf(this.mBadPackageListSync.get(i).version));
                out.attribute(null, "flag", String.valueOf(this.mBadPackageListSync.get(i).errorFlags));
                out.endTag(null, TAG_PACKAGE);
            }
            return true;
        } catch (IOException e) {
            VSlog.w("SpManager", "Cannot save package", e);
            return false;
        }
    }

    private void scheduleLoadBadPackages() {
        this.mSaverHandler.sendEmptyMessage(1);
    }

    private void scheduleSaveBadPackages() {
        if (!this.mSaverHandler.hasMessages(2)) {
            this.mSaverHandler.sendEmptyMessageDelayed(2, 1000L);
        }
    }

    private void scheduleCheckPackageChange() {
        this.mSaverHandler.sendEmptyMessage(3);
    }

    public void setPackageStateChange(PackageStateChange listener) {
        this.mChangeListener = listener;
    }

    /* loaded from: classes.dex */
    class H extends Handler {
        public static final int CHECK_PACKAGE_CHANGE = 3;
        public static final int FILL_IN_VERSION_CODE = 4;
        public static final int LOAD_BAD_PACKAGES = 1;
        public static final int SAVE_BAD_PACKAGES = 2;

        public H(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 1) {
                BadPackageManager.this.loadBadPackages();
            } else if (i == 2) {
                BadPackageManager.this.saveBadPackages();
            } else if (i == 3) {
                BadPackageManager.this.checkPackageChange();
            } else if (i == 4) {
                BadPackageManager.this.handleFillInVersionCode(msg);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getConsiderRealBadTimesRecent(String pkg) {
        return ("com.vivo.sps".equals(pkg) ? 2 : 1) * 3;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getConsiderRealBadTimesAllday(String pkg) {
        return ("com.vivo.sps".equals(pkg) ? 2 : 1) * 10;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ErrorRecord {
        int errorFlags;
        String pkg;
        long version;
        boolean isBad = false;
        long lastBadUpTime = 0;
        long lastBadElapsedTime = 0;
        private int counterElapsedTime = 0;
        private int counterUpTime = 0;

        ErrorRecord(String pkg, long version, int flag) {
            this.pkg = pkg;
            this.version = version;
            this.errorFlags = flag;
        }

        void setErrorFlag(int flag) {
            this.errorFlags |= flag;
        }

        void recordOnce() {
            long nowUpTime = SystemClock.uptimeMillis();
            long nowElapsedTime = SystemClock.elapsedRealtime();
            long j = this.lastBadUpTime;
            boolean z = true;
            if (j == 0 || this.lastBadElapsedTime == 0) {
                this.lastBadUpTime = nowUpTime;
                this.lastBadElapsedTime = nowElapsedTime;
                this.counterElapsedTime = 1;
                this.counterUpTime = 1;
                return;
            }
            if (nowUpTime - j > BadPackageManager.CONSIDER_REAL_BAD_INTERVAL_RECENT) {
                this.counterUpTime = 1;
                this.lastBadUpTime = nowUpTime;
            } else {
                this.counterUpTime++;
            }
            if (nowElapsedTime - this.lastBadElapsedTime > 86400000) {
                this.counterElapsedTime = 1;
                this.lastBadElapsedTime = nowElapsedTime;
            } else {
                this.counterElapsedTime++;
            }
            if (this.counterUpTime < BadPackageManager.getConsiderRealBadTimesRecent(this.pkg) && this.counterElapsedTime < BadPackageManager.getConsiderRealBadTimesAllday(this.pkg)) {
                z = false;
            }
            this.isBad = z;
        }

        public String toString() {
            return String.format("pkg:%s version:%d count1:%d count2:%d", this.pkg, Long.valueOf(this.version), Integer.valueOf(this.counterUpTime), Integer.valueOf(this.counterElapsedTime));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ErrorRecordList {
        final ArrayList<ErrorRecord> mList;

        private ErrorRecordList() {
            this.mList = new ArrayList<>();
        }

        void record(String pkg, long version, int flag) {
            synchronized (this.mList) {
                ErrorRecord record = getLocked(pkg);
                if (record == null) {
                    record = new ErrorRecord(pkg, version, flag);
                    this.mList.add(record);
                }
                if (record.version != version) {
                    removeLocked(pkg);
                    record = new ErrorRecord(pkg, version, flag);
                    this.mList.add(record);
                }
                record.recordOnce();
                if (record.isBad) {
                    BadPackageManager.this.addBadPackage(record.pkg, record.version, record.errorFlags);
                    removeLocked(pkg);
                }
            }
        }

        private ErrorRecord removeLocked(String pkg) {
            Iterator<ErrorRecord> it = this.mList.iterator();
            while (it.hasNext()) {
                ErrorRecord record = it.next();
                if (record.pkg.equals(pkg)) {
                    it.remove();
                    return record;
                }
            }
            return null;
        }

        private ErrorRecord getLocked(String pkg) {
            Iterator<ErrorRecord> it = this.mList.iterator();
            while (it.hasNext()) {
                ErrorRecord record = it.next();
                if (record.pkg.equals(pkg)) {
                    return record;
                }
            }
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BadPackageItem {
        int errorFlags;
        String pkg;
        long version;

        BadPackageItem(String pkg, long version, int flag) {
            this.pkg = pkg;
            this.version = version;
            this.errorFlags = flag;
        }

        void setErrorFlag(int flag) {
            this.errorFlags |= flag;
        }

        public String toString() {
            return String.format(Locale.getDefault(), "%s %d %s", this.pkg, Long.valueOf(this.version), BadPackageManager.errorFlags2String(this.errorFlags));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BadPackageList implements Iterable<BadPackageItem> {
        final ArrayList<BadPackageItem> mList;

        private BadPackageList() {
            this.mList = new ArrayList<>();
        }

        BadPackageItem add(String pkg, long version, int flag) {
            BadPackageItem item = get(pkg);
            if (item == null) {
                BadPackageItem item2 = new BadPackageItem(pkg, version, flag);
                this.mList.add(item2);
                return item2;
            } else if (item.version != version) {
                item.version = version;
                return item;
            } else {
                return null;
            }
        }

        BadPackageItem remove(String pkg) {
            Iterator<BadPackageItem> it = this.mList.iterator();
            while (it.hasNext()) {
                BadPackageItem item = it.next();
                if (item.pkg.equals(pkg)) {
                    it.remove();
                    return item;
                }
            }
            return null;
        }

        BadPackageItem get(String pkg) {
            Iterator<BadPackageItem> it = this.mList.iterator();
            while (it.hasNext()) {
                BadPackageItem item = it.next();
                if (item.pkg.equals(pkg)) {
                    return item;
                }
            }
            return null;
        }

        BadPackageItem get(int index) {
            return this.mList.get(index);
        }

        int size() {
            return this.mList.size();
        }

        boolean contains(String pkg) {
            return get(pkg) != null;
        }

        @Override // java.lang.Iterable
        public Iterator<BadPackageItem> iterator() {
            return this.mList.iterator();
        }

        ArrayList<BadPackageItem> copyList() {
            return (ArrayList) this.mList.clone();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BadPackageQuickAccess extends BadPackageList {
        private BadPackageQuickAccess() {
            super();
        }

        @Override // com.vivo.services.rms.sp.BadPackageManager.BadPackageList
        BadPackageItem add(String pkg, long version, int flag) {
            if (BadPackageManager.this.mChangeListener != null) {
                BadPackageManager.this.mChangeListener.onPackageTurnBad(pkg);
            }
            VSlog.i("SpManager", pkg + "(" + version + ") is bad!");
            return super.add(pkg, version, flag);
        }

        @Override // com.vivo.services.rms.sp.BadPackageManager.BadPackageList
        BadPackageItem remove(String pkg) {
            VSlog.i("SpManager", pkg + " becomes good!");
            return super.remove(pkg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static String errorFlags2String(int flags) {
        StringBuilder sb = new StringBuilder();
        if ((flags & 1) != 0) {
            sb.append("JE|");
        }
        if ((flags & 2) != 0) {
            sb.append("NE|");
        }
        if ((flags & 4) != 0) {
            sb.append("ANR_BC|");
        }
        if ((flags & 8) != 0) {
            sb.append("ANR_SRV|");
        }
        if ((flags & 16) != 0) {
            sb.append("ANR_MAIN|");
        }
        if ((flags & 32) != 0) {
            sb.append("ANR_OTHER|");
        }
        if ((flags & 64) != 0) {
            sb.append("MEM_JAVA|");
        }
        if ((flags & 128) != 0) {
            sb.append("MEM_NATIVE|");
        }
        if ((flags & 256) != 0) {
            sb.append("MEM_OTHER|");
        }
        if ((flags & 512) != 0) {
            sb.append("POWER|");
        }
        if ((flags & Consts.ProcessStates.FOCUS) != 0) {
            sb.append("SPS_LATE|");
        }
        if ((flags & Consts.ProcessStates.VIRTUAL_DISPLAY) != 0) {
            sb.append("SPA_LATE|");
        }
        if ((flags & 4096) != 0) {
            sb.append("INTER|");
        }
        if ((flags & EmergencyBroadcastManager.FLAG_RECEIVER_KEYAPP) != 0) {
            sb.append("CONFIG|");
        }
        if ((flags & 16384) != 0) {
            sb.append("DEBUG|");
        }
        if ((32768 & flags) != 0) {
            sb.append("OTHER|");
        }
        return sb.toString();
    }

    private BadPackageManager() {
        this.mErrorRecordList = new ErrorRecordList();
        this.mBadPackagesList = new BadPackageQuickAccess();
        this.mLock = new Object();
        this.mBadPackageListSync = new BadPackageList();
        this.mSyncLock = new Object();
        HandlerThread thread = new HandlerThread("sps-bad-packages");
        thread.start();
        this.mSaverHandler = new H(thread.getLooper());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Instance {
        private static final BadPackageManager INSTANCE = new BadPackageManager();

        private Instance() {
        }
    }

    public static BadPackageManager getInstance() {
        return Instance.INSTANCE;
    }
}