package com.android.server.autofill.ui;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.widget.ScrollView;
import com.android.server.autofill.Helper;

/* loaded from: classes.dex */
public class CustomScrollView extends ScrollView {
    private static final String TAG = "CustomScrollView";
    private int mHeight;
    private int mWidth;

    public CustomScrollView(Context context) {
        super(context);
        this.mWidth = -1;
        this.mHeight = -1;
    }

    public CustomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mWidth = -1;
        this.mHeight = -1;
    }

    public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mWidth = -1;
        this.mHeight = -1;
    }

    public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mWidth = -1;
        this.mHeight = -1;
    }

    @Override // android.widget.ScrollView, android.widget.FrameLayout, android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getChildCount() == 0) {
            Slog.e(TAG, "no children");
            return;
        }
        this.mWidth = View.MeasureSpec.getSize(widthMeasureSpec);
        calculateDimensions();
        setMeasuredDimension(this.mWidth, this.mHeight);
    }

    private void calculateDimensions() {
        if (this.mHeight != -1) {
            return;
        }
        TypedValue typedValue = new TypedValue();
        Point point = new Point();
        Context context = getContext();
        context.getDisplayNoVerify().getSize(point);
        context.getTheme().resolveAttribute(17956885, typedValue, true);
        View child = getChildAt(0);
        int childHeight = child.getMeasuredHeight();
        int maxHeight = (int) typedValue.getFraction(point.y, point.y);
        this.mHeight = Math.min(childHeight, maxHeight);
        if (Helper.sDebug) {
            Slog.d(TAG, "calculateDimensions(): maxHeight=" + maxHeight + ", childHeight=" + childHeight + ", w=" + this.mWidth + ", h=" + this.mHeight);
        }
    }
}