package com.android.server.wm;

import android.util.FtFeature;
import java.util.ArrayList;
import java.util.Map;

/* loaded from: classes.dex */
public class VivoFreeformMultiWindowConfig {
    private static final String ALLOW_SPLIT_APPS_PACKAGENAME_lIST = "AllowSplitAppsList";
    private static final String FORCE_FULL_SCREEN_ACTIVITIES_LIST = "ForceFullScreenActivitieslist";
    private static final String FORCE_FULL_SCREEN_ACTIVITIES_LIST_FREEFORM = "ForceFullScreenActivitieslistFreeform";
    public static final String FREEFORM_EMERGENT_ACTIVITY = "FreeFormEmergentActivity";
    public static final String FREEFORM_ENABLED_APP = "FreeFormEnabledApp";
    public static final String FREEFORM_FULLSCREEN_APP = "FreeFormFullScreenApp";
    private static final String IGNORE_RELAUNCH_ACTIVITY = "IgnoreRelaunchActivity";
    private static final String IGNORE_RELAUNCH_APP = "IgnoreRelaunchApp";
    private static final String IME_LIST = "IMEList";
    private static final String IME_LIST_FREEFORM = "IMEListFreeform";
    public static final boolean IS_VIVO_FREEFORM_SUPPORT = FtFeature.isFeatureSupport("vivo.software.freeform");
    private static final String NEED_FORCE_EXIT_MULTIWINDOW_TASK_LIST = "mNeedForceExitMultiWindowTaskList";
    private static final String NEED_IGNORE_ACTIVITY_LIST = "mNeedIgnoreActivitytList";
    private static final String NEED_RELAUNCH_APP = "NeedRelaunchApp";
    private static final String TAG = "VivoFreeformMultiWindowConfig";
    public static final int TYPE_ALLOW_SPLIT_APPS_PACKAGENAME_LIST = 1;
    public static final int TYPE_FORCE_FULL_SCREEN_ACTIVITIES_LIST = 2;
    public static final int TYPE_FORCE_FULL_SCREEN_ACTIVITIES_LIST_FREEFORM = 14;
    public static final int TYPE_FREEFORM_EMERGENT_ACTIVITY = 12;
    public static final int TYPE_FREEFORM_ENABLED_APP = 11;
    public static final int TYPE_FREEFORM_FULLSCREEN_APP = 10;
    public static final int TYPE_IGNORE_RELAUNCH_ACTIVITY = 9;
    public static final int TYPE_IGNORE_RELAUNCH_APP = 8;
    public static final int TYPE_IME_LIST = 7;
    public static final int TYPE_IME_LIST_FREEFORM = 15;
    public static final int TYPE_NEED_FORCE_EXIT_MULTIWINDOW_TASK_LIST = 3;
    public static final int TYPE_NEED_IGNORE_ACTIVITY_LISTT = 4;
    public static final int TYPE_NEED_RELAUNCH_APP = 5;
    public static final int TYPE_SCREENOBSERVER_CONSTANTS = 6;
    private static VivoFreeformMultiWindowConfig sVivoFreeformMultiWindowConfig;
    private ArrayList<String> mAllowSplitAppslist = new ArrayList<>();
    private ArrayList<String> mForceFullScreenActivitylist = new ArrayList<>();
    private ArrayList<String> mNeedForceExitMultiWindowTaskList = new ArrayList<>();
    private ArrayList<String> mNeedIgnoreActivitytList = new ArrayList<>();
    private ArrayList<String> mNeedRelaunchApp = new ArrayList<>();
    private ArrayList<String> mIgnoreRelaunchActivity = new ArrayList<>();
    private ArrayList<String> mImeList = new ArrayList<>();
    private ArrayList<String> mIgnoreRelaunchApp = new ArrayList<>();
    private ArrayList<String> mFreeFormEnabledApp = new ArrayList<>();
    private ArrayList<String> mFreeFormFullScreenApp = new ArrayList<>();
    private ArrayList<String> mFreeFormEmergentActivity = new ArrayList<>();
    private ArrayList<String> mForceFullScreenActivitylistFreeform = new ArrayList<>();
    private ArrayList<String> mImeListFreeform = new ArrayList<>();

    public static VivoFreeformMultiWindowConfig getInstance() {
        if (sVivoFreeformMultiWindowConfig == null) {
            sVivoFreeformMultiWindowConfig = new VivoFreeformMultiWindowConfig();
        }
        return sVivoFreeformMultiWindowConfig;
    }

    private VivoFreeformMultiWindowConfig() {
    }

