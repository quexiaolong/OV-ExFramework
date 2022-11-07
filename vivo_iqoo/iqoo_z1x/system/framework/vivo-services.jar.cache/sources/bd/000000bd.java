package com.android.server.am;

import android.os.RemoteException;
import android.os.ShellCommand;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.vcodetransbase.EventTransfer;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import vendor.vivo.hardware.vcode.V1_0.IVcode;

/* loaded from: classes.dex */
public class VcodeCommandImpl {
    private String TAG = "VCode/CommandImpl";
    private ShellCommand mShellCommand;

    private static native int runTraceData(String str, String str2, String str3, byte[] bArr);

    private static native int runTraceEvent(String str, String str2, String str3);

    public VcodeCommandImpl(ShellCommand cmd) {
        this.mShellCommand = null;
        this.mShellCommand = cmd;
    }

    public int runVcodeCommand(PrintWriter pw) throws RemoteException {
        String opt = this.mShellCommand.getNextOption();
        if (opt != null) {
            char c = 65535;
            int hashCode = opt.hashCode();
            if (hashCode != 1492) {
                if (hashCode != 1499) {
                    if (hashCode != 1505) {
                        if (hashCode == 43002099 && opt.equals("--hal")) {
                            c = 2;
                        }
                    } else if (opt.equals("-n")) {
                        c = 1;
                    }
                } else if (opt.equals("-h")) {
                    c = 3;
                }
            } else if (opt.equals("-a")) {
                c = 0;
            }
            if (c == 0) {
                String arg = this.mShellCommand.getNextArgRequired();
                return runVcodeCommand(arg, pw);
            } else if (c == 1) {
                String arg2 = this.mShellCommand.getNextArgRequired();
                return runVcodeCommandNative(arg2, pw);
            } else if (c == 2) {
                String arg3 = this.mShellCommand.getNextArgRequired();
                return runVcodeCommandHal(arg3, pw);
            } else if (c == 3) {
                dumpVcode(pw);
                return 0;
            }
        }
        return 0;
    }

    private int runVcodeCommand(String cmd, PrintWriter pw) throws RemoteException {
        if (cmd != null) {
            char c = 65535;
            int hashCode = cmd.hashCode();
            if (hashCode != -902265784) {
                if (hashCode != 3076010) {
                    if (hashCode == 110620997 && cmd.equals("trace")) {
                        c = 1;
                    }
                } else if (cmd.equals("data")) {
                    c = 2;
                }
            } else if (cmd.equals("single")) {
                c = 0;
            }
            if (c == 0) {
                return runVcodeSingleEvent(pw);
            }
            if (c == 1) {
                return runVcodeTraceEvent(pw);
            }
            if (c == 2) {
                return runVcodeDataEvent(pw);
            }
        }
        pw.println("Error: vcode options is error, please check it.");
        dumpVcode(pw);
        return 0;
    }

    private int runVcodeCommandNative(String cmd, PrintWriter pw) throws RemoteException {
        VLog.d(this.TAG, "runVcodeCommandNative cmd: " + cmd);
        if (cmd != null) {
            char c = 65535;
            int hashCode = cmd.hashCode();
            if (hashCode != 3076010) {
                if (hashCode == 110620997 && cmd.equals("trace")) {
                    c = 0;
                }
            } else if (cmd.equals("data")) {
                c = 1;
            }
            if (c == 0) {
                return runVcodeTraceNative(pw);
            }
            if (c == 1) {
                return runVcodeDataNative(pw);
            }
        }
        pw.println("Error: vcode options is error, please check it.");
        dumpVcode(pw);
        return 0;
    }

    private int runVcodeTraceNative(PrintWriter pw) {
        VcodeCommand vcmd = parseVcodeCommandArfs(pw);
        if (vcmd.moduleID == null) {
            pw.println("moduleID can't be null");
            return 0;
        } else if (vcmd.eventID == null) {
            pw.println("eventID can't be null");
            return 0;
        } else {
            String params = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (vcmd.params != null) {
                for (Map.Entry<String, String> entry : vcmd.params.entrySet()) {
                    params = (params + entry.getKey() + ":" + entry.getValue()) + "&";
                }
            }
            try {
                runTraceEvent(vcmd.moduleID, vcmd.eventID, params);
            } catch (Exception e) {
                pw.println("runVcodeTraceNative " + e.getMessage());
            }
            return 0;
        }
    }

