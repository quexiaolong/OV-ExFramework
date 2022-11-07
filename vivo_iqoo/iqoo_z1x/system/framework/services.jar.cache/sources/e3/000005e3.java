package com.android.server.accessibility;

import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Slog;
import android.view.MagnificationSpec;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.IAccessibilityInteractionConnection;
import android.view.accessibility.IAccessibilityInteractionConnectionCallback;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class ActionReplacingCallback extends IAccessibilityInteractionConnectionCallback.Stub {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "ActionReplacingCallback";
    private final IAccessibilityInteractionConnection mConnectionWithReplacementActions;
    boolean mDone;
    private final int mInteractionId;
    private final Object mLock = new Object();
    boolean mMultiNodeCallbackHappened;
    AccessibilityNodeInfo mNodeFromOriginalWindow;
    List<AccessibilityNodeInfo> mNodesFromOriginalWindow;
    List<AccessibilityNodeInfo> mNodesWithReplacementActions;
    private final IAccessibilityInteractionConnectionCallback mServiceCallback;
    boolean mSingleNodeCallbackHappened;

    public ActionReplacingCallback(IAccessibilityInteractionConnectionCallback serviceCallback, IAccessibilityInteractionConnection connectionWithReplacementActions, int interactionId, int interrogatingPid, long interrogatingTid) {
        this.mServiceCallback = serviceCallback;
        this.mConnectionWithReplacementActions = connectionWithReplacementActions;
        this.mInteractionId = interactionId;
        long identityToken = Binder.clearCallingIdentity();
        try {
            try {
                this.mConnectionWithReplacementActions.findAccessibilityNodeInfoByAccessibilityId(AccessibilityNodeInfo.ROOT_NODE_ID, (Region) null, interactionId + 1, this, 0, interrogatingPid, interrogatingTid, (MagnificationSpec) null, (Bundle) null);
            } catch (RemoteException e) {
                this.mMultiNodeCallbackHappened = true;
            }
        } finally {
            Binder.restoreCallingIdentity(identityToken);
        }
    }

    public void setFindAccessibilityNodeInfoResult(AccessibilityNodeInfo info, int interactionId) {
        synchronized (this.mLock) {
            if (interactionId == this.mInteractionId) {
                this.mNodeFromOriginalWindow = info;
                this.mSingleNodeCallbackHappened = true;
                boolean readyForCallback = this.mMultiNodeCallbackHappened;
                if (readyForCallback) {
                    replaceInfoActionsAndCallService();
                    return;
                }
                return;
            }
            Slog.e(LOG_TAG, "Callback with unexpected interactionId");
        }
    }

    public void setFindAccessibilityNodeInfosResult(List<AccessibilityNodeInfo> infos, int interactionId) {
        synchronized (this.mLock) {
            if (interactionId == this.mInteractionId) {
                this.mNodesFromOriginalWindow = infos;
            } else if (interactionId == this.mInteractionId + 1) {
                this.mNodesWithReplacementActions = infos;
            } else {
                Slog.e(LOG_TAG, "Callback with unexpected interactionId");
                return;
            }
            boolean callbackForSingleNode = this.mSingleNodeCallbackHappened;
            boolean callbackForMultipleNodes = this.mMultiNodeCallbackHappened;
            this.mMultiNodeCallbackHappened = true;
            if (callbackForSingleNode) {
                replaceInfoActionsAndCallService();
            }
            if (callbackForMultipleNodes) {
                replaceInfosActionsAndCallService();
            }
        }
    }

    public void setPerformAccessibilityActionResult(boolean succeeded, int interactionId) throws RemoteException {
        this.mServiceCallback.setPerformAccessibilityActionResult(succeeded, interactionId);
    }

    private void replaceInfoActionsAndCallService() {
        synchronized (this.mLock) {
            if (this.mDone) {
                return;
            }
            if (this.mNodeFromOriginalWindow != null) {
                replaceActionsOnInfoLocked(this.mNodeFromOriginalWindow);
            }
            recycleReplaceActionNodesLocked();
            AccessibilityNodeInfo nodeToReturn = this.mNodeFromOriginalWindow;
            this.mDone = true;
            try {
                this.mServiceCallback.setFindAccessibilityNodeInfoResult(nodeToReturn, this.mInteractionId);
            } catch (RemoteException e) {
            }
        }
    }

    private void replaceInfosActionsAndCallService() {
        synchronized (this.mLock) {
            if (this.mDone) {
                return;
            }
            if (this.mNodesFromOriginalWindow != null) {
                for (int i = 0; i < this.mNodesFromOriginalWindow.size(); i++) {
                    replaceActionsOnInfoLocked(this.mNodesFromOriginalWindow.get(i));
                }
            }
            recycleReplaceActionNodesLocked();
            List<AccessibilityNodeInfo> nodesToReturn = this.mNodesFromOriginalWindow == null ? null : new ArrayList<>(this.mNodesFromOriginalWindow);
            this.mDone = true;
            try {
                this.mServiceCallback.setFindAccessibilityNodeInfosResult(nodesToReturn, this.mInteractionId);
            } catch (RemoteException e) {
            }
        }
    }

    private void replaceActionsOnInfoLocked(AccessibilityNodeInfo info) {
        info.removeAllActions();
        info.setClickable(false);
        info.setFocusable(false);
        info.setContextClickable(false);
        info.setScrollable(false);
        info.setLongClickable(false);
        info.setDismissable(false);
        if (info.getSourceNodeId() == AccessibilityNodeInfo.ROOT_NODE_ID && this.mNodesWithReplacementActions != null) {
            for (int i = 0; i < this.mNodesWithReplacementActions.size(); i++) {
                AccessibilityNodeInfo nodeWithReplacementActions = this.mNodesWithReplacementActions.get(i);
                if (nodeWithReplacementActions.getSourceNodeId() == AccessibilityNodeInfo.ROOT_NODE_ID) {
                    List<AccessibilityNodeInfo.AccessibilityAction> actions = nodeWithReplacementActions.getActionList();
                    if (actions != null) {
                        for (int j = 0; j < actions.size(); j++) {
                            info.addAction(actions.get(j));
                        }
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS);
                        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
                    }
                    info.setClickable(nodeWithReplacementActions.isClickable());
                    info.setFocusable(nodeWithReplacementActions.isFocusable());
                    info.setContextClickable(nodeWithReplacementActions.isContextClickable());
                    info.setScrollable(nodeWithReplacementActions.isScrollable());
                    info.setLongClickable(nodeWithReplacementActions.isLongClickable());
                    info.setDismissable(nodeWithReplacementActions.isDismissable());
                }
            }
        }
    }

    private void recycleReplaceActionNodesLocked() {
        List<AccessibilityNodeInfo> list = this.mNodesWithReplacementActions;
        if (list == null) {
            return;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            AccessibilityNodeInfo nodeWithReplacementAction = this.mNodesWithReplacementActions.get(i);
            nodeWithReplacementAction.recycle();
        }
        this.mNodesWithReplacementActions = null;
    }
}