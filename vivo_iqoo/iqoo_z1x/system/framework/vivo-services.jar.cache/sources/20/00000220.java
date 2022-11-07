package com.android.server.display.color;

import android.os.SystemProperties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoLcmUtils {
    public static final int LCM_FAILED = -1;
    public static final int LCM_SUCCESS = 0;
    public static final int LCM_UNSUPPORT = -2;
    private static final String TAG = "VivoLcmUtils";
    private static final boolean DEBUG = SystemProperties.getBoolean("debug.vivo.lcmutils.log", true);
    private static Object mLock = new Object();

    public static String readKernelNode(String fileName) {
        StringBuilder sb;
        File file = new File(fileName);
        BufferedReader reader = null;
        String tempString = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(file));
                tempString = reader.readLine();
            } catch (FileNotFoundException e) {
                VSlog.e(TAG, "the readKernelNode is:" + e.getMessage());
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e2) {
                        e1 = e2;
                        sb = new StringBuilder();
                        sb.append("the readKernelNode is:");
                        sb.append(e1.getMessage());
                        VSlog.e(TAG, sb.toString());
                        return tempString;
                    }
                }
            } catch (Exception e3) {
                VSlog.e(TAG, "other exception is:" + e3.getMessage());
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                        e1 = e4;
                        sb = new StringBuilder();
                        sb.append("the readKernelNode is:");
                        sb.append(e1.getMessage());
                        VSlog.e(TAG, sb.toString());
                        return tempString;
                    }
                }
            }
            try {
                reader.close();
            } catch (IOException e5) {
                e1 = e5;
                sb = new StringBuilder();
                sb.append("the readKernelNode is:");
                sb.append(e1.getMessage());
                VSlog.e(TAG, sb.toString());
                return tempString;
            }
            return tempString;
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    VSlog.e(TAG, "the readKernelNode is:" + e1.getMessage());
                }
            }
            throw th;
        }
    }

    public static boolean peekKernelNode(String fileName) {
        String readString = readKernelNode(fileName);
        if (DEBUG) {
            VSlog.d(TAG, "PeekKernelNode = " + readString);
        }
        if (readString != null) {
            return true;
        }
        return false;
    }

    public static int writeKernelNode(String file_patch, String value) {
        String str;
        String str2;
        int iRet = 0;
        BufferedWriter bufWriter = null;
        synchronized (mLock) {
            try {
                bufWriter = new BufferedWriter(new FileWriter(file_patch));
                bufWriter.write(value);
                bufWriter.flush();
                try {
                    bufWriter.close();
                } catch (IOException e1) {
                    str = TAG;
                    str2 = "the writeKernelNode is:" + e1.getMessage();
                    VSlog.e(str, str2);
                    return iRet;
                }
            } catch (IOException e) {
                VSlog.e(TAG, "can't write the " + file_patch);
                iRet = -1;
                if (bufWriter != null) {
                    try {
                        bufWriter.close();
                    } catch (IOException e12) {
                        str = TAG;
                        str2 = "the writeKernelNode is:" + e12.getMessage();
                        VSlog.e(str, str2);
                        return iRet;
                    }
                }
            }
        }
        return iRet;
    }
}