    private int runVcodeDataNative(PrintWriter pw) {
        VcodeCommand vcmd = parseVcodeCommandArfs(pw);
        if (vcmd.moduleID == null) {
            pw.println("moduleID can't be null");
            return 0;
        } else if (vcmd.eventID == null) {
            pw.println("eventID can't be null");
            return 0;
        } else if (vcmd.fullName == null) {
            pw.println("full name can't be null");
            return 0;
        } else {
            byte[] data = getBytefromFile(vcmd.fullName, pw);
            if (data != null) {
                String str = this.TAG;
                VLog.d(str, "runVcodeDataNative:  data.length " + data.length + " data: " + new String(data));
                pw.println("runVcodeDataNative: length " + data.length + " data: " + new String(data));
            }
            String[] paths = vcmd.fullName.split("/");
            String fileName = paths[paths.length - 1];
            String str2 = this.TAG;
            VLog.d(str2, "runVcodeDataEvent:  fileName " + fileName);
            try {
                runTraceData(vcmd.moduleID, vcmd.eventID, fileName, data);
            } catch (Exception e) {
                pw.println("runVcodeDataNative " + e.getMessage());
            }
            return 0;
        }
    }

    private int runVcodeCommandHal(String cmd, PrintWriter pw) throws RemoteException {
        VLog.d(this.TAG, "runVcodeCommandHal cmd: " + cmd);
        if (cmd != null) {
            char c = 65535;
            int hashCode = cmd.hashCode();
            if (hashCode != 3076010) {
                if (hashCode == 110620997 && cmd.equals("trace")) {
                    c = 0;
                }
            } else if (cmd.equals("data")) {
                c = 1;
            }
            if (c == 0) {
                return runVcodeTraceHal(pw);
            }
            if (c == 1) {
                return runVcodeDataHal(pw);
            }
        }
        pw.println("Error: vcode options is error, please check it.");
        dumpVcode(pw);
        return 0;
    }

    private int runVcodeTraceHal(PrintWriter pw) {
        VcodeCommand vcmd = parseVcodeCommandArfs(pw);
        if (vcmd.moduleID == null) {
            pw.println("moduleID can't be null");
            return 0;
        } else if (vcmd.eventID == null) {
            pw.println("eventID can't be null");
            return 0;
        } else {
            String params = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
            if (vcmd.params != null) {
                for (Map.Entry<String, String> entry : vcmd.params.entrySet()) {
                    params = (params + entry.getKey() + ":" + entry.getValue()) + "&";
                }
            }
            try {
                IVcode service = IVcode.getService(true);
                if (service != null) {
                    service.trackEvent(vcmd.moduleID, vcmd.eventID, params);
                }
            } catch (Exception e) {
                pw.println("runVcodeTraceHal " + e.getMessage());
            }
            return 0;
        }
    }

    private int runVcodeDataHal(PrintWriter pw) {
        int length;
        ArrayList<Byte> dataList;
        VcodeCommand vcmd = parseVcodeCommandArfs(pw);
        if (vcmd.moduleID == null) {
            pw.println("moduleID can't be null");
            return 0;
        } else if (vcmd.eventID == null) {
            pw.println("eventID can't be null");
            return 0;
        } else if (vcmd.fullName == null) {
            pw.println("full name can't be null");
            return 0;
        } else {
            byte[] data = getBytefromFile(vcmd.fullName, pw);
            if (data == null) {
                pw.println("runVcodeDataHal data is null");
                return 0;
            }
            VLog.d(this.TAG, "runVcodeDataHal:  data.length " + data.length + " data: " + new String(data));
            pw.println("runVcodeDataHal: length " + data.length + " data: " + new String(data));
            String[] paths = vcmd.fullName.split("/");
            String fileName = paths[paths.length - 1];
            VLog.d(this.TAG, "runVcodeDataHal:  fileName " + fileName);
            if (data == null) {
                length = 0;
                dataList = null;
            } else {
                int length2 = data.length;
                ArrayList<Byte> dataList2 = new ArrayList<>(length2);
                for (byte b : data) {
                    dataList2.add(Byte.valueOf(b));
                }
                length = length2;
                dataList = dataList2;
            }
            try {
                IVcode service = IVcode.getService(true);
                if (service != null) {
                    service.trackEventData(vcmd.moduleID, vcmd.eventID, fileName, dataList, length);
                }
            } catch (Exception e) {
                pw.println("runVcodeDataHal " + e.getMessage());
            }
            return 0;
        }
    }

