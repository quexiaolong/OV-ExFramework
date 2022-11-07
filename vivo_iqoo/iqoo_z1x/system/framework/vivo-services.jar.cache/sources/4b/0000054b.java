package com.android.server.wm;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.os.FtBuild;
import android.os.IBinder;
import android.view.IWindow;
import android.view.SurfaceControl;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import java.util.ArrayList;
import java.util.Iterator;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoDragStateImpl implements IVivoDragState {
    static final String TAG = "VivoDragStateImpl";
    private DragState mDragState;
    private WindowManagerService mService;
    DragStateAttachFrame mDragAttachInd = null;
    final Interpolator mPathReturnInterpolator = new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);

    public VivoDragStateImpl() {
    }

    public VivoDragStateImpl(DragState state) {
        this.mDragState = state;
        this.mService = state.mService;
    }

    public void setDragStateAttach(DisplayContent displaycontent, WindowManagerService wms, IBinder winBinder) {
        if (this.mDragState == null) {
            return;
        }
        this.mDragAttachInd = new DragStateAttachFrame(displaycontent, wms, winBinder);
    }

    public void hideDragInd() {
        DragState dragState = this.mDragState;
        if (dragState == null) {
            return;
        }
        if (this.mDragAttachInd != null && dragState.mTargetWindow != null && this.mDragState.mTargetWindow.getAppToken() == null) {
            this.mDragAttachInd.hide(this.mDragState.mTargetWindow.toString());
        } else if (this.mDragAttachInd != null && this.mDragState.mTargetWindow == null) {
            VSlog.w("WindowManager", "targetwindow is null and dragattachind is not null");
        }
    }

    public boolean isVivoSplitDragDrop() {
        WindowManagerService windowManagerService = this.mService;
        return windowManagerService != null && windowManagerService.isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_DRAGDROP && this.mService.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay();
    }

    public TimeInterpolator getReturnInterpolator() {
        return this.mPathReturnInterpolator;
    }

    public void updateAttachIndAnimationByTransaction(ValueAnimator animation, SurfaceControl.Transaction transaction, String ANIMATED_PROPERTY_X, String ANIMATED_PROPERTY_Y, String ANIMATED_PROPERTY_ALPHA, String ANIMATED_PROPERTY_SCALE) {
        DragStateAttachFrame dragStateAttachFrame;
        PointF pos;
        if (!isVivoSplitDragDrop() || (dragStateAttachFrame = this.mDragAttachInd) == null || !dragStateAttachFrame.isSurfaceValid() || (pos = getCurrentIndPos(((Float) animation.getAnimatedValue(ANIMATED_PROPERTY_X)).floatValue(), ((Float) animation.getAnimatedValue(ANIMATED_PROPERTY_Y)).floatValue())) == null) {
            return;
        }
        this.mDragAttachInd.setPositionByTransaction(transaction, pos.x, pos.y);
        this.mDragAttachInd.setAlphaByTransaction(transaction, ((Float) animation.getAnimatedValue(ANIMATED_PROPERTY_ALPHA)).floatValue());
        this.mDragAttachInd.setMatrixByTransactionNotApply(transaction, ((Float) animation.getAnimatedValue(ANIMATED_PROPERTY_SCALE)).floatValue(), 0.0f, 0.0f, ((Float) animation.getAnimatedValue(ANIMATED_PROPERTY_SCALE)).floatValue());
    }

    public void destroyIndSurface(SurfaceControl.Transaction transaction) {
        DragStateAttachFrame dragStateAttachFrame = this.mDragAttachInd;
        if (dragStateAttachFrame != null) {
            dragStateAttachFrame.destroySurface(transaction);
        }
    }

    private String getPkgName(WindowState windowstate) {
        if (windowstate != null && windowstate.mActivityRecord != null) {
            return windowstate.mActivityRecord.packageName;
        }
        return null;
    }

    public void dragRecipientEntered(IWindow window) {
        DragStateAttachFrame dragStateAttachFrame;
        if (this.mDragState.mService.isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_DRAGDROP && (dragStateAttachFrame = this.mDragAttachInd) != null && dragStateAttachFrame.isAttachEnable() && this.mDragState.mService.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay()) {
            IBinder winBinder = window != null ? window.asBinder() : null;
            if (WindowManagerDebugConfig.DEBUG_DRAG && this.mDragState.mSurfaceControl != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("dragstate Entered to winBinder ");
                sb.append(winBinder);
                sb.append(" ");
                sb.append(window);
                sb.append(" ");
                sb.append(this.mDragState.mLocalWin);
                sb.append(" w ");
                sb.append(this.mDragState.mSurfaceControl.getWidth());
                sb.append(" h ");
                sb.append(this.mDragState.mSurfaceControl.getHeight());
                sb.append(" x ");
                sb.append(this.mDragState.mCurrentX);
                sb.append(" y ");
                sb.append(this.mDragState.mCurrentY);
                sb.append(" targetwindow ");
                sb.append(this.mDragState.mTargetWindow);
                sb.append(" ");
                sb.append(this.mDragState.mDataDescription != null ? this.mDragState.mDataDescription : "null");
                VSlog.i(TAG, sb.toString());
            }
            PointF indPos = getCurrentIndPos();
            if (winBinder != null && winBinder != this.mDragState.mLocalWin && indPos != null && this.mDragState.mTargetWindow != null && this.mDragState.mTargetWindow.getAppToken() != null) {
                if ((this.mDragState.mDataDescription != null && this.mDragState.mDataDescription.hasMimeType("text/plain")) || VivoMultiWindowConfig.getInstance().isVivoAllowAllTypeDragIndMaskApp(getPkgName(this.mDragState.mTargetWindow))) {
                    boolean ifSetupNew = this.mDragAttachInd.setupSurfaceAndShow(indPos.x, indPos.y, 1);
                    if (ifSetupNew) {
                        this.mDragAttachInd.vivoDrawSplitInd();
                    } else {
                        this.mDragAttachInd.show();
                    }
                }
            }
        }
    }

    public void dragRecipientExited(IWindow window) {
        DragStateAttachFrame dragStateAttachFrame;
        if (this.mDragState.mService.isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_DRAGDROP && (dragStateAttachFrame = this.mDragAttachInd) != null && dragStateAttachFrame.isAttachEnable() && this.mDragState.mService.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay()) {
            IBinder winBinder = window != null ? window.asBinder() : null;
            if (WindowManagerDebugConfig.DEBUG_DRAG && this.mDragState.mSurfaceControl != null) {
                VSlog.i(TAG, "dragstate Exited from winBinder " + winBinder + " " + window + " " + this.mDragState.mLocalWin + " w " + this.mDragState.mSurfaceControl.getWidth() + " h " + this.mDragState.mSurfaceControl.getHeight() + " x " + this.mDragState.mCurrentX + " y " + this.mDragState.mCurrentY);
            }
            if (winBinder != null && winBinder != this.mDragState.mLocalWin) {
                this.mDragAttachInd.hide("exited");
            }
        }
    }

    public void moveDragIndPosInTransaction(SurfaceControl.Transaction transaction) {
        DragStateAttachFrame dragStateAttachFrame;
        if (this.mDragState.mService.isVivoMultiWindowSupport() && VivoMultiWindowConfig.IS_VIVO_SPLIT_DRAGDROP && (dragStateAttachFrame = this.mDragAttachInd) != null && dragStateAttachFrame.isAttachEnable() && this.mDragState.mService.isInVivoMultiWindowIgnoreVisibilityFocusedDisplay()) {
            if (WindowManagerDebugConfig.DEBUG_DRAG && this.mDragState.mSurfaceControl != null) {
                VSlog.i(TAG, "dragstate move w=" + this.mDragState.mSurfaceControl.getWidth() + " h=" + this.mDragState.mSurfaceControl.getHeight() + " currentX " + this.mDragState.mCurrentX + " currentY " + this.mDragState.mCurrentY);
            }
            PointF indPos = getCurrentIndPos();
            if (indPos != null) {
                if (WindowManagerDebugConfig.DEBUG_DRAG) {
                    VSlog.i(TAG, "dragstate move indpos x = " + indPos.x + " y = " + indPos.y);
                }
                this.mDragAttachInd.setPositionInTransaction(transaction, indPos.x, indPos.y);
            }
        }
    }

    public boolean canDragGlobalFlag() {
        if ((this.mDragState.mFlags & 256) != 0) {
            return true;
        }
        return false;
    }

    PointF getCurrentIndPos() {
        if (this.mDragState.mSurfaceControl == null) {
            return null;
        }
        float surfacePosX = this.mDragState.mCurrentX - this.mDragState.mThumbOffsetX;
        float surfacePosY = this.mDragState.mCurrentY - this.mDragState.mThumbOffsetY;
        return getCurrentIndPos(surfacePosX, surfacePosY);
    }

    PointF getCurrentIndPos(float surfacePosX, float surfacePosY) {
        DragStateAttachFrame dragStateAttachFrame;
        if (this.mDragState.mSurfaceControl == null || (dragStateAttachFrame = this.mDragAttachInd) == null) {
            return null;
        }
        int dragWidth = dragStateAttachFrame.getWidthPx();
        int surfaceWidth = this.mDragState.mSurfaceControl.getWidth();
        float surfaceRighTopX = surfaceWidth + surfacePosX;
        float indPosX = surfaceRighTopX - (dragWidth / 2);
        float indPosY = surfacePosY - (dragWidth / 2);
        PointF indPointF = new PointF(indPosX, indPosY);
        return indPointF;
    }

    public void dragEventProcess(ArrayList<WindowState> notifiedWindows) {
        if (FtBuild.isOverSeas()) {
            return;
        }
        if (notifiedWindows == null || notifiedWindows.isEmpty()) {
            if (VivoMultiWindowConfig.DEBUG) {
                VSlog.w(TAG, "dragstate notifiedWindows is null or empty");
                return;
            }
            return;
        }
        WindowState dragWindow = null;
        WindowState primaryWindow = null;
        WindowState secondaryWindow = null;
        String dragPackage = null;
        String dropPackage = null;
        Iterator<WindowState> it = notifiedWindows.iterator();
        while (it.hasNext()) {
            WindowState win = it.next();
            if (win != null && win.getAppToken() != null && win.mAttrs.type >= 1 && win.mAttrs.type <= 99) {
                if (win.mClient.asBinder() == this.mDragState.mLocalWin) {
                    dragWindow = win;
                }
                if (win.getWindowingMode() == 3) {
                    primaryWindow = win;
                } else if (win.getWindowingMode() == 4) {
                    secondaryWindow = win;
                }
            }
        }
        if (primaryWindow != null && secondaryWindow != null && dragWindow != null && dragWindow.getOwningPackage() != null) {
            dragPackage = dragWindow.getOwningPackage();
            if (dragWindow.getOwningPackage().equals(primaryWindow.getOwningPackage())) {
                dropPackage = secondaryWindow.getOwningPackage();
            } else if (dragWindow.getOwningPackage().equals(secondaryWindow.getOwningPackage())) {
                dropPackage = primaryWindow.getOwningPackage();
            }
        }
        if (dragPackage == null || dropPackage == null) {
            return;
        }
        VivoMultiWindowConfig.getInstance().splitDragEventThreadRun(this.mDragState.mService.mContext, dragPackage, dropPackage);
    }
}