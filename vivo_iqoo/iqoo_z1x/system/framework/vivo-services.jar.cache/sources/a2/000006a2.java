package com.vivo.services.popupcamera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import com.vivo.common.utils.VLog;

/* loaded from: classes.dex */
public class PopupFrontCameraPermissionHelper {
    private static final String POPUP_CAMERA_CONTROL_PROJECTION_ALWAYS_DENY = "alwaysdeny";
    private static final String POPUP_CAMERA_CONTROL_PROJECTION_CURRENT_STATE = "currentstate";
    private static final String POPUP_CAMERA_CONTROL_PROJECTION_PKGNAME = "pkgname";
    private static final String POPUP_CAMERA_CONTROL_PROJECTION_SET_BYUSER = "setbyuser";
    private static final String POPUP_CAMERA_CONTROL_URI = "content://com.vivo.permissionmanager.provider.permission/pop_camera_control";
    private static final String TAG = "PopupCameraManagerService";

    /* JADX WARN: Code restructure failed: missing block: B:16:0x0060, code lost:
        if (r11 == null) goto L12;
     */
    /* JADX WARN: Code restructure failed: missing block: B:18:0x0063, code lost:
        r0 = new com.vivo.services.popupcamera.PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState(r17, r12, r13, r14);
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0068, code lost:
        return r0;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static com.vivo.services.popupcamera.PopupFrontCameraPermissionHelper.PopupFrontCameraPermissionState getFrontCameraPermissionStateFromSettings(android.content.Context r16, java.lang.String r17) {
        /*
            r1 = r17
            java.lang.String r0 = "alwaysdeny"
            java.lang.String r2 = "currentstate"
            android.content.ContentResolver r9 = r16.getContentResolver()
            java.lang.String r3 = "content://com.vivo.permissionmanager.provider.permission/pop_camera_control"
            android.net.Uri r10 = android.net.Uri.parse(r3)
            r11 = 0
            r12 = 0
            r13 = 0
            r14 = 0
            int r3 = android.app.ActivityManager.getCurrentUser()     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            android.net.Uri r4 = android.content.ContentProvider.maybeAddUserId(r10, r3)     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            java.lang.String r3 = "pkgname"
            java.lang.String[] r5 = new java.lang.String[]{r3, r2, r0}     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            java.lang.String r6 = "pkgname=?"
            r15 = 1
            java.lang.String[] r7 = new java.lang.String[r15]     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            r3 = 0
            r7[r3] = r1     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            r8 = 0
            r3 = r9
            android.database.Cursor r3 = r3.query(r4, r5, r6, r7, r8)     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            r11 = r3
            if (r11 == 0) goto L4f
            int r3 = r11.getCount()     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            if (r3 < r15) goto L4f
            r11.moveToNext()     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            int r2 = r11.getColumnIndex(r2)     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            int r2 = r11.getInt(r2)     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            r12 = r2
            int r0 = r11.getColumnIndex(r0)     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            int r0 = r11.getInt(r0)     // Catch: java.lang.Throwable -> L55 java.lang.Exception -> L57
            r14 = 1
            r13 = r0
        L4f:
            if (r11 == 0) goto L63
        L51:
            r11.close()
            goto L63
        L55:
            r0 = move-exception
            goto L69
        L57:
            r0 = move-exception
            java.lang.String r2 = "PopupCameraManagerService"
            java.lang.String r3 = "getFrontCameraPermissionStateFromSettings get error"
            com.vivo.common.utils.VLog.d(r2, r3)     // Catch: java.lang.Throwable -> L55
            r14 = 0
            if (r11 == 0) goto L63
            goto L51
        L63:
            com.vivo.services.popupcamera.PopupFrontCameraPermissionHelper$PopupFrontCameraPermissionState r0 = new com.vivo.services.popupcamera.PopupFrontCameraPermissionHelper$PopupFrontCameraPermissionState
            r0.<init>(r1, r12, r13, r14)
            return r0
        L69:
            if (r11 == 0) goto L6e
            r11.close()
        L6e:
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.popupcamera.PopupFrontCameraPermissionHelper.getFrontCameraPermissionStateFromSettings(android.content.Context, java.lang.String):com.vivo.services.popupcamera.PopupFrontCameraPermissionHelper$PopupFrontCameraPermissionState");
    }

    public static void setFrontCameraPermissionStateToSettings(Context context, PopupFrontCameraPermissionState ps) {
        ContentResolver resolver = context.getContentResolver();
        Uri uri = Uri.parse(POPUP_CAMERA_CONTROL_URI);
        ContentValues cv = new ContentValues();
        cv.put(POPUP_CAMERA_CONTROL_PROJECTION_CURRENT_STATE, Integer.valueOf(ps.currentState));
        if (ps.currentState == 1) {
            cv.put(POPUP_CAMERA_CONTROL_PROJECTION_ALWAYS_DENY, Integer.valueOf(ps.alwaysDeny));
        }
        cv.put(POPUP_CAMERA_CONTROL_PROJECTION_PKGNAME, ps.packageName);
        cv.put(POPUP_CAMERA_CONTROL_PROJECTION_SET_BYUSER, (Integer) 1);
        if (resolver != null) {
            try {
                resolver.update(uri, cv, "pkgname=?", new String[]{ps.packageName});
            } catch (Exception e) {
                VLog.d(TAG, "setFrontCameraPermissionStateToSettings" + ps + " failed");
            }
        }
    }

    /* loaded from: classes.dex */
    public static class PopupFrontCameraPermissionState {
        public int alwaysDeny;
        public int currentState;
        public boolean isValid;
        public String packageName;

        public PopupFrontCameraPermissionState(String name, int state, int always, boolean valid) {
            this.currentState = -1;
            this.alwaysDeny = -1;
            this.isValid = false;
            this.packageName = name;
            this.currentState = state;
            this.alwaysDeny = always;
            this.isValid = valid;
        }

        public boolean isPopupFrontCameraPermissionGranted() {
            return this.currentState == 0;
        }

        public boolean isAlwaysDeny() {
            return this.alwaysDeny == 1;
        }

        public boolean isPermissionStateValid() {
            return this.isValid;
        }

        public String toString() {
            return "{" + this.packageName + " isPopupFrontCameraPermissionGranted=" + isPopupFrontCameraPermissionGranted() + " isAlwaysDeny=" + isAlwaysDeny() + "isValid=" + this.isValid + "}";
        }
    }
}