    private int runVcodeSingleEvent(PrintWriter pw) {
        VcodeCommand vcmd = parseVcodeCommandArfs(pw);
        try {
            EventTransfer.getInstance().singleEvent(vcmd.moduleID, vcmd.eventID, vcmd.startTime, vcmd.duration, vcmd.params);
            return 0;
        } catch (IllegalArgumentException e) {
            pw.println("runVcodeSingleEvent " + e.getMessage());
            return 0;
        }
    }

    private int runVcodeTraceEvent(PrintWriter pw) {
        VcodeCommand vcmd = parseVcodeCommandArfs(pw);
        try {
            EventTransfer.getInstance().traceEvent(vcmd.moduleID, vcmd.eventID, vcmd.pierceParams, vcmd.params);
            return 0;
        } catch (IllegalArgumentException e) {
            pw.println("runVcodeTraceEvent " + e.getMessage());
            return 0;
        }
    }

    private int runVcodeDataEvent(PrintWriter pw) {
        VcodeCommand vcmd = parseVcodeCommandArfs(pw);
        if (vcmd.fullName == null) {
            pw.println("full name can't be null");
            return 0;
        }
        byte[] data = getBytefromFile(vcmd.fullName, pw);
        if (data != null) {
            String str = this.TAG;
            VLog.d(str, "runVcodeDataEvent:  data.length" + data.length + " data: " + new String(data));
            pw.println("runVcodeDataEvent: length " + data.length + " data: " + new String(data));
        }
        String[] paths = vcmd.fullName.split("/");
        String fileName = paths[paths.length - 1];
        String str2 = this.TAG;
        VLog.d(str2, "runVcodeDataEvent:  fileName " + fileName);
        try {
            if (data != null) {
                EventTransfer.getInstance().dataEvent(vcmd.moduleID, vcmd.eventID, fileName, data, data.length);
            } else {
                EventTransfer.getInstance().dataEvent(vcmd.moduleID, vcmd.eventID, fileName, data, 0);
            }
        } catch (IllegalArgumentException e) {
            pw.println("runVcodeDataEvent" + e.getMessage());
        }
        return 0;
    }

