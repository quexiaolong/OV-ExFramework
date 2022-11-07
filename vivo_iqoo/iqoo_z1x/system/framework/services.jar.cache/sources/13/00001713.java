package com.android.server.policy;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.util.DisplayMetrics;

/* loaded from: classes2.dex */
public final class IconUtilities {
    private ColorFilter mDisabledColorFilter;
    private final DisplayMetrics mDisplayMetrics;
    private int mIconHeight;
    private int mIconTextureHeight;
    private int mIconTextureWidth;
    private int mIconWidth;
    private final Rect mOldBounds = new Rect();
    private final Canvas mCanvas = new Canvas();

    public IconUtilities(Context context) {
        this.mIconWidth = -1;
        this.mIconHeight = -1;
        this.mIconTextureWidth = -1;
        this.mIconTextureHeight = -1;
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        this.mDisplayMetrics = metrics;
        float density = metrics.density;
        float blurPx = 5.0f * density;
        int dimension = (int) resources.getDimension(17104896);
        this.mIconHeight = dimension;
        this.mIconWidth = dimension;
        int i = dimension + ((int) (2.0f * blurPx));
        this.mIconTextureHeight = i;
        this.mIconTextureWidth = i;
        this.mCanvas.setDrawFilter(new PaintFlagsDrawFilter(4, 2));
    }

    public Bitmap createIconBitmap(Drawable icon) {
        int width = this.mIconWidth;
        int height = this.mIconHeight;
        if (icon instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) icon;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        } else if (icon instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
            if (bitmapDrawable.getBitmap().getDensity() == 0) {
                bitmapDrawable.setTargetDensity(this.mDisplayMetrics);
            }
        }
        int sourceWidth = icon.getIntrinsicWidth();
        int sourceHeight = icon.getIntrinsicHeight();
        if (sourceWidth > 0 && sourceHeight > 0) {
            if (width < sourceWidth || height < sourceHeight) {
                float ratio = sourceWidth / sourceHeight;
                if (sourceWidth > sourceHeight) {
                    height = (int) (width / ratio);
                } else if (sourceHeight > sourceWidth) {
                    width = (int) (height * ratio);
                }
            } else if (sourceWidth < width && sourceHeight < height) {
                width = sourceWidth;
                height = sourceHeight;
            }
        }
        int textureWidth = this.mIconTextureWidth;
        int textureHeight = this.mIconTextureHeight;
        Bitmap bitmap = Bitmap.createBitmap(textureWidth, textureHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = this.mCanvas;
        canvas.setBitmap(bitmap);
        int left = (textureWidth - width) / 2;
        int top = (textureHeight - height) / 2;
        this.mOldBounds.set(icon.getBounds());
        icon.setBounds(left, top, left + width, top + height);
        icon.draw(canvas);
        icon.setBounds(this.mOldBounds);
        return bitmap;
    }

    public ColorFilter getDisabledColorFilter() {
        ColorFilter colorFilter = this.mDisabledColorFilter;
        if (colorFilter != null) {
            return colorFilter;
        }
        ColorMatrix brightnessMatrix = new ColorMatrix();
        int brightnessI = (int) (255.0f * 0.5f);
        float scale = 1.0f - 0.5f;
        float[] mat = brightnessMatrix.getArray();
        mat[0] = scale;
        mat[6] = scale;
        mat[12] = scale;
        mat[4] = brightnessI;
        mat[9] = brightnessI;
        mat[14] = brightnessI;
        ColorMatrix filterMatrix = new ColorMatrix();
        filterMatrix.setSaturation(0.0f);
        filterMatrix.preConcat(brightnessMatrix);
        ColorMatrixColorFilter colorMatrixColorFilter = new ColorMatrixColorFilter(filterMatrix);
        this.mDisabledColorFilter = colorMatrixColorFilter;
        return colorMatrixColorFilter;
    }
}