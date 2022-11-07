package com.android.server.om;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.RemoteException;
import android.os.ShellCommand;
import android.os.UserHandle;
import android.util.TypedValue;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* loaded from: classes.dex */
final class OverlayManagerShellCommand extends ShellCommand {
    private final Context mContext;
    private final IOverlayManager mInterface;

    /* JADX INFO: Access modifiers changed from: package-private */
    public OverlayManagerShellCommand(Context ctx, IOverlayManager iom) {
        this.mContext = ctx;
        this.mInterface = iom;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public int onCommand(String cmd) {
        boolean z;
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        PrintWriter err = getErrPrintWriter();
        try {
            switch (cmd.hashCode()) {
                case -1361113425:
                    if (cmd.equals("set-priority")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case -1298848381:
                    if (cmd.equals("enable")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case -1097094790:
                    if (cmd.equals("lookup")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case -794624300:
                    if (cmd.equals("enable-exclusive")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                case 3322014:
                    if (cmd.equals("list")) {
                        z = false;
                        break;
                    }
                    z = true;
                    break;
                case 1671308008:
                    if (cmd.equals("disable")) {
                        z = true;
                        break;
                    }
                    z = true;
                    break;
                default:
                    z = true;
                    break;
            }
            if (z) {
                if (!z) {
                    if (!z) {
                        if (!z) {
                            if (!z) {
                                if (z) {
                                    return runLookup();
                                }
                                return handleDefaultCommands(cmd);
                            }
                            return runSetPriority();
                        }
                        return runEnableExclusive();
                    }
                    return runEnableDisable(false);
                }
                return runEnableDisable(true);
            }
            return runList();
        } catch (RemoteException e) {
            err.println("Remote exception: " + e);
            return -1;
        } catch (IllegalArgumentException e2) {
            err.println("Error: " + e2.getMessage());
            return -1;
        }
    }

    public void onHelp() {
        PrintWriter out = getOutPrintWriter();
        out.println("Overlay manager (overlay) commands:");
        out.println("  help");
        out.println("    Print this help text.");
        out.println("  dump [--verbose] [--user USER_ID] [[FIELD] PACKAGE]");
        out.println("    Print debugging information about the overlay manager.");
        out.println("    With optional parameter PACKAGE, limit output to the specified");
        out.println("    package. With optional parameter FIELD, limit output to");
        out.println("    the value of that SettingsItem field. Field names are");
        out.println("    case insensitive and out.println the m prefix can be omitted,");
        out.println("    so the following are equivalent: mState, mstate, State, state.");
        out.println("  list [--user USER_ID] [PACKAGE]");
        out.println("    Print information about target and overlay packages.");
        out.println("    Overlay packages are printed in priority order. With optional");
        out.println("    parameter PACKAGE, limit output to the specified package.");
        out.println("  enable [--user USER_ID] PACKAGE");
        out.println("    Enable overlay package PACKAGE.");
        out.println("  disable [--user USER_ID] PACKAGE");
        out.println("    Disable overlay package PACKAGE.");
        out.println("  enable-exclusive [--user USER_ID] [--category] PACKAGE");
        out.println("    Enable overlay package PACKAGE and disable all other overlays for");
        out.println("    its target package. If the --category option is given, only disables");
        out.println("    other overlays in the same category.");
        out.println("  set-priority [--user USER_ID] PACKAGE PARENT|lowest|highest");
        out.println("    Change the priority of the overlay PACKAGE to be just higher than");
        out.println("    the priority of PACKAGE_PARENT If PARENT is the special keyword");
        out.println("    'lowest', change priority of PACKAGE to the lowest priority.");
        out.println("    If PARENT is the special keyword 'highest', change priority of");
        out.println("    PACKAGE to the highest priority.");
        out.println("  lookup [--verbose] PACKAGE-TO-LOAD PACKAGE:TYPE/NAME");
        out.println("    Load a package and print the value of a given resource");
        out.println("    applying the current configuration and enabled overlays.");
        out.println("    For a more fine-grained alernative, use 'idmap2 lookup'.");
    }

    private int runList() throws RemoteException {
        PrintWriter out = getOutPrintWriter();
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            boolean z = false;
            if (opt == null) {
                String packageName = getNextArg();
                if (packageName != null) {
                    List<OverlayInfo> overlaysForTarget = this.mInterface.getOverlayInfosForTarget(packageName, userId);
                    if (overlaysForTarget.isEmpty()) {
                        OverlayInfo info = this.mInterface.getOverlayInfo(packageName, userId);
                        if (info != null) {
                            printListOverlay(out, info);
                        }
                        return 0;
                    }
                    out.println(packageName);
                    int n = overlaysForTarget.size();
                    for (int i = 0; i < n; i++) {
                        printListOverlay(out, overlaysForTarget.get(i));
                    }
                    return 0;
                }
                Map<String, List<OverlayInfo>> allOverlays = this.mInterface.getAllOverlays(userId);
                for (String targetPackageName : allOverlays.keySet()) {
                    out.println(targetPackageName);
                    List<OverlayInfo> overlaysForTarget2 = allOverlays.get(targetPackageName);
                    int n2 = overlaysForTarget2.size();
                    for (int i2 = 0; i2 < n2; i2++) {
                        printListOverlay(out, overlaysForTarget2.get(i2));
                    }
                    out.println();
                }
                return 0;
            }
            if (!((opt.hashCode() == 1333469547 && opt.equals("--user")) ? true : true)) {
                userId = UserHandle.parseUserArg(getNextArgRequired());
            } else {
                err.println("Error: Unknown option: " + opt);
                return 1;
            }
        }
    }

    private void printListOverlay(PrintWriter out, OverlayInfo oi) {
        String status;
        int i = oi.state;
        if (i == 2) {
            status = "[ ]";
        } else if (i == 3 || i == 6) {
            status = "[x]";
        } else {
            status = "---";
        }
        out.println(String.format("%s %s", status, oi.packageName));
    }

    private int runEnableDisable(boolean enable) throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                char c = 65535;
                if (opt.hashCode() == 1333469547 && opt.equals("--user")) {
                    c = 0;
                }
                if (c == 0) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    err.println("Error: Unknown option: " + opt);
                    return 1;
                }
            } else {
                String packageName = getNextArgRequired();
                return 1 ^ (this.mInterface.setEnabled(packageName, enable, userId) ? 1 : 0);
            }
        }
    }

