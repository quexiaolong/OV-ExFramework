package com.vivo.services.autorecover;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.SystemProperties;
import com.vivo.common.utils.VLog;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/* loaded from: classes.dex */
public class AlgorithmUtil {
    private static int BLACK_PIXEL_COUNT_THRESHOLD;
    public static boolean DEBUG_SCREEN_SHOT;
    private static int TRANSPARENT_PIXEL_COUNT_THRESHOLD;
    public static int BLACK_SCALE_PIXEL = SystemProperties.getInt("persist.vivo.blackpixel.scaled", 32);
    public static int TRANSPARENT_SCALE_PIXEL = SystemProperties.getInt("persist.vivo.transparentpixel.scaled", 32);
    private static int BLACK_RGB_THRESHOLD = SystemProperties.getInt("persist.vivo.blackpixel.rgb", 20);

    static {
        int i = TRANSPARENT_SCALE_PIXEL;
        TRANSPARENT_PIXEL_COUNT_THRESHOLD = i * i;
        int i2 = BLACK_SCALE_PIXEL;
        BLACK_PIXEL_COUNT_THRESHOLD = i2 * i2;
        DEBUG_SCREEN_SHOT = SystemProperties.getBoolean("persist.vivo.debug.screenshot", false);
    }

    public static void reloadThreshold() {
        BLACK_SCALE_PIXEL = SystemProperties.getInt("persist.vivo.blackpixel.scaled", 32);
        int i = SystemProperties.getInt("persist.vivo.transparentpixel.scaled", 32);
        TRANSPARENT_SCALE_PIXEL = i;
        int i2 = BLACK_SCALE_PIXEL;
        BLACK_PIXEL_COUNT_THRESHOLD = i2 * i2;
        TRANSPARENT_PIXEL_COUNT_THRESHOLD = i * i;
        BLACK_RGB_THRESHOLD = SystemProperties.getInt("persist.vivo.blackpixel.rgb", 20);
        DEBUG_SCREEN_SHOT = SystemProperties.getBoolean("persist.vivo.debug.screenshot", false);
    }

    public static boolean isBlack(Bitmap bmp) {
        Bitmap newbmp;
        int scaledWidth;
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int i = BLACK_SCALE_PIXEL;
        float widthScale = i / width;
        float heightScale = i / height;
        Matrix matrix = new Matrix();
        matrix.postScale(widthScale, heightScale);
        Bitmap newbmp2 = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        debugScaledScreenShot(newbmp2);
        int scaledWidth2 = newbmp2.getWidth();
        int scaledHeight = newbmp2.getHeight();
        int[] pixels = new int[scaledWidth2 * scaledHeight];
        newbmp2.getPixels(pixels, 0, scaledWidth2, 0, 0, scaledWidth2, scaledHeight);
        int blackPixel = 0;
        for (int i2 = 0; i2 < scaledHeight; i2++) {
            int j = 0;
            while (j < scaledWidth2) {
                int grey = pixels[(scaledWidth2 * i2) + j];
                long alpha = 255;
                if (!bmp.hasAlpha()) {
                    newbmp = newbmp2;
                    scaledWidth = scaledWidth2;
                } else {
                    newbmp = newbmp2;
                    scaledWidth = scaledWidth2;
                    alpha = (grey & 4278190080L) >> 24;
                }
                int red = (16711680 & grey) >> 16;
                int green = (65280 & grey) >> 8;
                int scaledHeight2 = scaledHeight;
                int grey2 = ((red + green) + (grey & 255)) / 3;
                if (alpha == 255) {
                    int red2 = BLACK_RGB_THRESHOLD;
                    if (grey2 <= red2) {
                        blackPixel++;
                    }
                }
                j++;
                newbmp2 = newbmp;
                scaledWidth2 = scaledWidth;
                scaledHeight = scaledHeight2;
            }
        }
        VLog.d(SystemAutoRecoverService.TAG, "black pixel = " + blackPixel);
        return blackPixel >= BLACK_PIXEL_COUNT_THRESHOLD;
    }

    public static boolean isTransparent(Bitmap bmp) {
        if (bmp.hasAlpha()) {
            int width = bmp.getWidth();
            int height = bmp.getHeight();
            int i = TRANSPARENT_SCALE_PIXEL;
            float widthScale = i / width;
            float heightScale = i / height;
            Matrix matrix = new Matrix();
            matrix.postScale(widthScale, heightScale);
            Bitmap newbmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
            debugScaledScreenShot(newbmp);
            int scaledWidth = newbmp.getWidth();
            int scaledHeight = newbmp.getHeight();
            int[] pixels = new int[scaledWidth * scaledHeight];
            newbmp.getPixels(pixels, 0, scaledWidth, 0, 0, scaledWidth, scaledHeight);
            int transparentPixel = 0;
            for (int i2 = 0; i2 < scaledHeight; i2++) {
                for (int j = 0; j < scaledWidth; j++) {
                    int argb = pixels[(scaledWidth * i2) + j];
                    long alpha = (argb & 4278190080L) >> 24;
                    if (alpha == 0) {
                        transparentPixel++;
                    }
                }
            }
            VLog.d(SystemAutoRecoverService.TAG, "transparentPixel pixel = " + transparentPixel);
            return transparentPixel >= TRANSPARENT_PIXEL_COUNT_THRESHOLD;
        }
        return false;
    }

    public static void debugScreenShot(String fileName, Bitmap bitmap) {
        File drectory = new File("/data/bbkcore/transparent");
        try {
            if (!drectory.exists() && drectory.mkdir()) {
                drectory.setReadable(true, false);
                drectory.setExecutable(true, false);
                drectory.setWritable(true, false);
            }
            File file = new File(drectory, fileName);
            try {
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 60, out);
                VLog.d(SystemAutoRecoverService.TAG, "screenshot saved success. name = " + fileName);
                out.flush();
                file.setReadable(true, false);
                file.setExecutable(true, false);
                file.setWritable(true, false);
                out.close();
            } catch (Exception e) {
                VLog.e(SystemAutoRecoverService.TAG, "Save pic error:" + e);
            }
        } catch (Exception e2) {
            VLog.e(SystemAutoRecoverService.TAG, "mkdir cause exception:" + e2);
        }
    }

    private static void debugScaledScreenShot(Bitmap bitmap) {
        if (DEBUG_SCREEN_SHOT) {
            Date stamp = new Date(System.currentTimeMillis());
            String currentTime = new SimpleDateFormat("yyyyMMdd-HHmmss").format(stamp);
            debugScreenShot("scaled_" + currentTime + ".png", bitmap);
        }
    }
}