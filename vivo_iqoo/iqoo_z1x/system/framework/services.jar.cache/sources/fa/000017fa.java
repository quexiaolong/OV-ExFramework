package com.android.server.power.batterysaver;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Slog;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.IoThread;
import com.android.server.wm.ActivityTaskManagerService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParserException;

/* loaded from: classes2.dex */
public class FileUpdater {
    private static final boolean DEBUG = false;
    private static final String PROP_SKIP_WRITE = "debug.batterysaver.no_write_files";
    private static final String TAG = "BatterySaverController";
    private static final String TAG_DEFAULT_ROOT = "defaults";
    private final int MAX_RETRIES;
    private final long RETRY_INTERVAL_MS;
    private final Context mContext;
    private final ArrayMap<String, String> mDefaultValues;
    private Runnable mHandleWriteOnHandlerRunnable;
    private final Handler mHandler;
    private final Object mLock;
    private final ArrayMap<String, String> mPendingWrites;
    private int mRetries;

    public FileUpdater(Context context) {
        this(context, IoThread.get().getLooper(), 10, ActivityTaskManagerService.KEY_DISPATCHING_TIMEOUT_MS);
    }

    FileUpdater(Context context, Looper looper, int maxRetries, int retryIntervalMs) {
        this.mLock = new Object();
        this.mPendingWrites = new ArrayMap<>();
        this.mDefaultValues = new ArrayMap<>();
        this.mRetries = 0;
        this.mHandleWriteOnHandlerRunnable = new Runnable() { // from class: com.android.server.power.batterysaver.-$$Lambda$FileUpdater$NUmipjKCJwbgmFbIcGS3uaz3QFk
            @Override // java.lang.Runnable
            public final void run() {
                FileUpdater.this.lambda$new$0$FileUpdater();
            }
        };
        this.mContext = context;
        this.mHandler = new Handler(looper);
        this.MAX_RETRIES = maxRetries;
        this.RETRY_INTERVAL_MS = retryIntervalMs;
    }

    public void systemReady(boolean runtimeRestarted) {
        synchronized (this.mLock) {
            if (runtimeRestarted) {
                if (loadDefaultValuesLocked()) {
                    Slog.d(TAG, "Default values loaded after runtime restart; writing them...");
                    restoreDefault();
                }
            } else {
                injectDefaultValuesFilename().delete();
            }
        }
    }

    public void writeFiles(ArrayMap<String, String> fileValues) {
        synchronized (this.mLock) {
            for (int i = fileValues.size() - 1; i >= 0; i--) {
                String file = fileValues.keyAt(i);
                String value = fileValues.valueAt(i);
                this.mPendingWrites.put(file, value);
            }
            this.mRetries = 0;
            this.mHandler.removeCallbacks(this.mHandleWriteOnHandlerRunnable);
            this.mHandler.post(this.mHandleWriteOnHandlerRunnable);
        }
    }

    public void restoreDefault() {
        synchronized (this.mLock) {
            this.mPendingWrites.clear();
            writeFiles(this.mDefaultValues);
        }
    }

    private String getKeysString(Map<String, String> source) {
        return new ArrayList(source.keySet()).toString();
    }

