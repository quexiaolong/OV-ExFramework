package com.android.server.content;

import android.content.Context;
import com.android.internal.util.IndentingPrintWriter;
import com.vivo.face.common.data.Constants;
import content.IVivoContentService;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoContentServiceImpl implements IVivoContentService {
    static final String TAG = "VivoContentServiceImpl";
    private ContentService mContentService;

    public VivoContentServiceImpl(ContentService contentService, Context context) {
        if (contentService == null) {
            VSlog.i(TAG, "container is " + contentService);
        }
        this.mContentService = contentService;
    }

    public boolean dumpLogDebug(IndentingPrintWriter pw, String[] args) {
        String opt;
        int opti = 0;
        while (opti < args.length && (opt = args[opti]) != null && opt.length() > 0 && opt.charAt(0) == '-') {
            opti++;
        }
        String cmd = opti < args.length ? args[opti] : Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        int opti2 = opti + 1;
        if ("log".equals(cmd)) {
            String type = args[opti2];
            if ("enable".equals(type)) {
                ContentService.DEBUG = true;
                pw.print("ContentService.DEBUG = true");
                return true;
            } else if ("disable".equals(type)) {
                ContentService.DEBUG = false;
                pw.print("ContentService.DEBUG = false");
                return true;
            }
        }
        return false;
    }
}