    private int runEnableExclusive() throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        boolean inCategory = false;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                char c = 65535;
                int hashCode = opt.hashCode();
                if (hashCode != 66265758) {
                    if (hashCode == 1333469547 && opt.equals("--user")) {
                        c = 0;
                    }
                } else if (opt.equals("--category")) {
                    c = 1;
                }
                if (c == 0) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else if (c == 1) {
                    inCategory = true;
                } else {
                    err.println("Error: Unknown option: " + opt);
                    return 1;
                }
            } else {
                String overlay = getNextArgRequired();
                return inCategory ? 1 ^ (this.mInterface.setEnabledExclusiveInCategory(overlay, userId) ? 1 : 0) : 1 ^ (this.mInterface.setEnabledExclusive(overlay, true, userId) ? 1 : 0);
            }
        }
    }

    private int runSetPriority() throws RemoteException {
        PrintWriter err = getErrPrintWriter();
        int userId = 0;
        while (true) {
            String opt = getNextOption();
            if (opt != null) {
                char c = 65535;
                if (opt.hashCode() == 1333469547 && opt.equals("--user")) {
                    c = 0;
                }
                if (c == 0) {
                    userId = UserHandle.parseUserArg(getNextArgRequired());
                } else {
                    err.println("Error: Unknown option: " + opt);
                    return 1;
                }
            } else {
                String packageName = getNextArgRequired();
                String newParentPackageName = getNextArgRequired();
                return "highest".equals(newParentPackageName) ? 1 ^ (this.mInterface.setHighestPriority(packageName, userId) ? 1 : 0) : "lowest".equals(newParentPackageName) ? 1 ^ (this.mInterface.setLowestPriority(packageName, userId) ? 1 : 0) : 1 ^ (this.mInterface.setPriority(packageName, newParentPackageName, userId) ? 1 : 0);
            }
        }
    }

    private int runLookup() throws RemoteException {
        PrintWriter out = getOutPrintWriter();
        PrintWriter err = getErrPrintWriter();
        boolean verbose = "--verbose".equals(getNextOption());
        String packageToLoad = getNextArgRequired();
        String fullyQualifiedResourceName = getNextArgRequired();
        Pattern regex = Pattern.compile("(.*?):(.*?)/(.*?)");
        Matcher matcher = regex.matcher(fullyQualifiedResourceName);
        if (!matcher.matches()) {
            err.println("Error: bad resource name, doesn't match package:type/name");
            return 1;
        }
        PackageManager pm = this.mContext.getPackageManager();
        if (pm == null) {
            err.println("Error: failed to get package manager");
            return 1;
        }
        try {
            Resources res = pm.getResourcesForApplication(packageToLoad);
            AssetManager assets = res.getAssets();
            try {
                assets.setResourceResolutionLoggingEnabled(true);
            } catch (Throwable th) {
                e = th;
            }
            try {
                try {
                    TypedValue value = new TypedValue();
                    res.getValue(fullyQualifiedResourceName, value, false);
                    CharSequence valueString = value.coerceToString();
                    String resolution = assets.getLastResourceResolution();
                    res.getValue(fullyQualifiedResourceName, value, true);
                    CharSequence resolvedString = value.coerceToString();
                    if (verbose) {
                        out.println(resolution);
                    }
                    if (valueString.equals(resolvedString)) {
                        out.println(valueString);
                    } else {
                        out.println(((Object) valueString) + " -> " + ((Object) resolvedString));
                    }
                    assets.setResourceResolutionLoggingEnabled(false);
                    return 0;
                } catch (Resources.NotFoundException e) {
                    try {
                        try {
                            String pkg = matcher.group(1);
                            String type = matcher.group(2);
                            String name = matcher.group(3);
                            int resid = res.getIdentifier(name, type, pkg);
                            try {
                                if (resid == 0) {
                                    throw new Resources.NotFoundException();
                                }
                                TypedArray array = res.obtainTypedArray(resid);
                                if (verbose) {
                                    try {
                                        String pkg2 = assets.getLastResourceResolution();
                                        out.println(pkg2);
                                    } catch (Resources.NotFoundException e2) {
                                        err.println("Error: failed to get the resource " + fullyQualifiedResourceName);
                                        assets.setResourceResolutionLoggingEnabled(false);
                                        return 1;
                                    }
                                }
                                TypedValue tv = new TypedValue();
                                int i = 0;
                                while (true) {
                                    Pattern regex2 = regex;
                                    if (i >= array.length()) {
                                        array.recycle();
                                        assets.setResourceResolutionLoggingEnabled(false);
                                        return 0;
                                    }
                                    array.getValue(i, tv);
                                    out.println(tv.coerceToString());
                                    i++;
                                    regex = regex2;
                                }
                            } catch (Resources.NotFoundException e3) {
                            }
                        } catch (Resources.NotFoundException e4) {
                        }
                    } catch (Throwable th2) {
                        e = th2;
                        assets.setResourceResolutionLoggingEnabled(false);
                        throw e;
                    }
                }
            } catch (Throwable th3) {
                e = th3;
                assets.setResourceResolutionLoggingEnabled(false);
                throw e;
            }
        } catch (PackageManager.NameNotFoundException e5) {
            err.println("Error: failed to get resources for package " + packageToLoad);
            return 1;
        }
    }
}