    private VcodeCommand parseVcodeCommandArfs(PrintWriter pw) {
        VcodeCommand cmd = new VcodeCommand();
        while (true) {
            String opt = this.mShellCommand.getNextOption();
            if (opt != null) {
                char c = 65535;
                int hashCode = opt.hashCode();
                if (hashCode != 1496) {
                    if (hashCode != 1497) {
                        if (hashCode != 1504) {
                            if (hashCode != 1507) {
                                if (hashCode != 1387288) {
                                    if (hashCode != 1387303) {
                                        if (hashCode == 1387424 && opt.equals("--pp")) {
                                            c = 3;
                                        }
                                    } else if (opt.equals("--ls")) {
                                        c = 4;
                                    }
                                } else if (opt.equals("--ld")) {
                                    c = 5;
                                }
                            } else if (opt.equals("-p")) {
                                c = 2;
                            }
                        } else if (opt.equals("-m")) {
                            c = 0;
                        }
                    } else if (opt.equals("-f")) {
                        c = 6;
                    }
                } else if (opt.equals("-e")) {
                    c = 1;
                }
                switch (c) {
                    case 0:
                        cmd.moduleID = this.mShellCommand.getNextArgRequired();
                        break;
                    case 1:
                        cmd.eventID = this.mShellCommand.getNextArgRequired();
                        break;
                    case 2:
                        String key = this.mShellCommand.getNextArgRequired();
                        String value = this.mShellCommand.getNextArgRequired();
                        cmd.params.put(key, value);
                        break;
                    case 3:
                        String key2 = this.mShellCommand.getNextArgRequired();
                        String value2 = this.mShellCommand.getNextArgRequired();
                        cmd.pierceParams.put(key2, value2);
                        break;
                    case 4:
                        cmd.startTime = Long.parseLong(this.mShellCommand.getNextArgRequired());
                        break;
                    case 5:
                        cmd.duration = Long.parseLong(this.mShellCommand.getNextArgRequired());
                        break;
                    case 6:
                        cmd.fullName = this.mShellCommand.getNextArgRequired();
                        break;
                    default:
                        pw.println("Error: " + opt + " options is not support!");
                        break;
                }
            } else {
                cmd.dumpVcodeArgs(pw);
                return cmd;
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:52:0x0110  */
    /* JADX WARN: Removed duplicated region for block: B:54:0x0115 A[RETURN] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public byte[] getBytefromFile(java.lang.String r11, java.io.PrintWriter r12) {
        /*
            Method dump skipped, instructions count: 367
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.VcodeCommandImpl.getBytefromFile(java.lang.String, java.io.PrintWriter):byte[]");
    }

    public void dumpVcode(PrintWriter pw) {
        pw.println("Vcode dump options");
        pw.println("adb shell am vcode [-a <single/trace/data>]");
        pw.println("    [-a <single/trace/data>]: Event transfer type for Framework");
        pw.println("        [-a <single>]: singleEvent");
        pw.println("        [-a <trace>]: traceEvent");
        pw.println("        [-a <data>]: dataEvent");
        pw.println("    [-n <trace/data>]: Event transfer type for native");
        pw.println("        [-n <trace>]: traceEvent");
        pw.println("        [-n <data>]: dataEvent");
        pw.println("    [--hal <trace/data>]: Event transfer type for hal");
        pw.println("        [--hal <trace>]: traceEvent");
        pw.println("        [--hal <data>]: dataEvent");
        pw.println("    [-m <moduleId>]");
        pw.println("    [-e <eventId>]");
        pw.println("    [-p <key value>]: Params");
        pw.println("    [--pp <key value>]: Pierce Params ");
        pw.println("    [--ls <starttime>]");
        pw.println("    [--ld <duration>]");
        pw.println("    [-f <fullname>]");
        pw.println("    [-h for help]");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class VcodeCommand {
        public HashMap<String, String> params;
        public HashMap<String, String> pierceParams;
        public String moduleID = null;
        public String eventID = null;
        public long startTime = -1;
        public long duration = -1;
        public String fullName = null;

        public VcodeCommand() {
            this.params = null;
            this.pierceParams = null;
            this.params = new HashMap<>();
            this.pierceParams = new HashMap<>();
        }

        public void dumpVcodeArgs(PrintWriter pw) {
            pw.println(" vcode args dump ");
            pw.println(" ");
            pw.println(" moduleID: " + this.moduleID);
            pw.println(" eventID: " + this.eventID);
            HashMap<String, String> hashMap = this.params;
            if (hashMap != null) {
                for (Map.Entry<String, String> entry : hashMap.entrySet()) {
                    pw.println(" params key: " + entry.getKey() + " value: " + entry.getValue());
                }
            } else {
                pw.println(" params: is null");
            }
            HashMap<String, String> hashMap2 = this.pierceParams;
            if (hashMap2 != null) {
                for (Map.Entry<String, String> entry2 : hashMap2.entrySet()) {
                    pw.println(" pierce-params key: " + entry2.getKey() + " value: " + entry2.getValue());
                }
            } else {
                pw.println(" pierceParams: is null");
            }
            if (this.startTime != -1) {
                pw.println(" startTime: " + this.startTime);
            }
            if (this.duration != -1) {
                pw.println(" duration: " + this.duration);
            }
            if (this.fullName != null) {
                pw.println(" fullName: " + this.fullName);
            }
        }
    }
}