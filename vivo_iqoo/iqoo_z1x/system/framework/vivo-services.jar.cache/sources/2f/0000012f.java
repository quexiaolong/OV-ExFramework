package com.android.server.audio;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.ActivityTaskManager;
import android.app.AppOpsManager;
import android.app.IActivityManager;
import android.app.IActivityTaskManager;
import android.app.INotificationManager;
import android.app.IProcessObserver;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.VivoPolicyManagerInternal;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.media.AudioAttributes;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecordingConfiguration;
import android.media.AudioSystem;
import android.media.audiopolicy.AudioProductStrategy;
import android.multidisplay.MultiDisplayManager;
import android.os.Binder;
import android.os.FtBuild;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.FtFeature;
import android.util.Log;
import com.android.internal.telecom.ITelecomService;
import com.android.internal.telephony.VivoTelephonyApiParams;
import com.android.server.LocalServices;
import com.android.server.VivoDoubleInstanceServiceImpl;
import com.android.server.am.firewall.VivoFirewall;
import com.android.server.audio.AudioService;
import com.android.server.audio.IVivoAudioService;
import com.android.server.policy.VivoRatioControllerUtilsImpl;
import com.android.server.wm.VivoAppShareManager;
import com.vivo.appshare.AppShareConfig;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.media.FeatureService;
import com.vivo.services.security.server.VivoPermissionUtils;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class VivoAudioServiceImpl implements IVivoAudioService {
    private static final int AMV_SET_MUTE = 1;
    private static final int AMV_SET_RESET = 2;
    private static final int AMV_SET_UNMUTE = 0;
    private static final String MODE_RINGER = "mode_ringer_ext";
    private static final int MSG_CHECK_MUSIC_ACTIVE = 11;
    public static final int MSG_CHECK_RECORDING_EVENT = 209;
    private static final int MSG_PERSIST_DELTA_VOLUME = 206;
    public static final int MSG_PERSIST_VIBRATE_SETTING = 201;
    private static final int MSG_POM_NEW = 208;
    private static final int MSG_SET_ALL_DELTAVOLUMES = 207;
    private static final int MSG_SET_DEVICE_DELTA_VOLUME = 205;
    private static final int MSG_SET_DEVICE_VOLUME = 0;
    private static final int PERSIST_DELAY = 500;
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final int[] STREAM_VOLUME_OPS = {34, 36, 35, 36, 37, 38, 39, 36, 36, 36, 64, 36};
    static final String TAG = "VivoAudioServiceImpl";
    public static final String VIBRATE_IN_SILENT_ENABLED = "vivo_vibrate_in_silent_enable";
    protected static final String VIVO_VIBRATE_ON = "vivo_vibrate_on";
    protected int[] MAX_STREAM_VOLUME;
    protected int[] MIN_STREAM_VOLUME;
    protected AudioService mAS;
    private IActivityTaskManager mActivityTaskManager;
    private AppOpsManager mAppOps;
    protected AudioService.AudioHandler mAudioHandler;
    protected ContentResolver mContentResolver;
    protected Context mContext;
    private DevicePolicyManager mDevicePolicManager;
    private FeatureService mFeatureService;
    private boolean mIsControlledByRemote;
    private long mSetStreamVolumeTime;
    private boolean mUseFixedVolume;
    private VivoAppShareManager mVivoAppShareManager;
    private VivoDoubleInstanceServiceImpl mVivoDoubleInstanceService;
    private VivoPolicyManagerInternal mVivoPolicyManagerInternal;
    private VivoRatioControllerUtilsImpl mVivoRatioControllerUtils;
    private VivoVolumeStreamState[] mVivoStreamStates;
    private List<String> mPlatformList = new ArrayList<String>() { // from class: com.android.server.audio.VivoAudioServiceImpl.1
        {
            add("MTK6762");
            add("MTK6765");
            add("MTK6771");
        }
    };
    private NotificationManager mNotificationManager = null;
    private INotificationManager mNotificationService = null;
    private final ArrayMap<Integer, Integer> mRingerModeExternalMap = new ArrayMap<>();
    private final ArrayMap<Integer, Integer> mVibrateSettingMap = new ArrayMap<>();
    private HashMap<String, Integer> mModeClientMap = new HashMap<>();
    private final ArrayList<SetSpeakerphoneOnDeathHandler> mSetSpeakerphoneOnDeathHandler = new ArrayList<>();
    private final HashMap<Integer, Integer> mMutePiidMap = new HashMap<>();
    private final ArrayList<PomSetModeDeathHandler> mPomSetModeRecoryHandlers = new ArrayList<>();
    private final int VOLUME_DELTA_MAX_ZOOM_NUM = 10;
    int mFixedVolumeDevices = 2890752;
    private String mAppSharePackageName = null;
    private int mAppShareUserId = -1;
    private boolean mMuteMusicForVoip = false;
    private int mMuteMusicDevForVoip = -1;
    private int mMuteMusicVolBackup = -1;
    private IProcessObserver mProcessObserver = new IProcessObserver.Stub() { // from class: com.android.server.audio.VivoAudioServiceImpl.2
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foreground) {
            if (VivoAudioServiceImpl.this.mAS != null && foreground) {
                List<AudioPlaybackConfiguration> currAPC = VivoAudioServiceImpl.this.mAS.getActivePlaybackConfigurations();
                int i = 0;
                while (true) {
                    if (i >= currAPC.size()) {
                        break;
                    }
                    AudioPlaybackConfiguration tempAPC = currAPC.get(i);
                    if (tempAPC.getClientPid() != pid || tempAPC.getPlayerState() != 2) {
                        i++;
                    } else {
                        int piid = tempAPC.getPlayerInterfaceId();
                        synchronized (VivoAudioServiceImpl.this.mMutePiidMap) {
                            if (VivoAudioServiceImpl.this.mMutePiidMap.get(new Integer(piid)) != null) {
                                VivoAudioServiceImpl.this.mAS.unmuteBgPlayer(piid);
                                VivoAudioServiceImpl.this.mMutePiidMap.remove(new Integer(piid));
                            }
                        }
                    }
                }
            }
            if (VivoAudioServiceImpl.this.mAS != null) {
                VivoAudioServiceImpl.this.mAS.triggerMsgForVoip();
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onProcessDied(int pid, int uid) {
        }
    };

    public VivoAudioServiceImpl(AudioService as, Context context) {
        this.mAS = null;
        this.mContentResolver = null;
        this.mAudioHandler = null;
        this.mFeatureService = null;
        this.mVivoDoubleInstanceService = null;
        if (as == null) {
            VSlog.e(TAG, "AudioService is null ,return here!!");
            return;
        }
        VSlog.v(TAG, "VivoAudioServiceImpl construct!  as:  " + as);
        this.mAS = as;
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mAudioHandler = this.mAS.getAudioHandler();
        this.MAX_STREAM_VOLUME = this.mAS.getMaxStreamVolume();
        this.MIN_STREAM_VOLUME = this.mAS.getMinStreamVolume();
        this.mAppOps = (AppOpsManager) context.getSystemService("appops");
        this.mUseFixedVolume = this.mContext.getResources().getBoolean(17891575);
        this.mFeatureService = this.mAS.getFeatureService();
        this.mVivoDoubleInstanceService = VivoDoubleInstanceServiceImpl.getInstance();
        this.mVivoPolicyManagerInternal = (VivoPolicyManagerInternal) LocalServices.getService(VivoPolicyManagerInternal.class);
        this.mDevicePolicManager = (DevicePolicyManager) this.mContext.getSystemService("device_policy");
        if (FtBuild.isOverSeas()) {
            SystemProperties.set("persist.vivo.karaoke.enable", "false");
        } else {
            SystemProperties.set("persist.vivo.karaoke.enable", "true");
        }
        VSlog.v(TAG, "VivoAudioServiceImpl construct!  out!");
        registerProcessObserver();
        DevicePolicyManager devicePolicyManager = this.mDevicePolicManager;
        if (devicePolicyManager != null) {
            int type = devicePolicyManager.getCustomType();
            if (type > 0) {
                readRestrictionPolicy();
                registerVivoPolicyListener();
            }
        }
        initAppShareController();
        this.mVivoRatioControllerUtils = VivoRatioControllerUtilsImpl.getInstance();
        this.mVivoAppShareManager = VivoAppShareManager.getInstance();
    }

    public void dummy() {
    }

    public int readPersistedVibrateSettingVivoAsUser(int orPerVibSet, boolean mHasVibrator, int userId) {
        int i;
        int i2 = 1;
        if (mHasVibrator) {
            i = 1;
        } else {
            i = 0;
        }
        int orPerVibSet2 = AudioSystem.getValueForVibrateSetting(0, 1, i);
        if (!mHasVibrator) {
            i2 = 0;
        }
        AudioSystem.getValueForVibrateSetting(orPerVibSet2, 0, i2);
        String str = SystemProperties.get("ro.vivo.op.entry", "no");
        int orPerVibSet3 = (str.contains("CMCC") || str.contains("UNICOM")) ? 0 : 0;
        return Settings.System.getIntForUser(this.mContentResolver, VIVO_VIBRATE_ON, orPerVibSet3, userId);
    }

    public boolean readCameraSoundForcedFromVivoInt() {
        String prop = SystemProperties.get("ro.product.model.bbk", (String) null);
        if (prop != null) {
            String str = SystemProperties.get("ro.vivo.op.entry", "no");
            if (str.contains("CMCC_RW") || str.equals("CMCC")) {
                return true;
            }
            return false;
        }
        return false;
    }

    public int getAbsVolumeIndexByVivo(int maxIdex, int index) {
        int index2;
        if (index == 0) {
            index2 = 0;
        } else {
            index2 = (maxIdex + 5) / 10;
        }
        if (index2 > 15) {
            Log.d(TAG, "bluetooth set index out out of range index = " + index2);
            return 15;
        }
        return index2;
    }

    public boolean setVibrateSettingAsUser(int vibrateType, int vibrateSetting, boolean persist, int userId) {
        Log.d(TAG, "setVibrateSettingAsUser by " + userId + " value:" + vibrateSetting);
        if (persist && this.mAS.getOriVibSetting() != vibrateSetting) {
            this.mAS.setOriVibSetting(vibrateSetting);
            this.mAS.broadcastVibrateSetting(vibrateType);
            AudioService.sendMsg(this.mAudioHandler, 201, 2, userId, vibrateSetting, (Object) null, 0);
        }
        this.mVibrateSettingMap.put(Integer.valueOf(userId), Integer.valueOf(vibrateSetting));
        return true;
    }

    public int getVibrateSettingAsUser(int vibrateType, int userId) {
        Log.d(TAG, "getVibrateSettingAsUser by " + userId + " value:" + this.mVibrateSettingMap.get(Integer.valueOf(userId)));
        if (this.mVibrateSettingMap.get(Integer.valueOf(userId)) != null) {
            return (this.mVibrateSettingMap.get(Integer.valueOf(userId)).intValue() >> (vibrateType * 2)) & 3;
        }
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && this.mVivoDoubleInstanceService.getDoubleAppUserId() != -10000) {
            return (this.mVibrateSettingMap.get(Integer.valueOf(getCurrentUserId())).intValue() >> (vibrateType * 2)) & 3;
        }
        return -1;
    }

    public int getRingerModeExternalAsUser(int userId) {
        Log.d(TAG, "getRingerModeExternalAsUser by " + userId + " value:" + this.mRingerModeExternalMap.get(Integer.valueOf(userId)));
        if (this.mRingerModeExternalMap.get(Integer.valueOf(userId)) != null) {
            return this.mRingerModeExternalMap.get(Integer.valueOf(userId)).intValue();
        }
        VivoDoubleInstanceServiceImpl vivoDoubleInstanceServiceImpl = this.mVivoDoubleInstanceService;
        if (vivoDoubleInstanceServiceImpl != null && vivoDoubleInstanceServiceImpl.isDoubleInstanceEnable() && this.mVivoDoubleInstanceService.getDoubleAppUserId() != -10000 && this.mRingerModeExternalMap.get(Integer.valueOf(getCurrentUserId())) != null) {
            return this.mRingerModeExternalMap.get(Integer.valueOf(getCurrentUserId())).intValue();
        }
        return -1;
    }

    public void setRingerModeExtAsUser(int ringerMode, int userId) {
        Log.d(TAG, "setRingerModeExtAsUser ringermode:" + ringerMode + " userId:" + userId);
        this.mRingerModeExternalMap.put(Integer.valueOf(userId), Integer.valueOf(ringerMode));
    }

    public int getIntForUser(String keyName, int defaultValue, int userId) {
        return Settings.System.getIntForUser(this.mContentResolver, keyName, defaultValue, userId);
    }

    public void putIntForUser(String keyName, int value, int userId) {
        Settings.System.putIntForUser(this.mContentResolver, keyName, value, userId);
    }

    public boolean createVivoStreamStates(int numStreamTypes, int[] streamVolumeAlias) {
        if (this.mAS == null) {
            return false;
        }
        VivoVolumeStreamState[] streams = new VivoVolumeStreamState[numStreamTypes];
        this.mVivoStreamStates = streams;
        for (int i = 0; i < numStreamTypes; i++) {
            streams[i] = new VivoVolumeStreamState(Settings.System.VOLUME_SETTINGS_INT[streamVolumeAlias[i]], i);
        }
        return true;
    }

    public int getStreamVolumeMaxDelta(int streamType) {
        AudioService audioService = this.mAS;
        if (audioService == null) {
            return -1;
        }
        return audioService.getStreamMaxVolume(streamType) * 10;
    }

    public void checkDeltaVolumeForSetStream(int streamType, int index, int flags, String callingPackage) {
        boolean change;
        AudioService audioService = this.mAS;
        if (audioService == null) {
            return;
        }
        int[] alias = audioService.getStreamVolumeAlias();
        ensureValidStreamType(streamType);
        int streamTypeAlias = alias[streamType];
        if (streamTypeAlias == 3) {
            AudioService.VolumeStreamState[] mTotalStreamState = this.mAS.getVSSForStream();
            AudioService.VolumeStreamState asStreamState = mTotalStreamState[streamType];
            VivoVolumeStreamState streamState = this.mVivoStreamStates[streamTypeAlias];
            int device = this.mAS.getDeviceForStream(streamType);
            int oldIndex = asStreamState.getIndex(device);
            int tempIndex = this.mAS.rescaleIndex(index * 10, streamType, streamTypeAlias);
            int minIndex = (this.MIN_STREAM_VOLUME[3] + 1) * 10;
            int maxIndex = (this.MAX_STREAM_VOLUME[3] - 1) * 10;
            VSlog.v(TAG, "Set-Clear The DetalIndex volume from thirdparty app tempIndex:" + tempIndex + " minIndex:" + minIndex + " maxIndex:" + maxIndex);
            if ((tempIndex <= 0 && getStreamVolumeDelta(streamType) < minIndex) || (tempIndex >= maxIndex && getStreamVolumeDelta(streamType) > maxIndex)) {
                if (oldIndex == asStreamState.getValidIndex(tempIndex, true) && streamState.getDeltaIndex(device) != 0) {
                    change = true;
                } else {
                    change = false;
                }
                setStreamVolumeDeltaInt(streamType, 0, flags, callingPackage, Binder.getCallingUid(), change);
            }
        }
    }

    public void checkDeltaVolumeForAdjustStream(int streamType, int direction, int step, int oldIndex, int flags, int uid, int device, String callingPackage) {
        boolean change;
        AudioService audioService = this.mAS;
        if (audioService == null) {
            return;
        }
        int[] alias = audioService.getStreamVolumeAlias();
        int streamTypeAlias = alias[streamType];
        if (streamTypeAlias == 3) {
            AudioService.VolumeStreamState[] mTotalStreamState = this.mAS.getVSSForStream();
            AudioService.VolumeStreamState asStreamState = mTotalStreamState[streamType];
            VivoVolumeStreamState streamState = this.mVivoStreamStates[streamTypeAlias];
            int minIndex = (this.MIN_STREAM_VOLUME[3] + 1) * 10;
            int maxIndex = (this.MAX_STREAM_VOLUME[3] - 1) * 10;
            int indexTemp = (direction * step) + oldIndex;
            VSlog.v(TAG, "Adjust clear the DetalIndex volume from thirdparty app index:" + indexTemp + " minIndex:" + minIndex + " maxIndex:" + maxIndex);
            if ((indexTemp <= 0 && getStreamVolumeDelta(streamType) < minIndex) || (indexTemp >= maxIndex && getStreamVolumeDelta(streamType) > maxIndex)) {
                if (oldIndex == asStreamState.getValidIndex(indexTemp, true) && streamState.getDeltaIndex(device) != 0) {
                    change = true;
                    setStreamVolumeDeltaInt(streamType, 0, flags, callingPackage, uid, change);
                }
                change = false;
                setStreamVolumeDeltaInt(streamType, 0, flags, callingPackage, uid, change);
            }
        }
    }

    public void setStreamVolumeDelta(int streamType, int index, int flags, String callingPackage) {
        int uid;
        AudioService audioService = this.mAS;
        if (audioService == null) {
            return;
        }
        AudioService.VolumeStreamState[] mTotalStreamState = audioService.getVSSForStream();
        int[] alias = this.mAS.getStreamVolumeAlias();
        if (this.mUseFixedVolume) {
            VSlog.w(TAG, "setStreamVolumeDeltaZoom fixed volume");
        } else if (streamType < 0 || streamType >= mTotalStreamState.length) {
            VSlog.e(TAG, "setStreamVolumeFeature streamType: " + streamType + " error return");
        } else if (index < 0 || index > this.mAS.getStreamMaxVolume(streamType) * 10) {
            VSlog.e(TAG, "setStreamVolumeFeature index : " + index + "error return ");
        } else {
            VSlog.v(TAG, "setStreamVolumeDelta stream=" + streamType + ", index=" + index + ", flags: " + flags + ", packageName:" + callingPackage);
            ensureValidStreamType(streamType);
            int streamTypeAlias = alias[streamType];
            if (streamTypeAlias != 3) {
                VSlog.e(TAG, "setStreamVolumeDelta is just for MUSIC STREAM , streamType: " + streamType + " error return");
                return;
            }
            AudioService.VolumeStreamState streamState = mTotalStreamState[streamTypeAlias];
            int device = this.mAS.getDeviceForStream(streamType);
            int oldIndex = streamState.getIndex(device);
            int uid2 = Binder.getCallingUid();
            if ((device & 896) == 0 && (flags & 64) != 0) {
                return;
            }
            if (uid2 != 1000) {
                uid = uid2;
            } else {
                uid = UserHandle.getUid(getCurrentUserId(), UserHandle.getAppId(uid2));
            }
            if (this.mAppOps.noteOp(STREAM_VOLUME_OPS[streamTypeAlias], uid, callingPackage) != 0) {
                VSlog.e(TAG, "setStreamVolumeDeltaZoom, callingPackage: " + callingPackage + " not allowed, return ");
                return;
            }
            int volIndex = index / 10;
            int DeltaIndex = index % 10;
            if (oldIndex / 10 != volIndex) {
                setStreamVolumeDeltaIndex(streamType, DeltaIndex, device, false);
            } else {
                setStreamVolumeDeltaIndex(streamType, DeltaIndex, device, true);
            }
            this.mAS.setStreamVolume(streamType, volIndex, flags, callingPackage, callingPackage, Binder.getCallingUid(), true);
        }
    }

    public int getStreamVolumeDelta(int streamType) {
        int i;
        AudioService audioService = this.mAS;
        if (audioService == null) {
            return -1;
        }
        int[] alias = audioService.getStreamVolumeAlias();
        AudioService.VolumeStreamState[] mTotalStreamState = this.mAS.getVSSForStream();
        ensureValidStreamType(streamType);
        int streamTypeAlias = alias[streamType];
        if (streamTypeAlias != 3) {
            VSlog.e(TAG, "getStreamVolumeDelta is just for MUSIC STREAM , streamType: " + streamType + " error return");
            return 0;
        }
        int device = this.mAS.getDeviceForStream(streamType);
        synchronized (AudioService.VolumeStreamState.class) {
            int deltaIndex = this.mVivoStreamStates[streamType].getDeltaIndex(device);
            int index = mTotalStreamState[streamType].getIndex(device);
            if (this.mAS.isStreamMute(streamType)) {
                index = 0;
                deltaIndex = 0;
            }
            if (index != 0 && alias[streamType] == 3 && (this.mFixedVolumeDevices & device) != 0) {
                index = mTotalStreamState[streamType].getMaxIndex();
            }
            i = (((index + 5) / 10) * 10) + deltaIndex;
        }
        return i;
    }

    public void setStreamVolumeDeltaIndex(int streamType, int index, int device, boolean force) {
        AudioService audioService = this.mAS;
        if (audioService == null) {
            return;
        }
        VivoVolumeStreamState vivoStreamState = this.mVivoStreamStates[streamType];
        AudioService.VolumeStreamState[] mTotalStreamState = audioService.getVSSForStream();
        AudioService.VolumeStreamState asStreamState = mTotalStreamState[streamType];
        VSlog.v(TAG, "setStreamVolumeDeltaIndex: streamType " + streamType + ", index :" + index + ", device :" + device);
        int[] alias = this.mAS.getStreamVolumeAlias();
        int oldDeltaIndex = vivoStreamState.getDeltaIndex(device);
        int volumeIndex = asStreamState.getIndex(device);
        if (vivoStreamState.setDeltaIndex(index, device, alias)) {
            AudioService.sendMsg(this.mAudioHandler, 205, 2, device, streamType, alias, 0);
            if (force) {
                AudioService.sendMsg(this.mAudioHandler, 0, 2, device, 0, asStreamState, 0);
                if (index == oldDeltaIndex || volumeIndex >= 10) {
                    return;
                }
                if (index != 0 && oldDeltaIndex != 0) {
                    return;
                }
                vivoStreamState.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", streamType);
                vivoStreamState.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", volumeIndex);
                vivoStreamState.mVolumeChanged.putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", volumeIndex);
                vivoStreamState.mVolumeChanged.putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE_ALIAS", alias[streamType]);
                vivoStreamState.mVolumeChanged.putExtra("deltaVolume", index);
                sendBroadcastToAll(vivoStreamState.mVolumeChanged);
                VSlog.v(TAG, "volume changed Broadcast index:" + index);
            }
        }
    }

    public void applyStreamVolumeDeltaIndexHook(int streamType, boolean isMute, int device) {
        int deltaIndex;
        AudioService audioService = this.mAS;
        if (audioService == null) {
            return;
        }
        int[] alias = audioService.getStreamVolumeAlias();
        VivoVolumeStreamState vivoStreamState = this.mVivoStreamStates[streamType];
        if (alias[streamType] == 3) {
            if (isMute) {
                deltaIndex = 0;
            } else {
                deltaIndex = vivoStreamState.getDeltaIndex(device);
            }
            setStreamVolumeDeltaIndexHook(streamType, deltaIndex, device);
        }
    }

    public int getDeltaVolumeForStream(int streamType, int device) {
        if (this.mAS == null) {
            return -1;
        }
        VivoVolumeStreamState vivoStreamState = this.mVivoStreamStates[streamType];
        VSlog.v(TAG, "getDeltaVolumeForStream: streamType " + streamType + ", device: " + device + ", vivoStreamState.getDeltaIndex(device) : " + vivoStreamState.getDeltaIndex(device));
        return vivoStreamState.getDeltaIndex(device);
    }

    public void dumpVivoStreamState(PrintWriter pw, int streamType) {
        String deviceName;
        if (this.mAS == null) {
            return;
        }
        pw.print("\n");
        pw.print("   Delta: ");
        Set Deltaset = this.mVivoStreamStates[streamType].mDeltaIndex.entrySet();
        Iterator j = Deltaset.iterator();
        while (j.hasNext()) {
            Map.Entry<Integer, Integer> entry = j.next();
            int device1 = entry.getKey().intValue();
            pw.print(Integer.toHexString(device1));
            if (device1 == 1073741824) {
                deviceName = "default";
            } else {
                deviceName = AudioSystem.getOutputDeviceName(device1) + "_delta";
            }
            if (!deviceName.isEmpty()) {
                pw.print(" (");
                pw.print(deviceName);
                pw.print(")");
            }
            pw.print(": ");
            int index = entry.getValue().intValue();
            pw.print(index);
            if (j.hasNext()) {
                pw.print(", ");
            }
        }
    }

    private int getCurrentUserId() {
        long ident = Binder.clearCallingIdentity();
        int i = 0;
        try {
            IActivityManager am = ActivityManager.getService();
            UserInfo currentUser = am != null ? am.getCurrentUser() : null;
            if (currentUser != null) {
                i = currentUser.id;
            }
            return i;
        } catch (RemoteException e) {
            return 0;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void setStreamVolumeDeltaInt(int streamType, int index, int flags, String callingPackage, int uid, boolean change) {
        int[] alias = this.mAS.getStreamVolumeAlias();
        if (this.mUseFixedVolume) {
            return;
        }
        VSlog.v(TAG, "setStreamVolumeDeltaInt stream=" + streamType + ", index=" + index + ", flags: " + flags + ", packageName:" + callingPackage + " uid:" + uid);
        ensureValidStreamType(streamType);
        int streamTypeAlias = alias[streamType];
        int device = this.mAS.getDeviceForStream(streamType);
        if ((device & 896) == 0 && (flags & 64) != 0) {
            return;
        }
        if (uid == 1000) {
            uid = UserHandle.getUid(getCurrentUserId(), UserHandle.getAppId(uid));
        }
        if (this.mAppOps.noteOp(STREAM_VOLUME_OPS[streamTypeAlias], uid, callingPackage) != 0) {
            VSlog.e(TAG, "setStreamVolume, callingPackage: " + callingPackage + " not allowed, return ");
            return;
        }
        VSlog.v(TAG, "setStreamVolumeDeltaInt stream=" + streamType + ", index:" + index + " change:" + change);
        setStreamVolumeDeltaIndex(streamType, index, device, change);
    }

    private void sendBroadcastToAll(Intent intent) {
        intent.addFlags(67108864);
        intent.addFlags(268435456);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void applyAllDeltaVolume(int[] alias) {
        for (int i = 0; i < AudioSystem.getNumStreamTypes(); i++) {
            int streamType = alias[i];
            Set Deltaset = this.mVivoStreamStates[streamType].mDeltaIndex.entrySet();
            for (Map.Entry<Integer, Integer> entry : Deltaset) {
                int device1 = entry.getKey().intValue();
                int index = entry.getValue().intValue();
                setStreamVolumeDeltaIndexHook(streamType, index, device1);
            }
            int deltaVolume = this.mVivoStreamStates[streamType].getDeltaIndex(1073741824);
            setStreamVolumeDeltaIndexHook(streamType, deltaVolume, 1073741824);
        }
    }

    private void setDeviceDeltaVolume(int[] alias, int device, int setStreamType) {
        synchronized (VivoVolumeStreamState.class) {
            this.mVivoStreamStates[setStreamType].applyDeviceDeltaVolume_syncVSS(device);
            int numStreamTypes = AudioSystem.getNumStreamTypes();
            for (int streamType = numStreamTypes - 1; streamType >= 0; streamType--) {
                if (streamType != setStreamType && alias[streamType] == setStreamType) {
                    int streamDevice = this.mAS.getDeviceForStream(streamType);
                    if (device != streamDevice && (device & 896) != 0) {
                        this.mVivoStreamStates[streamType].applyDeviceDeltaVolume_syncVSS(device);
                    }
                    this.mVivoStreamStates[streamType].applyDeviceDeltaVolume_syncVSS(streamDevice);
                }
            }
        }
        AudioService.sendMsg(this.mAudioHandler, 206, 2, device, setStreamType, alias, 500);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int setStreamVolumeDeltaIndexHook(int streamType, int index, int device) {
        if (this.mFeatureService == null) {
            FeatureService featureService = this.mAS.getFeatureService();
            this.mFeatureService = featureService;
            if (featureService == null) {
                VSlog.w(TAG, "mFeatureService == null return ");
                return -1;
            }
        }
        this.mFeatureService.setStreamVolumeDeltaIndexHook(streamType, index, device);
        return 0;
    }

    public boolean getVibeInSilentFromVivo() {
        return Settings.System.getIntForUser(this.mContentResolver, VIBRATE_IN_SILENT_ENABLED, 1, getCurrentUserId()) == 1;
    }

    public boolean handleMessageExt(Message msg) {
        int i = msg.what;
        if (i == 201) {
            persistVibrateSetting(msg.arg1, msg.arg2);
            return true;
        }
        switch (i) {
            case 205:
                setDeviceDeltaVolume((int[]) msg.obj, msg.arg1, msg.arg2);
                return true;
            case 206:
                persistDeltaVolume(msg.arg1, msg.arg2);
                return true;
            case 207:
                applyAllDeltaVolume((int[]) msg.obj);
                return true;
            case MSG_POM_NEW /* 208 */:
                try {
                    this.mFeatureService.onAudioModeChange(msg.arg1, msg.arg2, (IBinder) msg.obj);
                    Log.w(TAG, "pom_new  2 setModeint() onAudioModeChange mode:" + msg.arg1 + "newModeOwnerPid:" + msg.arg2);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return true;
                }
            case MSG_CHECK_RECORDING_EVENT /* 209 */:
                checkRecordEventForVoip();
                return true;
            default:
                return false;
        }
    }

    protected void persistVibrateSetting(int userId, int vibrateSetting) {
        Settings.System.putIntForUser(this.mContentResolver, VIVO_VIBRATE_ON, vibrateSetting, userId);
    }

    protected void persistDeltaVolume(int device, int streamType) {
        if (this.mUseFixedVolume) {
            return;
        }
        Settings.System.putIntForUser(this.mContentResolver, this.mVivoStreamStates[streamType].getSettingNameForVolumeDelta(device), this.mVivoStreamStates[streamType].getDeltaIndex(device), -2);
    }

    private void ensureValidStreamType(int streamType) {
        AudioService.VolumeStreamState[] mTotalStreamState = this.mAS.getVSSForStream();
        if (streamType < 0 || streamType >= mTotalStreamState.length) {
            throw new IllegalArgumentException("Bad stream type " + streamType);
        }
    }

    public void landFillLoudVolume() {
        AudioSystem.sendParametersFromASysToAPM("loud_volume=1");
    }

    private String getAppName(int pID) {
        ActivityManager am = (ActivityManager) this.mContext.getSystemService(VivoFirewall.TYPE_ACTIVITY);
        List l = am.getRunningAppProcesses();
        this.mContext.getPackageManager();
        for (ActivityManager.RunningAppProcessInfo info : l) {
            try {
            } catch (Exception e) {
                VLog.w(TAG, "Error>> :" + e.toString());
            }
            if (info.pid == pID) {
                String processName = info.processName;
                return processName;
            }
            continue;
        }
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
    }

    public void landFillNewReceiver(int mode, String caller) {
        if (mode == 0 && caller == "AudioPomService") {
            AudioSystem.sendParametersFromASysToAPM("new_receiver=1");
        }
        if (mode == 2) {
            AudioSystem.sendParametersFromASysToAPM("mode_owner=" + caller);
            return;
        }
        int modeOwnerPid = this.mAS.getModeOwnerPidPublic();
        if (modeOwnerPid != 0) {
            String modeOwner = getAppName(modeOwnerPid);
            AudioSystem.sendParametersFromASysToAPM("mode_owner=" + modeOwner);
        }
    }

    public void getBluetoothType(BluetoothDevice device) {
        BluetoothClass btClass = device.getBluetoothClass();
        if (btClass == null) {
            Log.w(TAG, "btClass = NULL");
            return;
        }
        int type = btClass.getDeviceClass();
        AudioSystem.sendParametersFromASysToAPM("bluetooth_type=" + type);
    }

    public void setMode_Pom(int mode, int pid, String callingPackage, IBinder cb) {
        Log.d(TAG, "setMode_Pom(mode=" + mode + ", callingPackage=" + callingPackage + ")");
        if (!this.mAS.checkAudioSettingsPermission("setMode()")) {
            return;
        }
        if (mode == 2 && this.mContext.checkCallingOrSelfPermission("android.permission.MODIFY_PHONE_STATE") != 0) {
            Log.w(TAG, "MODIFY_PHONE_STATE Permission Denial: setMode(MODE_IN_CALL) from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
        } else if (mode != 0 && mode != 3) {
        } else {
            synchronized (this.mPomSetModeRecoryHandlers) {
                if (mode == -1) {
                    mode = this.mAS.getMode();
                }
                boolean mHdlrExist = this.mAS.checkSetModeHdlrExist(cb);
                if (mode == 0) {
                    if (!mHdlrExist) {
                        Log.d(TAG, "setMode_Pom :mode=" + mode + "SetModeDeathHandler don't have pid :" + pid + ",return.");
                        return;
                    }
                } else if (mode == 3 && mHdlrExist) {
                    Log.d(TAG, "setMode_Pom :mode=" + mode + "SetModeDeathHandler already exist pid :" + pid + ",return.");
                    return;
                }
                int newModeOwnerPid = this.mAS.setModeIntFromPom(mode, cb, pid, callingPackage);
                if (mHdlrExist) {
                    Iterator iter2 = this.mPomSetModeRecoryHandlers.iterator();
                    while (true) {
                        if (!iter2.hasNext()) {
                            break;
                        }
                        PomSetModeDeathHandler h_iter = iter2.next();
                        if (h_iter.getPid() == pid) {
                            Log.d(TAG, "rm pid:" + pid + " ,from mPomSetModeRecoryHandlers");
                            iter2.remove();
                            h_iter.getBinder().unlinkToDeath(h_iter, 0);
                            break;
                        }
                    }
                    if (mode == 0) {
                        Log.d(TAG, "add pid:" + Binder.getCallingPid() + " ,to mPomSetModeRecoryHandlers");
                        PomSetModeDeathHandler hdlr = new PomSetModeDeathHandler(cb, pid, callingPackage);
                        try {
                            cb.linkToDeath(hdlr, 0);
                        } catch (RemoteException e) {
                            Log.w(TAG, "setMode_Pom() could not link to " + cb + " binder death");
                        }
                        hdlr.setMode(3);
                        this.mPomSetModeRecoryHandlers.add(0, hdlr);
                    }
                }
                if (newModeOwnerPid != 0) {
                    Log.i(TAG, "In setMode(), calling disconnectBluetoothSco()");
                    this.mAS.SetModeDisconnectBluetoothSco(newModeOwnerPid);
                }
            }
        }
    }

    public void PomModeChangeBroadcast(int mode, int newpid, int pid, IBinder cb) {
        if (this.mAS == null) {
            return;
        }
        if (newpid != 0 && mode == 3) {
            AudioService.sendMsg(this.mAudioHandler, (int) MSG_POM_NEW, 2, mode, newpid, cb, 0);
        } else {
            AudioService.sendMsg(this.mAudioHandler, (int) MSG_POM_NEW, 2, mode, pid, cb, 0);
        }
        Log.e(TAG, "setModeint() onAudioModeChange mode:" + mode);
    }

    /* loaded from: classes.dex */
    private class PomSetModeDeathHandler implements IBinder.DeathRecipient {
        private String mCaller;
        private IBinder mCb;
        private int mMode = 0;
        private int mPid;

        PomSetModeDeathHandler(IBinder cb, int pid, String Caller) {
            this.mCb = cb;
            this.mPid = pid;
            this.mCaller = Caller;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            int oldModeOwnerPid = 0;
            int newModeOwnerPid = 0;
            Log.w(VivoAudioServiceImpl.TAG, "setMode() client died");
            if (!VivoAudioServiceImpl.this.mPomSetModeRecoryHandlers.isEmpty()) {
                oldModeOwnerPid = ((PomSetModeDeathHandler) VivoAudioServiceImpl.this.mPomSetModeRecoryHandlers.get(0)).getPid();
            }
            int index = VivoAudioServiceImpl.this.mPomSetModeRecoryHandlers.indexOf(this);
            if (index < 0) {
                Log.w(VivoAudioServiceImpl.TAG, "unregistered setMode() client died");
            } else {
                newModeOwnerPid = VivoAudioServiceImpl.this.mAS.setModeIntFromPom(0, this.mCb, this.mPid, VivoAudioServiceImpl.TAG);
                try {
                    VivoAudioServiceImpl.this.mFeatureService.onAudioModeChange(-2, this.mPid, (IBinder) null);
                    Log.w(VivoAudioServiceImpl.TAG, "pom_new onAudioModeChange binderDied mode:MODE_INVALID,pid:" + this.mPid);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (newModeOwnerPid != oldModeOwnerPid && newModeOwnerPid != 0) {
                Log.i(VivoAudioServiceImpl.TAG, "In binderDied(), calling disconnectBluetoothSco()");
                VivoAudioServiceImpl.this.mAS.SetModeDisconnectBluetoothSco(newModeOwnerPid);
            }
        }

        public int getPid() {
            return this.mPid;
        }

        public void setMode(int mode) {
            this.mMode = mode;
        }

        public int getMode() {
            return this.mMode;
        }

        public IBinder getBinder() {
            return this.mCb;
        }

        public String getCaller() {
            return this.mCaller;
        }
    }

    public void PomRemoveSetModeDeathHandler(int pid) {
        synchronized (this.mPomSetModeRecoryHandlers) {
            Iterator iter = this.mPomSetModeRecoryHandlers.iterator();
            while (iter.hasNext()) {
                PomSetModeDeathHandler hdlr = iter.next();
                if (hdlr.getPid() == pid) {
                    Log.d(TAG, "rm pid:" + Binder.getCallingPid() + "from mPomSetModeRecoryHandlers");
                    iter.remove();
                    hdlr.getBinder().unlinkToDeath(hdlr, 0);
                    return;
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public class VivoVolumeStreamState {
        protected final ConcurrentHashMap<Integer, Integer> mDeltaIndex;
        private final int mStreamType;
        private final String mVivoVolumeIndexSettingName;
        private final Intent mVolumeChanged;

        private VivoVolumeStreamState(String settingName, int streamType) {
            ConcurrentHashMap<Integer, Integer> concurrentHashMap = new ConcurrentHashMap<>(8, 0.75f, 4);
            this.mDeltaIndex = concurrentHashMap;
            this.mVivoVolumeIndexSettingName = settingName;
            this.mStreamType = streamType;
            concurrentHashMap.put(1073741824, 0);
            readSettingsForDeltaVolume();
            this.mVolumeChanged = new Intent("android.media.VOLUME_CHANGED_ACTION");
        }

        public int getDeltaIndex(int device) {
            int intValue;
            synchronized (AudioService.VolumeStreamState.class) {
                Integer index = this.mDeltaIndex.get(Integer.valueOf(device));
                if (index == null) {
                    index = this.mDeltaIndex.get(1073741824);
                }
                intValue = index.intValue();
            }
            return intValue;
        }

        public String getSettingNameForVolumeDelta(int device) {
            String name = this.mVivoVolumeIndexSettingName;
            String suffix = AudioSystem.getOutputDeviceName(device);
            if (suffix.isEmpty()) {
                return name;
            }
            return name + "_" + suffix + "_Delta";
        }

        public boolean setDeltaIndex(int index, int device, int[] alias) {
            synchronized (VivoVolumeStreamState.class) {
                int oldIndex = getDeltaIndex(device);
                synchronized (this) {
                    this.mDeltaIndex.put(Integer.valueOf(device), Integer.valueOf(index));
                }
                if (oldIndex == index) {
                    return false;
                }
                boolean currentDevice = device == VivoAudioServiceImpl.this.mAS.getDeviceForStream(this.mStreamType);
                int numStreamTypes = AudioSystem.getNumStreamTypes();
                for (int streamType = numStreamTypes - 1; streamType >= 0; streamType--) {
                    if (streamType != this.mStreamType && alias[streamType] == this.mStreamType) {
                        int scaledIndex = VivoAudioServiceImpl.this.mAS.rescaleIndex(index, this.mStreamType, streamType);
                        VivoAudioServiceImpl.this.mVivoStreamStates[streamType].setDeltaIndex(scaledIndex, device, alias);
                        if (currentDevice) {
                            VivoAudioServiceImpl.this.mVivoStreamStates[streamType].setDeltaIndex(scaledIndex, VivoAudioServiceImpl.this.mAS.getDeviceForStream(streamType), alias);
                        }
                    }
                }
                return true;
            }
        }

        protected void readSettingsForDeltaVolume() {
            synchronized (AudioService.VolumeStreamState.class) {
                int remainingDevices = 1342177279;
                int i = 0;
                while (remainingDevices != 0) {
                    int device = 1 << i;
                    if ((device & remainingDevices) != 0) {
                        remainingDevices &= ~device;
                        String VolDeltaName = getSettingNameForVolumeDelta(device);
                        int DelIndex = Settings.System.getIntForUser(VivoAudioServiceImpl.this.mContentResolver, VolDeltaName, 0, -2);
                        if (DelIndex != -1) {
                            Log.d(VivoAudioServiceImpl.TAG, "readSettingsForDeltaVolume mStreamType:" + this.mStreamType + " device:" + device + "name:" + getSettingNameForVolumeDelta(device) + " index:" + DelIndex);
                            this.mDeltaIndex.put(Integer.valueOf(device), Integer.valueOf(DelIndex));
                        }
                    }
                    i++;
                }
            }
        }

        public void applyDeviceDeltaVolume_syncVSS(int device) {
            int index = getDeltaIndex(device);
            VivoAudioServiceImpl.this.setStreamVolumeDeltaIndexHook(this.mStreamType, index, device);
        }
    }

    /* loaded from: classes.dex */
    public static class VivoVolumeStreamStateImpl implements IVivoAudioService.IVivoVolumeStreamState {
        public VivoVolumeStreamStateImpl(AudioService.VolumeStreamState vss, AudioService as, String settingName, int streamType) {
        }
    }

    private void dumpSetSpeakerphoneOnStack() {
        synchronized (this.mSetSpeakerphoneOnDeathHandler) {
            if (!this.mSetSpeakerphoneOnDeathHandler.isEmpty()) {
                Iterator iter = this.mSetSpeakerphoneOnDeathHandler.iterator();
                while (iter.hasNext()) {
                    SetSpeakerphoneOnDeathHandler h = iter.next();
                    Log.d(TAG, "###### SpeakerphoneOnClient, pid:" + h.getPid() + ", caller(" + h.getCaller() + ") state : " + h.getState());
                }
            } else {
                Log.d(TAG, "###### setSpeakerphoneOnClient is Empty !");
            }
        }
    }

    /* loaded from: classes.dex */
    private class SetSpeakerphoneOnDeathHandler implements IBinder.DeathRecipient {
        private String mCaller;
        private IBinder mCb;
        private int mPid;
        private boolean mState;

        SetSpeakerphoneOnDeathHandler(IBinder cb, int pid, String callingPackage, boolean state) {
            this.mCb = cb;
            this.mPid = pid;
            this.mCaller = callingPackage;
            this.mState = state;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (VivoAudioServiceImpl.this.mSetSpeakerphoneOnDeathHandler) {
                Log.w(VivoAudioServiceImpl.TAG, "SetSpeakerPhoneOnHandler() client died");
                int index = VivoAudioServiceImpl.this.mSetSpeakerphoneOnDeathHandler.indexOf(this);
                if (index < 0) {
                    Log.w(VivoAudioServiceImpl.TAG, "unregistered setSpeakerPhoneOnHandler() client died");
                } else {
                    Log.d(VivoAudioServiceImpl.TAG, "setSepakerphoneOn remove " + getPid());
                    VivoAudioServiceImpl.this.mSetSpeakerphoneOnDeathHandler.remove(this);
                }
            }
        }

        public int getPid() {
            return this.mPid;
        }

        public IBinder getBinder() {
            return this.mCb;
        }

        public String getCaller() {
            return this.mCaller;
        }

        public boolean getState() {
            return this.mState;
        }

        public void setState(boolean state) {
            this.mState = state;
        }
    }

    private static ITelecomService getITelecomService() {
        return ITelecomService.Stub.asInterface(ServiceManager.getService("telecom"));
    }

    public boolean isDialingOrInCall(String callingPackage) {
        VivoTelephonyApiParams ret;
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                try {
                    VivoTelephonyApiParams param = new VivoTelephonyApiParams("API_TAG_isDialingOrInCall");
                    param.put("callingPackage", callingPackage);
                    ITelecomService service = getITelecomService();
                    Method method = service.getClass().getMethod("vivoTelephonyApi", VivoTelephonyApiParams.class);
                    ret = (VivoTelephonyApiParams) method.invoke(service, param);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e2) {
                    e2.printStackTrace();
                }
            } catch (InvocationTargetException e3) {
                e3.printStackTrace();
            } catch (Exception e4) {
                e4.printStackTrace();
                Log.e(TAG, "isDialingOrInCall Exception");
            }
            if (ret == null) {
                Log.e(TAG, "isDialingOrInCall() vivoTelephonyApi return null!");
                return false;
            }
            return ret.getAsBoolean("isDialingOrInCall").booleanValue();
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean isVoipPackage(String callingPackage) {
        ArrayList<String> packList = new ArrayList<>();
        this.mFeatureService.getPackageList(1, packList);
        for (int i = 0; i < packList.size(); i++) {
            if (callingPackage.equals(packList.get(i))) {
                return true;
            }
        }
        return false;
    }

    public static String getTopPkgName() {
        ActivityManager.StackInfo info;
        String topTaskPkg = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        long ident = Binder.clearCallingIdentity();
        try {
            IActivityTaskManager activityTaskManager = ActivityTaskManager.getService();
            if (activityTaskManager != null && (info = activityTaskManager.getFocusedStackInfo()) != null && info.topActivity != null) {
                topTaskPkg = info.topActivity.getPackageName();
            }
        } catch (Exception e) {
            VLog.e(TAG, VLog.getStackTraceString(e));
        }
        Binder.restoreCallingIdentity(ident);
        return topTaskPkg;
    }

    public boolean checkSetStreamVolumeAllow(int streamType, int index, int flags, String callingPackage, int uid) {
        ArrayList<String> packList = new ArrayList<>();
        boolean isAppInList = false;
        if (streamType != 3 || index != 0) {
            return true;
        }
        boolean isAppSetBg = !callingPackage.equals(getTopPkgName());
        if (!isAppSetBg) {
            return true;
        }
        FeatureService featureService = this.mFeatureService;
        if (featureService != null) {
            featureService.getPackageList(5, packList);
            for (int i = 0; i < packList.size() && !(isAppInList = callingPackage.equals(packList.get(i))); i++) {
            }
        }
        if (!isAppInList) {
            return true;
        }
        this.mFeatureService.reportAudioEpmEvent(5, "Set streamvolume 0 in background", callingPackage, "background");
        return false;
    }

    /* JADX WARN: Code restructure failed: missing block: B:57:0x01a1, code lost:
        r4 = r22.getState();
     */
    /* JADX WARN: Removed duplicated region for block: B:85:0x0223  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x022d  */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:115:0x0298 -> B:116:0x0299). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean checkSetSpeakerAllow(android.os.IBinder r26, int r27, java.lang.String r28, int r29, boolean r30) {
        /*
            Method dump skipped, instructions count: 667
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.audio.VivoAudioServiceImpl.checkSetSpeakerAllow(android.os.IBinder, int, java.lang.String, int, boolean):boolean");
    }

    private boolean muteGameAppForCall(int mode) {
        IActivityTaskManager service;
        ActivityManager.StackInfo info;
        String fgAppName = null;
        try {
            service = ActivityTaskManager.getService();
            this.mActivityTaskManager = service;
        } catch (RemoteException e) {
        }
        if (service != null && (info = service.getFocusedStackInfo()) != null && info.topActivity != null) {
            fgAppName = info.topActivity.getPackageName();
            boolean callActive = mode == 2 || isChatAppActive();
            if (fgAppName == null || !callActive || this.mFeatureService == null) {
                return false;
            }
            ArrayList<String> packlists = new ArrayList<>();
            this.mFeatureService.getPackageList(12, packlists);
            for (int i = 0; i < packlists.size(); i++) {
                if (fgAppName.equals(packlists.get(i))) {
                    VLog.i(TAG, "fgAppName: " + fgAppName + " need control music vol!");
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void handleMusicStreamVolForVoip(int ctl) {
        int i;
        if (FtFeature.isFeatureSupport("vivo.software.audio.abandommusicforcall")) {
            if (ctl == 1 && !this.mMuteMusicForVoip) {
                int device = this.mAS.getDeviceForStream(3);
                int i2 = this.mMuteMusicDevForVoip;
                if (device != i2 && i2 != -1) {
                    handleMusicStreamVolForVoip(0);
                }
                this.mMuteMusicVolBackup = this.mAS.getStreamVolume(3);
                this.mMuteMusicDevForVoip = device;
                this.mAS.setStreamVolume(3, 0, 0, VivoPermissionUtils.OS_PKG, VivoPermissionUtils.OS_PKG, 1000, true);
                VLog.i(TAG, "handleMusicStreamVolForVoip , set music mute for device " + device + ", backup volume " + this.mMuteMusicVolBackup);
                this.mMuteMusicForVoip = true;
            } else if (ctl == 0) {
                int i3 = this.mMuteMusicVolBackup;
                if (i3 != -1 && (i = this.mMuteMusicDevForVoip) != -1) {
                    this.mAS.updateStreamVolume(3, i3, 0, i);
                    VLog.i(TAG, "handleMusicStreamVolForVoip , set music unmute for device " + this.mMuteMusicDevForVoip);
                    this.mMuteMusicForVoip = false;
                    this.mMuteMusicVolBackup = -1;
                    this.mMuteMusicDevForVoip = -1;
                }
            } else if (ctl == 2) {
                this.mMuteMusicForVoip = false;
                this.mMuteMusicVolBackup = -1;
                this.mMuteMusicDevForVoip = -1;
            }
        }
    }

    private void checkRecordEventForVoip() {
        if (!FtFeature.isFeatureSupport("vivo.software.audio.abandommusicforcall")) {
            return;
        }
        int i = 0;
        int mode = this.mAS.getMode();
        if (mode == 3 || mode == 2) {
            boolean needMuteVolume = muteGameAppForCall(mode);
            i = needMuteVolume;
        }
        handleMusicStreamVolForVoip(i);
    }

    public boolean isChatAppActive() {
        int modeOwnerUid = this.mAS.getModeOwnerUid();
        if (modeOwnerUid == 0) {
            return false;
        }
        boolean chatAppActive = false;
        long ident = Binder.clearCallingIdentity();
        String packName = this.mContext.getPackageManager().getNameForUid(modeOwnerUid);
        Binder.restoreCallingIdentity(ident);
        List<AudioRecordingConfiguration> arcs = this.mAS.getActiveRecordingConfigurationsExt();
        int i = 0;
        while (true) {
            if (i >= arcs.size()) {
                break;
            }
            AudioRecordingConfiguration arc = arcs.get(i);
            if (packName == null || !packName.equals(arc.getClientPackageName()) || arc.getClientAudioSource() != 7) {
                i++;
            } else {
                VLog.i(TAG, "get voip pack :" + arc.getClientPackageName());
                chatAppActive = true;
                break;
            }
        }
        if (!chatAppActive || this.mFeatureService == null) {
            return false;
        }
        ArrayList<String> packlists = new ArrayList<>();
        this.mFeatureService.getPackageList(11, packlists);
        for (int i2 = 0; i2 < packlists.size(); i2++) {
            if (packName != null && packName.equals(packlists.get(i2))) {
                VLog.i(TAG, "chat app packName: " + packName + " is active!");
                return true;
            }
        }
        return false;
    }

    public void triggerMsgForVoipVas() {
        AudioService.sendMsg(this.mAudioHandler, (int) MSG_CHECK_RECORDING_EVENT, 0, 0, 0, (Object) null, 500);
    }

    public void processVolumeEventForVivoAudio(int index, int streamType) {
        if (index != 0 && streamType == 3) {
            handleMusicStreamVolForVoip(2);
        }
    }

    public void registerProcessObserver() {
        try {
            VSlog.d(TAG, "registerProcessObserver");
            ActivityManagerNative.getDefault().registerProcessObserver(this.mProcessObserver);
        } catch (Exception e) {
            VSlog.e(TAG, "error registerProcessObserver " + e);
        }
    }

    public void playerEvent(int piid, int event, int spid, int suid) {
        ArrayList<String> packList = new ArrayList<>();
        if (event == 2) {
            long ident = Binder.clearCallingIdentity();
            String packName = this.mContext.getPackageManager().getNameForUid(suid);
            Binder.restoreCallingIdentity(ident);
            this.mFeatureService.getPackageList(3, packList);
            if (packName == null || packList.size() == 0 || this.mFeatureService.isApplicationForeground(packName)) {
                return;
            }
            synchronized (this.mMutePiidMap) {
                int idx = 0;
                while (true) {
                    if (idx >= packList.size()) {
                        break;
                    } else if (!packName.contains(packList.get(idx))) {
                        idx++;
                    } else {
                        this.mAS.muteBgPlayer(piid);
                        this.mMutePiidMap.put(Integer.valueOf(piid), 1);
                        this.mFeatureService.reportAudioEpmEvent(4, "Player in background", packName, "background");
                        break;
                    }
                }
            }
            return;
        }
        synchronized (this.mMutePiidMap) {
            if (this.mMutePiidMap.get(new Integer(piid)) != null) {
                this.mAS.unmuteBgPlayer(piid);
                this.mMutePiidMap.remove(new Integer(piid));
            }
        }
    }

    public void setMode(int mode, IBinder cb, String callingPackage) {
        if (this.mModeClientMap.get(callingPackage) != null) {
            this.mModeClientMap.remove(callingPackage);
        }
        this.mModeClientMap.put(callingPackage, Integer.valueOf(mode));
        if (this.mPlatformList.contains(SystemProperties.get("ro.vivo.product.platform", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK))) {
            ArrayList<String> packList = new ArrayList<>();
            boolean isPackageInList = false;
            this.mFeatureService.getPackageList(4, packList);
            int i = 0;
            while (true) {
                if (i >= packList.size()) {
                    break;
                } else if (!callingPackage.equals(packList.get(i))) {
                    i++;
                } else {
                    isPackageInList = true;
                    break;
                }
            }
            if (isPackageInList && mode == 3) {
                AudioSystem.setParameters("SetAudioCustomScene=App1");
            } else if (isPackageInList && mode != 3) {
                AudioSystem.setParameters("SetAudioCustomScene=default");
            }
        }
    }

    public int checkGetMode(int currMode) {
        if (this.mFeatureService == null) {
            return -1;
        }
        ArrayList<String> packList = new ArrayList<>();
        this.mFeatureService.getPackageList(0, packList);
        if (packList.size() == 0) {
            return -1;
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        String pckName = this.mContext.getPackageManager().getNameForUid(uid);
        Binder.restoreCallingIdentity(ident);
        if (pckName == null) {
            VSlog.e(TAG, "could not get pckName for uid " + uid);
            return -1;
        }
        int i = 0;
        while (true) {
            if (i >= packList.size()) {
                break;
            }
            if (pckName.contains(packList.get(i))) {
                if (this.mModeClientMap.get(pckName) == null) {
                    VSlog.i(TAG, "no history setMode of pckName " + pckName);
                    break;
                }
                int historyMode = this.mModeClientMap.get(pckName).intValue();
                if (currMode == 3 && historyMode == 0) {
                    VSlog.i(TAG, "change mode to 0 when current is : " + currMode);
                    this.mFeatureService.reportAudioEpmEvent(1, "getMode ", pckName, "game voip mute");
                    return historyMode;
                }
            }
            i++;
        }
        return -1;
    }

    public void readRestrictionPolicy() {
        DevicePolicyManager devicePolicyManager = this.mDevicePolicManager;
        if (devicePolicyManager != null) {
            int type = devicePolicyManager.getCustomType();
            if (type > 0) {
                int policy = this.mDevicePolicManager.getRestrictionPolicy(null, 15, UserHandle.getCallingUserId());
                if (policy == 1) {
                    AudioSystem.setParameters("vivo_mic_mute=true");
                } else {
                    AudioSystem.setParameters("vivo_mic_mute=false");
                }
            }
        }
    }

    private void registerVivoPolicyListener() {
        VivoPolicyManagerInternal vivoPolicyManagerInternal = this.mVivoPolicyManagerInternal;
        if (vivoPolicyManagerInternal != null) {
            vivoPolicyManagerInternal.setVivoPolicyListener(new VivoPolicyManagerInternal.VivoPolicyListener() { // from class: com.android.server.audio.VivoAudioServiceImpl.3
                public void onVivoPolicyChanged(int poId) {
                    VLog.d(VivoAudioServiceImpl.TAG, "onVivoPolicyChanged");
                    if (poId == 0 || poId == 15) {
                        int policy = VivoAudioServiceImpl.this.mDevicePolicManager.getRestrictionPolicy(null, 15, UserHandle.getCallingUserId());
                        if (policy == 1) {
                            VLog.d(VivoAudioServiceImpl.TAG, "enable mic mute");
                            AudioSystem.setParameters("vivo_mic_mute=true");
                            return;
                        }
                        VLog.d(VivoAudioServiceImpl.TAG, "disable mic mute");
                        AudioSystem.setParameters("vivo_mic_mute=false");
                    }
                }
            });
        } else {
            VLog.e(TAG, "mVivoPolicyManagerInternal is null, set vivo policy listener fail");
        }
    }

    public void restoreStreamVolumeMaxMin() {
        VLog.d(TAG, "restoreStreamVolumeMaxMin");
        if (AudioProductStrategy.getAudioProductStrategies().size() > 0) {
            int numStreamTypes = AudioSystem.getNumStreamTypes();
            for (int streamType = numStreamTypes - 1; streamType >= 0; streamType--) {
                AudioAttributes attr = AudioProductStrategy.getAudioAttributesForStrategyWithLegacyStreamType(streamType);
                int maxVolume = AudioSystem.getMaxVolumeIndexForAttributes(attr);
                int minVolume = AudioSystem.getMinVolumeIndexForAttributes(attr);
                VLog.d(TAG, "AudioNative STREAMTYPE (" + streamType + ") MAX (" + maxVolume + ") MIN (" + minVolume + ")");
                if (maxVolume == -1 || minVolume == -1) {
                    int mIndexMax = this.MAX_STREAM_VOLUME[streamType] * 10;
                    int mIndexMin = this.MIN_STREAM_VOLUME[streamType] * 10;
                    VLog.d(TAG, "AudioNative STREAMTYPE (" + streamType + ") MAX/MIN volume error! reinit.... MAX (" + mIndexMax + ") MIN (" + mIndexMin + ")");
                    AudioSystem.initStreamVolume(streamType, mIndexMin / 10, mIndexMax / 10);
                }
            }
        }
    }

    public void playSoundEffectVolumeWithApp(int effectType, float volume, int pid, String packageName) {
        if (shouldBlockAudioForAppShare(pid, packageName)) {
            return;
        }
        this.mAS.playSoundEffectVolume(effectType, volume);
    }

    public void notifyAppSharePackageChanged(String packageName, int userId) {
        this.mAppSharePackageName = packageName;
        this.mAppShareUserId = userId;
    }

    public void updateAppShareInputHandle(boolean isControlledByRemote) {
        this.mIsControlledByRemote = isControlledByRemote;
    }

    private void initAppShareController() {
        String appShareInputHandle = Settings.System.getString(this.mContext.getContentResolver(), "appshare_input_handle");
        if (TextUtils.isEmpty(appShareInputHandle)) {
            appShareInputHandle = "local";
        }
        this.mIsControlledByRemote = "remote".equals(appShareInputHandle);
    }

    private boolean shouldBlockAudioForAppShare(int pid, String packageName) {
        if (AppShareConfig.SUPPROT_APPSHARE && !TextUtils.isEmpty(this.mAppSharePackageName) && this.mAppShareUserId != -1 && this.mIsControlledByRemote) {
            int callingUid = Binder.getCallingUid();
            int userId = UserHandle.getUserId(callingUid);
            if (this.mVivoRatioControllerUtils.isImeApplication(this.mContext, pid, callingUid)) {
                if (MultiDisplayManager.isAppShareDisplayId(this.mVivoRatioControllerUtils.getCurrentInputMethodDisplayId())) {
                    VSlog.i(TAG, "block sound of " + packageName + ", because ime is on app share display.");
                    return true;
                }
            } else if (this.mAppSharePackageName.equals(packageName) && this.mAppShareUserId == userId) {
                VSlog.i(TAG, "block sound of " + packageName + ", because it is sharing.");
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean shouldBlockSetStreamVolumeForAppShare(String packageName, int userId) {
        if (AppShareConfig.SUPPROT_APPSHARE && !TextUtils.isEmpty(this.mAppSharePackageName) && this.mAppShareUserId != -1 && this.mIsControlledByRemote && this.mAppSharePackageName.equals(packageName) && this.mAppShareUserId == userId) {
            VSlog.i(TAG, "block set stream volume of " + packageName + "," + userId + ", because it is controlled by remote!");
            long lastSetStreamVolumeTime = this.mSetStreamVolumeTime;
            long currentTimeMillis = System.currentTimeMillis();
            this.mSetStreamVolumeTime = currentTimeMillis;
            if (currentTimeMillis - lastSetStreamVolumeTime > 50) {
                this.mVivoAppShareManager.forbidUseHardware(3);
                return true;
            }
            return true;
        }
        return false;
    }
}