package com.android.server.display.color;

import android.app.ActivityTaskManager;
import android.content.res.Configuration;
import android.opengl.Matrix;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.SparseArray;
import java.lang.reflect.Array;
import java.util.Arrays;

/* loaded from: classes.dex */
public class DisplayTransformManager {
    private static final float COLOR_SATURATION_BOOSTED = 1.1f;
    private static final float COLOR_SATURATION_NATURAL = 1.0f;
    private static final int DISPLAY_COLOR_ENHANCED = 2;
    private static final int DISPLAY_COLOR_MANAGED = 0;
    private static final int DISPLAY_COLOR_UNMANAGED = 1;
    public static final int LEVEL_COLOR_MATRIX_AUTO_CCT = 130;
    public static final int LEVEL_COLOR_MATRIX_DISPLAY_WHITE_BALANCE = 125;
    public static final int LEVEL_COLOR_MATRIX_GRAYSCALE = 200;
    public static final int LEVEL_COLOR_MATRIX_INVERT_COLOR = 300;
    public static final int LEVEL_COLOR_MATRIX_NIGHT_DISPLAY = 100;
    public static final int LEVEL_COLOR_MATRIX_SATURATION = 150;
    public static final int LEVEL_DC_COLOR_MATRIX = 401;
    public static final int LEVEL_NLL_COLOR_MATRIX = 400;
    static final String PERSISTENT_PROPERTY_COMPOSITION_COLOR_MODE = "persist.sys.sf.color_mode";
    static final String PERSISTENT_PROPERTY_DISPLAY_COLOR = "persist.sys.sf.native_mode";
    static final String PERSISTENT_PROPERTY_SATURATION = "persist.sys.sf.color_saturation";
    private static final int SURFACE_FLINGER_TRANSACTION_COLOR_MATRIX = 1015;
    private static final int SURFACE_FLINGER_TRANSACTION_DALTONIZER = 1014;
    private static final int SURFACE_FLINGER_TRANSACTION_DISPLAY_COLOR = 1023;
    private static final int SURFACE_FLINGER_TRANSACTION_QUERY_COLOR_MANAGED = 1030;
    private static final int SURFACE_FLINGER_TRANSACTION_QUERY_DISPLAY_COLOR_SUPPORT = 1027;
    private static final int SURFACE_FLINGER_TRANSACTION_SATURATION = 1022;
    private static final String TAG = "DisplayTransformManager";
    private static final String SURFACE_FLINGER = "SurfaceFlinger";
    private static final IBinder sFlinger = ServiceManager.getService(SURFACE_FLINGER);
    private final SparseArray<float[]> mColorMatrix = new SparseArray<>(5);
    private final float[][] mTempColorMatrix = (float[][]) Array.newInstance(float.class, 2, 16);
    private int mData = 0;
    private final Object mDaltonizerModeLock = new Object();
    private int mDaltonizerMode = -1;

    public void setData(int data) {
        this.mData = data;
    }

    public float[] getColorMatrix(int key) {
        float[] copyOf;
        synchronized (this.mColorMatrix) {
            float[] value = this.mColorMatrix.get(key);
            copyOf = value == null ? null : Arrays.copyOf(value, value.length);
        }
        return copyOf;
    }

    public void setColorMatrix(int level, float[] value) {
        if (value != null && value.length != 16) {
            throw new IllegalArgumentException("Expected length: 16 (4x4 matrix), actual length: " + value.length);
        }
        synchronized (this.mColorMatrix) {
            float[] oldValue = this.mColorMatrix.get(level);
            if (!Arrays.equals(oldValue, value)) {
                if (value == null) {
                    this.mColorMatrix.remove(level);
                } else if (oldValue == null) {
                    this.mColorMatrix.put(level, Arrays.copyOf(value, value.length));
                } else {
                    System.arraycopy(value, 0, oldValue, 0, value.length);
                }
                applyColorMatrix(computeColorMatrixLocked());
            }
        }
    }

    public void updateColorMatrix(int level, float[] value) {
        if (value != null && value.length != 16) {
            throw new IllegalArgumentException("Expected length: 16 (4x4 matrix), actual length: " + value.length);
        }
        synchronized (this.mColorMatrix) {
            float[] oldValue = this.mColorMatrix.get(level);
            if (!Arrays.equals(oldValue, value)) {
                if (value == null) {
                    this.mColorMatrix.remove(level);
                } else if (oldValue == null) {
                    this.mColorMatrix.put(level, Arrays.copyOf(value, value.length));
                } else {
                    System.arraycopy(value, 0, oldValue, 0, value.length);
                }
            }
        }
    }

    public void setDaltonizerMode(int mode) {
        synchronized (this.mDaltonizerModeLock) {
            if (this.mDaltonizerMode != mode) {
                this.mDaltonizerMode = mode;
                applyDaltonizerMode(mode);
            }
        }
    }

    private float[] computeColorMatrixLocked() {
        int count = this.mColorMatrix.size();
        if (count == 0) {
            return null;
        }
        float[][] result = this.mTempColorMatrix;
        Matrix.setIdentityM(result[0], 0);
        for (int i = 0; i < count; i++) {
            float[] rhs = this.mColorMatrix.valueAt(i);
            Matrix.multiplyMM(result[(i + 1) % 2], 0, result[i % 2], 0, rhs, 0);
        }
        int i2 = count % 2;
        return result[i2];
    }

