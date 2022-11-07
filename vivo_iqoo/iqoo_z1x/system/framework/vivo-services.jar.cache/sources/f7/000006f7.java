package com.vivo.services.rms.appmng;

import android.content.ContentValues;
import android.os.UserHandle;
import android.util.EventLog;
import com.vivo.services.rms.ProcessInfo;
import com.vivo.services.rms.util.JniTool;
import java.io.File;
import vivo.app.epm.ExceptionPolicyManager;

/* loaded from: classes.dex */
public class DeathReason {
    private static final String FILE_PATH = "/proc/vivo_rsc/check_lmk";
    private static boolean LMK_READY = isLMKReady();
    private static final String REASON_BY_LMK = "by lmk";

    public static void fillReason(ProcessInfo app) {
        if (LMK_READY && app.mKillReason == null && isByLmk(app.mPid)) {
            app.mKillReason = REASON_BY_LMK;
            EventLog.writeEvent(30023, Integer.valueOf(UserHandle.getUserId(app.mUid)), Integer.valueOf(app.mPid), app.mProcName, Integer.valueOf(app.mAdj), REASON_BY_LMK);
            ContentValues cv = new ContentValues();
            cv.put("adj", Integer.valueOf(app.mAdj));
            cv.put("type", "lmk");
            ExceptionPolicyManager.getInstance().reportEvent(33, System.currentTimeMillis(), cv);
        }
    }

    private static boolean isByLmk(int pid) {
        StringBuilder sb = new StringBuilder();
        sb.append(pid);
        sb.append(" 1");
        return JniTool.writeFile(FILE_PATH, sb.toString()) >= 0;
    }

    private static boolean isLMKReady() {
        File file = new File(FILE_PATH);
        return file.exists() && file.isFile();
    }
}