    private ArrayMap<String, String> cloneMap(ArrayMap<String, String> source) {
        return new ArrayMap<>(source);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: handleWriteOnHandler */
    public void lambda$new$0$FileUpdater() {
        synchronized (this.mLock) {
            if (this.mPendingWrites.size() == 0) {
                return;
            }
            ArrayMap<String, String> writes = cloneMap(this.mPendingWrites);
            boolean needRetry = false;
            int size = writes.size();
            for (int i = 0; i < size; i++) {
                String file = writes.keyAt(i);
                String value = writes.valueAt(i);
                if (ensureDefaultLoaded(file)) {
                    try {
                        injectWriteToFile(file, value);
                        removePendingWrite(file);
                    } catch (IOException e) {
                        needRetry = true;
                    }
                }
            }
            if (needRetry) {
                scheduleRetry();
            }
        }
    }

    private void removePendingWrite(String file) {
        synchronized (this.mLock) {
            this.mPendingWrites.remove(file);
        }
    }

    private void scheduleRetry() {
        synchronized (this.mLock) {
            if (this.mPendingWrites.size() == 0) {
                return;
            }
            int i = this.mRetries + 1;
            this.mRetries = i;
            if (i > this.MAX_RETRIES) {
                doWtf("Gave up writing files: " + getKeysString(this.mPendingWrites));
                return;
            }
            this.mHandler.removeCallbacks(this.mHandleWriteOnHandlerRunnable);
            this.mHandler.postDelayed(this.mHandleWriteOnHandlerRunnable, this.RETRY_INTERVAL_MS);
        }
    }

    private boolean ensureDefaultLoaded(String file) {
        synchronized (this.mLock) {
            if (this.mDefaultValues.containsKey(file)) {
                return true;
            }
            try {
                String originalValue = injectReadFromFileTrimmed(file);
                synchronized (this.mLock) {
                    this.mDefaultValues.put(file, originalValue);
                    saveDefaultValuesLocked();
                }
                return true;
            } catch (IOException e) {
                injectWtf("Unable to read from file", e);
                removePendingWrite(file);
                return false;
            }
        }
    }

    String injectReadFromFileTrimmed(String file) throws IOException {
        return IoUtils.readFileAsString(file).trim();
    }

    void injectWriteToFile(String file, String value) throws IOException {
        if (injectShouldSkipWrite()) {
            Slog.i(TAG, "Skipped writing to '" + file + "'");
            return;
        }
        try {
            FileWriter out = new FileWriter(file);
            out.write(value);
            out.close();
        } catch (IOException | RuntimeException e) {
            Slog.w(TAG, "Failed writing '" + value + "' to '" + file + "': " + e.getMessage());
            throw e;
        }
    }

    private void saveDefaultValuesLocked() {
        AtomicFile file = new AtomicFile(injectDefaultValuesFilename());
        FileOutputStream outs = null;
        try {
            file.getBaseFile().getParentFile().mkdirs();
            outs = file.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(outs, StandardCharsets.UTF_8.name());
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.startTag(null, TAG_DEFAULT_ROOT);
            XmlUtils.writeMapXml(this.mDefaultValues, fastXmlSerializer, (XmlUtils.WriteMapCallback) null);
            fastXmlSerializer.endTag(null, TAG_DEFAULT_ROOT);
            fastXmlSerializer.endDocument();
            file.finishWrite(outs);
        } catch (IOException | RuntimeException | XmlPullParserException e) {
            Slog.e(TAG, "Failed to write to file " + file.getBaseFile(), e);
            file.failWrite(outs);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:0x003c, code lost:
        android.util.Slog.e(com.android.server.power.batterysaver.FileUpdater.TAG, "Invalid root tag: " + r9);
     */
    /* JADX WARN: Code restructure failed: missing block: B:16:0x0051, code lost:
        if (r5 == null) goto L26;
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x0053, code lost:
        r5.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x0056, code lost:
        return false;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    boolean loadDefaultValuesLocked() {
        /*
            r13 = this;
            java.lang.String r0 = "BatterySaverController"
            android.util.AtomicFile r1 = new android.util.AtomicFile
            java.io.File r2 = r13.injectDefaultValuesFilename()
            r1.<init>(r2)
            r2 = 0
            r3 = 0
            r4 = 1
            java.io.FileInputStream r5 = r1.openRead()     // Catch: java.lang.Throwable -> L72 java.io.FileNotFoundException -> L8c
            org.xmlpull.v1.XmlPullParser r6 = android.util.Xml.newPullParser()     // Catch: java.lang.Throwable -> L66
            java.nio.charset.Charset r7 = java.nio.charset.StandardCharsets.UTF_8     // Catch: java.lang.Throwable -> L66
            java.lang.String r7 = r7.name()     // Catch: java.lang.Throwable -> L66
            r6.setInput(r5, r7)     // Catch: java.lang.Throwable -> L66
        L1f:
            int r7 = r6.next()     // Catch: java.lang.Throwable -> L66
            r8 = r7
            if (r7 == r4) goto L60
            r7 = 2
            if (r8 == r7) goto L2a
            goto L1f
        L2a:
            int r7 = r6.getDepth()     // Catch: java.lang.Throwable -> L66
            java.lang.String r9 = r6.getName()     // Catch: java.lang.Throwable -> L66
            java.lang.String r10 = "defaults"
            if (r7 != r4) goto L57
            boolean r10 = r10.equals(r9)     // Catch: java.lang.Throwable -> L66
            if (r10 != 0) goto L1f
            java.lang.StringBuilder r10 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L66
            r10.<init>()     // Catch: java.lang.Throwable -> L66
            java.lang.String r11 = "Invalid root tag: "
            r10.append(r11)     // Catch: java.lang.Throwable -> L66
            r10.append(r9)     // Catch: java.lang.Throwable -> L66
            java.lang.String r10 = r10.toString()     // Catch: java.lang.Throwable -> L66
            android.util.Slog.e(r0, r10)     // Catch: java.lang.Throwable -> L66
            if (r5 == 0) goto L56
            r5.close()     // Catch: java.lang.Throwable -> L72 java.lang.Throwable -> L72 java.lang.Throwable -> L72 java.io.FileNotFoundException -> L8c
        L56:
            return r3
        L57:
            java.lang.String[] r11 = new java.lang.String[r4]     // Catch: java.lang.Throwable -> L66
            r12 = 0
            android.util.ArrayMap r10 = com.android.internal.util.XmlUtils.readThisArrayMapXml(r6, r10, r11, r12)     // Catch: java.lang.Throwable -> L66
            r2 = r10
            goto L1f
        L60:
            if (r5 == 0) goto L8e
            r5.close()     // Catch: java.lang.Throwable -> L72 java.lang.Throwable -> L72 java.lang.Throwable -> L72 java.io.FileNotFoundException -> L8c
            goto L8e
        L66:
            r6 = move-exception
            if (r5 == 0) goto L71
            r5.close()     // Catch: java.lang.Throwable -> L6d
            goto L71
        L6d:
            r7 = move-exception
            r6.addSuppressed(r7)     // Catch: java.lang.Throwable -> L72 java.lang.Throwable -> L72 java.lang.Throwable -> L72 java.io.FileNotFoundException -> L8c
        L71:
            throw r6     // Catch: java.lang.Throwable -> L72 java.lang.Throwable -> L72 java.lang.Throwable -> L72 java.io.FileNotFoundException -> L8c
        L72:
            r5 = move-exception
            java.lang.StringBuilder r6 = new java.lang.StringBuilder
            r6.<init>()
            java.lang.String r7 = "Failed to read file "
            r6.append(r7)
            java.io.File r7 = r1.getBaseFile()
            r6.append(r7)
            java.lang.String r6 = r6.toString()
            android.util.Slog.e(r0, r6, r5)
            goto L8f
        L8c:
            r0 = move-exception
            r2 = 0
        L8e:
        L8f:
            if (r2 == 0) goto L9c
            android.util.ArrayMap<java.lang.String, java.lang.String> r0 = r13.mDefaultValues
            r0.clear()
            android.util.ArrayMap<java.lang.String, java.lang.String> r0 = r13.mDefaultValues
            r0.putAll(r2)
            return r4
        L9c:
            return r3
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.power.batterysaver.FileUpdater.loadDefaultValuesLocked():boolean");
    }

    private void doWtf(String message) {
        injectWtf(message, null);
    }

    void injectWtf(String message, Throwable e) {
        Slog.wtf(TAG, message, e);
    }

    File injectDefaultValuesFilename() {
        File dir = new File(Environment.getDataSystemDirectory(), "battery-saver");
        dir.mkdirs();
        return new File(dir, "default-values.xml");
    }

    boolean injectShouldSkipWrite() {
        return SystemProperties.getBoolean(PROP_SKIP_WRITE, false);
    }

    ArrayMap<String, String> getDefaultValuesForTest() {
        return this.mDefaultValues;
    }
}