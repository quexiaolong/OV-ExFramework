package com.android.server.accessibility.gestures;

import android.content.Context;
import android.view.MotionEvent;
import com.android.server.accessibility.gestures.GestureMatcher;

/* loaded from: classes.dex */
class MultiFingerMultiTapAndHold extends MultiFingerMultiTap {
    /* JADX INFO: Access modifiers changed from: package-private */
    public MultiFingerMultiTapAndHold(Context context, int fingers, int taps, int gestureId, GestureMatcher.StateChangeListener listener) {
        super(context, fingers, taps, gestureId, listener);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.MultiFingerMultiTap, com.android.server.accessibility.gestures.GestureMatcher
    public void onPointerDown(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        super.onPointerDown(event, rawEvent, policyFlags);
        if (this.mIsTargetFingerCountReached && this.mCompletedTapCount + 1 == this.mTargetTapCount) {
            completeAfterLongPressTimeout(event, rawEvent, policyFlags);
        }
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.android.server.accessibility.gestures.MultiFingerMultiTap, com.android.server.accessibility.gestures.GestureMatcher
    public void onUp(MotionEvent event, MotionEvent rawEvent, int policyFlags) {
        if (this.mCompletedTapCount + 1 == this.mTargetFingerCount) {
            cancelGesture(event, rawEvent, policyFlags);
            return;
        }
        super.onUp(event, rawEvent, policyFlags);
        cancelAfterDoubleTapTimeout(event, rawEvent, policyFlags);
    }

    @Override // com.android.server.accessibility.gestures.MultiFingerMultiTap, com.android.server.accessibility.gestures.GestureMatcher
    public String getGestureName() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.mTargetFingerCount);
        builder.append("-Finger ");
        if (this.mTargetTapCount == 1) {
            builder.append("Single");
        } else if (this.mTargetTapCount == 2) {
            builder.append("Double");
        } else if (this.mTargetTapCount == 3) {
            builder.append("Triple");
        } else if (this.mTargetTapCount > 3) {
            builder.append(this.mTargetTapCount);
        }
        builder.append(" Tap and hold");
        return builder.toString();
    }
}