package com.android.server.uri;

import android.content.Context;
import android.net.Uri;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.vivo.common.doubleinstance.DoubleInstanceConfig;
import com.vivo.services.rms.ProcessList;
import java.util.ArrayList;
import java.util.Set;

/* loaded from: classes.dex */
public class VivoUriGrantsManagerServiceImpl implements IVivoUriGrantsManagerService {
    static final String TAG = "VivoUriGrantsManagerServiceImpl";
    private static final String URI_AUTHORITY = "vivoshare_uri_authority";
    private static Set<String> specialProviderForSystemUidPkg;
    private Context mContext;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
    private DoubleInstanceConfig mDoubleInstanceConfig = DoubleInstanceConfig.getInstance();

    public boolean weatherInDoubleInstanceWhiteList(String targetPkg) {
        ArrayList<String> doubleAppWhiteList;
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && (doubleAppWhiteList = DoubleInstanceConfig.getInstance().getSupportedAppPackageName()) != null && doubleAppWhiteList.contains(targetPkg)) {
            return true;
        }
        return false;
    }

    public boolean checkUriPermissionForDoubleInstance(int userId, String authority) {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && userId == 999) {
            if (authority.equals("com.whatsapp.provider.media") || authority.equals("com.facebook.katana.securefileprovider") || authority.equals("com.android.bbkmusic.fileprovider") || authority.equals("com.vivo.weather.share.fileprovider") || authority.equals("com.vivo.magazine.image.provider") || authority.equals("com.bbk.calendar.fileprovider") || authority.equals("jp.naver.line.android.line.common.FileProvider") || authority.equals("com.tencent.mm.external.fileprovider") || authority.equals("com.baidu.input_vivo") || authority.equals("com.vivo.ai.ime.fileprovider") || authority.equals("com.tencent.mobileqq.fileprovider")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isDoubleAppUserExist() {
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable()) {
            return this.mVivoDoubleInstanceService.isDoubleAppUserExist();
        }
        return false;
    }

    public UriPermission findOrCreateDoubleAppUriPermission(String sourcePkg, String targetPkg, int targetUid, GrantUri grantUri, ArrayMap<GrantUri, UriPermission> targetUris) {
        GrantUri otherUri;
        int i = 0;
        int sourceUserId = grantUri != null ? grantUri.sourceUserId : 0;
        int i2 = 128;
        if (sourceUserId == 999) {
            Uri uri = grantUri.uri;
            if (!grantUri.prefix) {
                i2 = 0;
            }
            otherUri = new GrantUri(0, uri, i2);
        } else if (sourceUserId == 0) {
            Uri uri2 = grantUri.uri;
            if (grantUri.prefix) {
                i = 128;
            }
            otherUri = new GrantUri((int) ProcessList.CACHED_APP_MAX_ADJ, uri2, i);
        } else {
            return null;
        }
        UriPermission otherPerm = targetUris.get(otherUri);
        if (otherPerm == null) {
            UriPermission otherPerm2 = new UriPermission(sourcePkg, targetPkg, targetUid, otherUri);
            targetUris.put(otherUri, otherPerm2);
            return otherPerm2;
        }
        return otherPerm;
    }

    static {
        ArraySet arraySet = new ArraySet();
        specialProviderForSystemUidPkg = arraySet;
        arraySet.add("com.android.settings.fileprovider");
        specialProviderForSystemUidPkg.add("com.vivo.smartshot.sharefileprovider");
        specialProviderForSystemUidPkg.add("com.vivo.assistant.upgrade");
        specialProviderForSystemUidPkg.add("com.iqoo.engineermode.provider");
        specialProviderForSystemUidPkg.add("com.vivo.assistant.share");
    }

    public boolean isSpecialProviderForSystemUidPkg(String authority) {
        if (authority == null || !specialProviderForSystemUidPkg.contains(authority)) {
            return false;
        }
        return true;
    }

    public boolean isSpecialProviderForVivoShare(String authority) {
        Context context;
        if (authority == null || (context = this.mContext) == null || !authority.equals(Settings.System.getString(context.getContentResolver(), URI_AUTHORITY))) {
            return false;
        }
        return true;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }
}