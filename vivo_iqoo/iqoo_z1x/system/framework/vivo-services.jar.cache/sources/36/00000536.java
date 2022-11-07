package com.android.server.wm;

import android.graphics.GraphicBuffer;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Debug;
import android.os.IRemoteCallback;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.ArraySet;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.view.animation.ScaleAnimation;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoAppTransitionImpl implements IVivoAppTransition {
    static final boolean ENABLE_EXIT = SystemProperties.getBoolean("persist.debug.transition.exit", false);
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_DOWN_VIVO = 1010;
    private static final int NEXT_TRANSIT_TYPE_THUMBNAIL_SCALE_UP_VIVO = 1009;
    private static final String TAG = "VivoAppTransitionImpl";
    private static final int TRANSITION_ENTER_APP_ALPHA_DURATION = 50;
    private static final int TRANSITION_ENTER_APP_ALPHA_OFFSET = 0;
    private static final int TRANSITION_ENTER_DURATION_TOTAL = 250;
    private static final int TRANSITION_ENTER_ICON_ALPHA_DURATION = 50;
    private static final int TRANSITION_ENTER_ICON_ALPHA_OFFSET = 29;
    private static final int TRANSITION_EXIT_APP_ALPHA_DURATION = 180;
    private static final int TRANSITION_EXIT_APP_ALPHA_OFFSET = 0;
    private static final int TRANSITION_EXIT_APP_CENTER_ALPHA_DURATION = 230;
    private static final int TRANSITION_EXIT_APP_CENTER_ALPHA_OFFSET = 120;
    private static final float TRANSITION_EXIT_APP_SCALE = 0.5f;
    private static final int TRANSITION_EXIT_DURATION_TOTAL = 250;
    private static final int TRANSITION_EXIT_ICON_ALPHA_DURATION = 45;
    private static final int TRANSITION_LAUNCHER_DURATION = 250;
    private static final float TRANSITION_LAUNCHER_SCALE = 0.9f;
    private boolean mAnimateExit;
    private IRemoteCallback mAnimateTransitionCallback;
    private AppTransition mAppTransition;
    private final Rect mTmpRect = new Rect();
    long delayAnimate = Long.parseLong(SystemProperties.get("persist.sys.delayAnimate", "0"));
    float widthScale = Float.parseFloat(SystemProperties.get("persist.sys.widthScale", "1"));
    float heightScale = Float.parseFloat(SystemProperties.get("persist.sys.heightScale", "0.75"));
    Animation.AnimationListener mLlistener = new Animation.AnimationListener() { // from class: com.android.server.wm.VivoAppTransitionImpl.1
        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationStart(Animation animation) {
            if (WindowManagerDebugConfig.DEBUG_ANIM) {
                VSlog.d("xindonghua", "onAnimationStart!");
            }
            if (VivoAppTransitionImpl.this.mAnimateTransitionCallback != null) {
                VivoAppTransitionImpl.this.mAnimateTransitionCallback = null;
            }
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationEnd(Animation animation) {
        }

        @Override // android.view.animation.Animation.AnimationListener
        public void onAnimationRepeat(Animation animation) {
        }
    };
    private Interpolator mThumbnailBezierVivoInterpolator = createBezierInterpolator(new PointF(0.16f, 0.17f), new PointF(0.13f, 1.0f));
    private Interpolator mSecThumbnailBezierVivoInterpolator = createBezierInterpolator(new PointF(0.0f, TRANSITION_EXIT_APP_SCALE), new PointF(0.0f, 1.0f));

    public Animation createNullAnimation(boolean enter) {
        float alpha = enter ? 0.0f : 1.0f;
        Animation a = new AlphaAnimation(alpha, alpha);
        if (alpha > 0.0f) {
            a.setZAdjustment(1);
        }
        a.setDuration(300L);
        a.setFillAfter(true);
        return a;
    }

    public VivoAppTransitionImpl(AppTransition appTransition) {
        this.mAppTransition = appTransition;
    }

    public Interpolator createBezierInterpolator(PointF... points) {
        Path bezierPath = VivoBezierUtil.buildPath(points[0], points[1]);
        Interpolator bezierVivoInterpolator = new PathInterpolator(bezierPath);
        return bezierVivoInterpolator;
    }

    public void overridePendingAppTransitionThumbFromLauncher(GraphicBuffer srcThumb, int startX, int startY, boolean scaleUp, boolean animateExit, IRemoteCallback animateCallback) {
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            StringBuilder sb = new StringBuilder();
            sb.append("overridePendingAppTransitionThumbFromLauncher  : srcThumb = ");
            sb.append(srcThumb);
            sb.append(", bmpWidth = ");
            sb.append(srcThumb != null ? Integer.valueOf(srcThumb.getWidth()) : "null");
            sb.append(", bmpHeight = ");
            sb.append(srcThumb != null ? Integer.valueOf(srcThumb.getHeight()) : "null");
            sb.append(", startX = ");
            sb.append(startX);
            sb.append(", startY = ");
            sb.append(startY);
            sb.append(", scaleUp = ");
            sb.append(scaleUp);
            sb.append(", animateExit = ");
            sb.append(animateExit);
            sb.append(", animateCallback = ");
            sb.append(animateCallback);
            sb.append(", isTransitionSet() = ");
            sb.append(this.mAppTransition.isTransitionSet());
            VSlog.i("xindonghua", sb.toString());
        }
        if (this.mAppTransition.isTransitionSet()) {
            this.mAppTransition.mNextAppTransitionType = scaleUp ? 1009 : 1010;
            this.mAppTransition.mNextAppTransitionScaleUp = scaleUp;
            this.mAnimateExit = animateExit;
            int srcWidth = srcThumb != null ? srcThumb.getWidth() : 0;
            int srcHeight = srcThumb != null ? srcThumb.getHeight() : 0;
            this.mAppTransition.putDefaultNextAppTransitionCoordinates(startX, startY, srcWidth, srcHeight, srcThumb);
            this.mAppTransition.postAnimationCallback();
            this.mAnimateTransitionCallback = animateCallback;
        }
    }

    public Animation createThumbnailEnterExitAnimationVivoLocked(int nextAppTransitionType, int thumbTransitState, Rect containingFrame, int transit, WindowContainer container) {
        boolean z;
        Animation alpha;
        float scaleW;
        Animation scale;
        if (nextAppTransitionType != 1009 && nextAppTransitionType != 1010) {
            return null;
        }
        this.mAppTransition.mNextAppTransitionScaleUp = nextAppTransitionType == 1009;
        int appWidth = containingFrame.width();
        int appHeight = containingFrame.height();
        GraphicBuffer thumbnailHeader = this.mAppTransition.getAppTransitionThumbnailHeader(container);
        this.mAppTransition.getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = thumbnailHeader != null ? thumbnailHeader.getWidth() : 0;
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1.0f;
        int thumbHeightI = thumbnailHeader != null ? thumbnailHeader.getHeight() : 0;
        float thumbHeight = thumbHeightI > 0 ? thumbHeightI : 1.0f;
        if (thumbTransitState == 0) {
            float scaleW2 = thumbWidth / appWidth;
            float scaleH = thumbHeight / appHeight;
            Animation scale2 = new ScaleAnimation(scaleW2, 1.0f, scaleH, 1.0f, AppTransition.computePivot(this.mTmpRect.left, scaleW2), AppTransition.computePivot(this.mTmpRect.top, scaleH));
            scale2.setInterpolator(this.mSecThumbnailBezierVivoInterpolator);
            scale2.setDuration(250L);
            Animation alpha2 = new AlphaAnimation(0.0f, 1.0f);
            alpha2.setDuration(50L);
            AnimationSet set = new AnimationSet(false);
            z = true;
            set.setZAdjustment(1);
            set.addAnimation(scale2);
            set.addAnimation(alpha2);
            alpha = set;
        } else if (thumbTransitState != 1) {
            if (thumbTransitState == 2) {
                if (!this.mAnimateExit) {
                    alpha = new AlphaAnimation(1.0f, 1.0f);
                    alpha.setDuration(250L);
                    z = true;
                } else {
                    alpha = new ScaleAnimation(TRANSITION_LAUNCHER_SCALE, 1.0f, TRANSITION_LAUNCHER_SCALE, 1.0f, appWidth / 2, appHeight / 2);
                    alpha.setInterpolator(this.mThumbnailBezierVivoInterpolator);
                    alpha.setDuration(250L);
                    z = true;
                }
            } else if (thumbTransitState == 3) {
                if (!ENABLE_EXIT) {
                    scaleW = 0.5f;
                    this.mTmpRect.left = (int) ((appWidth * TRANSITION_EXIT_APP_SCALE) / 2.0f);
                    this.mTmpRect.top = (int) ((appHeight * TRANSITION_EXIT_APP_SCALE) / 2.0f);
                    scale = new ScaleAnimation(1.0f, TRANSITION_EXIT_APP_SCALE, 1.0f, TRANSITION_EXIT_APP_SCALE, AppTransition.computePivot(this.mTmpRect.left, (float) TRANSITION_EXIT_APP_SCALE), AppTransition.computePivot(this.mTmpRect.top, (float) TRANSITION_EXIT_APP_SCALE));
                } else {
                    float scaleH2 = this.widthScale;
                    scaleW = (scaleH2 * thumbWidth) / appWidth;
                    float f = this.heightScale;
                    float scaleH3 = (f * thumbHeight) / appHeight;
                    float widthOffset = ((1.0f - scaleH2) * thumbWidth) / 2.0f;
                    float heightOffset = ((1.0f - f) * thumbHeight) / 2.0f;
                    float pivotX = AppTransition.computePivot(this.mTmpRect.left + ((int) widthOffset), scaleW);
                    float pivotY = AppTransition.computePivot(this.mTmpRect.top + ((int) heightOffset), scaleH3);
                    scale = new ScaleAnimation(1.0f, scaleW, 1.0f, scaleH3, pivotX, pivotY);
                }
                scale.setInterpolator(this.mThumbnailBezierVivoInterpolator);
                scale.setDuration(250L);
                Animation alpha3 = new AlphaAnimation(1.0f, 0.0f);
                if (!ENABLE_EXIT) {
                    alpha3.setDuration(180L);
                    alpha3.setStartOffset(0L);
                    alpha3.setAnimationListener(this.mLlistener);
                } else {
                    alpha3.setDuration(230L);
                    alpha3.setAnimationListener(this.mLlistener);
                }
                AnimationSet set2 = new AnimationSet(false);
                set2.addAnimation(scale);
                set2.addAnimation(alpha3);
                set2.setZAdjustment(1);
                alpha = set2;
                z = true;
            } else {
                throw new RuntimeException("Invalid thumbnail transition state");
            }
        } else if (!this.mAnimateExit) {
            alpha = new AlphaAnimation(1.0f, 1.0f);
            alpha.setDuration(250L);
            z = true;
        } else if (transit == 14) {
            alpha = new AlphaAnimation(1.0f, 0.0f);
            z = true;
        } else {
            alpha = new ScaleAnimation(1.0f, TRANSITION_LAUNCHER_SCALE, 1.0f, TRANSITION_LAUNCHER_SCALE, appWidth / 2, appHeight / 2);
            alpha.setInterpolator(this.mThumbnailBezierVivoInterpolator);
            alpha.setDuration(250L);
            z = true;
        }
        alpha.setFillAfter(z);
        alpha.initialize(appWidth, appHeight, appWidth, appHeight);
        if (WindowManagerDebugConfig.DEBUG_ANIM) {
            String animName = this.mAppTransition.mNextAppTransitionScaleUp ? "ANIM_THUMBNAIL_SCALE_UP_VIVO" : "ANIM_THUMBNAIL_SCALE_DOWN_VIVO";
            VSlog.v(TAG, "applyAnimation: anim=" + alpha + " nextAppTransition=" + animName + " transit=" + AppTransition.appTransitionToString(transit) + " thumbTransitState=" + thumbTransitState + " Callers=" + Debug.getCallers(3));
        }
        return alpha;
    }

    Animation createThumbnailScaleAnimationVivoLocked(int appWidth, int appHeight, int transit, GraphicBuffer thumbnailHeader) {
        Animation alpha;
        this.mAppTransition.getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = thumbnailHeader != null ? thumbnailHeader.getWidth() : 0;
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1.0f;
        int thumbHeightI = thumbnailHeader != null ? thumbnailHeader.getHeight() : 0;
        float thumbHeight = thumbHeightI > 0 ? thumbHeightI : 1.0f;
        float scaleW = appWidth / thumbWidth;
        float scaleH = appHeight / thumbHeight;
        if (this.mAppTransition.mNextAppTransitionScaleUp) {
            Animation scale = new ScaleAnimation(1.0f, scaleW, 1.0f, scaleH, AppTransition.computePivot(this.mTmpRect.left, 1.0f / scaleW), AppTransition.computePivot(this.mTmpRect.top, 1.0f / scaleH));
            scale.setInterpolator(this.mThumbnailBezierVivoInterpolator);
            scale.setDuration(250L);
            Animation alpha2 = new AlphaAnimation(1.0f, 0.0f);
            alpha2.setDuration(50L);
            alpha2.setStartOffset(29L);
            AnimationSet set = new AnimationSet(false);
            set.addAnimation(scale);
            set.addAnimation(alpha2);
            alpha = set;
        } else {
            Animation scale2 = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, AppTransition.computePivot(this.mTmpRect.left, 1.0f / scaleW), AppTransition.computePivot(this.mTmpRect.top, 1.0f / scaleH));
            scale2.setInterpolator(this.mThumbnailBezierVivoInterpolator);
            scale2.setDuration(250L);
            Animation alpha3 = new AlphaAnimation(0.0f, 1.0f);
            alpha3.setDuration(45L);
            AnimationSet set2 = new AnimationSet(false);
            set2.addAnimation(scale2);
            set2.addAnimation(alpha3);
            alpha = set2;
        }
        alpha.setFillAfter(true);
        alpha.initialize(appWidth, appHeight, appWidth, appHeight);
        return alpha;
    }

    Animation createThumbnailScrimScaleAnimationVivoLocked(int appWidth, int appHeight, int transit, GraphicBuffer thumbnailHeader) {
        Animation alpha;
        this.mAppTransition.getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = thumbnailHeader != null ? thumbnailHeader.getWidth() : 0;
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1.0f;
        int thumbHeightI = thumbnailHeader != null ? thumbnailHeader.getHeight() : 0;
        float thumbHeight = thumbHeightI > 0 ? thumbHeightI : 1.0f;
        float scaleW = appWidth / thumbWidth;
        float scaleH = appHeight / thumbHeight;
        if (this.mAppTransition.mNextAppTransitionScaleUp) {
            Animation scale = new ScaleAnimation(1.0f, scaleW, 1.0f, scaleH, AppTransition.computePivot(this.mTmpRect.left, 1.0f / scaleW), AppTransition.computePivot(this.mTmpRect.top, 1.0f / scaleH));
            scale.setInterpolator(this.mThumbnailBezierVivoInterpolator);
            scale.setDuration(250L);
            Animation alpha2 = new AlphaAnimation(0.0f, 1.0f);
            alpha2.setDuration(50L);
            AnimationSet set = new AnimationSet(false);
            set.addAnimation(scale);
            set.addAnimation(alpha2);
            alpha = set;
        } else {
            Animation scale2 = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, AppTransition.computePivot(this.mTmpRect.left, 1.0f / scaleW), AppTransition.computePivot(this.mTmpRect.top, 1.0f / scaleH));
            scale2.setInterpolator(this.mThumbnailBezierVivoInterpolator);
            scale2.setDuration(250L);
            Animation alpha3 = new AlphaAnimation(1.0f, 0.0f);
            if (thumbnailHeader != null || !ENABLE_EXIT) {
                alpha3.setDuration(180L);
                alpha3.setStartOffset(0L);
            } else {
                alpha3.setDuration(230L);
                alpha3.setStartOffset(120L);
            }
            AnimationSet set2 = new AnimationSet(false);
            set2.addAnimation(scale2);
            set2.addAnimation(alpha3);
            alpha = set2;
        }
        alpha.setFillAfter(true);
        alpha.initialize(appWidth, appHeight, appWidth, appHeight);
        return alpha;
    }

    boolean isNextThumbnailTransitionScaleUpVivo() {
        return this.mAppTransition.mNextAppTransitionType == 1009 || this.mAppTransition.mNextAppTransitionType == 1010;
    }

    public Animation setAnimationForVivoFreeform(boolean freeform, Animation a, int transit, boolean enter, Rect frame, Rect displayFrame, WindowManagerService service, WindowContainer container) {
        ActivityRecord target = null;
        if (container instanceof ActivityRecord) {
            target = (ActivityRecord) container;
        } else if (container instanceof ActivityStack) {
            target = ((ActivityStack) container).getTopMostActivity();
        }
        if (target == null) {
            return a;
        }
        if ((service.isVivoFreeFormValid() && freeform) || ((service.isClosingFreeForm() || service.isExitingFreeForm()) && target.getIsFromFreeform())) {
            service.setFreeformAnimating(true);
            Animation a2 = loadAnimationForVivoFreeform(a, transit, enter, frame, displayFrame, target);
            if (target.getIsFromFreeform() && service.isClosingFreeForm() && service.isTopFullscreenIsTranslucency()) {
                a2 = null;
                service.setTopFullscreenIsTranslucency(false);
            }
            if (a2 != null) {
                a2.setZAdjustment(1);
            }
            service.setClosingFreeForm(false);
            return a2;
        }
        return a;
    }

    /* JADX WARN: Removed duplicated region for block: B:27:0x006e A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:28:0x006f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    private android.view.animation.Animation loadAnimationForVivoFreeform(android.view.animation.Animation r7, int r8, boolean r9, android.graphics.Rect r10, android.graphics.Rect r11, com.android.server.wm.ActivityRecord r12) {
        /*
            r6 = this;
            r0 = 0
            r1 = 1056964608(0x3f000000, float:0.5)
            r2 = 50593834(0x304002a, float:3.87915E-37)
            r3 = 1065353216(0x3f800000, float:1.0)
            if (r8 == 0) goto L56
            switch(r8) {
                case 6: goto L31;
                case 7: goto L42;
                case 8: goto L56;
                case 9: goto Le;
                case 10: goto L56;
                case 11: goto Le;
                default: goto Ld;
            }
        Ld:
            goto L6c
        Le:
            if (r9 != 0) goto L2c
            com.android.server.wm.AppTransition r4 = r6.mAppTransition
            com.android.server.wm.WindowManagerService r4 = r4.mService
            android.content.Context r4 = r4.mContext
            android.view.animation.Animation r0 = android.view.animation.AnimationUtils.loadAnimation(r4, r2)
            android.view.animation.Animation r0 = r6.createFreeformWindowExitAnim(r10)
            if (r12 == 0) goto L6c
            java.lang.String r2 = r12.packageName
            java.lang.String r4 = "com.tencent.mm"
            boolean r2 = r4.equals(r2)
            if (r2 == 0) goto L6c
            r0 = 0
            goto L6c
        L2c:
            android.view.animation.Animation r0 = r6.createFreeformTaskOpenAnim(r10)
            goto L6c
        L31:
            if (r9 == 0) goto L42
            com.android.server.wm.AppTransition r2 = r6.mAppTransition
            com.android.server.wm.WindowManagerService r2 = r2.mService
            boolean r2 = r2.isEnteringFreeForm()
            if (r2 == 0) goto L42
            android.view.animation.Animation r0 = r6.createFreeformTaskOpenAnim(r10)
            goto L6c
        L42:
            if (r10 == 0) goto L6c
            android.view.animation.AlphaAnimation r2 = new android.view.animation.AlphaAnimation
            if (r9 == 0) goto L4c
            r2.<init>(r1, r3)
            goto L4f
        L4c:
            r2.<init>(r3, r3)
        L4f:
            r0 = r2
            r4 = 300(0x12c, double:1.48E-321)
            r0.setDuration(r4)
            goto L6c
        L56:
            if (r9 == 0) goto L5d
            android.view.animation.Animation r0 = r6.createFreeformTaskOpenAnim(r10)
            goto L6c
        L5d:
            com.android.server.wm.AppTransition r4 = r6.mAppTransition
            com.android.server.wm.WindowManagerService r4 = r4.mService
            android.content.Context r4 = r4.mContext
            android.view.animation.Animation r0 = android.view.animation.AnimationUtils.loadAnimation(r4, r2)
            android.view.animation.Animation r0 = r6.createFreeformWindowExitAnim(r10)
        L6c:
            if (r0 == 0) goto L6f
            return r0
        L6f:
            if (r7 != 0) goto L7d
            android.view.animation.AlphaAnimation r2 = new android.view.animation.AlphaAnimation
            if (r9 == 0) goto L79
            r2.<init>(r1, r3)
            goto L7c
        L79:
            r2.<init>(r3, r3)
        L7c:
            r7 = r2
        L7d:
            return r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.VivoAppTransitionImpl.loadAnimationForVivoFreeform(android.view.animation.Animation, int, boolean, android.graphics.Rect, android.graphics.Rect, com.android.server.wm.ActivityRecord):android.view.animation.Animation");
    }

    private Animation createFreeformTaskOpenAnim(Rect frame) {
        Rect real = new Rect(frame);
        this.mAppTransition.mService.scaleFreeformBack(real);
        Animation scale = new ScaleAnimation(0.3f, 1.0f, 0.3f, 1.0f, (real.width() * 1.0f) / 2.0f, (real.height() * 1.0f) / 2.0f);
        Interpolator freeformTaskOpenInterpolator = AnimationUtils.loadInterpolator(this.mAppTransition.mService.mContext, 50659334);
        scale.setInterpolator(freeformTaskOpenInterpolator);
        scale.setDuration(400L);
        Animation alpha = new AlphaAnimation(0.0f, 1.0f);
        alpha.setDuration(150L);
        AnimationSet set = new AnimationSet(false);
        set.addAnimation(scale);
        set.addAnimation(alpha);
        return set;
    }

    private Animation createFreeformWindowExitAnim(Rect frame) {
        Rect real = new Rect(frame);
        this.mAppTransition.mService.scaleFreeformBack(real);
        Animation scale = new ScaleAnimation(1.0f, TRANSITION_EXIT_APP_SCALE, 1.0f, TRANSITION_EXIT_APP_SCALE, (real.width() * 1.0f) / 2.0f, (real.height() * 1.0f) / 2.0f);
        scale.setDuration(150L);
        Animation alpha = new AlphaAnimation(1.0f, 0.0f);
        alpha.setDuration(150L);
        AnimationSet set = new AnimationSet(false);
        set.addAnimation(scale);
        set.addAnimation(alpha);
        return set;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public Animation createThumbnailFromLauncherAnimationLocked(int transit, boolean enter, Rect containingFrame, WindowContainer container) {
        Animation alpha;
        int appWidth = containingFrame.width();
        int appHeight = containingFrame.height();
        GraphicBuffer thumbnailHeader = this.mAppTransition.getAppTransitionThumbnailHeader(container);
        this.mAppTransition.getDefaultNextAppTransitionStartRect(this.mTmpRect);
        int thumbWidthI = thumbnailHeader != null ? thumbnailHeader.getWidth() : 0;
        float thumbWidth = thumbWidthI > 0 ? thumbWidthI : 1.0f;
        int thumbHeightI = thumbnailHeader != null ? thumbnailHeader.getHeight() : 0;
        float thumbHeight = thumbHeightI > 0 ? thumbHeightI : 1.0f;
        if (enter) {
            float scaleW = thumbWidth / appWidth;
            float scaleH = thumbHeight / appHeight;
            Animation scale = new ScaleAnimation(scaleW, 1.0f, scaleH, 1.0f, AppTransition.computePivot(this.mTmpRect.left, scaleW), AppTransition.computePivot(this.mTmpRect.top, scaleH));
            scale.setInterpolator(this.mSecThumbnailBezierVivoInterpolator);
            scale.setDuration(250L);
            Animation alpha2 = new AlphaAnimation(0.0f, 1.0f);
            alpha2.setDuration(50L);
            AnimationSet set = new AnimationSet(false);
            set.setZAdjustment(1);
            set.addAnimation(scale);
            set.addAnimation(alpha2);
            alpha = set;
        } else {
            alpha = new ScaleAnimation(1.0f, TRANSITION_LAUNCHER_SCALE, 1.0f, TRANSITION_LAUNCHER_SCALE, appWidth / 2, appHeight / 2);
            alpha.setInterpolator(this.mThumbnailBezierVivoInterpolator);
            alpha.setDuration(250L);
        }
        alpha.setFillAfter(true);
        alpha.initialize(appWidth, appHeight, appWidth, appHeight);
        return alpha;
    }

    public void overridePendingAppTransitionThumbnailFromLauncher(int startX, int startY, int startWidth, int startHeight) {
        if (this.mAppTransition.canOverridePendingAppTransition()) {
            this.mAppTransition.clear();
            this.mAppTransition.mNextAppTransitionType = 50;
            this.mAppTransition.putDefaultNextAppTransitionCoordinates(startX, startY, startWidth, startHeight, (GraphicBuffer) null);
            this.mAppTransition.postAnimationCallback();
        }
    }

    public int goodToGo(int transit, ActivityRecord topOpeningApp, ArraySet<ActivityRecord> openingApps, boolean useRecentsAnim) {
        long uptimeMillis;
        int redoLayout;
        long uptimeMillis2;
        this.mAppTransition.mNextAppTransition = -1;
        this.mAppTransition.mNextAppTransitionFlags = 0;
        this.mAppTransition.setAppTransitionState(2);
        WindowContainer wc = topOpeningApp != null ? topOpeningApp.getAnimatingContainer() : null;
        AnimationAdapter topOpeningAnim = wc != null ? wc.getAnimation() : null;
        if (useRecentsAnim) {
            AppTransition appTransition = this.mAppTransition;
            if (topOpeningAnim != null) {
                uptimeMillis2 = topOpeningAnim.getStatusBarTransitionsStartTime();
            } else {
                uptimeMillis2 = SystemClock.uptimeMillis();
            }
            redoLayout = appTransition.notifyAppTransitionStartingLocked(transit, 500L, uptimeMillis2, 120L);
        } else {
            AppTransition appTransition2 = this.mAppTransition;
            long durationHint = topOpeningAnim != null ? topOpeningAnim.getDurationHint() : 0L;
            if (topOpeningAnim != null) {
                uptimeMillis = topOpeningAnim.getStatusBarTransitionsStartTime();
            } else {
                uptimeMillis = SystemClock.uptimeMillis();
            }
            redoLayout = appTransition2.notifyAppTransitionStartingLocked(transit, durationHint, uptimeMillis, 120L);
        }
        if (this.mAppTransition.mDisplayContent != null) {
            this.mAppTransition.mDisplayContent.getDockedDividerController().checkMinimizeChanged(true);
        }
        if (this.mAppTransition.mRemoteAnimationController != null) {
            this.mAppTransition.mRemoteAnimationController.goodToGo();
        }
        return redoLayout;
    }
}