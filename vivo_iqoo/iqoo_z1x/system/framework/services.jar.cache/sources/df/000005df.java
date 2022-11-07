package com.android.server.accessibility;

import android.graphics.Region;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.view.IWindow;
import android.view.WindowInfo;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.accessibility.IAccessibilityInteractionConnection;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.accessibility.AccessibilitySecurityPolicy;
import com.android.server.wm.WindowManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public class AccessibilityWindowManager {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "AccessibilityWindowManager";
    private static int sNextWindowId;
    private final AccessibilityEventSender mAccessibilityEventSender;
    private final AccessibilitySecurityPolicy.AccessibilityUserManager mAccessibilityUserManager;
    private final Handler mHandler;
    private final Object mLock;
    private RemoteAccessibilityConnection mPictureInPictureActionReplacingConnection;
    private final AccessibilitySecurityPolicy mSecurityPolicy;
    private int mTopFocusedDisplayId;
    private IBinder mTopFocusedWindowToken;
    private boolean mTouchInteractionInProgress;
    private final WindowManagerInternal mWindowManagerInternal;
    private final SparseArray<RemoteAccessibilityConnection> mGlobalInteractionConnections = new SparseArray<>();
    private final SparseArray<IBinder> mGlobalWindowTokens = new SparseArray<>();
    private final SparseArray<SparseArray<RemoteAccessibilityConnection>> mInteractionConnections = new SparseArray<>();
    private final SparseArray<SparseArray<IBinder>> mWindowTokens = new SparseArray<>();
    private int mActiveWindowId = -1;
    private int mTopFocusedWindowId = -1;
    private int mAccessibilityFocusedWindowId = -1;
    private long mAccessibilityFocusNodeId = 2147483647L;
    private int mAccessibilityFocusedDisplayId = -1;
    private final SparseArray<DisplayWindowsObserver> mDisplayWindowsObservers = new SparseArray<>();
    private final ArrayMap<IBinder, IBinder> mHostEmbeddedMap = new ArrayMap<>();
    private final SparseArray<IBinder> mWindowIdMap = new SparseArray<>();

    /* loaded from: classes.dex */
    public interface AccessibilityEventSender {
        void sendAccessibilityEventForCurrentUserLocked(AccessibilityEvent accessibilityEvent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class DisplayWindowsObserver implements WindowManagerInternal.WindowsForAccessibilityCallback {
        private final int mDisplayId;
        private boolean mHasWatchOutsideTouchWindow;
        private List<AccessibilityWindowInfo> mWindows;
        private final SparseArray<AccessibilityWindowInfo> mA11yWindowInfoById = new SparseArray<>();
        private final SparseArray<WindowInfo> mWindowInfoById = new SparseArray<>();
        private final List<WindowInfo> mCachedWindowInfos = new ArrayList();
        private boolean mTrackingWindows = false;

        DisplayWindowsObserver(int displayId) {
            this.mDisplayId = displayId;
        }

        boolean startTrackingWindowsLocked() {
            boolean result = true;
            if (!this.mTrackingWindows) {
                this.mTrackingWindows = true;
                result = AccessibilityWindowManager.this.mWindowManagerInternal.setWindowsForAccessibilityCallback(this.mDisplayId, this);
                if (!result) {
                    this.mTrackingWindows = false;
                    Slog.w(AccessibilityWindowManager.LOG_TAG, "set windowsObserver callbacks fail, displayId:" + this.mDisplayId);
                }
            }
            return result;
        }

        void stopTrackingWindowsLocked() {
            if (this.mTrackingWindows) {
                AccessibilityWindowManager.this.mWindowManagerInternal.setWindowsForAccessibilityCallback(this.mDisplayId, null);
                this.mTrackingWindows = false;
                clearWindowsLocked();
            }
        }

        boolean isTrackingWindowsLocked() {
            return this.mTrackingWindows;
        }

        List<AccessibilityWindowInfo> getWindowListLocked() {
            return this.mWindows;
        }

        AccessibilityWindowInfo findA11yWindowInfoByIdLocked(int windowId) {
            return this.mA11yWindowInfoById.get(windowId);
        }

        WindowInfo findWindowInfoByIdLocked(int windowId) {
            return this.mWindowInfoById.get(windowId);
        }

        AccessibilityWindowInfo getPictureInPictureWindowLocked() {
            List<AccessibilityWindowInfo> list = this.mWindows;
            if (list != null) {
                int windowCount = list.size();
                for (int i = 0; i < windowCount; i++) {
                    AccessibilityWindowInfo window = this.mWindows.get(i);
                    if (window.isInPictureInPictureMode()) {
                        return window;
                    }
                }
                return null;
            }
            return null;
        }

        void setActiveWindowLocked(int windowId) {
            List<AccessibilityWindowInfo> list = this.mWindows;
            if (list != null) {
                int windowCount = list.size();
                for (int i = 0; i < windowCount; i++) {
                    AccessibilityWindowInfo window = this.mWindows.get(i);
                    if (window.getId() == windowId) {
                        window.setActive(true);
                        AccessibilityWindowManager.this.mAccessibilityEventSender.sendAccessibilityEventForCurrentUserLocked(AccessibilityEvent.obtainWindowsChangedEvent(windowId, 32));
                    } else {
                        window.setActive(false);
                    }
                }
            }
        }

        void setAccessibilityFocusedWindowLocked(int windowId) {
            List<AccessibilityWindowInfo> list = this.mWindows;
            if (list != null) {
                int windowCount = list.size();
                for (int i = 0; i < windowCount; i++) {
                    AccessibilityWindowInfo window = this.mWindows.get(i);
                    if (window.getId() == windowId) {
                        AccessibilityWindowManager.this.mAccessibilityFocusedDisplayId = this.mDisplayId;
                        window.setAccessibilityFocused(true);
                        AccessibilityWindowManager.this.mAccessibilityEventSender.sendAccessibilityEventForCurrentUserLocked(AccessibilityEvent.obtainWindowsChangedEvent(windowId, 128));
                    } else {
                        window.setAccessibilityFocused(false);
                    }
                }
            }
        }

        boolean computePartialInteractiveRegionForWindowLocked(int windowId, Region outRegion) {
            List<AccessibilityWindowInfo> list = this.mWindows;
            if (list == null) {
                return false;
            }
            Region windowInteractiveRegion = null;
            boolean windowInteractiveRegionChanged = false;
            int windowCount = list.size();
            Region currentWindowRegions = new Region();
            for (int i = windowCount - 1; i >= 0; i--) {
                AccessibilityWindowInfo currentWindow = this.mWindows.get(i);
                if (windowInteractiveRegion == null) {
                    if (currentWindow.getId() == windowId) {
                        currentWindow.getRegionInScreen(currentWindowRegions);
                        outRegion.set(currentWindowRegions);
                        windowInteractiveRegion = outRegion;
                    }
                } else if (currentWindow.getType() != 4) {
                    currentWindow.getRegionInScreen(currentWindowRegions);
                    if (windowInteractiveRegion.op(currentWindowRegions, Region.Op.DIFFERENCE)) {
                        windowInteractiveRegionChanged = true;
                    }
                }
            }
            return windowInteractiveRegionChanged;
        }

        List<Integer> getWatchOutsideTouchWindowIdLocked(int targetWindowId) {
            WindowInfo targetWindow = this.mWindowInfoById.get(targetWindowId);
            if (targetWindow != null && this.mHasWatchOutsideTouchWindow) {
                List<Integer> outsideWindowsId = new ArrayList<>();
                for (int i = 0; i < this.mWindowInfoById.size(); i++) {
                    WindowInfo window = this.mWindowInfoById.valueAt(i);
                    if (window != null && window.layer < targetWindow.layer && window.hasFlagWatchOutsideTouch) {
                        outsideWindowsId.add(Integer.valueOf(this.mWindowInfoById.keyAt(i)));
                    }
                }
                return outsideWindowsId;
            }
            return Collections.emptyList();
        }

        @Override // com.android.server.wm.WindowManagerInternal.WindowsForAccessibilityCallback
        public void onWindowsForAccessibilityChanged(boolean forceSend, int topFocusedDisplayId, IBinder topFocusedWindowToken, List<WindowInfo> windows) {
            synchronized (AccessibilityWindowManager.this.mLock) {
                if (shouldUpdateWindowsLocked(forceSend, windows)) {
                    AccessibilityWindowManager.this.mTopFocusedDisplayId = topFocusedDisplayId;
                    AccessibilityWindowManager.this.mTopFocusedWindowToken = topFocusedWindowToken;
                    cacheWindows(windows);
                    updateWindowsLocked(AccessibilityWindowManager.this.mAccessibilityUserManager.getCurrentUserIdLocked(), windows);
                    AccessibilityWindowManager.this.mLock.notifyAll();
                }
            }
        }

        private boolean shouldUpdateWindowsLocked(boolean forceSend, List<WindowInfo> windows) {
            int windowCount;
            if (forceSend || this.mCachedWindowInfos.size() != (windowCount = windows.size())) {
                return true;
            }
            if (!this.mCachedWindowInfos.isEmpty() || !windows.isEmpty()) {
                for (int i = 0; i < windowCount; i++) {
                    WindowInfo oldWindow = this.mCachedWindowInfos.get(i);
                    WindowInfo newWindow = windows.get(i);
                    if (windowChangedNoLayer(oldWindow, newWindow)) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        }

        private void cacheWindows(List<WindowInfo> windows) {
            int oldWindowCount = this.mCachedWindowInfos.size();
            for (int i = oldWindowCount - 1; i >= 0; i--) {
                this.mCachedWindowInfos.remove(i).recycle();
            }
            int newWindowCount = windows.size();
            for (int i2 = 0; i2 < newWindowCount; i2++) {
                WindowInfo newWindow = windows.get(i2);
                this.mCachedWindowInfos.add(WindowInfo.obtain(newWindow));
            }
        }

        private boolean windowChangedNoLayer(WindowInfo oldWindow, WindowInfo newWindow) {
            if (oldWindow == newWindow) {
                return false;
            }
            if (oldWindow == null || newWindow == null || oldWindow.type != newWindow.type || oldWindow.focused != newWindow.focused) {
                return true;
            }
            if (oldWindow.token == null) {
                if (newWindow.token != null) {
                    return true;
                }
            } else if (!oldWindow.token.equals(newWindow.token)) {
                return true;
            }
            if (oldWindow.parentToken == null) {
                if (newWindow.parentToken != null) {
                    return true;
                }
            } else if (!oldWindow.parentToken.equals(newWindow.parentToken)) {
                return true;
            }
            if (oldWindow.activityToken == null) {
                if (newWindow.activityToken != null) {
                    return true;
                }
            } else if (!oldWindow.activityToken.equals(newWindow.activityToken)) {
                return true;
            }
            if (!oldWindow.regionInScreen.equals(newWindow.regionInScreen)) {
                return true;
            }
            if ((oldWindow.childTokens == null || newWindow.childTokens == null || oldWindow.childTokens.equals(newWindow.childTokens)) && TextUtils.equals(oldWindow.title, newWindow.title) && oldWindow.accessibilityIdOfAnchor == newWindow.accessibilityIdOfAnchor && oldWindow.inPictureInPicture == newWindow.inPictureInPicture && oldWindow.hasFlagWatchOutsideTouch == newWindow.hasFlagWatchOutsideTouch && oldWindow.displayId == newWindow.displayId) {
                return false;
            }
            return true;
        }

        private void clearWindowsLocked() {
            List<WindowInfo> windows = Collections.emptyList();
            int activeWindowId = AccessibilityWindowManager.this.mActiveWindowId;
            updateWindowsLocked(AccessibilityWindowManager.this.mAccessibilityUserManager.getCurrentUserIdLocked(), windows);
            AccessibilityWindowManager.this.mActiveWindowId = activeWindowId;
            this.mWindows = null;
        }

        private void updateWindowsLocked(int userId, List<WindowInfo> windows) {
            AccessibilityWindowInfo window;
            if (this.mWindows == null) {
                this.mWindows = new ArrayList();
            }
            List<AccessibilityWindowInfo> oldWindowList = new ArrayList<>(this.mWindows);
            SparseArray<AccessibilityWindowInfo> oldWindowsById = this.mA11yWindowInfoById.clone();
            boolean shouldClearAccessibilityFocus = false;
            this.mWindows.clear();
            this.mA11yWindowInfoById.clear();
            for (int i = 0; i < this.mWindowInfoById.size(); i++) {
                this.mWindowInfoById.valueAt(i).recycle();
            }
            this.mWindowInfoById.clear();
            this.mHasWatchOutsideTouchWindow = false;
            int windowCount = windows.size();
            boolean z = true;
            boolean isTopFocusedDisplay = this.mDisplayId == AccessibilityWindowManager.this.mTopFocusedDisplayId;
            boolean isAccessibilityFocusedDisplay = this.mDisplayId == AccessibilityWindowManager.this.mAccessibilityFocusedDisplayId;
            if (isTopFocusedDisplay) {
                if (windowCount <= 0) {
                    AccessibilityWindowManager.this.mTopFocusedWindowId = -1;
                } else {
                    AccessibilityWindowManager accessibilityWindowManager = AccessibilityWindowManager.this;
                    accessibilityWindowManager.mTopFocusedWindowId = accessibilityWindowManager.findWindowIdLocked(userId, accessibilityWindowManager.mTopFocusedWindowToken);
                }
                if (!AccessibilityWindowManager.this.mTouchInteractionInProgress) {
                    AccessibilityWindowManager.this.mActiveWindowId = -1;
                }
            }
            boolean activeWindowGone = true;
            if (isAccessibilityFocusedDisplay) {
                shouldClearAccessibilityFocus = AccessibilityWindowManager.this.mAccessibilityFocusedWindowId != -1;
            }
            if (windowCount > 0) {
                int i2 = 0;
                while (i2 < windowCount) {
                    WindowInfo windowInfo = windows.get(i2);
                    if (this.mTrackingWindows) {
                        window = populateReportedWindowLocked(userId, windowInfo);
                    } else {
                        window = null;
                    }
                    if (window != null) {
                        window.setLayer((windowCount - 1) - window.getLayer());
                        int windowId = window.getId();
                        if (window.isFocused() && isTopFocusedDisplay) {
                            if (!AccessibilityWindowManager.this.mTouchInteractionInProgress) {
                                AccessibilityWindowManager.this.mActiveWindowId = windowId;
                                window.setActive(z);
                            } else if (windowId == AccessibilityWindowManager.this.mActiveWindowId) {
                                activeWindowGone = false;
                            }
                        }
                        if (!this.mHasWatchOutsideTouchWindow && windowInfo.hasFlagWatchOutsideTouch) {
                            this.mHasWatchOutsideTouchWindow = z;
                        }
                        this.mWindows.add(window);
                        this.mA11yWindowInfoById.put(windowId, window);
                        this.mWindowInfoById.put(windowId, WindowInfo.obtain(windowInfo));
                    }
                    i2++;
                    z = true;
                }
                int accessibilityWindowCount = this.mWindows.size();
                if (isTopFocusedDisplay) {
                    if (AccessibilityWindowManager.this.mTouchInteractionInProgress && activeWindowGone) {
                        AccessibilityWindowManager accessibilityWindowManager2 = AccessibilityWindowManager.this;
                        accessibilityWindowManager2.mActiveWindowId = accessibilityWindowManager2.mTopFocusedWindowId;
                    }
                    for (int i3 = 0; i3 < accessibilityWindowCount; i3++) {
                        AccessibilityWindowInfo window2 = this.mWindows.get(i3);
                        if (window2.getId() == AccessibilityWindowManager.this.mActiveWindowId) {
                            window2.setActive(true);
                        }
                    }
                }
                if (isAccessibilityFocusedDisplay) {
                    int i4 = 0;
                    while (true) {
                        if (i4 >= accessibilityWindowCount) {
                            break;
                        }
                        AccessibilityWindowInfo window3 = this.mWindows.get(i4);
                        if (window3.getId() != AccessibilityWindowManager.this.mAccessibilityFocusedWindowId) {
                            i4++;
                        } else {
                            window3.setAccessibilityFocused(true);
                            shouldClearAccessibilityFocus = false;
                            break;
                        }
                    }
                }
            }
            sendEventsForChangedWindowsLocked(oldWindowList, oldWindowsById);
            int oldWindowCount = oldWindowList.size();
            for (int i5 = oldWindowCount - 1; i5 >= 0; i5--) {
                oldWindowList.remove(i5).recycle();
            }
            if (shouldClearAccessibilityFocus) {
                AccessibilityWindowManager accessibilityWindowManager3 = AccessibilityWindowManager.this;
                accessibilityWindowManager3.clearAccessibilityFocusLocked(accessibilityWindowManager3.mAccessibilityFocusedWindowId);
            }
        }

        private void sendEventsForChangedWindowsLocked(List<AccessibilityWindowInfo> oldWindows, SparseArray<AccessibilityWindowInfo> oldWindowsById) {
            List<AccessibilityEvent> events = new ArrayList<>();
            int oldWindowsCount = oldWindows.size();
            for (int i = 0; i < oldWindowsCount; i++) {
                AccessibilityWindowInfo window = oldWindows.get(i);
                if (this.mA11yWindowInfoById.get(window.getId()) == null) {
                    events.add(AccessibilityEvent.obtainWindowsChangedEvent(window.getId(), 2));
                }
            }
            int newWindowCount = this.mWindows.size();
            for (int i2 = 0; i2 < newWindowCount; i2++) {
                AccessibilityWindowInfo newWindow = this.mWindows.get(i2);
                AccessibilityWindowInfo oldWindow = oldWindowsById.get(newWindow.getId());
                if (oldWindow == null) {
                    events.add(AccessibilityEvent.obtainWindowsChangedEvent(newWindow.getId(), 1));
                } else {
                    int changes = newWindow.differenceFrom(oldWindow);
                    if (changes != 0) {
                        events.add(AccessibilityEvent.obtainWindowsChangedEvent(newWindow.getId(), changes));
                    }
                }
            }
            int numEvents = events.size();
            for (int i3 = 0; i3 < numEvents; i3++) {
                AccessibilityWindowManager.this.mAccessibilityEventSender.sendAccessibilityEventForCurrentUserLocked(events.get(i3));
            }
        }

        private AccessibilityWindowInfo populateReportedWindowLocked(int userId, WindowInfo window) {
            int windowId = AccessibilityWindowManager.this.findWindowIdLocked(userId, window.token);
            if (windowId < 0) {
                return null;
            }
            AccessibilityWindowInfo reportedWindow = AccessibilityWindowInfo.obtain();
            reportedWindow.setId(windowId);
            reportedWindow.setType(getTypeForWindowManagerWindowType(window.type));
            reportedWindow.setLayer(window.layer);
            reportedWindow.setFocused(window.focused);
            reportedWindow.setRegionInScreen(window.regionInScreen);
            reportedWindow.setTitle(window.title);
            reportedWindow.setAnchorId(window.accessibilityIdOfAnchor);
            reportedWindow.setPictureInPicture(window.inPictureInPicture);
            reportedWindow.setDisplayId(window.displayId);
            int parentId = AccessibilityWindowManager.this.findWindowIdLocked(userId, window.parentToken);
            if (parentId >= 0) {
                reportedWindow.setParentId(parentId);
            }
            if (window.childTokens != null) {
                int childCount = window.childTokens.size();
                for (int i = 0; i < childCount; i++) {
                    IBinder childToken = (IBinder) window.childTokens.get(i);
                    int childId = AccessibilityWindowManager.this.findWindowIdLocked(userId, childToken);
                    if (childId >= 0) {
                        reportedWindow.addChild(childId);
                    }
                }
            }
            return reportedWindow;
        }

        private int getTypeForWindowManagerWindowType(int windowType) {
            if (windowType != 1 && windowType != 2 && windowType != 3 && windowType != 4 && windowType != 1005) {
                if (windowType != 2017 && windowType != 2024) {
                    if (windowType == 2032) {
                        return 4;
                    }
                    if (windowType == 2034) {
                        return 5;
                    }
                    if (windowType != 2036 && windowType != 2038 && windowType != 2019 && windowType != 2020) {
                        switch (windowType) {
                            case 1000:
                            case 1001:
                            case 1002:
                            case 1003:
                                break;
                            default:
                                switch (windowType) {
                                    case 2000:
                                    case 2001:
                                    case 2003:
                                        break;
                                    case 2002:
                                        break;
                                    default:
                                        switch (windowType) {
                                            case 2005:
                                            case 2007:
                                                break;
                                            case 2006:
                                            case 2008:
                                            case 2009:
                                            case 2010:
                                                break;
                                            case 2011:
                                            case 2012:
                                                return 2;
                                            default:
                                                switch (windowType) {
                                                    case 2040:
                                                    case 2041:
                                                    case 2042:
                                                        break;
                                                    default:
                                                        return -1;
                                                }
                                        }
                                }
                        }
                    }
                }
                return 3;
            }
            return 1;
        }

        void dumpLocked(FileDescriptor fd, PrintWriter pw, String[] args) {
            List<AccessibilityWindowInfo> list = this.mWindows;
            if (list != null) {
                int windowCount = list.size();
                for (int j = 0; j < windowCount; j++) {
                    if (j == 0) {
                        pw.append("Display[");
                        pw.append((CharSequence) Integer.toString(this.mDisplayId));
                        pw.append("] : ");
                        pw.println();
                    }
                    if (j > 0) {
                        pw.append(',');
                        pw.println();
                    }
                    pw.append("Window[");
                    AccessibilityWindowInfo window = this.mWindows.get(j);
                    pw.append((CharSequence) window.toString());
                    pw.append(']');
                }
                pw.println();
            }
        }
    }

    /* loaded from: classes.dex */
    public final class RemoteAccessibilityConnection implements IBinder.DeathRecipient {
        private final IAccessibilityInteractionConnection mConnection;
        private final String mPackageName;
        private final int mUid;
        private final int mUserId;
        private final int mWindowId;

        RemoteAccessibilityConnection(int windowId, IAccessibilityInteractionConnection connection, String packageName, int uid, int userId) {
            this.mWindowId = windowId;
            this.mPackageName = packageName;
            this.mUid = uid;
            this.mUserId = userId;
            this.mConnection = connection;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public int getUid() {
            return this.mUid;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public String getPackageName() {
            return this.mPackageName;
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        public IAccessibilityInteractionConnection getRemote() {
            return this.mConnection;
        }

        void linkToDeath() throws RemoteException {
            this.mConnection.asBinder().linkToDeath(this, 0);
        }

        void unlinkToDeath() {
            this.mConnection.asBinder().unlinkToDeath(this, 0);
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            unlinkToDeath();
            synchronized (AccessibilityWindowManager.this.mLock) {
                AccessibilityWindowManager.this.removeAccessibilityInteractionConnectionLocked(this.mWindowId, this.mUserId);
            }
        }
    }

    public AccessibilityWindowManager(Object lock, Handler handler, WindowManagerInternal windowManagerInternal, AccessibilityEventSender accessibilityEventSender, AccessibilitySecurityPolicy securityPolicy, AccessibilitySecurityPolicy.AccessibilityUserManager accessibilityUserManager) {
        this.mLock = lock;
        this.mHandler = handler;
        this.mWindowManagerInternal = windowManagerInternal;
        this.mAccessibilityEventSender = accessibilityEventSender;
        this.mSecurityPolicy = securityPolicy;
        this.mAccessibilityUserManager = accessibilityUserManager;
    }

    public void startTrackingWindows(int displayId) {
        synchronized (this.mLock) {
            DisplayWindowsObserver observer = this.mDisplayWindowsObservers.get(displayId);
            if (observer == null) {
                observer = new DisplayWindowsObserver(displayId);
            }
            if (observer.isTrackingWindowsLocked()) {
                return;
            }
            if (observer.startTrackingWindowsLocked()) {
                this.mDisplayWindowsObservers.put(displayId, observer);
            }
        }
    }

    public void stopTrackingWindows(int displayId) {
        synchronized (this.mLock) {
            DisplayWindowsObserver observer = this.mDisplayWindowsObservers.get(displayId);
            if (observer != null) {
                observer.stopTrackingWindowsLocked();
                this.mDisplayWindowsObservers.remove(displayId);
            }
        }
    }

    public boolean isTrackingWindowsLocked() {
        int count = this.mDisplayWindowsObservers.size();
        if (count > 0) {
            return true;
        }
        return false;
    }

    public boolean isTrackingWindowsLocked(int displayId) {
        DisplayWindowsObserver observer = this.mDisplayWindowsObservers.get(displayId);
        if (observer != null) {
            return observer.isTrackingWindowsLocked();
        }
        return false;
    }

    public List<AccessibilityWindowInfo> getWindowListLocked(int displayId) {
        DisplayWindowsObserver observer = this.mDisplayWindowsObservers.get(displayId);
        if (observer != null) {
            return observer.getWindowListLocked();
        }
        return null;
    }

    public int addAccessibilityInteractionConnection(IWindow window, IBinder leashToken, IAccessibilityInteractionConnection connection, String packageName, int userId) throws RemoteException {
        Object obj;
        String packageName2;
        int windowId;
        int displayId;
        IBinder token;
        IBinder token2 = window.asBinder();
        int displayId2 = this.mWindowManagerInternal.getDisplayIdForWindow(token2);
        Object obj2 = this.mLock;
        synchronized (obj2) {
            try {
                int resolvedUserId = this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(userId);
                int resolvedUid = UserHandle.getUid(resolvedUserId, UserHandle.getCallingAppId());
                try {
                    String packageName3 = this.mSecurityPolicy.resolveValidReportedPackageLocked(packageName, UserHandle.getCallingAppId(), resolvedUserId, Binder.getCallingPid());
                    try {
                        int windowId2 = sNextWindowId;
                        sNextWindowId = windowId2 + 1;
                        if (this.mSecurityPolicy.isCallerInteractingAcrossUsers(userId)) {
                            try {
                                RemoteAccessibilityConnection wrapper = new RemoteAccessibilityConnection(windowId2, connection, packageName3, resolvedUid, -1);
                                wrapper.linkToDeath();
                                this.mGlobalInteractionConnections.put(windowId2, wrapper);
                                this.mGlobalWindowTokens.put(windowId2, token2);
                                packageName2 = packageName3;
                                windowId = windowId2;
                                obj = obj2;
                                displayId = displayId2;
                                token = token2;
                            } catch (Throwable th) {
                                th = th;
                                obj = obj2;
                                while (true) {
                                    try {
                                        break;
                                    } catch (Throwable th2) {
                                        th = th2;
                                    }
                                }
                                throw th;
                            }
                        } else {
                            windowId = windowId2;
                            obj = obj2;
                            displayId = displayId2;
                            packageName2 = packageName3;
                            token = token2;
                            try {
                                RemoteAccessibilityConnection wrapper2 = new RemoteAccessibilityConnection(windowId2, connection, packageName3, resolvedUid, resolvedUserId);
                                wrapper2.linkToDeath();
                                getInteractionConnectionsForUserLocked(resolvedUserId).put(windowId, wrapper2);
                                getWindowTokensForUserLocked(resolvedUserId).put(windowId, token);
                            } catch (Throwable th3) {
                                th = th3;
                                while (true) {
                                    break;
                                    break;
                                }
                                throw th;
                            }
                        }
                        boolean shouldComputeWindows = isTrackingWindowsLocked(displayId);
                        try {
                            registerIdLocked(leashToken, windowId);
                            if (shouldComputeWindows) {
                                this.mWindowManagerInternal.computeWindowsForAccessibility(displayId);
                            }
                            this.mWindowManagerInternal.setAccessibilityIdToSurfaceMetadata(token, windowId);
                            return windowId;
                        } catch (Throwable th4) {
                            th = th4;
                            while (true) {
                                break;
                                break;
                            }
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        obj = obj2;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    obj = obj2;
                    while (true) {
                        break;
                        break;
                    }
                    throw th;
                }
            } catch (Throwable th7) {
                th = th7;
            }
        }
    }

    public void removeAccessibilityInteractionConnection(IWindow window) {
        synchronized (this.mLock) {
            this.mSecurityPolicy.resolveCallingUserIdEnforcingPermissionsLocked(UserHandle.getCallingUserId());
            IBinder token = window.asBinder();
            int removedWindowId = removeAccessibilityInteractionConnectionInternalLocked(token, this.mGlobalWindowTokens, this.mGlobalInteractionConnections);
            if (removedWindowId >= 0) {
                onAccessibilityInteractionConnectionRemovedLocked(removedWindowId, token);
                return;
            }
            int userCount = this.mWindowTokens.size();
            for (int i = 0; i < userCount; i++) {
                int userId = this.mWindowTokens.keyAt(i);
                int removedWindowIdForUser = removeAccessibilityInteractionConnectionInternalLocked(token, getWindowTokensForUserLocked(userId), getInteractionConnectionsForUserLocked(userId));
                if (removedWindowIdForUser >= 0) {
                    onAccessibilityInteractionConnectionRemovedLocked(removedWindowIdForUser, token);
                    return;
                }
            }
        }
    }

    public RemoteAccessibilityConnection getConnectionLocked(int userId, int windowId) {
        RemoteAccessibilityConnection connection = this.mGlobalInteractionConnections.get(windowId);
        if (connection == null && isValidUserForInteractionConnectionsLocked(userId)) {
            connection = getInteractionConnectionsForUserLocked(userId).get(windowId);
        }
        if (connection != null && connection.getRemote() != null) {
            return connection;
        }
        return null;
    }

    private int removeAccessibilityInteractionConnectionInternalLocked(IBinder windowToken, SparseArray<IBinder> windowTokens, SparseArray<RemoteAccessibilityConnection> interactionConnections) {
        int count = windowTokens.size();
        for (int i = 0; i < count; i++) {
            if (windowTokens.valueAt(i) == windowToken) {
                int windowId = windowTokens.keyAt(i);
                windowTokens.removeAt(i);
                RemoteAccessibilityConnection wrapper = interactionConnections.get(windowId);
                wrapper.unlinkToDeath();
                interactionConnections.remove(windowId);
                return windowId;
            }
        }
        return -1;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeAccessibilityInteractionConnectionLocked(int windowId, int userId) {
        IBinder window = null;
        if (userId == -1) {
            IBinder window2 = this.mGlobalWindowTokens.get(windowId);
            window = window2;
            this.mGlobalWindowTokens.remove(windowId);
            this.mGlobalInteractionConnections.remove(windowId);
        } else {
            if (isValidUserForWindowTokensLocked(userId)) {
                IBinder window3 = getWindowTokensForUserLocked(userId).get(windowId);
                window = window3;
                getWindowTokensForUserLocked(userId).remove(windowId);
            }
            if (isValidUserForInteractionConnectionsLocked(userId)) {
                getInteractionConnectionsForUserLocked(userId).remove(windowId);
            }
        }
        onAccessibilityInteractionConnectionRemovedLocked(windowId, window);
    }

    private void onAccessibilityInteractionConnectionRemovedLocked(int windowId, IBinder binder) {
        if (!isTrackingWindowsLocked() && windowId >= 0 && this.mActiveWindowId == windowId) {
            this.mActiveWindowId = -1;
        }
        if (binder != null) {
            this.mWindowManagerInternal.setAccessibilityIdToSurfaceMetadata(binder, -1);
        }
        unregisterIdLocked(windowId);
    }

    public IBinder getWindowTokenForUserAndWindowIdLocked(int userId, int windowId) {
        IBinder windowToken = this.mGlobalWindowTokens.get(windowId);
        if (windowToken == null && isValidUserForWindowTokensLocked(userId)) {
            return getWindowTokensForUserLocked(userId).get(windowId);
        }
        return windowToken;
    }

    public int getWindowOwnerUserId(IBinder windowToken) {
        return this.mWindowManagerInternal.getWindowOwnerUserId(windowToken);
    }

    public int findWindowIdLocked(int userId, IBinder token) {
        int userIndex;
        int globalIndex = this.mGlobalWindowTokens.indexOfValue(token);
        if (globalIndex >= 0) {
            return this.mGlobalWindowTokens.keyAt(globalIndex);
        }
        if (isValidUserForWindowTokensLocked(userId) && (userIndex = getWindowTokensForUserLocked(userId).indexOfValue(token)) >= 0) {
            return getWindowTokensForUserLocked(userId).keyAt(userIndex);
        }
        return -1;
    }

    public void associateEmbeddedHierarchyLocked(IBinder host, IBinder embedded) {
        associateLocked(embedded, host);
    }

    public void disassociateEmbeddedHierarchyLocked(IBinder token) {
        disassociateLocked(token);
    }

    public int resolveParentWindowIdLocked(int windowId) {
        IBinder token = getTokenLocked(windowId);
        if (token == null) {
            return windowId;
        }
        IBinder resolvedToken = resolveTopParentTokenLocked(token);
        int resolvedWindowId = getWindowIdLocked(resolvedToken);
        return resolvedWindowId != -1 ? resolvedWindowId : windowId;
    }

    private IBinder resolveTopParentTokenLocked(IBinder token) {
        IBinder hostToken = getHostTokenLocked(token);
        if (hostToken == null) {
            return token;
        }
        return resolveTopParentTokenLocked(hostToken);
    }

    public boolean computePartialInteractiveRegionForWindowLocked(int windowId, Region outRegion) {
        int windowId2 = resolveParentWindowIdLocked(windowId);
        DisplayWindowsObserver observer = getDisplayWindowObserverByWindowIdLocked(windowId2);
        if (observer != null) {
            return observer.computePartialInteractiveRegionForWindowLocked(windowId2, outRegion);
        }
        return false;
    }

    public void updateActiveAndAccessibilityFocusedWindowLocked(int userId, int windowId, long nodeId, int eventType, int eventAction) {
        if (eventType == 32) {
            synchronized (this.mLock) {
                if (!isTrackingWindowsLocked()) {
                    int findFocusedWindowId = findFocusedWindowId(userId);
                    this.mTopFocusedWindowId = findFocusedWindowId;
                    if (windowId == findFocusedWindowId) {
                        this.mActiveWindowId = windowId;
                    }
                }
            }
        } else if (eventType == 128) {
            synchronized (this.mLock) {
                if (this.mTouchInteractionInProgress && this.mActiveWindowId != windowId) {
                    setActiveWindowLocked(windowId);
                }
            }
        } else if (eventType == 32768) {
            synchronized (this.mLock) {
                if (this.mAccessibilityFocusedWindowId != windowId) {
                    clearAccessibilityFocusLocked(this.mAccessibilityFocusedWindowId);
                    setAccessibilityFocusedWindowLocked(windowId);
                }
                this.mAccessibilityFocusNodeId = nodeId;
            }
        } else if (eventType == 65536) {
            synchronized (this.mLock) {
                if (this.mAccessibilityFocusNodeId == nodeId) {
                    this.mAccessibilityFocusNodeId = 2147483647L;
                }
                if (this.mAccessibilityFocusNodeId == 2147483647L && this.mAccessibilityFocusedWindowId == windowId && eventAction != 64) {
                    this.mAccessibilityFocusedWindowId = -1;
                    this.mAccessibilityFocusedDisplayId = -1;
                }
            }
        }
    }

    public void onTouchInteractionStart() {
        synchronized (this.mLock) {
            this.mTouchInteractionInProgress = true;
        }
    }

    public void onTouchInteractionEnd() {
        synchronized (this.mLock) {
            this.mTouchInteractionInProgress = false;
            int oldActiveWindow = this.mActiveWindowId;
            setActiveWindowLocked(this.mTopFocusedWindowId);
            boolean accessibilityFocusOnlyInActiveWindow = isTrackingWindowsLocked() ? false : true;
            if (oldActiveWindow != this.mActiveWindowId && this.mAccessibilityFocusedWindowId == oldActiveWindow && accessibilityFocusOnlyInActiveWindow) {
                clearAccessibilityFocusLocked(oldActiveWindow);
            }
        }
    }

    public int getActiveWindowId(int userId) {
        if (this.mActiveWindowId == -1 && !this.mTouchInteractionInProgress) {
            this.mActiveWindowId = findFocusedWindowId(userId);
        }
        return this.mActiveWindowId;
    }

    public int getActiveWindowIdInDisplay(int userId, int displayId) {
        int findWindowIdLocked;
        IBinder token = this.mWindowManagerInternal.getTopWindowTokenInDisplay(displayId);
        if (token == null) {
            return getActiveWindowId(userId);
        }
        synchronized (this.mLock) {
            findWindowIdLocked = findWindowIdLocked(userId, token);
        }
        return findWindowIdLocked;
    }

    private void setActiveWindowLocked(int windowId) {
        int i = this.mActiveWindowId;
        if (i != windowId) {
            this.mAccessibilityEventSender.sendAccessibilityEventForCurrentUserLocked(AccessibilityEvent.obtainWindowsChangedEvent(i, 32));
            this.mActiveWindowId = windowId;
            int count = this.mDisplayWindowsObservers.size();
            for (int i2 = 0; i2 < count; i2++) {
                DisplayWindowsObserver observer = this.mDisplayWindowsObservers.valueAt(i2);
                if (observer != null) {
                    observer.setActiveWindowLocked(windowId);
                }
            }
        }
    }

    private void setAccessibilityFocusedWindowLocked(int windowId) {
        int i = this.mAccessibilityFocusedWindowId;
        if (i != windowId) {
            this.mAccessibilityEventSender.sendAccessibilityEventForCurrentUserLocked(AccessibilityEvent.obtainWindowsChangedEvent(i, 128));
            this.mAccessibilityFocusedWindowId = windowId;
            int count = this.mDisplayWindowsObservers.size();
            for (int i2 = 0; i2 < count; i2++) {
                DisplayWindowsObserver observer = this.mDisplayWindowsObservers.valueAt(i2);
                if (observer != null) {
                    observer.setAccessibilityFocusedWindowLocked(windowId);
                }
            }
        }
    }

    public AccessibilityWindowInfo findA11yWindowInfoByIdLocked(int windowId) {
        int windowId2 = resolveParentWindowIdLocked(windowId);
        DisplayWindowsObserver observer = getDisplayWindowObserverByWindowIdLocked(windowId2);
        if (observer != null) {
            return observer.findA11yWindowInfoByIdLocked(windowId2);
        }
        return null;
    }

    public WindowInfo findWindowInfoByIdLocked(int windowId) {
        int windowId2 = resolveParentWindowIdLocked(windowId);
        DisplayWindowsObserver observer = getDisplayWindowObserverByWindowIdLocked(windowId2);
        if (observer != null) {
            return observer.findWindowInfoByIdLocked(windowId2);
        }
        return null;
    }

    public int getFocusedWindowId(int focusType) {
        if (focusType == 1) {
            return this.mTopFocusedWindowId;
        }
        if (focusType == 2) {
            return this.mAccessibilityFocusedWindowId;
        }
        return -1;
    }

    public AccessibilityWindowInfo getPictureInPictureWindowLocked() {
        AccessibilityWindowInfo windowInfo = null;
        int count = this.mDisplayWindowsObservers.size();
        for (int i = 0; i < count; i++) {
            DisplayWindowsObserver observer = this.mDisplayWindowsObservers.valueAt(i);
            if (observer != null) {
                AccessibilityWindowInfo pictureInPictureWindowLocked = observer.getPictureInPictureWindowLocked();
                windowInfo = pictureInPictureWindowLocked;
                if (pictureInPictureWindowLocked != null) {
                    break;
                }
            }
        }
        return windowInfo;
    }

    public void setPictureInPictureActionReplacingConnection(IAccessibilityInteractionConnection connection) throws RemoteException {
        synchronized (this.mLock) {
            if (this.mPictureInPictureActionReplacingConnection != null) {
                this.mPictureInPictureActionReplacingConnection.unlinkToDeath();
                this.mPictureInPictureActionReplacingConnection = null;
            }
            if (connection != null) {
                RemoteAccessibilityConnection wrapper = new RemoteAccessibilityConnection(-3, connection, "foo.bar.baz", 1000, -1);
                this.mPictureInPictureActionReplacingConnection = wrapper;
                wrapper.linkToDeath();
            }
        }
    }

    public RemoteAccessibilityConnection getPictureInPictureActionReplacingConnection() {
        return this.mPictureInPictureActionReplacingConnection;
    }

    public void notifyOutsideTouch(int userId, int targetWindowId) {
        List<RemoteAccessibilityConnection> connectionList = new ArrayList<>();
        synchronized (this.mLock) {
            DisplayWindowsObserver observer = getDisplayWindowObserverByWindowIdLocked(targetWindowId);
            if (observer != null) {
                List<Integer> outsideWindowsIds = observer.getWatchOutsideTouchWindowIdLocked(targetWindowId);
                for (int i = 0; i < outsideWindowsIds.size(); i++) {
                    connectionList.add(getConnectionLocked(userId, outsideWindowsIds.get(i).intValue()));
                }
            }
        }
        for (int i2 = 0; i2 < connectionList.size(); i2++) {
            RemoteAccessibilityConnection connection = connectionList.get(i2);
            if (connection != null) {
                try {
                    connection.getRemote().notifyOutsideTouch();
                } catch (RemoteException e) {
                }
            }
        }
    }

    public int getDisplayIdByUserIdAndWindowIdLocked(int userId, int windowId) {
        IBinder windowToken = getWindowTokenForUserAndWindowIdLocked(userId, windowId);
        int displayId = this.mWindowManagerInternal.getDisplayIdForWindow(windowToken);
        return displayId;
    }

    public ArrayList<Integer> getDisplayListLocked() {
        ArrayList<Integer> displayList = new ArrayList<>();
        int count = this.mDisplayWindowsObservers.size();
        for (int i = 0; i < count; i++) {
            DisplayWindowsObserver observer = this.mDisplayWindowsObservers.valueAt(i);
            if (observer != null) {
                displayList.add(Integer.valueOf(observer.mDisplayId));
            }
        }
        return displayList;
    }

    private int findFocusedWindowId(int userId) {
        int findWindowIdLocked;
        IBinder token = this.mWindowManagerInternal.getFocusedWindowToken();
        synchronized (this.mLock) {
            findWindowIdLocked = findWindowIdLocked(userId, token);
        }
        return findWindowIdLocked;
    }

    private boolean isValidUserForInteractionConnectionsLocked(int userId) {
        return this.mInteractionConnections.indexOfKey(userId) >= 0;
    }

    private boolean isValidUserForWindowTokensLocked(int userId) {
        return this.mWindowTokens.indexOfKey(userId) >= 0;
    }

    private SparseArray<RemoteAccessibilityConnection> getInteractionConnectionsForUserLocked(int userId) {
        SparseArray<RemoteAccessibilityConnection> connection = this.mInteractionConnections.get(userId);
        if (connection == null) {
            SparseArray<RemoteAccessibilityConnection> connection2 = new SparseArray<>();
            this.mInteractionConnections.put(userId, connection2);
            return connection2;
        }
        return connection;
    }

    private SparseArray<IBinder> getWindowTokensForUserLocked(int userId) {
        SparseArray<IBinder> windowTokens = this.mWindowTokens.get(userId);
        if (windowTokens == null) {
            SparseArray<IBinder> windowTokens2 = new SparseArray<>();
            this.mWindowTokens.put(userId, windowTokens2);
            return windowTokens2;
        }
        return windowTokens;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearAccessibilityFocusLocked(int windowId) {
        this.mHandler.sendMessage(PooledLambda.obtainMessage($$Lambda$AccessibilityWindowManager$Ky3Q5Gg_NEaXwBlFb7wxyjIUci0.INSTANCE, this, Integer.valueOf(this.mAccessibilityUserManager.getCurrentUserIdLocked()), Integer.valueOf(windowId)));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void clearAccessibilityFocusMainThread(int userId, int windowId) {
        synchronized (this.mLock) {
            RemoteAccessibilityConnection connection = getConnectionLocked(userId, windowId);
            if (connection == null) {
                return;
            }
            try {
                connection.getRemote().clearAccessibilityFocus();
            } catch (RemoteException e) {
            }
        }
    }

    private DisplayWindowsObserver getDisplayWindowObserverByWindowIdLocked(int windowId) {
        int count = this.mDisplayWindowsObservers.size();
        for (int i = 0; i < count; i++) {
            DisplayWindowsObserver observer = this.mDisplayWindowsObservers.valueAt(i);
            if (observer != null && observer.findWindowInfoByIdLocked(windowId) != null) {
                return this.mDisplayWindowsObservers.get(observer.mDisplayId);
            }
        }
        return null;
    }

    void associateLocked(IBinder embedded, IBinder host) {
        this.mHostEmbeddedMap.put(embedded, host);
    }

    void disassociateLocked(IBinder token) {
        this.mHostEmbeddedMap.remove(token);
        for (int i = this.mHostEmbeddedMap.size() - 1; i >= 0; i--) {
            if (this.mHostEmbeddedMap.valueAt(i) != null && this.mHostEmbeddedMap.valueAt(i).equals(token)) {
                this.mHostEmbeddedMap.removeAt(i);
            }
        }
    }

    void registerIdLocked(IBinder token, int windowId) {
        this.mWindowIdMap.put(windowId, token);
    }

    void unregisterIdLocked(int windowId) {
        IBinder token = this.mWindowIdMap.get(windowId);
        if (token == null) {
            return;
        }
        disassociateLocked(token);
        this.mWindowIdMap.remove(windowId);
    }

    IBinder getTokenLocked(int windowId) {
        return this.mWindowIdMap.get(windowId);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getWindowIdLocked(IBinder token) {
        int index = this.mWindowIdMap.indexOfValue(token);
        if (index == -1) {
            return index;
        }
        return this.mWindowIdMap.keyAt(index);
    }

    IBinder getHostTokenLocked(IBinder token) {
        return this.mHostEmbeddedMap.get(token);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        int count = this.mDisplayWindowsObservers.size();
        for (int i = 0; i < count; i++) {
            DisplayWindowsObserver observer = this.mDisplayWindowsObservers.valueAt(i);
            if (observer != null) {
                observer.dumpLocked(fd, pw, args);
            }
        }
    }
}