    public void setMultiWindowConfig(Map<String, ArrayList<String>> map) {
        for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            char c = 65535;
            switch (key.hashCode()) {
                case -2106698235:
                    if (key.equals(NEED_RELAUNCH_APP)) {
                        c = 4;
                        break;
                    }
                    break;
                case -1766114234:
                    if (key.equals(NEED_FORCE_EXIT_MULTIWINDOW_TASK_LIST)) {
                        c = 2;
                        break;
                    }
                    break;
                case -1687235407:
                    if (key.equals(FORCE_FULL_SCREEN_ACTIVITIES_LIST)) {
                        c = 1;
                        break;
                    }
                    break;
                case -1661171745:
                    if (key.equals(IME_LIST)) {
                        c = 5;
                        break;
                    }
                    break;
                case -1352475929:
                    if (key.equals(IGNORE_RELAUNCH_ACTIVITY)) {
                        c = 7;
                        break;
                    }
                    break;
                case -1262048978:
                    if (key.equals(NEED_IGNORE_ACTIVITY_LIST)) {
                        c = 3;
                        break;
                    }
                    break;
                case -950818257:
                    if (key.equals(IME_LIST_FREEFORM)) {
                        c = '\f';
                        break;
                    }
                    break;
                case 88594608:
                    if (key.equals(FREEFORM_ENABLED_APP)) {
                        c = '\b';
                        break;
                    }
                    break;
                case 618464513:
                    if (key.equals(FORCE_FULL_SCREEN_ACTIVITIES_LIST_FREEFORM)) {
                        c = 11;
                        break;
                    }
                    break;
                case 1019728577:
                    if (key.equals(ALLOW_SPLIT_APPS_PACKAGENAME_lIST)) {
                        c = 0;
                        break;
                    }
                    break;
                case 1644941240:
                    if (key.equals(FREEFORM_EMERGENT_ACTIVITY)) {
                        c = '\n';
                        break;
                    }
                    break;
                case 1877174454:
                    if (key.equals(FREEFORM_FULLSCREEN_APP)) {
                        c = '\t';
                        break;
                    }
                    break;
                case 1996426057:
                    if (key.equals(IGNORE_RELAUNCH_APP)) {
                        c = 6;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    this.mAllowSplitAppslist.clear();
                    this.mAllowSplitAppslist.addAll(entry.getValue());
                    break;
                case 1:
                    this.mForceFullScreenActivitylist.clear();
                    this.mForceFullScreenActivitylist.addAll(entry.getValue());
                    break;
                case 2:
                    this.mNeedForceExitMultiWindowTaskList.clear();
                    this.mNeedForceExitMultiWindowTaskList.addAll(entry.getValue());
                    break;
                case 3:
                    this.mNeedIgnoreActivitytList.clear();
                    this.mNeedIgnoreActivitytList.addAll(entry.getValue());
                    break;
                case 4:
                    this.mNeedRelaunchApp.clear();
                    this.mNeedRelaunchApp.addAll(entry.getValue());
                    break;
                case 5:
                    this.mImeList.clear();
                    this.mImeList.addAll(entry.getValue());
                    break;
                case 6:
                    this.mIgnoreRelaunchApp.clear();
                    this.mIgnoreRelaunchApp.addAll(entry.getValue());
                    break;
                case 7:
                    this.mIgnoreRelaunchActivity.clear();
                    this.mIgnoreRelaunchActivity.addAll(entry.getValue());
                    break;
                case '\b':
                    this.mFreeFormEnabledApp.clear();
                    this.mFreeFormEnabledApp.addAll(entry.getValue());
                    break;
                case '\t':
                    this.mFreeFormFullScreenApp.clear();
                    this.mFreeFormFullScreenApp.addAll(entry.getValue());
                    break;
                case '\n':
                    this.mFreeFormEmergentActivity.clear();
                    this.mFreeFormEmergentActivity.addAll(entry.getValue());
                    break;
                case 11:
                    this.mForceFullScreenActivitylistFreeform.clear();
                    this.mForceFullScreenActivitylistFreeform.addAll(entry.getValue());
                    break;
                case '\f':
                    this.mImeListFreeform.clear();
                    this.mImeListFreeform.addAll(entry.getValue());
                    break;
            }
        }
    }

    public ArrayList<String> getMultiWindowConfig(int type) {
        switch (type) {
            case 1:
                return this.mAllowSplitAppslist;
            case 2:
                return this.mForceFullScreenActivitylist;
            case 3:
                return this.mNeedForceExitMultiWindowTaskList;
            case 4:
                return this.mNeedIgnoreActivitytList;
            case 5:
                return this.mNeedRelaunchApp;
            case 6:
            case 13:
            default:
                return null;
            case 7:
                return this.mImeList;
            case 8:
                return this.mIgnoreRelaunchApp;
            case 9:
                return this.mIgnoreRelaunchActivity;
            case 10:
                return this.mFreeFormFullScreenApp;
            case 11:
                return this.mFreeFormEnabledApp;
            case 12:
                return this.mFreeFormEmergentActivity;
            case 14:
                return this.mForceFullScreenActivitylistFreeform;
            case 15:
                return this.mImeListFreeform;
        }
    }
}