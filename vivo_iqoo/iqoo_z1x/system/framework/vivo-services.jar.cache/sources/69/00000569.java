package com.android.server.wm;

import android.content.Context;
import android.os.IBinder;
import android.os.SystemProperties;
import android.view.DisplayInfo;
import android.view.IApplicationToken;
import android.view.SurfaceSession;
import android.view.animation.Animation;
import com.vivo.services.rms.ProcessList;
import java.util.ArrayList;
import java.util.Iterator;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoMultiWindowTrans {
    static final int LONG_TIME_OUT = 3;
    static final int NORMAL_TIME_OUT = 2;
    static final int SHORT_TIME_OUT = 1;
    static final int SUPER_SHORT_TIME_OUT = 4;
    static final int TRANSDST_LAYER = 40;
    static final int WINDOW_FREEZETRANS_LONG_TIMEOUT = 2500;
    static final int WINDOW_FREEZETRANS_SHORT_TIMEOUT = 800;
    static final int WINDOW_FREEZETRANS_SUPER_SHORT_TIMEOUT = SystemProperties.getInt("persist.vivo.multiwindow_supershort_timeout", (int) ProcessList.HEAVY_WEIGHT_APP_ADJ);
    static final int WINDOW_FREEZETRANS_TIMEOUT = 1200;
    private boolean bFinishedImmediatelyOne;
    final Context mContext;
    final DisplayContent mDisplayContent;
    final SurfaceSession mSession;
    private final WindowManagerService mWindowManagerService;
    Animation mAnimation = null;
    MultiWindowFreezeWindowFrame freezeWindowFrame = null;
    int mNextAppTrans = -1;
    ArrayList mTargetApps = new ArrayList();

    /* JADX INFO: Access modifiers changed from: package-private */
    public VivoMultiWindowTrans(Context context, DisplayContent displaycontent, SurfaceSession session, WindowManagerService service) {
        this.bFinishedImmediatelyOne = false;
        this.mContext = context;
        this.mSession = session;
        this.bFinishedImmediatelyOne = false;
        this.mDisplayContent = displaycontent;
        this.mWindowManagerService = service;
    }

    boolean addWaitingTransitionTargetToken(IBinder token) {
        if (VivoMultiWindowTransManager.DEBUG_PERFORMANCE) {
            long time = System.currentTimeMillis();
            VSlog.i("vivo_debug_multitrans", "addWindowTransitionTarget time = " + time + ", token = " + token);
        }
        if (token != null && !this.mTargetApps.contains(token)) {
            this.mTargetApps.add(token);
            return true;
        }
        return false;
    }

    public void removeAllSetMultiWindowTransition() {
        Animation animation = this.mAnimation;
        if (animation != null) {
            animation.cancel();
            this.mAnimation = null;
        }
        MultiWindowFreezeWindowFrame multiWindowFreezeWindowFrame = this.freezeWindowFrame;
        if (multiWindowFreezeWindowFrame != null) {
            multiWindowFreezeWindowFrame.destroyAll(this.mDisplayContent.getPendingTransaction());
            this.freezeWindowFrame = null;
        }
        this.mTargetApps.clear();
        this.mNextAppTrans = -1;
        this.bFinishedImmediatelyOne = false;
        if (VivoMultiWindowTransManager.DEBUG_PERFORMANCE) {
            long time = System.currentTimeMillis();
            VSlog.i("vivo_debug_multitrans", "DEBUG_PERFORMANCE removeAllSetMultiWindowTransition time = " + time + ", freezeWindowFrame = " + this.freezeWindowFrame);
        }
    }

    void makeFreezeWindowFrame(boolean bgsurface, int timeOut) {
        int dw;
        int dh;
        if (VivoMultiWindowTransManager.DEBUG_PERFORMANCE) {
            long time = System.currentTimeMillis();
            VSlog.i("vivo_debug_multitrans", "DEBUG_PERFORMANCE makeFreezeWindowFrame begin time = " + time + ", freezeWindowFrame = " + this.freezeWindowFrame);
        }
        if (this.freezeWindowFrame == null) {
            DisplayInfo displayinfo = this.mDisplayContent.getDisplayInfo();
            if (displayinfo.rotation != 1 && displayinfo.rotation != 3) {
                dw = displayinfo.logicalWidth;
                dh = displayinfo.logicalHeight;
            } else {
                dw = displayinfo.logicalHeight;
                dh = displayinfo.logicalWidth;
            }
            MultiWindowFreezeWindowFrame multiWindowFreezeWindowFrame = new MultiWindowFreezeWindowFrame(this.mDisplayContent, this.mSession, 0, 0, dw, dh, displayinfo.layerStack, displayinfo.rotation, bgsurface, 0, ((40 * 10000) + 1000) - 1);
            this.freezeWindowFrame = multiWindowFreezeWindowFrame;
            if (multiWindowFreezeWindowFrame != null) {
                multiWindowFreezeWindowFrame.show();
                this.mWindowManagerService.vivoReSendMultiWindowAnimTimeOut(timeOut);
            }
        }
        if (VivoMultiWindowTransManager.DEBUG_PERFORMANCE) {
            long time2 = System.currentTimeMillis();
            VSlog.i("vivo_debug_multitrans", "DEBUG_PERFORMANCE makeFreezeWindowFrame end time = " + time2 + ", freezeWindowFrame = " + this.freezeWindowFrame);
        }
    }

    public boolean isAnimating() {
        return (this.freezeWindowFrame == null || this.mAnimation == null) ? false : true;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isTransReady() {
        return this.mNextAppTrans != -1;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setAppWinDrawnAndRemoveInWaitingList(ActivityRecord ar) {
        String pkgName;
        if (VivoMultiWindowTransManager.DEBUG_PERFORMANCE) {
            long time = System.currentTimeMillis();
            VSlog.i("vivo_debug_multitrans", "DEBUG_PERFORMANCE setAppWinDrawnAndRemoveInWaitingList time = " + time + ", ar = " + ar + ", mTargetApps  = " + this.mTargetApps);
        }
        if (this.mTargetApps != null) {
            if (ar != null && ar.appToken != null) {
                boolean findAndRemove = false;
                int i = 0;
                while (true) {
                    if (i >= this.mTargetApps.size()) {
                        break;
                    }
                    IBinder token = (IBinder) this.mTargetApps.get(i);
                    if (token != null) {
                        findAndRemove = ar.appToken != null && this.mTargetApps.remove(ar.appToken);
                        if (!findAndRemove && (pkgName = ar.packageName) != null && token.toString() != null && token.toString().contains(pkgName) && (findAndRemove = this.mTargetApps.remove(token))) {
                            VSlog.i("vivo_debug_multitrans", "target is removed by pkg. token is " + token);
                        }
                    }
                    if (!findAndRemove) {
                        i++;
                    } else if (VivoMultiWindowTransManager.DEBUG) {
                        VSlog.i("vivo_debug_multitrans", "apptoken is found int target list. ActivityRecord is " + ar + " del token is " + token + "  targetlist may be clear and left list is " + this.mTargetApps);
                    }
                }
                if (findAndRemove) {
                    if (!ar.isActivityTypeHome()) {
                        this.mTargetApps.clear();
                        return;
                    }
                    this.mTargetApps.add(new IApplicationToken.Stub() { // from class: com.android.server.wm.VivoMultiWindowTrans.1
                        public String getName() {
                            return "temporary";
                        }

                        public String toString() {
                            StringBuilder sb = new StringBuilder(128);
                            sb.append("Token{");
                            sb.append(Integer.toHexString(System.identityHashCode(this)));
                            sb.append(' ');
                            sb.append(getName());
                            sb.append('}');
                            return sb.toString();
                        }
                    });
                    this.mWindowManagerService.vivoReSendMultiWindowAnimTimeOut(300L);
                    return;
                }
                return;
            }
            return;
        }
        VSlog.wtf("vivo_debug_multitrans", " target app list is null");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public ArrayList setWaitingTokenAndWindowTransition(ArrayList animTokenList, int appTransit) {
        if (animTokenList == null || animTokenList.size() <= 0) {
            return null;
        }
        ArrayList targetList = new ArrayList();
        Iterator it = animTokenList.iterator();
        while (it.hasNext()) {
            Object item = it.next();
            if (addWaitingTransitionTargetToken((IBinder) item)) {
                targetList.add(item);
            }
        }
        this.mNextAppTrans = appTransit;
        if (VivoMultiWindowTransManager.DEBUG_PERFORMANCE) {
            long time = System.currentTimeMillis();
            StringBuilder sb = new StringBuilder();
            sb.append("DEBUG_PERFORMANCE setWaitingTokenAndWindowTransition time = ");
            sb.append(time);
            sb.append(", transit = ");
            sb.append(appTransit);
            sb.append(", animlist size = ");
            sb.append(animTokenList != null ? animTokenList.size() : 0);
            VSlog.i("vivo_debug_multitrans", sb.toString());
        }
        return targetList;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void doTimeout() {
        this.mTargetApps.clear();
    }

    public String toString() {
        return "MultiWindowFreezeWindowFrame";
    }

    public boolean isAllTargetProcessed() {
        ArrayList arrayList = this.mTargetApps;
        return arrayList == null || arrayList.size() <= 0;
    }

    public void createMultiWindowFreezeWindowFrame(int timeout) {
        makeFreezeWindowFrame(true, timeout);
    }

    public void setFinishedImmediatelyOneNotified(boolean notifyone) {
        this.bFinishedImmediatelyOne = notifyone;
        if (VivoMultiWindowTransManager.DEBUG) {
            VSlog.i("vivo_debug_multitrans", "setFinishedImmediatelyOneNotified notifyone = " + notifyone);
        }
    }

    public boolean getFinishedIfOneNotified() {
        return this.bFinishedImmediatelyOne;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static int getTimeOutByTimeInd(int ind) {
        if (ind == 1) {
            return 800;
        }
        if (ind == 2) {
            return WINDOW_FREEZETRANS_TIMEOUT;
        }
        if (ind == 3) {
            return WINDOW_FREEZETRANS_LONG_TIMEOUT;
        }
        if (ind == 4) {
            return WINDOW_FREEZETRANS_SUPER_SHORT_TIMEOUT;
        }
        return 0;
    }
}