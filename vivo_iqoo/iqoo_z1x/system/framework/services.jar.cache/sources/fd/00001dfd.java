package com.android.server.wm;

import android.app.WindowConfiguration;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.proto.ProtoOutputStream;
import com.android.server.wm.ConfigurationContainer;
import java.io.PrintWriter;
import java.util.ArrayList;

/* loaded from: classes2.dex */
public abstract class ConfigurationContainer<E extends ConfigurationContainer> {
    static final int BOUNDS_CHANGE_NONE = 0;
    static final int BOUNDS_CHANGE_POSITION = 1;
    static final int BOUNDS_CHANGE_SIZE = 2;
    private boolean mHasOverrideConfiguration;
    private Rect mReturnBounds = new Rect();
    private Configuration mRequestedOverrideConfiguration = new Configuration();
    private Configuration mResolvedOverrideConfiguration = new Configuration();
    private Configuration mFullConfiguration = new Configuration();
    private Configuration mMergedOverrideConfiguration = new Configuration();
    private ArrayList<ConfigurationContainerListener> mChangeListeners = new ArrayList<>();
    private final Configuration mRequestsTmpConfig = new Configuration();
    private final Configuration mResolvedTmpConfig = new Configuration();
    private final Rect mTmpRect = new Rect();

    protected abstract E getChildAt(int i);

    protected abstract int getChildCount();

    protected abstract ConfigurationContainer getParent();

    public Configuration getConfiguration() {
        return this.mFullConfiguration;
    }

