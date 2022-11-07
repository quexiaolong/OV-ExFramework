package com.android.server.pm;

import android.content.pm.ProviderInfo;
import android.content.pm.parsing.component.ParsedProvider;
import android.os.UserHandle;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.vivo.services.rms.sp.SpManagerImpl;

/* loaded from: classes.dex */
public class VivoComponentResolverImpl implements IVivoComponentResolver {
    static final String TAG = "VivoComponentResolverImpl";

    /* loaded from: classes.dex */
    private static class Instance {
        private static final VivoComponentResolverImpl INSTANCE = new VivoComponentResolverImpl();

        private Instance() {
        }
    }

    private VivoComponentResolverImpl() {
    }

    public static VivoComponentResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    public boolean shouldAddProviderForSps(ProviderInfo info, ParsedProvider p, AndroidPackage pkg, String processName, int uid) {
        boolean isInSps = false;
        String processNameVivo = null;
        if (info.metaData != null && info.metaData.getString("vivo_process") != null) {
            processNameVivo = info.metaData.getString("vivo_process");
            isInSps = SpManagerImpl.getInstance().isSuperSystemProcess(processName, uid) && "com.vivo.sps".equals(processNameVivo) && SpManagerImpl.getInstance().canStartOnSuperProcess(info.packageName, uid);
        }
        if (!SpManagerImpl.getInstance().isSuperSystemProcess(processName, uid) && "com.vivo.sps".equals(processNameVivo) && SpManagerImpl.getInstance().canStartOnSuperProcess(info.packageName, uid)) {
            return false;
        }
        return processName == null || (p.getProcessName().equals(processName) && UserHandle.isSameApp(pkg.getUid(), uid)) || isInSps;
    }
}