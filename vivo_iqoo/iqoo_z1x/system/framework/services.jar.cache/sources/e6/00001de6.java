package com.android.server.wm;

import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes2.dex */
public interface AnimationAdapter {
    public static final long STATUS_BAR_TRANSITION_DURATION = 120;

    void dump(PrintWriter printWriter, String str);

    void dumpDebug(ProtoOutputStream protoOutputStream);

    long getDurationHint();

    boolean getShowWallpaper();

    long getStatusBarTransitionsStartTime();

    void onAnimationCancelled(SurfaceControl surfaceControl);

    void startAnimation(SurfaceControl surfaceControl, SurfaceControl.Transaction transaction, int i, SurfaceAnimator.OnAnimationFinishedCallback onAnimationFinishedCallback);

    default void dumpDebug(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        dumpDebug(proto);
        proto.end(token);
    }

    default boolean shouldDeferAnimationFinish(Runnable endDeferFinishCallback) {
        return false;
    }
}