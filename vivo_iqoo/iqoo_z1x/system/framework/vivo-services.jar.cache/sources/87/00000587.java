package com.android.server.wm;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.FileObserver;
import android.os.Handler;
import android.os.SystemProperties;
import android.util.ArrayMap;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.vivo.face.common.data.Constants;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoWindowPolicyControllerImpl implements IVivoWindowPolicyController {
    private Context mContext;
    private FileObserver mFileObserver;
    private VivoRatioControllerUtilsImpl mVivoRatioControllerUtils;
    private static ArrayMap<String, String> sNavPolicyList = new ArrayMap<>();
    private static ArrayMap<String, String> sNavFixColorList = new ArrayMap<>();
    private static ArrayMap<String, String> sAlienScreenPolicyList = new ArrayMap<>();
    private static ArrayMap<String, String> sPaddingPolicyList = new ArrayMap<>();
    private static ArrayMap<String, String> sInternalFlagList = new ArrayMap<>();
    private static ArrayMap<String, String> sHomeIndicatorList = new ArrayMap<>();
    private static boolean sEnableNavImmerse = false;
    private static VivoWindowPolicyControllerImpl sInstance = null;
    private final String TAG = "VivoWindowPolicyControllerImpl";
    private boolean isSecureSurpport = false;
    private boolean overSeaProduct = SystemProperties.get("ro.vivo.product.overseas", "no").equals("yes");
    private Handler mIoHandler = new Handler();
    private final boolean DBG = SystemProperties.getBoolean("persist.vivopolicy.debug", false);
    public String defaultAlienScreenPolicy = "NOTKEEPFULL";
    private Runnable retriveFileRunnable = new Runnable() { // from class: com.android.server.wm.VivoWindowPolicyControllerImpl.1
        @Override // java.lang.Runnable
        public void run() {
            if (VivoWindowPolicyControllerImpl.this.DBG) {
                VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:retriveFileRunnable!");
            }
            File serverFile = new File("/data/bbkcore/vivo_window_policy.xml");
            VivoWindowPolicyControllerImpl.this.readXmlFileFromUnifiedConfig("WindowPolicy", "1", "1.0", "WindowPolicy", serverFile);
        }
    };
    private Runnable reObserverListRunnable = new Runnable() { // from class: com.android.server.wm.VivoWindowPolicyControllerImpl.2
        @Override // java.lang.Runnable
        public void run() {
            if (VivoWindowPolicyControllerImpl.this.DBG) {
                VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:reObserverListRunnable ");
            }
            VivoWindowPolicyControllerImpl.this.decryptAndReadList("/data/bbkcore/vivo_window_policy.xml");
            VivoWindowPolicyControllerImpl.this.observeFile();
        }
    };

    public VivoWindowPolicyControllerImpl(Context context) {
        this.mContext = null;
        this.mContext = context;
    }

    public static synchronized VivoWindowPolicyControllerImpl getInstance(Context context) {
        VivoWindowPolicyControllerImpl vivoWindowPolicyControllerImpl;
        synchronized (VivoWindowPolicyControllerImpl.class) {
            if (sInstance == null) {
                sInstance = new VivoWindowPolicyControllerImpl(context);
            }
            vivoWindowPolicyControllerImpl = sInstance;
        }
        return vivoWindowPolicyControllerImpl;
    }

    public void init() {
        if (this.DBG) {
            VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:init");
        }
        checkSecureConfig();
        sNavPolicyList.clear();
        sNavFixColorList.clear();
        sAlienScreenPolicyList.clear();
        sPaddingPolicyList.clear();
        sInternalFlagList.clear();
        sHomeIndicatorList.clear();
        boolean defaultRes = false;
        boolean backupRes = decryptAndReadList("/data/bbkcore/vivo_window_policy.xml");
        if (!backupRes) {
            defaultRes = decryptAndReadList("/system/etc/vivo_window_policy.xml");
        }
        if (this.isSecureSurpport) {
            if (this.DBG) {
                VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:try to retrive from UCS on Boot");
            }
            this.mIoHandler.removeCallbacks(this.retriveFileRunnable);
            postRetriveFile();
        }
        if (this.DBG) {
            VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:defaultRes=" + defaultRes + " backupRes=" + backupRes);
        }
        observeFile();
        this.mVivoRatioControllerUtils = VivoRatioControllerUtilsImpl.getInstance();
    }

    public void postRetriveFile() {
        this.mIoHandler.postDelayed(this.retriveFileRunnable, 10000L);
    }

    private void checkSecureConfig() {
        String ucSecure = SystemProperties.get("persist.vivo.unifiedconfig.sec", "no");
        if (this.DBG) {
            VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:checkSecureConfig ucSecure=" + ucSecure);
        }
        if (ucSecure != null && !Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(ucSecure) && ucSecure.equals("yes")) {
            this.isSecureSurpport = true;
        }
    }

    /* JADX WARN: Not initialized variable reg: 19, insn: 0x032f: MOVE  (r6 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY]) = (r19 I:??[int, float, boolean, short, byte, char, OBJECT, ARRAY] A[D('success' boolean)]), block:B:176:0x032e */
    /* JADX WARN: Removed duplicated region for block: B:155:0x0302 A[Catch: Exception -> 0x0306, TRY_ENTER, TRY_LEAVE, TryCatch #17 {Exception -> 0x0306, blocks: (B:155:0x0302, B:147:0x02e5), top: B:190:0x0042 }] */
    /* JADX WARN: Removed duplicated region for block: B:164:0x0318 A[Catch: all -> 0x032d, TRY_LEAVE, TryCatch #10 {all -> 0x032d, blocks: (B:65:0x018b, B:70:0x01c1, B:73:0x01c9, B:75:0x01cf, B:77:0x01d6, B:80:0x01de, B:82:0x01e4, B:84:0x01eb, B:87:0x01f3, B:90:0x01fb, B:92:0x0203, B:94:0x0207, B:99:0x022d, B:102:0x0235, B:104:0x023b, B:106:0x0242, B:109:0x024a, B:111:0x0250, B:113:0x0257, B:116:0x025f, B:118:0x0265, B:139:0x02c5, B:96:0x0221, B:153:0x02fd, B:162:0x0314, B:164:0x0318), top: B:186:0x0042 }] */
    /* JADX WARN: Removed duplicated region for block: B:182:0x0333 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:187:0x0323 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:204:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private boolean readXmlFile(java.io.InputStream r23) {
        /*
            Method dump skipped, instructions count: 825
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoWindowPolicyControllerImpl.readXmlFile(java.io.InputStream):boolean");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void readXmlFileFromUnifiedConfig(String module, String type, String version, String identifier, File file) {
        Cursor cursor;
        ContentResolver resolver = this.mContext.getContentResolver();
        String[] selectionArgs = {module, type, version, identifier};
        Cursor cursor2 = null;
        byte[] filecontent = null;
        try {
            try {
                try {
                    cursor = resolver.query(Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs"), null, null, selectionArgs, null);
                    if (cursor != null) {
                        cursor.moveToFirst();
                        if (cursor.getCount() > 0) {
                            while (!cursor.isAfterLast()) {
                                filecontent = cursor.getBlob(cursor.getColumnIndex("filecontent"));
                                cursor.moveToNext();
                                if (this.DBG) {
                                    VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:content = " + new String(filecontent));
                                }
                            }
                        } else if (this.DBG) {
                            VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:no data!");
                        }
                    }
                } catch (Exception e) {
                }
            } catch (Exception e2) {
                VSlog.e("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:open database error! " + e2.fillInStackTrace());
                if (0 != 0) {
                    cursor2.close();
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            if (filecontent != null) {
                String result = new String(filecontent);
                writeByBufferedWriter(result, file);
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    cursor2.close();
                } catch (Exception e3) {
                }
            }
            throw th;
        }
    }

    private void writeByBufferedWriter(String string, File desFile) {
        if (string == null) {
            return;
        }
        synchronized (sNavPolicyList) {
            BufferedWriter bWriter = null;
            try {
                try {
                    if (desFile.exists() && desFile.isFile()) {
                        desFile.delete();
                    }
                    if (this.DBG) {
                        VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:writeByBufferedWriter createNewFile");
                    }
                    desFile.createNewFile();
                    bWriter = new BufferedWriter(new FileWriter(desFile));
                    bWriter.write(string);
                    if (this.DBG) {
                        VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:writeByBufferedWriter: " + string);
                    }
                    bWriter.close();
                } catch (Exception e) {
                    if (this.DBG) {
                        VSlog.e("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:Buffered write failed! " + e.fillInStackTrace());
                    }
                    if (bWriter != null) {
                        bWriter.close();
                    }
                }
            } catch (Exception e2) {
            }
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:19:0x003c, code lost:
        if (0 != 0) goto L29;
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x0054, code lost:
        if (r5.DBG == false) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x0056, code lost:
        vivo.util.VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:parseAndAddContent end " + r7);
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x006a, code lost:
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:47:?, code lost:
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private void parseAndAddContent(java.io.StringReader r6, java.util.ArrayList<java.lang.String> r7, boolean r8) {
        /*
            r5 = this;
            boolean r0 = r5.DBG
            java.lang.String r1 = "VivoWindowPolicyControllerImpl"
            if (r0 == 0) goto Lb
            java.lang.String r0 = "DEBUG_WINDOWPOLICY:parseAndAddContent start"
            vivo.util.VSlog.d(r1, r0)
        Lb:
            if (r7 != 0) goto L14
            java.util.ArrayList r0 = new java.util.ArrayList
            r0.<init>()
            r7 = r0
            goto L17
        L14:
            r7.clear()
        L17:
            r0 = 0
            java.io.BufferedReader r2 = new java.io.BufferedReader     // Catch: java.lang.Throwable -> L3f java.lang.Exception -> L41
            r2.<init>(r6)     // Catch: java.lang.Throwable -> L3f java.lang.Exception -> L41
            r0 = r2
            java.lang.String r2 = r0.readLine()     // Catch: java.lang.Throwable -> L3f java.lang.Exception -> L41
            java.lang.String r3 = ""
        L24:
            if (r2 == 0) goto L38
            r3 = r2
            if (r3 == 0) goto L32
            boolean r4 = r7.contains(r3)     // Catch: java.lang.Throwable -> L3f java.lang.Exception -> L41
            if (r4 != 0) goto L32
            r7.add(r3)     // Catch: java.lang.Throwable -> L3f java.lang.Exception -> L41
        L32:
            java.lang.String r4 = r0.readLine()     // Catch: java.lang.Throwable -> L3f java.lang.Exception -> L41
            r2 = r4
            goto L24
        L38:
            r0.close()     // Catch: java.lang.Throwable -> L3f java.lang.Exception -> L41
            r0 = 0
            if (r0 == 0) goto L52
            goto L51
        L3f:
            r1 = move-exception
            goto L48
        L41:
            r2 = move-exception
            if (r0 == 0) goto L4e
            r0.close()     // Catch: java.lang.Throwable -> L3f java.lang.Exception -> L4c
            goto L4e
        L48:
            if (r0 == 0) goto L4b
            r0 = 0
        L4b:
            throw r1
        L4c:
            r3 = move-exception
            goto L4f
        L4e:
        L4f:
            if (r0 == 0) goto L52
        L51:
            r0 = 0
        L52:
            boolean r2 = r5.DBG
            if (r2 == 0) goto L6a
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "DEBUG_WINDOWPOLICY:parseAndAddContent end "
            r2.append(r3)
            r2.append(r7)
            java.lang.String r2 = r2.toString()
            vivo.util.VSlog.d(r1, r2)
        L6a:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoWindowPolicyControllerImpl.parseAndAddContent(java.io.StringReader, java.util.ArrayList, boolean):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean decryptAndReadList(String srcPath) {
        boolean res = false;
        synchronized (sNavPolicyList) {
            if (this.DBG) {
                VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY: start decryptAndReadList  from " + srcPath + " before sNavPolicyList=" + sNavPolicyList + " sAlienScreenPolicyList=" + sAlienScreenPolicyList + " sPaddingPolicyList=" + sPaddingPolicyList + " sInternalFlagList=" + sInternalFlagList + " sHomeIndicatorList" + sHomeIndicatorList);
            }
            try {
                File file = new File(srcPath);
                String result = readByBufferedReader(file);
                if (result != null) {
                    if (this.DBG) {
                        VSlog.d("VivoWindowPolicyControllerImpl", "Decode = " + result);
                    }
                    res = readXmlFile(new ByteArrayInputStream(result.getBytes()));
                }
            } catch (Exception e) {
                VSlog.e("VivoWindowPolicyControllerImpl", "decode data error! " + e.fillInStackTrace());
            }
            if (this.DBG) {
                VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY: after decryptAndReadList sNavPolicyList=" + sNavPolicyList);
            }
            if (this.DBG) {
                VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY: after decryptAndReadList  sAlienScreenPolicyList=" + sAlienScreenPolicyList + " sPaddingPolicyList=" + sPaddingPolicyList + " sInternalFlagList=" + sInternalFlagList + " sHomeIndicatorList=" + sHomeIndicatorList);
            }
        }
        return res;
    }

    private String readByBufferedReader(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        BufferedReader bReader = null;
        StringBuffer buffer = null;
        try {
            try {
                try {
                    bReader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String line = bReader.readLine();
                        if (line == null) {
                            break;
                        }
                        if (buffer == null) {
                            buffer = new StringBuffer();
                        }
                        if (buffer != null) {
                            buffer.append(line);
                            buffer.append("\n");
                        }
                    }
                    bReader.close();
                } catch (Throwable th) {
                    if (bReader != null) {
                        try {
                            bReader.close();
                        } catch (Exception e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
            }
        } catch (Exception e3) {
            VSlog.e("VivoWindowPolicyControllerImpl", "Buffered Reader failed! " + e3.fillInStackTrace());
            if (bReader != null) {
                bReader.close();
            }
        }
        if (buffer != null) {
            return buffer.toString();
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void observeFile() {
        if (this.DBG) {
            VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:observeShieldListServer set");
        }
        FileObserver fileObserver = this.mFileObserver;
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
        if (this.DBG) {
            VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:observeShieldListServer set fileToObserve:/data/bbkcore/vivo_window_policy.xml");
        }
        File file = new File("/data/bbkcore/vivo_window_policy.xml");
        try {
            if (!file.exists()) {
                if (this.DBG) {
                    VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:observeShieldListServer file not exist ,create new one");
                }
                file.createNewFile();
            }
        } catch (Exception e) {
            if (this.DBG) {
                VSlog.e("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:create file error");
            }
        }
        FileObserver fileObserver2 = new FileObserver("/data/bbkcore/vivo_window_policy.xml") { // from class: com.android.server.wm.VivoWindowPolicyControllerImpl.3
            @Override // android.os.FileObserver
            public void onEvent(int event, String path) {
                if (VivoWindowPolicyControllerImpl.this.DBG) {
                    VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:observeShieldListServer fired event=" + event + " path=" + path);
                }
                if (8 == event) {
                    VivoWindowPolicyControllerImpl.this.decryptAndReadList("/data/bbkcore/vivo_window_policy.xml");
                }
                if (event == 1024 || event == 512) {
                    if (VivoWindowPolicyControllerImpl.this.DBG) {
                        VSlog.d("VivoWindowPolicyControllerImpl", "DEBUG_WINDOWPOLICY:observeShieldListServer file deleted");
                    }
                    VivoWindowPolicyControllerImpl.this.mIoHandler.removeCallbacks(VivoWindowPolicyControllerImpl.this.reObserverListRunnable);
                    VivoWindowPolicyControllerImpl.this.mIoHandler.postDelayed(VivoWindowPolicyControllerImpl.this.reObserverListRunnable, 2000L);
                }
            }
        };
        this.mFileObserver = fileObserver2;
        fileObserver2.startWatching();
    }

    public String getPolicyAlienScreen(String pkg) {
        String res = sAlienScreenPolicyList.get(pkg);
        String tempRes = this.mVivoRatioControllerUtils.getAlienScreenRatioPolicyForPackage(pkg);
        if (tempRes != null && !tempRes.equals(Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK)) {
            res = tempRes;
        }
        return res != null ? res : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public String getPolicyPaddingColor(String pkg) {
        String res = sPaddingPolicyList.get(pkg);
        return res != null ? res : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public String getPolicyNavColor(String pkg) {
        String res = sNavPolicyList.get(pkg);
        return res != null ? res : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public String getFixNavColor(String pkg) {
        String res = sNavFixColorList.get(pkg);
        return res != null ? res : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public String getInternalFlag(String pkg) {
        String res = sInternalFlagList.get(pkg);
        return res != null ? res : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public String getPolicyHomeIndicator(String pkg) {
        String res = sHomeIndicatorList.get(pkg);
        return res != null ? res : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public String getDefaultAlienScreenPolicy() {
        return this.defaultAlienScreenPolicy;
    }
}