    public void onConfigurationChanged(Configuration newParentConfig) {
        this.mResolvedTmpConfig.setTo(this.mResolvedOverrideConfiguration);
        resolveOverrideConfiguration(newParentConfig);
        this.mFullConfiguration.setTo(newParentConfig);
        this.mFullConfiguration.updateFrom(this.mResolvedOverrideConfiguration);
        onMergedOverrideConfigurationChanged();
        if (!this.mResolvedTmpConfig.equals(this.mResolvedOverrideConfiguration)) {
            for (int i = this.mChangeListeners.size() - 1; i >= 0; i--) {
                this.mChangeListeners.get(i).onRequestedOverrideConfigurationChanged(this.mResolvedOverrideConfiguration);
            }
        }
        for (int i2 = this.mChangeListeners.size() - 1; i2 >= 0; i2--) {
            this.mChangeListeners.get(i2).onMergedOverrideConfigurationChanged(this.mMergedOverrideConfiguration);
        }
        int i3 = getChildCount();
        for (int i4 = i3 - 1; i4 >= 0; i4--) {
            ConfigurationContainer child = getChildAt(i4);
            child.onConfigurationChanged(this.mFullConfiguration);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void resolveOverrideConfiguration(Configuration newParentConfig) {
        this.mResolvedOverrideConfiguration.setTo(this.mRequestedOverrideConfiguration);
    }

    public Configuration getRequestedOverrideConfiguration() {
        return this.mRequestedOverrideConfiguration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Configuration getResolvedOverrideConfiguration() {
        return this.mResolvedOverrideConfiguration;
    }

    public void onRequestedOverrideConfigurationChanged(Configuration overrideConfiguration) {
        this.mHasOverrideConfiguration = !Configuration.EMPTY.equals(overrideConfiguration);
        this.mRequestedOverrideConfiguration.setTo(overrideConfiguration);
        ConfigurationContainer parent = getParent();
        onConfigurationChanged(parent != null ? parent.getConfiguration() : Configuration.EMPTY);
    }

    public Configuration getMergedOverrideConfiguration() {
        return this.mMergedOverrideConfiguration;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onMergedOverrideConfigurationChanged() {
        ConfigurationContainer parent = getParent();
        if (parent != null) {
            this.mMergedOverrideConfiguration.setTo(parent.getMergedOverrideConfiguration());
            this.mMergedOverrideConfiguration.updateFrom(this.mResolvedOverrideConfiguration);
        } else {
            this.mMergedOverrideConfiguration.setTo(this.mResolvedOverrideConfiguration);
        }
        for (int i = getChildCount() - 1; i >= 0; i--) {
            ConfigurationContainer child = getChildAt(i);
            child.onMergedOverrideConfigurationChanged();
        }
    }

    public boolean matchParentBounds() {
        return getRequestedOverrideBounds().isEmpty();
    }

    public boolean equivalentRequestedOverrideBounds(Rect bounds) {
        return equivalentBounds(getRequestedOverrideBounds(), bounds);
    }

    public static boolean equivalentBounds(Rect bounds, Rect other) {
        return bounds == other || (bounds != null && (bounds.equals(other) || (bounds.isEmpty() && other == null))) || (other != null && other.isEmpty() && bounds == null);
    }

    public Rect getBounds() {
        this.mReturnBounds.set(getConfiguration().windowConfiguration.getBounds());
        return this.mReturnBounds;
    }

    public void getBounds(Rect outBounds) {
        outBounds.set(getBounds());
    }

    public void getPosition(Point out) {
        Rect bounds = getBounds();
        out.set(bounds.left, bounds.top);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Rect getResolvedOverrideBounds() {
        this.mReturnBounds.set(getResolvedOverrideConfiguration().windowConfiguration.getBounds());
        return this.mReturnBounds;
    }

    public Rect getRequestedOverrideBounds() {
        this.mReturnBounds.set(getRequestedOverrideConfiguration().windowConfiguration.getBounds());
        return this.mReturnBounds;
    }

    public boolean hasOverrideBounds() {
        return !getRequestedOverrideBounds().isEmpty();
    }

    public void getRequestedOverrideBounds(Rect outBounds) {
        outBounds.set(getRequestedOverrideBounds());
    }

    public int setBounds(Rect bounds) {
        int boundsChange = diffRequestedOverrideBounds(bounds);
        if (boundsChange == 0) {
            return boundsChange;
        }
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setBounds(bounds);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
        return boundsChange;
    }

    public int setBounds(int left, int top, int right, int bottom) {
        this.mTmpRect.set(left, top, right, bottom);
        return setBounds(this.mTmpRect);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int diffRequestedOverrideBounds(Rect bounds) {
        if (equivalentRequestedOverrideBounds(bounds)) {
            return 0;
        }
        int boundsChange = 0;
        Rect existingBounds = getRequestedOverrideBounds();
        if (bounds == null || existingBounds.left != bounds.left || existingBounds.top != bounds.top) {
            boundsChange = 0 | 1;
        }
        if (bounds == null || existingBounds.width() != bounds.width() || existingBounds.height() != bounds.height()) {
            return boundsChange | 2;
        }
        return boundsChange;
    }

    boolean hasOverrideConfiguration() {
        return this.mHasOverrideConfiguration;
    }

    public WindowConfiguration getWindowConfiguration() {
        return this.mFullConfiguration.windowConfiguration;
    }

    public int getWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode();
    }

    public int getRequestedOverrideWindowingMode() {
        return this.mRequestedOverrideConfiguration.windowConfiguration.getWindowingMode();
    }

    public void setWindowingMode(int windowingMode) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setWindowingMode(windowingMode);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    public void setAlwaysOnTop(boolean alwaysOnTop) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setAlwaysOnTop(alwaysOnTop);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void setDisplayWindowingMode(int windowingMode) {
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setDisplayWindowingMode(windowingMode);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    public boolean inMultiWindowMode() {
        int windowingMode = this.mFullConfiguration.windowConfiguration.getWindowingMode();
        return WindowConfiguration.inMultiWindowMode(windowingMode);
    }

    public boolean inSplitScreenWindowingMode() {
        int windowingMode = this.mFullConfiguration.windowConfiguration.getWindowingMode();
        return windowingMode == 3 || windowingMode == 4;
    }

    public boolean inSplitScreenSecondaryWindowingMode() {
        int windowingMode = this.mFullConfiguration.windowConfiguration.getWindowingMode();
        return windowingMode == 4;
    }

    public boolean inSplitScreenPrimaryWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode() == 3;
    }

    public boolean supportsSplitScreenWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.supportSplitScreenWindowingMode();
    }

    public boolean inPinnedWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode() == 2;
    }

    public boolean inFreeformWindowingMode() {
        return this.mFullConfiguration.windowConfiguration.getWindowingMode() == 5;
    }

    public int getActivityType() {
        return this.mFullConfiguration.windowConfiguration.getActivityType();
    }

    public void setActivityType(int activityType) {
        int currentActivityType = getActivityType();
        if (currentActivityType == activityType) {
            return;
        }
        if (currentActivityType != 0) {
            throw new IllegalStateException("Can't change activity type once set: " + this + " activityType=" + WindowConfiguration.activityTypeToString(activityType));
        }
        this.mRequestsTmpConfig.setTo(getRequestedOverrideConfiguration());
        this.mRequestsTmpConfig.windowConfiguration.setActivityType(activityType);
        onRequestedOverrideConfigurationChanged(this.mRequestsTmpConfig);
    }

    public boolean isActivityTypeHome() {
        return getActivityType() == 2;
    }

    public boolean isActivityTypeRecents() {
        return getActivityType() == 3;
    }

    public boolean isActivityTypeAssistant() {
        return getActivityType() == 4;
    }

    public boolean isActivityTypeDream() {
        return getActivityType() == 5;
    }

    public boolean isActivityTypeStandard() {
        return getActivityType() == 1;
    }

    public boolean isActivityTypeStandardOrUndefined() {
        int activityType = getActivityType();
        return activityType == 1 || activityType == 0;
    }

    public boolean hasCompatibleActivityType(ConfigurationContainer other) {
        int thisType = getActivityType();
        int otherType = other.getActivityType();
        if (thisType == otherType) {
            return true;
        }
        if (thisType == 4) {
            return false;
        }
        return thisType == 0 || otherType == 0;
    }

    public boolean isCompatible(int windowingMode, int activityType) {
        int thisActivityType = getActivityType();
        int thisWindowingMode = getWindowingMode();
        boolean sameActivityType = thisActivityType == activityType;
        boolean sameWindowingMode = thisWindowingMode == windowingMode;
        if (sameActivityType && sameWindowingMode) {
            return true;
        }
        if ((activityType != 0 && activityType != 1) || !isActivityTypeStandardOrUndefined()) {
            return sameActivityType;
        }
        return sameWindowingMode;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void registerConfigurationChangeListener(ConfigurationContainerListener listener) {
        if (this.mChangeListeners.contains(listener)) {
            return;
        }
        this.mChangeListeners.add(listener);
        listener.onRequestedOverrideConfigurationChanged(this.mResolvedOverrideConfiguration);
        listener.onMergedOverrideConfigurationChanged(this.mMergedOverrideConfiguration);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unregisterConfigurationChangeListener(ConfigurationContainerListener listener) {
        this.mChangeListeners.remove(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean containsListener(ConfigurationContainerListener listener) {
        return this.mChangeListeners.contains(listener);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void onParentChanged(ConfigurationContainer newParent, ConfigurationContainer oldParent) {
        if (newParent != null) {
            onConfigurationChanged(newParent.mFullConfiguration);
            onMergedOverrideConfigurationChanged();
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void dumpDebug(ProtoOutputStream proto, long fieldId, int logLevel) {
        if (logLevel != 0 && !this.mHasOverrideConfiguration) {
            return;
        }
        long token = proto.start(fieldId);
        this.mRequestedOverrideConfiguration.dumpDebug(proto, 1146756268033L, logLevel == 2);
        if (logLevel == 0) {
            this.mFullConfiguration.dumpDebug(proto, 1146756268034L, false);
            this.mMergedOverrideConfiguration.dumpDebug(proto, 1146756268035L, false);
        }
        proto.end(token);
    }

    public void dumpChildrenNames(PrintWriter pw, String prefix) {
        String childPrefix = prefix + " ";
        pw.println(getName() + " type=" + WindowConfiguration.activityTypeToString(getActivityType()) + " mode=" + WindowConfiguration.windowingModeToString(getWindowingMode()) + " override-mode=" + WindowConfiguration.windowingModeToString(getRequestedOverrideWindowingMode()));
        for (int i = getChildCount() + (-1); i >= 0; i += -1) {
            E cc = getChildAt(i);
            pw.print(childPrefix + "#" + i + " ");
            cc.dumpChildrenNames(pw, childPrefix);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public String getName() {
        return toString();
    }

    public boolean isAlwaysOnTop() {
        return this.mFullConfiguration.windowConfiguration.isAlwaysOnTop();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean hasChild() {
        return getChildCount() > 0;
    }
}