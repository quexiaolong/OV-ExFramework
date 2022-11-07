package com.vivo.services.vivolight;

import android.content.Context;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.SparseArray;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import vivo.app.vivolight.ILightChangeCallback;
import vivo.app.vivolight.IVivoLightManager;
import vivo.app.vivolight.VivoLightRecord;

/* loaded from: classes.dex */
public class VivoLightManagerService extends IVivoLightManager.Stub {
    private static final long GUARANTEED_INTERNAL_LONG = 2000;
    private static final long GUARANTEED_INTERNAL_SHORT = 500;
    private static final int MSG_UPDATE = 1;
    private static final int MSG_UPDATE_DELAY = 2;
    private static final String PROP_COLORED_LIGHT = "persist.sys.colored.light";
    private static final int SUPPORT_CAMERA_LIGHT_MODEL_VALUE = 2;
    private static final int SUPPORT_LINE_LIGHT_MODEL_VALUE = 1;
    public static final String TAG = "VivoLightManagerService";
    private static boolean isSupportCoolight;
    private static boolean isSupportLight;
    private static VivoLightManagerService sInstance;
    private boolean isUltraPower;
    private int lastId;
    private AudioManager mAudioManager;
    private Context mContext;
    private int mLastBrightness;
    private final LightHandler mLightHandler;
    private final HandlerThread mLightHandlerThread;
    private SensorUtil mSensorUtil;
    private static final String[] SUPPORT_LINE_LIGHT_MODEL = {"PD1824", "PD1824B", "PD1916", "PD1922"};
    private static final String[] SUPPORT_CAMERA_LIGHT_MODEL = {"PD1829", "PD1836", "PD1836F_EX"};
    private static final int[] MUSIC_LIGHT_TYPE = {32, 33, 34, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101};
    private int mLightType = 0;
    private final Object mLock = new Object();
    private final List<VivoLightRecord> mPlayLightInfos = new ArrayList();
    private final SparseArray<CallbackRecord> mCallbacks = new SparseArray<>();
    private final HashMap<Integer, ProcessObserver> mProcessObservers = new HashMap<>();
    private int mCurrentState = 0;
    private boolean isLimitLight = false;

