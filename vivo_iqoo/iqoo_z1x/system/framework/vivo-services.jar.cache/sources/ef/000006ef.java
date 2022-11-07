package com.vivo.services.rms;

import com.vivo.common.utils.VLog;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import libcore.io.IoUtils;

/* loaded from: classes.dex */
public class SysFsModifier {
    private static final String TAG = "SysFsModifier";
    private static final HashMap<String, String> RESTOREMAP = new HashMap<>();
    private static final byte[] BUFFER = new byte[128];
    private static final Object LOCK = new Object();

    public static boolean modify(ArrayList<String> fileNames, ArrayList<String> values) {
        synchronized (LOCK) {
            if (fileNames == null || values == null) {
                return false;
            }
            int size = fileNames.size();
            if (values.size() == size && size > 0) {
                for (int i = 0; i < size; i++) {
                    if (!modifyLocked(fileNames.get(i), values.get(i))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    public static boolean modifyLocked(String fileName, String value) {
        String oldValue;
        if (fileName == null || value == null) {
            return false;
        }
        File file = new File(fileName);
        if (file.exists() && (oldValue = readSysFs(file)) != null) {
            if (oldValue.equals(value)) {
                return true;
            }
            if (writeSysFs(file, value)) {
                if (!RESTOREMAP.containsKey(fileName)) {
                    RESTOREMAP.put(fileName, oldValue);
                }
                return true;
            }
        }
        return false;
    }

    public static void restore(ArrayList<String> fileNames) {
        synchronized (LOCK) {
            ArrayList<String> removes = new ArrayList<>();
            Iterator<String> it = fileNames.iterator();
            while (it.hasNext()) {
                String fileName = it.next();
                if (RESTOREMAP.containsKey(fileName)) {
                    File file = new File(fileName);
                    if (file.exists() && writeSysFs(file, RESTOREMAP.get(fileName))) {
                        removes.add(fileName);
                    }
                }
            }
            Iterator<String> it2 = removes.iterator();
            while (it2.hasNext()) {
                String key = it2.next();
                RESTOREMAP.remove(key);
            }
        }
    }

    public static void restore() {
        synchronized (LOCK) {
            ArrayList<String> removes = new ArrayList<>();
            for (String fileName : RESTOREMAP.keySet()) {
                File file = new File(fileName);
                if (file.exists() && writeSysFs(file, RESTOREMAP.get(fileName))) {
                    removes.add(fileName);
                }
            }
            Iterator<String> it = removes.iterator();
            while (it.hasNext()) {
                String key = it.next();
                RESTOREMAP.remove(key);
            }
            if (!RESTOREMAP.isEmpty()) {
                for (String fileName2 : RESTOREMAP.keySet()) {
                    VLog.e(TAG, String.format("restore fail  %s %s", fileName2, RESTOREMAP.get(fileName2)));
                }
            }
        }
    }

    private static String readSysFs(File file) {
        FileInputStream in = null;
        String result = null;
        try {
            try {
                in = new FileInputStream(file);
                int len = in.read(BUFFER);
                if (len > 0) {
                    result = new String(BUFFER, 0, len);
                }
            } catch (Exception e) {
                VLog.e(TAG, String.format("readSysFs fail %s %s", file.getName(), e.toString()));
            }
            return result;
        } finally {
            IoUtils.closeQuietly(in);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:10:0x0036, code lost:
        if (r0 == null) goto L6;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private static boolean writeSysFs(java.io.File r8, java.lang.String r9) {
        /*
            r0 = 0
            r1 = 0
            java.io.PrintWriter r2 = new java.io.PrintWriter     // Catch: java.lang.Throwable -> L16 java.lang.Exception -> L18
            java.io.FileOutputStream r3 = new java.io.FileOutputStream     // Catch: java.lang.Throwable -> L16 java.lang.Exception -> L18
            r3.<init>(r8)     // Catch: java.lang.Throwable -> L16 java.lang.Exception -> L18
            r2.<init>(r3)     // Catch: java.lang.Throwable -> L16 java.lang.Exception -> L18
            r0 = r2
            r0.write(r9)     // Catch: java.lang.Throwable -> L16 java.lang.Exception -> L18
            r1 = 1
        L12:
            r0.close()
            goto L39
        L16:
            r2 = move-exception
            goto L3a
        L18:
            r2 = move-exception
            java.lang.String r3 = "SysFsModifier"
            java.lang.String r4 = "writeSysFs fail %s %s"
            r5 = 2
            java.lang.Object[] r5 = new java.lang.Object[r5]     // Catch: java.lang.Throwable -> L16
            r6 = 0
            java.lang.String r7 = r8.getName()     // Catch: java.lang.Throwable -> L16
            r5[r6] = r7     // Catch: java.lang.Throwable -> L16
            r6 = 1
            java.lang.String r7 = r2.toString()     // Catch: java.lang.Throwable -> L16
            r5[r6] = r7     // Catch: java.lang.Throwable -> L16
            java.lang.String r4 = java.lang.String.format(r4, r5)     // Catch: java.lang.Throwable -> L16
            com.vivo.common.utils.VLog.e(r3, r4)     // Catch: java.lang.Throwable -> L16
            if (r0 == 0) goto L39
            goto L12
        L39:
            return r1
        L3a:
            if (r0 == 0) goto L3f
            r0.close()
        L3f:
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.rms.SysFsModifier.writeSysFs(java.io.File, java.lang.String):boolean");
    }
}