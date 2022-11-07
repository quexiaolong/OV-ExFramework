package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import com.android.server.UnifiedConfigThread;
import com.vivo.services.backup.util.VivoBackupCommonUtil;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class AlertWindowNotificationController {
    private static final String ACTION_UPDATE_CONFIG_CENTER = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_PermissionManager";
    private static final String IDENTIFY = "bgstart_floatwindow";
    private static final String TAG = "AlertWindowNotificationController";
    private static final String TAG_ALLOWED_APPS = "allowed_apps";
    private static final String TAG_FLOATING_WINDOW_APPS = "floatwindow-apps";
    private static final String TAG_ITEM = "item";
    private static final String TAG_NOT_SHOWING_APPS = "not_showing_apps";
    private static final Uri UNIFIED_CONFIG_CENTER_URI = Uri.parse("content://com.vivo.abe.unifiedconfig.provider/configs");
    private static final int UPDATE_WHITE_LIST_WHEN_BOOT_DELAY = 30000;
    private Context mContext;
    private Handler mIoHandler;
    private ArrayList<String> mWhiteList = new ArrayList<>();
    private Runnable mUpdateWhiteListRunnable = new Runnable() { // from class: com.android.server.wm.AlertWindowNotificationController.1
        @Override // java.lang.Runnable
        public void run() {
            if (WindowManagerDebugConfig.DEBUG) {
                VSlog.d(AlertWindowNotificationController.TAG, "run update white list");
            }
            AlertWindowNotificationController.this.updateWhiteList();
        }
    };

    public AlertWindowNotificationController(Context context) {
        this.mContext = context;
        registerBroadcast();
        this.mIoHandler = UnifiedConfigThread.getHandler();
    }

    public void systemReady() {
        this.mIoHandler.postDelayed(this.mUpdateWhiteListRunnable, VivoBackupCommonUtil.TIMEOUT_MONITOR_INTERVAL);
    }

    public boolean shouldShowNotification(String pkg) {
        synchronized (this) {
            if (this.mWhiteList.contains(pkg)) {
                return false;
            }
            return true;
        }
    }

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_CONFIG_CENTER);
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.wm.AlertWindowNotificationController.2
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                if (AlertWindowNotificationController.ACTION_UPDATE_CONFIG_CENTER.equals(intent.getAction())) {
                    if (WindowManagerDebugConfig.DEBUG) {
                        VSlog.d(AlertWindowNotificationController.TAG, "receive ACTION_UPDATE_CONFIG_CENTER");
                    }
                    Bundle extra = intent.getExtras();
                    String[] identifiers = (String[]) extra.get("identifiers");
                    if (identifiers != null && identifiers.length > 0) {
                        for (String identify : identifiers) {
                            if (AlertWindowNotificationController.IDENTIFY.equals(identify)) {
                                AlertWindowNotificationController.this.mIoHandler.removeCallbacks(AlertWindowNotificationController.this.mUpdateWhiteListRunnable);
                                AlertWindowNotificationController.this.mIoHandler.post(AlertWindowNotificationController.this.mUpdateWhiteListRunnable);
                                return;
                            }
                        }
                    }
                }
            }
        }, filter);
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:0x006a, code lost:
        if (0 == 0) goto L12;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x006f, code lost:
        if (com.android.server.wm.WindowManagerDebugConfig.DEBUG == false) goto L15;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0071, code lost:
        vivo.util.VSlog.d(com.android.server.wm.AlertWindowNotificationController.TAG, "id is : " + r10 + " ; targetIdentifier is : " + r12 + " ; fileVersion is : " + r13 + " ; content is : " + r1.toString());
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x00a5, code lost:
        return r1.toString();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private java.lang.String getContentFromConfigCenter() {
        /*
            r14 = this;
            java.lang.String r0 = "AlertWindowNotificationController"
            java.lang.StringBuffer r1 = new java.lang.StringBuffer
            r1.<init>()
            android.content.Context r2 = r14.mContext
            android.content.ContentResolver r2 = r2.getContentResolver()
            java.lang.String r3 = "PermissionManager"
            java.lang.String r4 = "1"
            java.lang.String r5 = "201609231546"
            java.lang.String r6 = "bgstart_floatwindow"
            java.lang.String[] r7 = new java.lang.String[]{r3, r4, r5, r6}
            r9 = 0
            r10 = 0
            r11 = 0
            java.lang.String r12 = ""
            java.lang.String r13 = ""
            android.net.Uri r4 = com.android.server.wm.AlertWindowNotificationController.UNIFIED_CONFIG_CENTER_URI     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            r5 = 0
            r6 = 0
            r8 = 0
            r3 = r2
            android.database.Cursor r3 = r3.query(r4, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            r9 = r3
            if (r9 == 0) goto L54
        L2d:
            boolean r3 = r9.moveToNext()     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            if (r3 == 0) goto L59
            r3 = 0
            int r3 = r9.getInt(r3)     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            r10 = r3
            r3 = 1
            java.lang.String r3 = r9.getString(r3)     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            r12 = r3
            r3 = 2
            java.lang.String r3 = r9.getString(r3)     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            r13 = r3
            r3 = 3
            byte[] r3 = r9.getBlob(r3)     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            r11 = r3
            java.lang.String r3 = new java.lang.String     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            r3.<init>(r11)     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            r1.append(r3)     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
            goto L2d
        L54:
            java.lang.String r3 = "cursor is null, lock failed, continue checking for update!"
            vivo.util.VSlog.i(r0, r3)     // Catch: java.lang.Throwable -> L5f java.lang.Exception -> L61
        L59:
            if (r9 == 0) goto L6d
        L5b:
            r9.close()
            goto L6d
        L5f:
            r0 = move-exception
            goto La6
        L61:
            r3 = move-exception
            java.lang.String r4 = "open database error!"
            vivo.util.VSlog.i(r0, r4)     // Catch: java.lang.Throwable -> L5f
            r3.printStackTrace()     // Catch: java.lang.Throwable -> L5f
            if (r9 == 0) goto L6d
            goto L5b
        L6d:
            boolean r3 = com.android.server.wm.WindowManagerDebugConfig.DEBUG
            if (r3 == 0) goto La1
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "id is : "
            r3.append(r4)
            r3.append(r10)
            java.lang.String r4 = " ; targetIdentifier is : "
            r3.append(r4)
            r3.append(r12)
            java.lang.String r4 = " ; fileVersion is : "
            r3.append(r4)
            r3.append(r13)
            java.lang.String r4 = " ; content is : "
            r3.append(r4)
            java.lang.String r4 = r1.toString()
            r3.append(r4)
            java.lang.String r3 = r3.toString()
            vivo.util.VSlog.d(r0, r3)
        La1:
            java.lang.String r0 = r1.toString()
            return r0
        La6:
            if (r9 == 0) goto Lab
            r9.close()
        Lab:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.AlertWindowNotificationController.getContentFromConfigCenter():java.lang.String");
    }

    public void updateWhiteList() {
        ArrayList<String> resultNotDecrypt = new ArrayList<>();
        NodeList allowedNodeList = getNodeList(TAG_ALLOWED_APPS);
        NodeList notShowNodeList = getNodeList(TAG_NOT_SHOWING_APPS);
        addNodeListToArray(allowedNodeList, resultNotDecrypt);
        addNodeListToArray(notShowNodeList, resultNotDecrypt);
        ArrayList<String> result = new ArrayList<>();
        Iterator<String> it = resultNotDecrypt.iterator();
        while (it.hasNext()) {
            String string = it.next();
            result.add(AlertWindowWhiteListCryUtils.decrypt(string));
        }
        if (WindowManagerDebugConfig.DEBUG) {
            VSlog.d(TAG, "get config file content is : " + result.toString());
        }
        synchronized (this) {
            this.mWhiteList.clear();
            this.mWhiteList.addAll(result);
        }
    }

    private void addNodeListToArray(NodeList nodeList, ArrayList<String> outArrayList) {
        if (nodeList != null) {
            int len = nodeList.getLength();
            for (int i = 0; i < len; i++) {
                Node n = nodeList.item(i);
                try {
                    if (n.getNodeName().equals(TAG_ITEM)) {
                        outArrayList.add(n.getFirstChild().getNodeValue());
                    }
                } catch (Exception e) {
                    VSlog.e(TAG, "getConfigList error please check config file " + e.getMessage());
                }
            }
        }
    }

    private NodeList getNodeList(String queryNode) {
        int index = 0;
        String content = getContentFromConfigCenter();
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(content.getBytes()));
            Element rootElement = doc.getDocumentElement();
            NodeList firstItems = rootElement.getElementsByTagName(queryNode);
            int lenSt = firstItems.getLength();
            while (index < lenSt && !firstItems.item(index).getParentNode().getNodeName().equals(TAG_FLOATING_WINDOW_APPS)) {
                index++;
            }
            if (index == lenSt) {
                return null;
            }
            NodeList nl = firstItems.item(index).getChildNodes();
            return nl;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}