    static {
        String[] strArr;
        isSupportLight = false;
        isSupportCoolight = false;
        boolean isUnderFactoryMode = SystemProperties.get("persist.sys.factory.mode", "no").equals("yes");
        if (!isUnderFactoryMode) {
            int propColoredLight = SystemProperties.getInt(PROP_COLORED_LIGHT, -1);
            String model = SystemProperties.get("ro.vivo.product.model", "null");
            if (propColoredLight > 0) {
                if (propColoredLight == 1) {
                    String[] strArr2 = SUPPORT_CAMERA_LIGHT_MODEL;
                    int length = strArr2.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        String item = strArr2[i];
                        if (!item.equals(model)) {
                            i++;
                        } else {
                            SystemProperties.set(PROP_COLORED_LIGHT, "2");
                            break;
                        }
                    }
                }
                isSupportLight = true;
            } else {
                int propColoredLight2 = -1;
                String[] strArr3 = SUPPORT_LINE_LIGHT_MODEL;
                int length2 = strArr3.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length2) {
                        break;
                    }
                    String item2 = strArr3[i2];
                    if (!item2.equals(model)) {
                        i2++;
                    } else {
                        propColoredLight2 = 1;
                        break;
                    }
                }
                if (propColoredLight2 < 0) {
                    String[] strArr4 = SUPPORT_CAMERA_LIGHT_MODEL;
                    int length3 = strArr4.length;
                    int i3 = 0;
                    while (true) {
                        if (i3 >= length3) {
                            break;
                        }
                        String item3 = strArr4[i3];
                        if (!item3.equals(model)) {
                            i3++;
                        } else {
                            propColoredLight2 = 2;
                            break;
                        }
                    }
                }
                if (propColoredLight2 > 0) {
                    isSupportLight = true;
                    SystemProperties.set(PROP_COLORED_LIGHT, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + propColoredLight2);
                }
            }
            for (String item4 : SUPPORT_LINE_LIGHT_MODEL) {
                if (item4.equals(model)) {
                    isSupportCoolight = true;
                    return;
                }
            }
            return;
        }
        VLog.d(TAG, "isUnderFactoryMode = true");
    }

    public int getLastBrightness() {
        return this.mLastBrightness;
    }

    public void setLastBrightness(int lastBrightness) {
        this.mLastBrightness = this.mLastBrightness;
    }

    public int getCurrentState() {
        return this.mCurrentState;
    }

    public void setCurrentState(int currentState) {
        VLog.d(TAG, "currentState:" + currentState);
        this.mCurrentState = currentState;
    }

    public void notifyUpdateLight() {
        this.mLightHandler.removeMessages(1);
        this.mLightHandler.removeMessages(2);
        this.mLightHandler.sendEmptyMessage(1);
    }

    public void notifyUpdateLight(long time) {
        this.mLightHandler.removeMessages(2);
        this.mLightHandler.sendEmptyMessageDelayed(2, time);
    }

    public static synchronized VivoLightManagerService getInstance(Context context) {
        VivoLightManagerService vivoLightManagerService;
        synchronized (VivoLightManagerService.class) {
            if (sInstance == null) {
                sInstance = new VivoLightManagerService(context);
            }
            vivoLightManagerService = sInstance;
        }
        return vivoLightManagerService;
    }

    private VivoLightManagerService(Context context) {
        VLog.d(TAG, "CircleLightManagerService construct");
        this.mContext = context;
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        HandlerThread handlerThread = new HandlerThread(TAG);
        this.mLightHandlerThread = handlerThread;
        handlerThread.start();
        this.mLightHandler = new LightHandler(this.mLightHandlerThread.getLooper());
        initListener();
    }

    public void initListener() {
        if (!isSupportLight()) {
            return;
        }
        DisplayListenerUtil displayListenerUtil = new DisplayListenerUtil(this.mContext, this);
        displayListenerUtil.register(this.mLightHandler);
        SuperSaverPowerUtil superSaverPowerUtil = new SuperSaverPowerUtil(this.mContext, this);
        superSaverPowerUtil.register();
        if (isSupportCoolight) {
            SettingMonitorUtil settingMonitorUtil = new SettingMonitorUtil(this.mContext, this);
            settingMonitorUtil.register(this.mLightHandler);
        }
    }

    private boolean isSupportLight() {
        return isSupportLight;
    }

    public int startLight(IBinder binder, VivoLightRecord record) {
        Bundle extra;
        if (isSupportLight()) {
            if (record == null || record.getPackageName() == null) {
                VLog.e(TAG, "start light record or packageName is null, do nothing");
                return -1;
            }
            int duration = record.getDuration();
            int lightType = record.getLightType();
            int lightBrightness = record.getLightBrightness();
            if (duration < 0 || lightType < 0 || lightBrightness < 0) {
                return -1;
            }
            if (!this.isLimitLight || duration == 0 || ((extra = record.getExtra()) != null && extra.getBoolean(SettingMonitorUtil.KEY_BEYOND_LIGHT_TIME_LIMIT))) {
                setGuaranteedTime(record);
                synchronized (this.mLock) {
                    int callingPid = Binder.getCallingPid();
                    record.setPid(callingPid);
                    if (!this.mProcessObservers.containsKey(Integer.valueOf(callingPid))) {
                        ProcessObserver observer = new ProcessObserver(callingPid, binder);
                        try {
                            binder.linkToDeath(observer, 0);
                            this.mProcessObservers.put(Integer.valueOf(callingPid), observer);
                        } catch (RemoteException e) {
                            VLog.w(TAG, "The calling process has already died");
                            return -1;
                        }
                    }
                    if (record.getGuaranteedTime() == 0) {
                        for (int i = this.mPlayLightInfos.size() - 1; i >= 0; i--) {
                            VivoLightRecord vivoLightRecord = this.mPlayLightInfos.get(i);
                            if (vivoLightRecord.getGuaranteedTime() == 0 && record.getPackageName().equals(vivoLightRecord.getPackageName()) && record.getPriority() == vivoLightRecord.getPriority() && record.getPid() == vivoLightRecord.getPid()) {
                                removeItemLocked(i, false);
                            }
                        }
                    }
                    int newLightId = createNewLightIdLocked();
                    record.setLightId(newLightId);
                    this.mPlayLightInfos.add(0, record);
                }
                VLog.d(TAG, "startLight=" + record);
                notifyUpdateLight();
                return record.getLightId();
            }
            return -1;
        }
        return -1;
    }

    private void setGuaranteedTime(VivoLightRecord record) {
        long l = SystemClock.elapsedRealtime();
        if (record.getDuration() > 5000) {
            record.setGuaranteedTime(record.getDuration() + l + GUARANTEED_INTERNAL_LONG);
        } else if (record.getDuration() > 0) {
            record.setGuaranteedTime(record.getDuration() + l + GUARANTEED_INTERNAL_SHORT);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateLight() {
        Bundle extra;
        synchronized (this.mLock) {
            int oldSize = this.mPlayLightInfos.size();
            boolean z = true;
            for (int i = this.mPlayLightInfos.size() - 1; i >= 0; i--) {
                VivoLightRecord vivoLightRecord = this.mPlayLightInfos.get(i);
                long guaranteedTime = vivoLightRecord.getGuaranteedTime();
                if (guaranteedTime > 0 && guaranteedTime < SystemClock.elapsedRealtime()) {
                    removeItemLocked(i, true);
                }
            }
            VLog.d(TAG, "mPlayLightInfos.oldSize=" + oldSize + ", newSize=" + this.mPlayLightInfos.size());
            if (this.isUltraPower) {
                if (this.mLightType != 0) {
                    stopLightInternalLocked(true);
                }
                return;
            }
            if (this.mPlayLightInfos.isEmpty()) {
                if (this.mLightType != 0) {
                    stopLightInternalLocked(true);
                }
            } else {
                this.mPlayLightInfos.sort(new Comparator<VivoLightRecord>() { // from class: com.vivo.services.vivolight.VivoLightManagerService.1
                    @Override // java.util.Comparator
                    public int compare(VivoLightRecord v1, VivoLightRecord v2) {
                        if (v1 == v2) {
                            return 0;
                        }
                        int compare = Integer.compare(v2.getPriority(), v1.getPriority());
                        if (compare == 0) {
                            return Integer.compare(v2.getLightId(), v1.getLightId());
                        }
                        return compare;
                    }
                });
                VivoLightRecord updateInfo = null;
                boolean needAccelerometerSensor = false;
                for (VivoLightRecord playLightInfo : this.mPlayLightInfos) {
                    int offFlag = playLightInfo.getOffFlag();
                    if (!this.isLimitLight || ((extra = playLightInfo.getExtra()) != null && extra.getBoolean(SettingMonitorUtil.KEY_BEYOND_LIGHT_TIME_LIMIT))) {
                        if ((offFlag & 4) != 0) {
                            needAccelerometerSensor = true;
                        }
                        if (offFlag != 0 && (this.mCurrentState & offFlag) == offFlag) {
                        }
                        updateInfo = playLightInfo;
                        break;
                    }
                }
                if (updateInfo == null) {
                    if (this.mLightType != 0) {
                        if (needAccelerometerSensor) {
                            z = false;
                        }
                        stopLightInternalLocked(z);
                    }
                } else {
                    if (needAccelerometerSensor) {
                        if (this.mSensorUtil == null) {
                            this.mSensorUtil = new SensorUtil(this.mContext, this);
                        }
                        this.mSensorUtil.registerAccelerometerListener();
                    } else if (this.mSensorUtil != null) {
                        this.mSensorUtil.unregisterAccelerometerListener();
                    }
                    if (updateInfo.getGuaranteedTime() > 0) {
                        this.mLightHandler.sendEmptyMessageDelayed(2, updateInfo.getGuaranteedTime() - SystemClock.elapsedRealtime());
                    }
                    if (updateInfo.getLightType() != this.mLightType) {
                        startLightInternalocked(updateInfo);
                    } else if (this.mLastBrightness != updateInfo.getLightBrightness()) {
                        updateLightBrightness(updateInfo);
                    }
                }
            }
        }
    }

    private void unregisterSensor() {
        SensorUtil sensorUtil = this.mSensorUtil;
        if (sensorUtil != null) {
            sensorUtil.unregisterAccelerometerListener();
            setCurrentState(this.mCurrentState & (-5));
        }
    }

    private void startLightInternalocked(VivoLightRecord record) {
        int lightType = record.getLightType();
        if (this.mAudioManager != null) {
            if (isMusicLightType(this.mLightType)) {
                if (isMusicLightType(lightType)) {
                    VLog.d(TAG, "setParameters == music, already");
                } else {
                    this.mAudioManager.setParameters("audio-light_enable=0");
                }
            } else if (isMusicLightType(lightType)) {
                this.mAudioManager.setParameters("audio-light_enable=1");
            } else {
                VLog.d(TAG, "setParameters != music, dont need");
            }
        } else {
            VLog.d(TAG, "mAudioManager is null");
        }
        notifyLocked(lightType);
        VLog.d(TAG, "startLightInternalocked=" + record);
        this.mLightType = lightType;
        int lightBrightness = record.getLightBrightness();
        this.mLastBrightness = lightBrightness;
        DualLightHalWrapper.setMode(this.mLightType, lightBrightness);
        DualLightHalWrapper.startLed(1);
    }

    private void updateLightBrightness(VivoLightRecord record) {
        VLog.d(TAG, "updateLightBrightness=" + record);
        int lightType = record.getLightType();
        notifyLocked(lightType);
        int lightBrightness = record.getLightBrightness();
        this.mLastBrightness = lightBrightness;
        DualLightHalWrapper.setBrightness(lightBrightness);
    }

    private static boolean isMusicLightType(int lightType) {
        int[] iArr;
        for (int type : MUSIC_LIGHT_TYPE) {
            if (lightType == type) {
                return true;
            }
        }
        return false;
    }

    private void notifyLocked(int lightType) {
        VLog.d(TAG, "notify lightType=" + lightType + ", mCallbacks=" + this.mCallbacks.size());
        for (int i = 0; i < this.mCallbacks.size(); i++) {
            CallbackRecord record = this.mCallbacks.valueAt(i);
            record.onLightStateChange(lightType);
        }
    }

    private int createNewLightIdLocked() {
        if (this.lastId == Integer.MAX_VALUE) {
            VLog.w(TAG, "lightId out of Integer range");
            this.lastId = 0;
            this.mPlayLightInfos.clear();
        }
        int i = this.lastId + 1;
        this.lastId = i;
        return i;
    }

    public void stopLightById(int lightId) {
        if (!isSupportLight()) {
            return;
        }
        VLog.d(TAG, "stopLightById=" + lightId);
        if (lightId < 0) {
            return;
        }
        synchronized (this.mLock) {
            int index = findVivoLightRecordLocked(lightId);
            if (index != -1) {
                removeItemLocked(index, true);
            }
        }
        notifyUpdateLight();
    }

    private void removeItemLocked(int index, boolean removeObserver) {
        VivoLightRecord remove = this.mPlayLightInfos.remove(index);
        if (removeObserver) {
            int pid = remove.getPid();
            if (!findVivoLightRecordByPidLocked(pid)) {
                ProcessObserver processObserver = this.mProcessObservers.remove(Integer.valueOf(pid));
                if (processObserver != null) {
                    processObserver.unLinkToDeath();
                } else {
                    VLog.e(TAG, "processObserver can't be null");
                }
            }
        }
    }

    private boolean findVivoLightRecordByPidLocked(int pid) {
        int numListeners = this.mPlayLightInfos.size();
        for (int i = 0; i < numListeners; i++) {
            VivoLightRecord vivoLightRecord = this.mPlayLightInfos.get(i);
            if (vivoLightRecord.getPid() == pid) {
                return true;
            }
        }
        return false;
    }

    public boolean updateBrightnessById(int lightId, int brightness) {
        VLog.d(TAG, "updateBrightnessById=" + lightId + "----" + brightness);
        if (isSupportLight() && lightId >= 0 && brightness >= 0) {
            synchronized (this.mLock) {
                int index = findVivoLightRecordLocked(lightId);
                if (index < 0) {
                    return false;
                }
                VivoLightRecord vivoLightRecord = this.mPlayLightInfos.get(index);
                vivoLightRecord.setLightBrightness(brightness);
                notifyUpdateLight();
                return true;
            }
        }
        return false;
    }

    public boolean updateLightById(int lightId, int lightType, int lightBrightness, int offFlag, int duration) {
        VLog.d(TAG, "updateLightById=" + lightId);
        if (isSupportLight() && lightId >= 0) {
            synchronized (this.mLock) {
                int index = findVivoLightRecordLocked(lightId);
                if (index < 0) {
                    return false;
                }
                VivoLightRecord vivoLightRecord = this.mPlayLightInfos.get(index);
                if (lightType >= 0) {
                    vivoLightRecord.setLightType(lightType);
                }
                if (lightBrightness >= 0) {
                    vivoLightRecord.setLightBrightness(lightBrightness);
                }
                if (offFlag >= 0) {
                    vivoLightRecord.setOffFlag(offFlag);
                }
                if (duration >= 0) {
                    vivoLightRecord.setDuration(duration);
                }
                if (vivoLightRecord.getDuration() > 0) {
                    setGuaranteedTime(vivoLightRecord);
                }
                notifyUpdateLight();
                return true;
            }
        }
        return false;
    }

    private void stopLightInternalLocked(boolean needStopAccelerometerSensor) {
        VLog.d(TAG, "stopLightInternalLocked");
        if (needStopAccelerometerSensor) {
            unregisterSensor();
        }
        if (this.mAudioManager == null) {
            VLog.d(TAG, "mAudioManager is null");
        } else if (!isMusicLightType(this.mLightType)) {
            VLog.d(TAG, "stop setParameters != music, dont need");
        } else {
            this.mAudioManager.setParameters("audio-light_enable=0");
        }
        this.mLightType = 0;
        notifyLocked(0);
        DualLightHalWrapper.setMode(0, 3);
        DualLightHalWrapper.startLed(1);
    }

    public void registerLightChangeCallback(ILightChangeCallback callback) {
        if (!isSupportLight()) {
            return;
        }
        VLog.d(TAG, "registerLightChangeCallback");
        if (callback == null) {
            VLog.e(TAG, "ILightChangeCallback is null");
            return;
        }
        int callingPid = Binder.getCallingPid();
        long token = Binder.clearCallingIdentity();
        try {
            registerLightChangeCallbackInternal(callback, callingPid);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void registerLightChangeCallbackInternal(ILightChangeCallback callback, int callingPid) {
        synchronized (this.mLock) {
            if (this.mCallbacks.get(callingPid) != null) {
                VLog.e(TAG, "The calling process has already registered an ICircleLightManagerCallback.");
                return;
            }
            CallbackRecord record = new CallbackRecord(callingPid, callback);
            try {
                IBinder binder = callback.asBinder();
                binder.linkToDeath(record, 0);
                this.mCallbacks.put(callingPid, record);
            } catch (RemoteException e) {
                VLog.w(TAG, "The calling process has already died");
            }
        }
    }

    public void unregisterLightChangeCallback(ILightChangeCallback callback) {
        if (!isSupportLight()) {
            return;
        }
        VLog.d(TAG, "unregisterLightChangeCallback");
        if (callback == null) {
            VLog.w(TAG, "unregisterLightChangeCallback param is null");
            return;
        }
        int callingPid = Binder.getCallingPid();
        removeCallBack(callingPid);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeVivoLightRecord(int pid) {
        boolean isRemove = false;
        synchronized (this.mLock) {
            for (int i = this.mPlayLightInfos.size() - 1; i >= 0; i--) {
                if (this.mPlayLightInfos.get(i).getPid() == pid) {
                    VLog.d(TAG, "onProcessDied pid=" + pid + ", remove");
                    isRemove = true;
                    this.mPlayLightInfos.remove(i);
                }
            }
        }
        if (isRemove) {
            notifyUpdateLight();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeProcessObServer(int pid) {
        synchronized (this.mLock) {
            ProcessObserver processObserver = this.mProcessObservers.remove(Integer.valueOf(pid));
            processObserver.unLinkToDeath();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void removeCallBack(int pid) {
        synchronized (this.mLock) {
            this.mCallbacks.remove(pid);
        }
    }

    public int getLightType() {
        VLog.d(TAG, "getLightType");
        return this.mLightType;
    }

    private int findVivoLightRecordLocked(int lightId) {
        int numListeners = this.mPlayLightInfos.size();
        for (int i = 0; i < numListeners; i++) {
            VivoLightRecord vivoLightRecord = this.mPlayLightInfos.get(i);
            if (vivoLightRecord.getLightId() == lightId) {
                return i;
            }
        }
        return -1;
    }

    public boolean isUltraPower() {
        return this.isUltraPower;
    }

    public void setUltraSavePower(boolean ultraPower) {
        this.isUltraPower = ultraPower;
    }

    public void setLimitLight(boolean limitLight) {
        this.isLimitLight = limitLight;
    }

    public boolean isLimitLight() {
        return this.isLimitLight;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LightHandler extends Handler {
        LightHandler(Looper looper) {
            super(looper);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int what = msg.what;
            VLog.d(VivoLightManagerService.TAG, "receive message what=" + what);
            if (what == 1 || what == 2) {
                VivoLightManagerService.this.updateLight();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class CallbackRecord implements IBinder.DeathRecipient {
        private final ILightChangeCallback mCallback;
        public final int mPid;

        public CallbackRecord(int pid, ILightChangeCallback callback) {
            this.mPid = pid;
            this.mCallback = callback;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            VLog.d(VivoLightManagerService.TAG, "Light state listener for pid " + this.mPid + " died.");
            VivoLightManagerService.this.removeCallBack(this.mPid);
            IBinder binder = this.mCallback.asBinder();
            binder.unlinkToDeath(this, 0);
        }

        public void onLightStateChange(int type) {
            try {
                VLog.d(VivoLightManagerService.TAG, "isMusicActive callback");
                this.mCallback.onLightStateChange(type);
            } catch (RemoteException ex) {
                VLog.w(VivoLightManagerService.TAG, "Failed to notify process " + this.mPid + " that circle light changed, assuming it died.", ex);
                binderDied();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ProcessObserver implements IBinder.DeathRecipient {
        private final IBinder binder;
        private final int pid;

        private ProcessObserver(int pid, IBinder binder) {
            this.pid = pid;
            this.binder = binder;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            VLog.d(VivoLightManagerService.TAG, "Light state listener for pid " + this.pid + " died.");
            VivoLightManagerService.this.removeVivoLightRecord(this.pid);
            VivoLightManagerService.this.removeProcessObServer(this.pid);
        }

        public void unLinkToDeath() {
            this.binder.unlinkToDeath(this, 0);
        }
    }
}