package com.android.server.policy.key;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import com.android.server.am.firewall.VivoFirewall;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.ProcessList;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class VivoSmartwakeCharContainer extends FrameLayout {
    private static final long ALPHA_ANIMATION_TIME = 700;
    private static final int ANIMATE_VIEW_HEIGHT = 300;
    private static final int ANIMATE_VIEW_UP_HEIGHT = 300;
    private static final int ANIMATE_VIEW_UP_WIDTH = 50;
    private static final int ANIMATE_VIEW_WIDTH = 300;
    private static final int ANIMATION_FRAME_NUM = 20;
    private static final int ANTIC_CLOCK_WISE = 32;
    private static final int BITMAP_SPLIT_COL_NUM = 4;
    private static final int CLOCK_WISE = 16;
    private static final String GESTURE_POINT_PATH = "/sys/touchscreen/gesture_point";
    private static final int MAX_POINT_NUM = 10;
    private static final int MSG_CHAR_ANIMATION_END = 2;
    private static final int MSG_START_ALPHA_ANIMATION = 1;
    private static final String TAG = "VivoSmartwakeCharContainer";
    private static int gDisappearTime;
    private static int gDisappearTimeSecure;
    private AnimateCharView mAnimateCharView;
    private int mDirectionValue;
    private SmartWakeCallback mEndCallback;
    private Handler mHandler;
    private int mImagesCharAt;
    private int mImagesCharC;
    private int mImagesCharE;
    private int mImagesCharF1;
    private int mImagesCharF2;
    private int mImagesCharHeartDownStart;
    private int mImagesCharHeartUpStart;
    private int mImagesCharO;
    private int mImagesCharS;
    private int mImagesCharUp;
    private int mImagesCharV;
    private int mImagesCharW;
    private int mImagesReversalCharO;
    private int mKeyCode;

    /* loaded from: classes.dex */
    public interface SmartWakeCallback {
        void AllAnimationEnd();

        void charAnimationEnd();
    }

    static {
        String product = SystemProperties.get("ro.product.model.bbk");
        gDisappearTimeSecure = ProcessList.PREVIOUS_APP_ADJ;
        gDisappearTime = 500;
        VLog.d(TAG, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + product + "," + gDisappearTimeSecure + "," + gDisappearTime);
    }

    public void updateDisappearTime(int keyCode, boolean isSecure) {
        if (keyCode == 302 && isSecure) {
            gDisappearTimeSecure = getDisappearTimeSecure();
        }
    }

    private int getDisappearTimeSecure() {
        ActivityManager am;
        List<ActivityManager.RunningAppProcessInfo> processInfos;
        Context context = getContext();
        if (context == null || (am = (ActivityManager) context.getSystemService(VivoFirewall.TYPE_ACTIVITY)) == null || (processInfos = am.getRunningAppProcesses()) == null) {
            return 1800;
        }
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfos) {
            if ("com.android.camera".equals(processInfo.processName)) {
                return ProcessList.PREVIOUS_APP_ADJ;
            }
        }
        return 1800;
    }

    public VivoSmartwakeCharContainer(Context context) {
        this(context, null);
    }

    public VivoSmartwakeCharContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VivoSmartwakeCharContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mAnimateCharView = null;
        this.mEndCallback = null;
        this.mKeyCode = -1;
        this.mDirectionValue = -1;
        this.mImagesCharC = 50464077;
        this.mImagesCharE = 50464079;
        this.mImagesCharO = 50464084;
        this.mImagesReversalCharO = 50464085;
        this.mImagesCharW = 50464087;
        this.mImagesCharUp = 50464086;
        this.mImagesCharF1 = 50464080;
        this.mImagesCharF2 = 50464081;
        this.mImagesCharAt = 50464076;
        this.mImagesCharV = 50463162;
        this.mImagesCharS = 50463161;
        this.mImagesCharHeartDownStart = 50464082;
        this.mImagesCharHeartUpStart = 50464083;
        this.mHandler = new Handler() { // from class: com.android.server.policy.key.VivoSmartwakeCharContainer.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i != 1) {
                    if (i == 2 && VivoSmartwakeCharContainer.this.mEndCallback != null) {
                        VivoSmartwakeCharContainer.this.mEndCallback.charAnimationEnd();
                        return;
                    }
                    return;
                }
                VivoSmartwakeCharContainer.this.alphaAnimation();
            }
        };
        setBackgroundColor(-16777216);
        this.mAnimateCharView = new AnimateCharView(this, context);
    }

    public void setmKeyCode(int keyCode) {
        VLog.d(TAG, "keyCode = " + keyCode);
        this.mKeyCode = keyCode;
    }

    public int getmKeyCode() {
        return this.mKeyCode;
    }

    public void setAnimEndlistener(SmartWakeCallback callback) {
        this.mEndCallback = callback;
    }

    public void startTrackAnimation(boolean isSecure) {
        int imageRes;
        int imageRes2;
        int imageRes3;
        FrameLayout.LayoutParams params;
        VLog.d(TAG, "startTrackAnimation isSecure: " + isSecure + ", mKeyCode: " + this.mKeyCode);
        this.mDirectionValue = -1;
        this.mAnimateCharView.recycle();
        removeAllViews();
        if (this.mKeyCode == 302) {
            setBackgroundResource(50464078);
            this.mHandler.sendEmptyMessageDelayed(2, isSecure ? gDisappearTimeSecure : gDisappearTime);
            return;
        }
        boolean isExceptional = false;
        ArrayList<Point> trackPoints = readPointsFromFile();
        if (trackPoints != null && trackPoints.size() > 0) {
            VLog.d(TAG, "Animation trackPoints size:" + trackPoints.size());
            Point tP = trackPoints.get(0);
            int samePointCount = 0;
            Iterator<Point> it = trackPoints.iterator();
            while (it.hasNext()) {
                Point temPoint = it.next();
                if (temPoint.x > getWidth() || temPoint.x < 0 || temPoint.y > getHeight() || temPoint.y < 0) {
                    isExceptional = true;
                    break;
                } else if (tP.x == temPoint.x && tP.y == temPoint.y) {
                    samePointCount++;
                }
            }
            if (samePointCount == trackPoints.size()) {
                isExceptional = true;
            }
        } else {
            VLog.d(TAG, "Animation trackPoints is null");
            isExceptional = true;
        }
        if (this.mKeyCode == 19) {
            if (trackPoints == null || trackPoints.size() < 2) {
                isExceptional = true;
            }
        } else if (trackPoints == null || trackPoints.size() < 3) {
            isExceptional = true;
        }
        VLog.d(TAG, "isExceptional = " + isExceptional);
        if (isExceptional) {
            int i = this.mKeyCode;
            if (i == 19) {
                imageRes3 = this.mImagesCharUp;
            } else if (i == 29) {
                imageRes3 = this.mImagesCharAt;
            } else if (i == 31) {
                imageRes3 = this.mImagesCharC;
            } else if (i == 36) {
                imageRes3 = this.mImagesCharHeartUpStart;
            } else if (i == 43) {
                int i2 = this.mDirectionValue;
                if (i2 == 16) {
                    imageRes3 = this.mImagesReversalCharO;
                } else if (i2 == 32) {
                    imageRes3 = this.mImagesCharO;
                } else if (trackPoints != null && trackPoints.size() >= 3) {
                    int size = trackPoints.size();
                    boolean isFlip = getDirection(trackPoints.get(0), trackPoints.get((size - 1) / 2), trackPoints.get(((size - 1) / 2) + 1));
                    imageRes3 = isFlip ? this.mImagesReversalCharO : this.mImagesCharO;
                } else {
                    imageRes3 = this.mImagesCharO;
                }
            } else if (i == 47) {
                imageRes3 = this.mImagesCharS;
            } else if (i != 33) {
                if (i == 34) {
                    imageRes3 = this.mImagesCharF1;
                } else if (i == 50) {
                    imageRes3 = this.mImagesCharV;
                } else if (i == 51) {
                    imageRes3 = this.mImagesCharW;
                } else {
                    SmartWakeCallback smartWakeCallback = this.mEndCallback;
                    if (smartWakeCallback != null) {
                        smartWakeCallback.AllAnimationEnd();
                        return;
                    }
                    return;
                }
            } else {
                imageRes3 = this.mImagesCharE;
            }
            if (19 == this.mKeyCode) {
                params = new FrameLayout.LayoutParams(50, (int) ProcessList.BACKUP_APP_ADJ);
                this.mAnimateCharView.setScaleY((getHeight() * 0.7f) / 300.0f);
                this.mAnimateCharView.setAnimationSize(50, ProcessList.BACKUP_APP_ADJ);
                this.mAnimateCharView.setDuration(650L);
            } else {
                params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
                this.mAnimateCharView.setScaleX((getWidth() * 0.65f) / 300.0f);
                this.mAnimateCharView.setScaleY((getWidth() * 0.65f) / 300.0f);
                this.mAnimateCharView.setAnimationSize(ProcessList.BACKUP_APP_ADJ, ProcessList.BACKUP_APP_ADJ);
            }
            params.gravity = 17;
            addView(this.mAnimateCharView, params);
            imageRes = imageRes3;
        } else {
            int i3 = this.mKeyCode;
            if (i3 == 19) {
                int imageRes4 = this.mImagesCharUp;
                addCharUpView(trackPoints);
                imageRes = imageRes4;
            } else if (i3 == 29) {
                int imageRes5 = this.mImagesCharAt;
                addCharAtView(trackPoints);
                imageRes = imageRes5;
            } else if (i3 == 31) {
                int imageRes6 = this.mImagesCharC;
                addCharCView(trackPoints);
                imageRes = imageRes6;
            } else if (i3 == 36) {
                int imageRes7 = getHeartImg(trackPoints);
                addCharHeartView(trackPoints);
                imageRes = imageRes7;
            } else if (i3 == 43) {
                int i4 = this.mDirectionValue;
                if (i4 == 16) {
                    imageRes2 = this.mImagesReversalCharO;
                } else if (i4 == 32) {
                    imageRes2 = this.mImagesCharO;
                } else {
                    int size2 = trackPoints.size();
                    if (size2 >= 3) {
                        boolean isFlip2 = getDirection(trackPoints.get(0), trackPoints.get((size2 - 1) / 2), trackPoints.get(((size2 - 1) / 2) + 1));
                        imageRes2 = isFlip2 ? this.mImagesReversalCharO : this.mImagesCharO;
                    } else {
                        imageRes2 = this.mImagesCharO;
                    }
                }
                addCharOView(trackPoints);
                imageRes = imageRes2;
            } else if (i3 == 47) {
                int imageRes8 = this.mImagesCharS;
                addCharSView(trackPoints);
                imageRes = imageRes8;
            } else if (i3 != 33) {
                if (i3 == 34) {
                    int imageRes9 = this.mImagesCharF1;
                    addCharFView(trackPoints);
                    imageRes = imageRes9;
                } else if (i3 == 50) {
                    int imageRes10 = this.mImagesCharV;
                    addCharVView(trackPoints);
                    imageRes = imageRes10;
                } else if (i3 == 51) {
                    int imageRes11 = this.mImagesCharW;
                    addCharWView(trackPoints);
                    imageRes = imageRes11;
                } else {
                    SmartWakeCallback smartWakeCallback2 = this.mEndCallback;
                    if (smartWakeCallback2 != null) {
                        smartWakeCallback2.AllAnimationEnd();
                        return;
                    }
                    return;
                }
            } else {
                int imageRes12 = this.mImagesCharE;
                addCharEView(trackPoints);
                imageRes = imageRes12;
            }
        }
        int imageRes13 = this.mKeyCode;
        if (19 == imageRes13) {
            this.mAnimateCharView.setAnimationSize(50, ProcessList.BACKUP_APP_ADJ);
            this.mAnimateCharView.setDuration(650L);
        } else {
            this.mAnimateCharView.setAnimationSize(ProcessList.BACKUP_APP_ADJ, ProcessList.BACKUP_APP_ADJ);
        }
        try {
            long start = System.currentTimeMillis();
            Bitmap bm = BitmapFactory.decodeResource(getResources(), imageRes);
            long end = System.currentTimeMillis();
            if (bm != null) {
                VLog.d(TAG, "Create bitmap costs time : " + (end - start));
            } else {
                VLog.d(TAG, "bitmap is null ");
            }
            this.mAnimateCharView.setSourceBitmap(bm);
            this.mAnimateCharView.startCharAnimation();
        } catch (OutOfMemoryError e) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private int getHeartImg(ArrayList<Point> trackPoints) {
        int img = this.mImagesCharHeartUpStart;
        if (trackPoints != null && trackPoints.size() > 0) {
            int size = trackPoints.size();
            Point startPosi = trackPoints.get(0);
            int smallerYCount = 0;
            for (int index = 1; index < size; index++) {
                if (trackPoints.get(index).y < startPosi.y) {
                    smallerYCount++;
                }
            }
            int index2 = smallerYCount * 2;
            int img2 = size < index2 ? this.mImagesCharHeartDownStart : this.mImagesCharHeartUpStart;
            return img2;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("track points is null: ");
        sb.append(trackPoints == null);
        VLog.d(TAG, sb.toString());
        return img;
    }

    private ArrayList<Point> readPointsFromFile() {
        int value;
        VLog.d(TAG, "Animation readPointsFromFile");
        ArrayList<Point> trackWPoints = new ArrayList<>();
        try {
            FileInputStream fin = new FileInputStream(GESTURE_POINT_PATH);
            int length = fin.available();
            byte[] buffer = new byte[length];
            fin.read(buffer);
            String sourceString = new String(buffer, "UTF-8");
            String[] arrayStrings = sourceString.split(" ");
            if (arrayStrings.length >= 20 && getmKeyCode() == 43) {
                this.mDirectionValue = Integer.valueOf(arrayStrings[19]).intValue();
            }
            int x = 0;
            for (int i = 0; i != arrayStrings.length && i < 20 && (value = Integer.valueOf(arrayStrings[i]).intValue()) != 65535; i++) {
                if (i % 2 == 0) {
                    x = Integer.valueOf(value).intValue();
                } else {
                    int y = Integer.valueOf(value).intValue();
                    trackWPoints.add(new Point(x, y));
                    VLog.d(TAG, "read point[" + (i / 2) + "](" + x + "," + y + ")");
                    x = 0;
                }
            }
            fin.close();
            return trackWPoints;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addCharCView(ArrayList<Point> trackPoints) {
        double degree;
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        int size = trackPoints.size();
        float endX = (float) trackPoints.get(size + (-1)).x;
        float endY = (float) trackPoints.get(size + (-1)).y;
        float startX = trackPoints.get(0).x;
        float startY = trackPoints.get(0).y;
        float slope = (endY - startY) / (endX - startX);
        float farDistance = 0.0f;
        for (int index = 1; index != size - 1; index++) {
            float dis = Math.abs((((trackPoints.get(index).x * slope) - trackPoints.get(index).y) + startY) - (slope * startX)) / FloatMath.sqrt((slope * slope) + 1.0f);
            if (dis > farDistance) {
                farDistance = dis;
            }
        }
        float height = getTwoPointsDistance(trackPoints.get(0), trackPoints.get(size - 1));
        float width = farDistance;
        double degree2 = getSlopeAngle(trackPoints.get(0), trackPoints.get(size - 1));
        if (degree2 > 0.0d) {
            degree = degree2 - 90.0d;
        } else {
            degree = degree2 + 90.0d;
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
        params.gravity = 5;
        params.rightMargin = getWidth() - trackPoints.get(0).x;
        params.topMargin = trackPoints.get(0).y;
        this.mAnimateCharView.setPivotX(300.0f);
        this.mAnimateCharView.setPivotY(0.0f);
        this.mAnimateCharView.setScaleX(width / 300.0f);
        this.mAnimateCharView.setScaleY(height / 300.0f);
        this.mAnimateCharView.setRotation((float) degree);
        addView(this.mAnimateCharView, params);
    }

    private void addCharEView(ArrayList<Point> trackPoints) {
        double degree;
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        int size = trackPoints.size();
        Point minDisPoint = trackPoints.get(0);
        Point maxDisPoint = trackPoints.get(0);
        for (int i = 0; i != size; i++) {
            if (trackPoints.get(i).y <= minDisPoint.y) {
                Point minDisPoint2 = trackPoints.get(i);
                minDisPoint = minDisPoint2;
            } else if (trackPoints.get(i).y >= maxDisPoint.y) {
                Point maxDisPoint2 = trackPoints.get(i);
                maxDisPoint = maxDisPoint2;
            }
        }
        float distance = getTwoPointsDistance(minDisPoint, maxDisPoint);
        double degree2 = getSlopeAngle(minDisPoint, maxDisPoint);
        if (degree2 > 0.0d) {
            degree = degree2 - 90.0d;
        } else {
            degree = degree2 + 90.0d;
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
        params.gravity = 3;
        params.leftMargin = ((minDisPoint.x + maxDisPoint.x) / 2) - 150;
        params.topMargin = ((minDisPoint.y + maxDisPoint.y) / 2) - 150;
        this.mAnimateCharView.setPivotX(150.0f);
        this.mAnimateCharView.setPivotY(150.0f);
        this.mAnimateCharView.setScaleX(distance / 300.0f);
        this.mAnimateCharView.setScaleY(distance / 300.0f);
        this.mAnimateCharView.setRotation((float) degree);
        addView(this.mAnimateCharView, params);
    }

    private void addCharOView(ArrayList<Point> trackPoints) {
        double degree;
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        int size = trackPoints.size();
        Point startPoint = trackPoints.get(0);
        Point maxDisPoint = trackPoints.get(0);
        float maxDistance = 0.0f;
        for (int index = 1; index != size; index++) {
            float dis = getTwoPointsDistance(trackPoints.get(index), startPoint);
            if (dis > maxDistance) {
                maxDistance = dis;
                Point maxDisPoint2 = trackPoints.get(index);
                maxDisPoint = maxDisPoint2;
            }
        }
        double degree2 = getSlopeAngle(maxDisPoint, startPoint);
        if (degree2 > 0.0d) {
            if (maxDisPoint.y < startPoint.y) {
                degree = degree2 - 270.0d;
            } else {
                degree = degree2 - 90.0d;
            }
        } else if (degree2 < 0.0d) {
            if (maxDisPoint.y < startPoint.y) {
                degree = degree2 + 270.0d;
            } else {
                degree = degree2 + 90.0d;
            }
        } else if (maxDisPoint.x > startPoint.x) {
            degree = degree2 - 90.0d;
        } else {
            degree = degree2 + 90.0d;
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
        params.gravity = 3;
        params.leftMargin = startPoint.x - 150;
        params.topMargin = startPoint.y;
        this.mAnimateCharView.setPivotX(150.0f);
        this.mAnimateCharView.setPivotY(0.0f);
        this.mAnimateCharView.setScaleX(maxDistance / 300.0f);
        this.mAnimateCharView.setScaleY(maxDistance / 300.0f);
        this.mAnimateCharView.setRotation((float) degree);
        addView(this.mAnimateCharView, params);
    }

    private void addCharWView(ArrayList<Point> trackPoints) {
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        int size = trackPoints.size();
        float endX = (float) trackPoints.get(size + (-1)).x;
        float endY = (float) trackPoints.get(size + (-1)).y;
        float startX = trackPoints.get(0).x;
        float startY = trackPoints.get(0).y;
        float slope = (endY - startY) / (endX - startX);
        float farDistance = 0.0f;
        for (int index = 1; index != size - 1; index++) {
            float dis = Math.abs((((trackPoints.get(index).x * slope) - trackPoints.get(index).y) + startY) - (slope * startX)) / FloatMath.sqrt((slope * slope) + 1.0f);
            if (dis > farDistance) {
                farDistance = dis;
            }
        }
        double degree = getSlopeAngle(trackPoints.get(0), trackPoints.get(size - 1));
        float width = getTwoPointsDistance(trackPoints.get(0), trackPoints.get(size - 1));
        float height = farDistance;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
        params.gravity = 3;
        params.leftMargin = trackPoints.get(0).x;
        params.topMargin = trackPoints.get(0).y;
        this.mAnimateCharView.setPivotX(0.0f);
        this.mAnimateCharView.setPivotY(0.0f);
        this.mAnimateCharView.setScaleX(width / 300.0f);
        this.mAnimateCharView.setScaleY(height / 300.0f);
        this.mAnimateCharView.setRotation((float) degree);
        addView(this.mAnimateCharView, params);
    }

    private void addCharFView(ArrayList<Point> trackPoints) {
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        float maxLen = 0.0f;
        int maxLenIndexStart = 0;
        int maxLenIndexEnd = 0;
        int size = trackPoints.size();
        for (int i = 0; i < size - 1; i++) {
            Point p1 = trackPoints.get(i);
            for (int j = i + 1; j < size; j++) {
                Point p2 = trackPoints.get(j);
                float len = getTwoPointsDistance(p1, p2);
                if (len > maxLen) {
                    maxLen = len;
                    maxLenIndexStart = i;
                    maxLenIndexEnd = j;
                }
            }
        }
        double degree = getSlopeAngle(trackPoints.get(maxLenIndexStart), trackPoints.get(maxLenIndexEnd));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
        Point midPoint = getMidPoint(trackPoints.get(maxLenIndexStart), trackPoints.get(maxLenIndexEnd));
        params.gravity = 3;
        this.mAnimateCharView.setPivotX(150.0f);
        this.mAnimateCharView.setPivotY(150.0f);
        params.leftMargin = midPoint.x - 150;
        params.topMargin = midPoint.y - 150;
        this.mAnimateCharView.setScaleX((0.6f * maxLen) / 300.0f);
        this.mAnimateCharView.setScaleY(maxLen / 300.0f);
        this.mAnimateCharView.setRotation((float) (degree + 80.0d));
        addView(this.mAnimateCharView, params);
    }

    private void addCharAtView(ArrayList<Point> trackPoints) {
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        float maxLen = 0.0f;
        Point midPoint = null;
        int size = trackPoints.size();
        for (int i = 0; i < size - 1; i++) {
            Point p1 = trackPoints.get(i);
            for (int j = i + 1; j < size; j++) {
                Point p2 = trackPoints.get(j);
                float len = getTwoPointsDistance(p1, p2);
                if (len > maxLen) {
                    maxLen = len;
                    midPoint = getMidPoint(p1, p2);
                }
            }
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
        params.gravity = 3;
        params.leftMargin = midPoint.x - 150;
        params.topMargin = midPoint.y - 150;
        this.mAnimateCharView.setScaleX(maxLen / 300.0f);
        this.mAnimateCharView.setScaleY(maxLen / 300.0f);
        addView(this.mAnimateCharView, params);
    }

    private void addCharUpView(ArrayList<Point> trackPoints) {
        double degree;
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        int size = trackPoints.size();
        double degree2 = getSlopeAngle(trackPoints.get(0), trackPoints.get(size + (-1)));
        if (degree2 > 0.0d) {
            degree = degree2 - 90.0d;
        } else {
            degree = degree2 + 90.0d;
        }
        float height = getTwoPointsDistance(trackPoints.get(0), trackPoints.get(size - 1));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(50, (int) ProcessList.BACKUP_APP_ADJ);
        params.gravity = 3;
        params.leftMargin = trackPoints.get(0).x - 25;
        params.topMargin = trackPoints.get(0).y - ProcessList.BACKUP_APP_ADJ;
        this.mAnimateCharView.setPivotX(25.0f);
        this.mAnimateCharView.setPivotY(300.0f);
        this.mAnimateCharView.setScaleY(height / 300.0f);
        this.mAnimateCharView.setRotation((float) degree);
        addView(this.mAnimateCharView, params);
    }

    private void addCharVView(ArrayList<Point> trackPoints) {
        double degree;
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        int size = trackPoints.size();
        Point firstPoint = trackPoints.get(0);
        Point lastPoint = trackPoints.get(trackPoints.size() + (-1));
        Point maxYPoint = trackPoints.get(0);
        for (int i = 0; i != size; i++) {
            if (trackPoints.get(i).y >= maxYPoint.y) {
                Point maxYPoint2 = trackPoints.get(i);
                maxYPoint = maxYPoint2;
            }
        }
        Point topMidlePoint = getMidPoint(firstPoint, lastPoint);
        float height = getTwoPointsDistance(topMidlePoint, maxYPoint);
        float width = getTwoPointsDistance(firstPoint, lastPoint);
        double degree2 = getSlopeAngle(topMidlePoint, maxYPoint);
        if (degree2 > 0.0d) {
            degree = degree2 - 90.0d;
        } else {
            degree = degree2 + 90.0d;
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
        Point midlePoint = getMidPoint(topMidlePoint, maxYPoint);
        params.gravity = 3;
        params.leftMargin = midlePoint.x - ((int) (width / 2.0f));
        params.topMargin = midlePoint.y - ((int) (height / 2.0f));
        this.mAnimateCharView.setPivotX(150.0f);
        this.mAnimateCharView.setPivotY(0.0f);
        this.mAnimateCharView.setScaleX(width / 300.0f);
        this.mAnimateCharView.setScaleY(height / 300.0f);
        this.mAnimateCharView.setRotation((float) degree);
        addView(this.mAnimateCharView, params);
    }

    private void addCharSView(ArrayList<Point> trackPoints) {
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
        this.mAnimateCharView.setScaleX((((float) getWidth()) * 0.65f) / 300.0f);
        this.mAnimateCharView.setScaleY((((float) getWidth()) * 0.65f) / 300.0f);
        this.mAnimateCharView.setAnimationSize(ProcessList.BACKUP_APP_ADJ, ProcessList.BACKUP_APP_ADJ);
        params.gravity = 17;
        addView(this.mAnimateCharView, params);
    }

    private boolean getCharHeartReverseOrde(ArrayList<Point> trackPoints, int size) {
        int firstXSum = 0;
        int lastXSum = 0;
        for (int i = 0; i < size / 2; i++) {
            firstXSum += trackPoints.get(i).x;
        }
        for (int i2 = size / 2; i2 < size; i2++) {
            lastXSum += trackPoints.get(i2).x;
        }
        return firstXSum > lastXSum;
    }

    private void addCharHeartView(ArrayList<Point> trackPoints) {
        double degree;
        VLog.d(TAG, "add char view keycode:" + this.mKeyCode);
        int size = trackPoints.size();
        boolean reverseOrde = getCharHeartReverseOrde(trackPoints, size);
        Point minXPoint = trackPoints.get(0);
        Point maxXPoint = trackPoints.get(0);
        Point maxYPoint = trackPoints.get(0);
        Point topPoint = new Point(0, Integer.MAX_VALUE);
        Point secondTopPoint = new Point(0, Integer.MAX_VALUE);
        for (int i = 0; i != size; i++) {
            if (trackPoints.get(i).x <= minXPoint.x) {
                Point minXPoint2 = trackPoints.get(i);
                minXPoint = minXPoint2;
            } else if (trackPoints.get(i).x >= maxXPoint.x) {
                Point maxXPoint2 = trackPoints.get(i);
                maxXPoint = maxXPoint2;
            }
            if (trackPoints.get(i).y >= maxYPoint.y) {
                Point maxYPoint2 = trackPoints.get(i);
                maxYPoint = maxYPoint2;
            }
            if (trackPoints.get(i).y <= topPoint.y) {
                secondTopPoint = topPoint;
                Point topPoint2 = trackPoints.get(i);
                topPoint = topPoint2;
            } else if (trackPoints.get(i).y <= secondTopPoint.y) {
                Point secondTopPoint2 = trackPoints.get(i);
                secondTopPoint = secondTopPoint2;
            }
        }
        Point topMidlePoint = getMidPoint(topPoint, secondTopPoint);
        float distance = getTwoPointsDistance(topMidlePoint, maxYPoint);
        double degree2 = getSlopeAngle(topMidlePoint, maxYPoint);
        if (degree2 > 0.0d) {
            degree = degree2 - 90.0d;
        } else {
            degree = degree2 + 90.0d;
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams((int) ProcessList.BACKUP_APP_ADJ, (int) ProcessList.BACKUP_APP_ADJ);
        Point midlePoint = getMidPoint(topMidlePoint, maxYPoint);
        params.gravity = 3;
        params.leftMargin = midlePoint.x - 150;
        params.topMargin = midlePoint.y - 150;
        this.mAnimateCharView.setPivotX(150.0f);
        this.mAnimateCharView.setPivotY(150.0f);
        this.mAnimateCharView.setScaleX(distance / 300.0f);
        this.mAnimateCharView.setScaleY(distance / 300.0f);
        this.mAnimateCharView.setRotation((float) degree);
        this.mAnimateCharView.setPlayInReverseOrde(reverseOrde);
        addView(this.mAnimateCharView, params);
    }

    public void startAlphaAnimation() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void alphaAnimation() {
        ObjectAnimator alphAnimator = ObjectAnimator.ofFloat(this, "alpha", 1.0f, 0.0f).setDuration(ALPHA_ANIMATION_TIME);
        alphAnimator.addListener(new Animator.AnimatorListener() { // from class: com.android.server.policy.key.VivoSmartwakeCharContainer.2
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animation) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animation) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animation) {
                VLog.d(VivoSmartwakeCharContainer.TAG, "All animation have ended,then call the callback : mEndCallback = " + VivoSmartwakeCharContainer.this.mEndCallback);
                if (VivoSmartwakeCharContainer.this.mEndCallback != null) {
                    VivoSmartwakeCharContainer.this.mEndCallback.AllAnimationEnd();
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animation) {
            }
        });
        alphAnimator.start();
    }

    private float getTwoPointsDistance(Point p1, Point p2) {
        float distance = FloatMath.sqrt(((p2.x - p1.x) * (p2.x - p1.x)) + ((p2.y - p1.y) * (p2.y - p1.y)));
        return distance;
    }

    private Point getMidPoint(Point p1, Point p2) {
        return new Point((p1.x + p2.x) / 2, (p1.y + p2.y) / 2);
    }

    private double getSlopeAngle(Point p1, Point p2) {
        float slope = (p2.y - p1.y) / (p2.x - p1.x);
        double degree = Math.atan(slope);
        return Math.toDegrees(degree);
    }

    private boolean getDirection(Point p1, Point p2, Point p3) {
        float result = ((p1.x - p3.x) * (p2.y - p3.y)) - ((p1.y - p3.y) * (p2.x - p3.x));
        if (result > 0.0f) {
            return true;
        }
        return false;
    }

    /* loaded from: classes.dex */
    public class AnimateCharView extends View implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        private static final int ANIMATION_TIME = 800;
        private int drawBitmapCount;
        private ValueAnimator mAnimator;
        private Bitmap mBitmap;
        private Rect mDst;
        private int mHeight;
        private Paint mPaint;
        private PaintFlagsDrawFilter mPaintFlags;
        private boolean mReverseOrde;
        private Rect mSrc;
        private Rect mTempRect;
        private int mWidth;

        public AnimateCharView(VivoSmartwakeCharContainer this$0, Context context) {
            this(this$0, context, null);
        }

        public AnimateCharView(VivoSmartwakeCharContainer this$0, Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public AnimateCharView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            this.drawBitmapCount = 0;
            this.mPaint = null;
            this.mSrc = new Rect(0, 0, 0, 0);
            this.mDst = new Rect(0, 0, ProcessList.BACKUP_APP_ADJ, ProcessList.BACKUP_APP_ADJ);
            this.mTempRect = new Rect();
            this.mPaintFlags = new PaintFlagsDrawFilter(0, 3);
            this.mAnimator = null;
            this.mBitmap = null;
            this.mWidth = 0;
            this.mHeight = 0;
            this.mReverseOrde = false;
            Paint paint = new Paint();
            this.mPaint = paint;
            paint.setAntiAlias(true);
            ValueAnimator duration = ValueAnimator.ofFloat(0.0f, 1.0f).setDuration(800L);
            this.mAnimator = duration;
            duration.setInterpolator(new LinearInterpolator());
            this.mAnimator.addListener(this);
            this.mAnimator.addUpdateListener(this);
        }

        public void setDuration(long duration) {
            this.mAnimator.setDuration(duration);
        }

        public void setPlayInReverseOrde(boolean reverseOrde) {
            this.mReverseOrde = reverseOrde;
        }

        public void setAnimationSize(int widht, int height) {
            this.mWidth = widht;
            this.mHeight = height;
            Rect rect = this.mDst;
            rect.right = rect.left + widht;
            Rect rect2 = this.mDst;
            rect2.bottom = rect2.top + height;
        }

        public void setSourceBitmap(Bitmap bitmaps) {
            this.mBitmap = bitmaps;
        }

        public void startCharAnimation() {
            this.drawBitmapCount = 0;
            VLog.d(VivoSmartwakeCharContainer.TAG, "startCharAnimation, drawBitmapCount:" + this.drawBitmapCount);
            this.mAnimator.start();
        }

        public void endAnimation() {
            this.mAnimator.end();
        }

        public void recycle() {
            Bitmap bitmap = this.mBitmap;
            if (bitmap != null && !bitmap.isRecycled()) {
                this.mBitmap.recycle();
                this.mBitmap = null;
            }
        }

        @Override // android.view.View
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            drawChar(canvas);
        }

        private void drawChar(Canvas canvas) {
            canvas.save();
            canvas.setDrawFilter(this.mPaintFlags);
            Bitmap bitmap = this.mBitmap;
            if (bitmap != null && !bitmap.isRecycled()) {
                int index = (int) (this.mAnimator.getAnimatedFraction() * 20.0f);
                if (index >= 20) {
                    index = 19;
                }
                if (this.mReverseOrde) {
                    index = 19 - index;
                }
                Rect clipRect = getClipRect(index, this.mTempRect);
                this.mSrc = clipRect;
                canvas.drawBitmap(this.mBitmap, clipRect, this.mDst, this.mPaint);
                this.drawBitmapCount++;
            }
            canvas.restore();
        }

        private Rect getClipRect(int index, Rect temp) {
            int col = index % 4;
            int row = index / 4;
            temp.left = this.mWidth * col;
            temp.top = this.mHeight * row;
            temp.right = temp.left + this.mWidth;
            temp.bottom = temp.top + this.mHeight;
            return temp;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            invalidate();
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            recycle();
            VLog.d(VivoSmartwakeCharContainer.TAG, "endCharAnimation, drawBitmapCount:" + this.drawBitmapCount);
            if (VivoSmartwakeCharContainer.this.mEndCallback != null) {
                VivoSmartwakeCharContainer.this.mEndCallback.charAnimationEnd();
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animation) {
        }
    }
}