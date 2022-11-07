package com.vivo.services.proxy;

import android.os.Bundle;
import android.os.SystemProperties;
import com.android.server.am.BroadcastProxyManager;
import com.vivo.services.proxy.game.GameSceneProxyManager;
import com.vivo.services.proxy.transact.TransactProxyManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/* loaded from: classes.dex */
public class ProxyConfigs {
    private static final String ALLOW_DUMP_PROPERTY = "persist.rms.allow_dump";
    public static final int CONFIG_OP_ADD = 1;
    public static final String CONFIG_OP_KEY = "op";
    public static final int CONFIG_OP_REMOVE = 2;
    public static final int CONFIG_OP_REPLACE = 0;
    private static final String CONFIG_PROXY_CTRL_MODULE_SET_NAME = "proxy_ctrl_module_set";
    private static final String CONFIG_PROXY_FEATURE_ENABLE = "proxy_feature_enable";
    public static final String CONFIG_VALUES_KEY = "values";
    public static final String CTRL_MODULE_FROZEN = "system_frozen";
    public static final String CTRL_MODULE_GAME = "system_game";
    public static final String CTRL_MODULE_GAMEWATCH = "com.vivo.gamewatch";
    public static final String CTRL_MODULE_PEM = "com.vivo.pem";
    public static final String CTRL_MODULE_RMS = "com.vivo.rms";
    public static final HashSet<String> CTRL_MODULE_SET;
    public static final String CTRL_MODULE_SHELL = "system_shell";
    public static boolean DEBUG_BQ = false;
    public static final int PROXY_FLAGS_BINDER_TRANSACT = 4;
    public static final int PROXY_FLAGS_BQ_PACKAGE = 1;
    public static final int PROXY_FLAGS_BQ_PROCESS = 2;

    static {
        HashSet<String> hashSet = new HashSet<>();
        CTRL_MODULE_SET = hashSet;
        DEBUG_BQ = false;
        hashSet.add(CTRL_MODULE_FROZEN);
        CTRL_MODULE_SET.add(CTRL_MODULE_GAME);
        CTRL_MODULE_SET.add(CTRL_MODULE_SHELL);
        CTRL_MODULE_SET.add("com.vivo.gamewatch");
        CTRL_MODULE_SET.add(CTRL_MODULE_RMS);
        CTRL_MODULE_SET.add(CTRL_MODULE_PEM);
    }

    public static void proxyApp(String pkg, int userId, int flags, boolean proxy, String module) {
        List<String> pkgList = new ArrayList<>(1);
        pkgList.add(pkg);
        proxyApp(pkgList, userId, flags, proxy, module);
    }

    public static void proxyApp(List<String> pkgList, int userId, int flags, boolean proxy, String module) {
        if ((flags & 1) != 0) {
            BroadcastProxyManager.proxyPackage(pkgList, userId, proxy, module);
        }
        if ((flags & 2) != 0) {
            BroadcastProxyManager.proxyProcess(pkgList, userId, proxy, module);
        }
        if ((flags & 4) != 0) {
            TransactProxyManager.proxyPackage(pkgList, userId, proxy, module);
        }
    }

    public static void setEnable(int masks, int flags) {
        if ((masks & 1) != 0) {
            BroadcastProxyManager.setProxyPackageEnable((flags & 1) != 0);
        }
        if ((masks & 2) != 0) {
            BroadcastProxyManager.setProxyProcessEnable((flags & 2) != 0);
        }
        if ((flags & 4) != 0) {
            TransactProxyManager.setEnable((flags & 4) != 0);
        }
    }

    public static boolean setBundle(Bundle configs) {
        if (configs == null) {
            return false;
        }
        for (String name : configs.keySet()) {
            Bundle bundle = configs.getBundle(name);
            if (bundle != null) {
                char c = 65535;
                int hashCode = name.hashCode();
                if (hashCode != 1674789085) {
                    if (hashCode == 1783919250 && name.equals(CONFIG_PROXY_CTRL_MODULE_SET_NAME)) {
                        c = 0;
                    }
                } else if (name.equals(CONFIG_PROXY_FEATURE_ENABLE)) {
                    c = 1;
                }
                if (c == 0) {
                    updateCollectionLocked(CTRL_MODULE_SET, bundle);
                    return true;
                } else if (c == 1) {
                    setEnable(bundle.getInt("masks", 0), bundle.getInt("flags", 0));
                    return true;
                } else {
                    return BroadcastProxyManager.setBundle(name, bundle);
                }
            }
        }
        return true;
    }

    public static void updateCollectionLocked(Collection<String> collection, Bundle bundle) {
        int op = bundle.getInt(CONFIG_OP_KEY, 0);
        ArrayList<String> values = bundle.getStringArrayList(CONFIG_VALUES_KEY);
        if (op == 0) {
            collection.clear();
        }
        if (values != null) {
            if (op == 2) {
                collection.removeAll(values);
            } else {
                collection.addAll(values);
            }
        }
    }

    public static void dump(PrintWriter pw, String[] args) {
        if (!isDumpAllowed()) {
            return;
        }
        pw.println("-------------------------------------------");
        if (args.length >= 2 && args[0].equals("--debug")) {
            DEBUG_BQ = Boolean.valueOf(args[1]).booleanValue();
            pw.print("DEBUG_BQ = ");
            pw.println(String.valueOf(DEBUG_BQ));
        } else if (args.length >= 1 && "--broadcast".equals(args[0])) {
            BroadcastProxyManager.dump(pw, args, 1);
        } else if (args.length >= 1 && "--transact".equals(args[0])) {
            TransactProxyManager.dump(pw, args, 1);
        } else if (args.length >= 1 && "--gameScene".equals(args[0])) {
            GameSceneProxyManager.dump(pw);
        } else if (args.length >= 5 && args[0].equals("--proxy")) {
            String target = args[1];
            int userId = Integer.valueOf(args[2]).intValue();
            int flags = Integer.valueOf(args[3]).intValue();
            String proxy = args[4];
            proxyApp(target, userId, flags, Boolean.valueOf(proxy).booleanValue(), CTRL_MODULE_SHELL);
            pw.println(String.format("proxyApp target=%s userId=%d flags=0x%x proxy=%s", target, Integer.valueOf(userId), Integer.valueOf(flags), proxy));
        } else if (args.length >= 3 && args[0].equals("--setEnable")) {
            int masks = Integer.valueOf(args[1]).intValue();
            int flags2 = Integer.valueOf(args[2]).intValue();
            setEnable(masks, flags2);
            pw.println(String.format("setEnable enable=%s masks=0x%x flags=0x%x", args[1], Integer.valueOf(masks), Integer.valueOf(flags2)));
        }
    }

    public static boolean isDumpAllowed() {
        return SystemProperties.getBoolean(ALLOW_DUMP_PROPERTY, false);
    }
}