    private void applyColorMatrix(float[] m) {
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken("android.ui.ISurfaceComposer");
        if (m != null) {
            data.writeInt(this.mColorMatrix.size());
            for (int i = 0; i < this.mColorMatrix.size(); i++) {
                data.writeInt(this.mColorMatrix.keyAt(i));
                if (this.mColorMatrix.keyAt(i) == 100) {
                    float[] value = this.mColorMatrix.get(100);
                    for (int j = 0; j < 16; j++) {
                        data.writeFloat(value[j]);
                    }
                } else if (this.mColorMatrix.keyAt(i) == 400) {
                    float[] value2 = this.mColorMatrix.get(LEVEL_NLL_COLOR_MATRIX);
                    for (int j2 = 0; j2 < 16; j2++) {
                        data.writeFloat(value2[j2]);
                    }
                    data.writeInt(this.mData);
                } else if (this.mColorMatrix.keyAt(i) == 401) {
                    float[] value3 = this.mColorMatrix.get(LEVEL_DC_COLOR_MATRIX);
                    for (int j3 = 0; j3 < 16; j3++) {
                        data.writeFloat(value3[j3]);
                    }
                    data.writeInt(this.mData);
                } else if (this.mColorMatrix.keyAt(i) == 130) {
                    float[] value4 = this.mColorMatrix.get(130);
                    for (int j4 = 0; j4 < 16; j4++) {
                        data.writeFloat(value4[j4]);
                    }
                }
            }
            for (int i2 = 0; i2 < 16; i2++) {
                data.writeFloat(m[i2]);
            }
        } else {
            data.writeInt(0);
        }
        try {
            try {
                sFlinger.transact(SURFACE_FLINGER_TRANSACTION_COLOR_MATRIX, data, null, 0);
            } catch (RemoteException ex) {
                Slog.e(TAG, "Failed to set color transform", ex);
            }
        } finally {
            data.recycle();
        }
    }

    private static void applyDaltonizerMode(int mode) {
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken("android.ui.ISurfaceComposer");
        data.writeInt(mode);
        try {
            try {
                sFlinger.transact(SURFACE_FLINGER_TRANSACTION_DALTONIZER, data, null, 0);
            } catch (RemoteException ex) {
                Slog.e(TAG, "Failed to set Daltonizer mode", ex);
            }
        } finally {
            data.recycle();
        }
    }

    public static boolean needsLinearColorMatrix() {
        return SystemProperties.getInt(PERSISTENT_PROPERTY_DISPLAY_COLOR, 1) != 1;
    }

    public static boolean needsLinearColorMatrix(int colorMode) {
        return colorMode != 2;
    }

    public boolean setColorMode(int colorMode, float[] nightDisplayMatrix, int compositionColorMode) {
        if (colorMode == 0) {
            applySaturation(1.0f);
            setDisplayColor(0, compositionColorMode);
        } else if (colorMode == 1) {
            applySaturation(COLOR_SATURATION_BOOSTED);
            setDisplayColor(0, compositionColorMode);
        } else if (colorMode == 2) {
            applySaturation(1.0f);
            setDisplayColor(1, compositionColorMode);
        } else if (colorMode == 3) {
            applySaturation(1.0f);
            setDisplayColor(2, compositionColorMode);
        } else if (colorMode >= 256 && colorMode <= 511) {
            applySaturation(1.0f);
            setDisplayColor(colorMode, compositionColorMode);
        }
        setColorMatrix(100, nightDisplayMatrix);
        updateConfiguration();
        return true;
    }

    public boolean isDeviceColorManaged() {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken("android.ui.ISurfaceComposer");
        try {
            sFlinger.transact(SURFACE_FLINGER_TRANSACTION_QUERY_COLOR_MANAGED, data, reply, 0);
            return reply.readBoolean();
        } catch (RemoteException ex) {
            Slog.e(TAG, "Failed to query wide color support", ex);
            return false;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private void applySaturation(float saturation) {
        SystemProperties.set(PERSISTENT_PROPERTY_SATURATION, Float.toString(saturation));
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken("android.ui.ISurfaceComposer");
        data.writeFloat(saturation);
        try {
            try {
                sFlinger.transact(SURFACE_FLINGER_TRANSACTION_SATURATION, data, null, 0);
            } catch (RemoteException ex) {
                Slog.e(TAG, "Failed to set saturation", ex);
            }
        } finally {
            data.recycle();
        }
    }

    public void setDisplayColor(int color, int compositionColorMode) {
        SystemProperties.set(PERSISTENT_PROPERTY_DISPLAY_COLOR, Integer.toString(color));
        if (compositionColorMode != -1) {
            SystemProperties.set(PERSISTENT_PROPERTY_COMPOSITION_COLOR_MODE, Integer.toString(compositionColorMode));
        }
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken("android.ui.ISurfaceComposer");
        data.writeInt(color);
        if (compositionColorMode != -1) {
            data.writeInt(compositionColorMode);
        }
        try {
            try {
                sFlinger.transact(SURFACE_FLINGER_TRANSACTION_DISPLAY_COLOR, data, null, 0);
            } catch (RemoteException ex) {
                Slog.e(TAG, "Failed to set display color", ex);
            }
        } finally {
            data.recycle();
        }
    }

    public boolean isDisplayColorSupport(int color) {
        IBinder flinger = ServiceManager.getService(SURFACE_FLINGER);
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(color);
            try {
                flinger.transact(1027, data, reply, 0);
                return reply.readBoolean();
            } catch (RemoteException ex) {
                Slog.e(TAG, "Failed to query display color support", ex);
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
        return false;
    }

    private void updateConfiguration() {
        try {
            ActivityTaskManager.getService().updateConfiguration((Configuration) null);
        } catch (RemoteException e) {
            Slog.e(TAG, "Could not update configuration", e);
        }
    }
}