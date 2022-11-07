package com.android.server.wm;

/* loaded from: classes2.dex */
public interface IVivoWindowPolicyController {
    public static final String ACTION_WINDOW_POLICY_FILE_CHANGED = "com.vivo.daemonService.unifiedconfig.update_finish_broadcast_WindowPolicy";
    public static final String ATTR_ITEM_ALIEN_POLICY = "alienScreenPolicy";
    public static final String ATTR_ITEM_APPLY_OPTION = "applyOption";
    public static final String ATTR_ITEM_HOMEINDICATOR_POLICY = "homeIndicatorPolicy";
    public static final String ATTR_ITEM_INTERNAL_FLAG = "internalFlag";
    public static final String ATTR_ITEM_NAME = "name";
    public static final String ATTR_ITEM_NAV_FIX_COLOR = "navFixColor";
    public static final String ATTR_ITEM_NAV_POLICY = "navColorPolicy";
    public static final String ATTR_ITEM_PADDING_POLICY = "paddingColorPolicy";
    public static final String BACKUP_WINDOW_POLICY_FILE_PATH = "/data/bbkcore/vivo_window_policy.xml";
    public static final String DEFAULT_WINDOW_POLICY_FILE_PATH = "/system/etc/vivo_window_policy.xml";
    public static final String TAG_DEFAULT_ALIENSCREEN_POLICY = "defaultAlienScreenPolicy";
    public static final String TAG_DEFAULT_IMMERSE_NAV = "defaultImmerseNavColor";
    public static final String TAG_ITEM = "item";
    public static final String TAG_ITEM_LIST = "itemList";
    public static final String TAG_WINDOW_POLICY = "window-policy";
    public static final String URI = "content://com.vivo.abe.unifiedconfig.provider/configs";

    String getDefaultAlienScreenPolicy();

    String getFixNavColor(String str);

    String getInternalFlag(String str);

    String getPolicyAlienScreen(String str);

    String getPolicyHomeIndicator(String str);

    String getPolicyNavColor(String str);

    String getPolicyPaddingColor(String str);

    void init();

    void postRetriveFile();
}