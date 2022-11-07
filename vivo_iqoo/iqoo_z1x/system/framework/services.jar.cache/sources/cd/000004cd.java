package com.android.server;

import android.net.NetworkSpecifier;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.Slog;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.lang.reflect.InvocationTargetException;

/* loaded from: classes.dex */
public class NetPluginDelegate {
    private static final boolean LOGV = true;
    private static final String TAG = "NetPluginDelegate";
    private static Class tcpBufferRelay = null;
    private static Object tcpBufferManagerObj = null;
    private static boolean extJarAvail = true;
    private static Class vendorPropRelay = null;
    private static Object vendorPropManagerObj = null;
    private static boolean vendorPropJarAvail = true;

    public static String get5GTcpBuffers(String currentTcpBuffer, NetworkSpecifier sepcifier) {
        Slog.v(TAG, "get5GTcpBuffers");
        if (!extJarAvail || !loadConnExtJar()) {
            return currentTcpBuffer;
        }
        try {
            Object ret = tcpBufferRelay.getMethod("get5GTcpBuffers", String.class, NetworkSpecifier.class).invoke(tcpBufferManagerObj, currentTcpBuffer, sepcifier);
            if (ret == null || !(ret instanceof String)) {
                return currentTcpBuffer;
            }
            String tcpBuffer = (String) ret;
            return tcpBuffer;
        } catch (NoSuchMethodException | SecurityException | InvocationTargetException e) {
            Log.w(TAG, "Failed to invoke get5GTcpBuffers()");
            e.printStackTrace();
            extJarAvail = false;
            return currentTcpBuffer;
        } catch (Exception e2) {
            Log.w(TAG, "Error calling get5GTcpBuffers Method on extension jar");
            e2.printStackTrace();
            extJarAvail = false;
            return currentTcpBuffer;
        }
    }

    public static void registerHandler(Handler mHandler) {
        Slog.v(TAG, "registerHandler");
        if (!extJarAvail || !loadConnExtJar()) {
            return;
        }
        try {
            tcpBufferRelay.getMethod("registerHandler", Handler.class).invoke(tcpBufferManagerObj, mHandler);
        } catch (NoSuchMethodException | SecurityException | InvocationTargetException e) {
            Log.w(TAG, "Failed to call registerHandler");
            e.printStackTrace();
            extJarAvail = false;
        } catch (Exception e2) {
            Log.w(TAG, "Error calling registerHandler Method on extension jar");
            e2.printStackTrace();
            extJarAvail = false;
        }
    }

    private static synchronized boolean loadConnExtJar() {
        synchronized (NetPluginDelegate.class) {
            String realProviderPath = Environment.getRootDirectory().getAbsolutePath() + "/framework/ConnectivityExt.jar";
            if (tcpBufferRelay == null || tcpBufferManagerObj == null) {
                boolean exists = new File(realProviderPath).exists();
                extJarAvail = exists;
                if (!exists) {
                    Log.w(TAG, "ConnectivityExt jar file not present");
                    return false;
                }
                if (tcpBufferRelay == null && tcpBufferManagerObj == null) {
                    Slog.v(TAG, "loading ConnectivityExt jar");
                    try {
                        try {
                            PathClassLoader classLoader = new PathClassLoader(realProviderPath, ClassLoader.getSystemClassLoader());
                            Class loadClass = classLoader.loadClass("com.qualcomm.qti.net.connextension.TCPBufferManager");
                            tcpBufferRelay = loadClass;
                            tcpBufferManagerObj = loadClass.newInstance();
                            Slog.v(TAG, "ConnectivityExt jar loaded");
                        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                            Log.w(TAG, "Failed to find, instantiate or access ConnectivityExt jar ");
                            e.printStackTrace();
                            extJarAvail = false;
                            return false;
                        }
                    } catch (Exception e2) {
                        Log.w(TAG, "unable to load ConnectivityExt jar");
                        e2.printStackTrace();
                        extJarAvail = false;
                        return false;
                    }
                }
                return true;
            }
            return true;
        }
    }

    private static synchronized boolean loadVendorPropJar() {
        synchronized (NetPluginDelegate.class) {
            String realProviderPath = Environment.getRootDirectory().getAbsolutePath() + "/framework/VendorPropExt.jar";
            if (vendorPropRelay == null || vendorPropManagerObj == null) {
                boolean exists = new File(realProviderPath).exists();
                vendorPropJarAvail = exists;
                if (!exists) {
                    Slog.w(TAG, "VendorPropExt jar file not present");
                    return false;
                }
                if (vendorPropRelay == null && vendorPropManagerObj == null) {
                    Slog.v(TAG, "loading VendorPropExt jar");
                    try {
                        try {
                            PathClassLoader classLoader = new PathClassLoader(realProviderPath, ClassLoader.getSystemClassLoader());
                            Class loadClass = classLoader.loadClass("com.qualcomm.qti.net.vendorpropextension.vendorPropManager");
                            vendorPropRelay = loadClass;
                            vendorPropManagerObj = loadClass.newInstance();
                            Slog.v(TAG, "VendorPropExt jar loaded");
                        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                            Slog.e(TAG, "Failed to find, instantiate or access VendorPropExt jar ");
                            e.printStackTrace();
                            vendorPropJarAvail = false;
                            return false;
                        }
                    } catch (Exception e2) {
                        Slog.e(TAG, "unable to load vendorPropExt jar");
                        e2.printStackTrace();
                        vendorPropJarAvail = false;
                        return false;
                    }
                }
                return true;
            }
            return true;
        }
    }

    public static String getConfig(String key, String currentConfigValue) {
        if (vendorPropJarAvail && loadVendorPropJar()) {
            try {
                Object ret = vendorPropRelay.getMethod("getConfig", String.class, String.class).invoke(vendorPropManagerObj, key, currentConfigValue);
                if (ret == null || !(ret instanceof String)) {
                    return currentConfigValue;
                }
                String configValue = (String) ret;
                return configValue;
            } catch (NoSuchMethodException | SecurityException | InvocationTargetException e) {
                Slog.e(TAG, "Failed to invoke getConfig()");
                e.printStackTrace();
                vendorPropJarAvail = false;
                return currentConfigValue;
            } catch (Exception e2) {
                Slog.e(TAG, "Error calling getConfig Method on vendorpropextension jar");
                e2.printStackTrace();
                vendorPropJarAvail = false;
                return currentConfigValue;
            }
        }
        return currentConfigValue;
    }
}