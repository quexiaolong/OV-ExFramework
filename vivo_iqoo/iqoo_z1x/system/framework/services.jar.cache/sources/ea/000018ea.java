package com.android.server.soundtrigger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import android.hardware.soundtrigger.SoundTriggerModule;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.internal.logging.MetricsLogger;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/* loaded from: classes2.dex */
public class SoundTriggerHelper implements SoundTrigger.StatusListener {
    private static final int CALL_INACTIVE_MSG_DELAY_MS = 1000;
    static final boolean DBG = true;
    private static final int INVALID_VALUE = Integer.MIN_VALUE;
    private static final int MSG_CALL_STATE_CHANGED = 0;
    public static final int STATUS_ERROR = Integer.MIN_VALUE;
    public static final int STATUS_OK = 0;
    static final String TAG = "SoundTriggerHelper";
    private final Context mContext;
    private final Handler mHandler;
    private HashMap<Integer, UUID> mKeyphraseUuidMap;
    private final HashMap<UUID, ModelData> mModelDataMap;
    private SoundTriggerModule mModule;
    final SoundTrigger.ModuleProperties mModuleProperties;
    private final PhoneStateListener mPhoneStateListener;
    private final PowerManager mPowerManager;
    private PowerSaveModeListener mPowerSaveModeListener;
    private final TelephonyManager mTelephonyManager;
    private final Object mLock = new Object();
    private boolean mCallActive = false;
    private boolean mIsPowerSaveMode = false;
    private boolean mServiceDisabled = false;
    private boolean mRecognitionRequested = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SoundTriggerHelper(Context context) {
        ArrayList<SoundTrigger.ModuleProperties> modules = new ArrayList<>();
        int status = SoundTrigger.listModules(modules);
        this.mContext = context;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mModelDataMap = new HashMap<>();
        this.mKeyphraseUuidMap = new HashMap<>();
        this.mPhoneStateListener = new MyCallStateListener();
        if (status != 0 || modules.size() == 0) {
            Slog.w(TAG, "listModules status=" + status + ", # of modules=" + modules.size());
            this.mModuleProperties = null;
            this.mModule = null;
        } else {
            this.mModuleProperties = modules.get(0);
        }
        Looper looper = Looper.myLooper();
        looper = looper == null ? Looper.getMainLooper() : looper;
        if (looper != null) {
            this.mHandler = new Handler(looper) { // from class: com.android.server.soundtrigger.SoundTriggerHelper.1
                @Override // android.os.Handler
                public void handleMessage(Message msg) {
                    if (msg.what == 0) {
                        synchronized (SoundTriggerHelper.this.mLock) {
                            SoundTriggerHelper.this.onCallStateChangedLocked(2 == msg.arg1);
                        }
                        return;
                    }
                    Slog.e(SoundTriggerHelper.TAG, "unknown message in handler:" + msg.what);
                }
            };
        } else {
            this.mHandler = null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int startGenericRecognition(UUID modelId, SoundTrigger.GenericSoundModel soundModel, IRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig recognitionConfig) {
        MetricsLogger.count(this.mContext, "sth_start_recognition", 1);
        if (modelId == null || soundModel == null || callback == null || recognitionConfig == null) {
            Slog.w(TAG, "Passed in bad data to startGenericRecognition().");
            return Integer.MIN_VALUE;
        }
        synchronized (this.mLock) {
            ModelData modelData = getOrCreateGenericModelDataLocked(modelId);
            if (modelData == null) {
                Slog.w(TAG, "Irrecoverable error occurred, check UUID / sound model data.");
                return Integer.MIN_VALUE;
            }
            return startRecognition(soundModel, modelData, callback, recognitionConfig, Integer.MIN_VALUE);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int startKeyphraseRecognition(int keyphraseId, SoundTrigger.KeyphraseSoundModel soundModel, IRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig recognitionConfig) {
        ModelData model;
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_start_recognition", 1);
            if (soundModel != null && callback != null && recognitionConfig != null) {
                Slog.d(TAG, "startKeyphraseRecognition for keyphraseId=" + keyphraseId + " soundModel=" + soundModel + ", callback=" + callback.asBinder() + ", recognitionConfig=" + recognitionConfig);
                StringBuilder sb = new StringBuilder();
                sb.append("moduleProperties=");
                sb.append(this.mModuleProperties);
                Slog.d(TAG, sb.toString());
                dumpModelStateLocked();
                ModelData model2 = getKeyphraseModelDataLocked(keyphraseId);
                if (model2 != null && !model2.isKeyphraseModel()) {
                    Slog.e(TAG, "Generic model with same UUID exists.");
                    return Integer.MIN_VALUE;
                }
                if (model2 != null && !model2.getModelId().equals(soundModel.getUuid())) {
                    int status = cleanUpExistingKeyphraseModelLocked(model2);
                    if (status != 0) {
                        return status;
                    }
                    removeKeyphraseModelLocked(keyphraseId);
                    model2 = null;
                }
                if (model2 != null) {
                    model = model2;
                } else {
                    model = createKeyphraseModelDataLocked(soundModel.getUuid(), keyphraseId);
                }
                return startRecognition(soundModel, model, callback, recognitionConfig, keyphraseId);
            }
            return Integer.MIN_VALUE;
        }
    }

    private int cleanUpExistingKeyphraseModelLocked(ModelData modelData) {
        int status = tryStopAndUnloadLocked(modelData, true, true);
        if (status != 0) {
            Slog.w(TAG, "Unable to stop or unload previous model: " + modelData.toString());
        }
        return status;
    }

    private int prepareForRecognition(ModelData modelData) {
        if (this.mModule == null) {
            SoundTriggerModule attachModule = SoundTrigger.attachModule(this.mModuleProperties.getId(), this, (Handler) null);
            this.mModule = attachModule;
            if (attachModule == null) {
                Slog.w(TAG, "prepareForRecognition: cannot attach to sound trigger module");
                return Integer.MIN_VALUE;
            }
        }
        if (!modelData.isModelLoaded()) {
            stopAndUnloadDeadModelsLocked();
            int[] handle = {0};
            int status = this.mModule.loadSoundModel(modelData.getSoundModel(), handle);
            if (status != 0) {
                Slog.w(TAG, "prepareForRecognition: loadSoundModel failed with status: " + status);
                return status;
            }
            modelData.setHandle(handle[0]);
            modelData.setLoaded();
            Slog.d(TAG, "prepareForRecognition: Sound model loaded with handle:" + handle[0]);
        }
        return 0;
    }

    int startRecognition(SoundTrigger.SoundModel soundModel, ModelData modelData, IRecognitionStatusCallback callback, SoundTrigger.RecognitionConfig recognitionConfig, int keyphraseId) {
        int status;
        synchronized (this.mLock) {
            if (this.mModuleProperties == null) {
                Slog.w(TAG, "Attempting startRecognition without the capability");
                return Integer.MIN_VALUE;
            }
            IRecognitionStatusCallback oldCallback = modelData.getCallback();
            if (oldCallback != null && oldCallback.asBinder() != callback.asBinder()) {
                Slog.w(TAG, "Canceling previous recognition for model id: " + modelData.getModelId());
                try {
                    oldCallback.onError(Integer.MIN_VALUE);
                } catch (RemoteException e) {
                    Slog.w(TAG, "RemoteException in onDetectionStopped", e);
                }
                modelData.clearCallback();
            }
            if (modelData.getSoundModel() != null) {
                boolean stopModel = false;
                boolean unloadModel = false;
                if (modelData.getSoundModel().equals(soundModel) && modelData.isModelStarted()) {
                    stopModel = true;
                    unloadModel = false;
                } else if (!modelData.getSoundModel().equals(soundModel)) {
                    stopModel = modelData.isModelStarted();
                    unloadModel = modelData.isModelLoaded();
                }
                if ((stopModel || unloadModel) && (status = tryStopAndUnloadLocked(modelData, stopModel, unloadModel)) != 0) {
                    Slog.w(TAG, "Unable to stop or unload previous model: " + modelData.toString());
                    return status;
                }
            }
            modelData.setCallback(callback);
            modelData.setRequested(true);
            modelData.setRecognitionConfig(recognitionConfig);
            modelData.setSoundModel(soundModel);
            if (!isRecognitionAllowed()) {
                initializeTelephonyAndPowerStateListeners();
                return 0;
            }
            int status2 = prepareForRecognition(modelData);
            if (status2 != 0) {
                Slog.w(TAG, "startRecognition failed to prepare model for recognition");
                return status2;
            }
            int status3 = startRecognitionLocked(modelData, false);
            if (status3 == 0) {
                initializeTelephonyAndPowerStateListeners();
            }
            return status3;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int stopGenericRecognition(UUID modelId, IRecognitionStatusCallback callback) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_stop_recognition", 1);
            if (callback != null && modelId != null) {
                ModelData modelData = this.mModelDataMap.get(modelId);
                if (modelData != null && modelData.isGenericModel()) {
                    int status = stopRecognition(modelData, callback);
                    if (status != 0) {
                        Slog.w(TAG, "stopGenericRecognition failed: " + status);
                    }
                    return status;
                }
                Slog.w(TAG, "Attempting stopRecognition on invalid model with id:" + modelId);
                return Integer.MIN_VALUE;
            }
            Slog.e(TAG, "Null callbackreceived for stopGenericRecognition() for modelid:" + modelId);
            return Integer.MIN_VALUE;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:23:0x0086 A[Catch: all -> 0x00a2, DONT_GENERATE, TryCatch #0 {, blocks: (B:4:0x0003, B:6:0x000f, B:7:0x0025, B:9:0x0027, B:11:0x002d, B:14:0x0034, B:16:0x0064, B:19:0x006b, B:21:0x0076, B:23:0x0086, B:25:0x0088, B:27:0x008a, B:28:0x00a0), top: B:33:0x0003 }] */
    /* JADX WARN: Removed duplicated region for block: B:25:0x0088 A[Catch: all -> 0x00a2, DONT_GENERATE, TryCatch #0 {, blocks: (B:4:0x0003, B:6:0x000f, B:7:0x0025, B:9:0x0027, B:11:0x002d, B:14:0x0034, B:16:0x0064, B:19:0x006b, B:21:0x0076, B:23:0x0086, B:25:0x0088, B:27:0x008a, B:28:0x00a0), top: B:33:0x0003 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public int stopKeyphraseRecognition(int r7, android.hardware.soundtrigger.IRecognitionStatusCallback r8) {
        /*
            r6 = this;
            java.lang.Object r0 = r6.mLock
            monitor-enter(r0)
            android.content.Context r1 = r6.mContext     // Catch: java.lang.Throwable -> La2
            java.lang.String r2 = "sth_stop_recognition"
            r3 = 1
            com.android.internal.logging.MetricsLogger.count(r1, r2, r3)     // Catch: java.lang.Throwable -> La2
            r1 = -2147483648(0xffffffff80000000, float:-0.0)
            if (r8 != 0) goto L27
            java.lang.String r2 = "SoundTriggerHelper"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La2
            r3.<init>()     // Catch: java.lang.Throwable -> La2
            java.lang.String r4 = "Null callback received for stopKeyphraseRecognition() for keyphraseId:"
            r3.append(r4)     // Catch: java.lang.Throwable -> La2
            r3.append(r7)     // Catch: java.lang.Throwable -> La2
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> La2
            android.util.Slog.e(r2, r3)     // Catch: java.lang.Throwable -> La2
            monitor-exit(r0)     // Catch: java.lang.Throwable -> La2
            return r1
        L27:
            com.android.server.soundtrigger.SoundTriggerHelper$ModelData r2 = r6.getKeyphraseModelDataLocked(r7)     // Catch: java.lang.Throwable -> La2
            if (r2 == 0) goto L8a
            boolean r3 = r2.isKeyphraseModel()     // Catch: java.lang.Throwable -> La2
            if (r3 != 0) goto L34
            goto L8a
        L34:
            java.lang.String r1 = "SoundTriggerHelper"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La2
            r3.<init>()     // Catch: java.lang.Throwable -> La2
            java.lang.String r4 = "stopRecognition for keyphraseId="
            r3.append(r4)     // Catch: java.lang.Throwable -> La2
            r3.append(r7)     // Catch: java.lang.Throwable -> La2
            java.lang.String r4 = ", callback ="
            r3.append(r4)     // Catch: java.lang.Throwable -> La2
            android.os.IBinder r4 = r8.asBinder()     // Catch: java.lang.Throwable -> La2
            r3.append(r4)     // Catch: java.lang.Throwable -> La2
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> La2
            android.util.Slog.d(r1, r3)     // Catch: java.lang.Throwable -> La2
            java.lang.String r1 = "SoundTriggerHelper"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La2
            r3.<init>()     // Catch: java.lang.Throwable -> La2
            java.lang.String r4 = "current callback="
            r3.append(r4)     // Catch: java.lang.Throwable -> La2
            if (r2 == 0) goto L74
            android.hardware.soundtrigger.IRecognitionStatusCallback r4 = r2.getCallback()     // Catch: java.lang.Throwable -> La2
            if (r4 != 0) goto L6b
            goto L74
        L6b:
            android.hardware.soundtrigger.IRecognitionStatusCallback r4 = r2.getCallback()     // Catch: java.lang.Throwable -> La2
            android.os.IBinder r4 = r4.asBinder()     // Catch: java.lang.Throwable -> La2
            goto L76
        L74:
            java.lang.String r4 = "null"
        L76:
            r3.append(r4)     // Catch: java.lang.Throwable -> La2
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> La2
            android.util.Slog.d(r1, r3)     // Catch: java.lang.Throwable -> La2
            int r1 = r6.stopRecognition(r2, r8)     // Catch: java.lang.Throwable -> La2
            if (r1 == 0) goto L88
            monitor-exit(r0)     // Catch: java.lang.Throwable -> La2
            return r1
        L88:
            monitor-exit(r0)     // Catch: java.lang.Throwable -> La2
            return r1
        L8a:
            java.lang.String r3 = "SoundTriggerHelper"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> La2
            r4.<init>()     // Catch: java.lang.Throwable -> La2
            java.lang.String r5 = "No model exists for given keyphrase Id "
            r4.append(r5)     // Catch: java.lang.Throwable -> La2
            r4.append(r7)     // Catch: java.lang.Throwable -> La2
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> La2
            android.util.Slog.e(r3, r4)     // Catch: java.lang.Throwable -> La2
            monitor-exit(r0)     // Catch: java.lang.Throwable -> La2
            return r1
        La2:
            r1 = move-exception
            monitor-exit(r0)     // Catch: java.lang.Throwable -> La2
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.soundtrigger.SoundTriggerHelper.stopKeyphraseRecognition(int, android.hardware.soundtrigger.IRecognitionStatusCallback):int");
    }

    private int stopRecognition(ModelData modelData, IRecognitionStatusCallback callback) {
        synchronized (this.mLock) {
            if (callback == null) {
                return Integer.MIN_VALUE;
            }
            if (this.mModuleProperties != null && this.mModule != null) {
                IRecognitionStatusCallback currentCallback = modelData.getCallback();
                if (modelData != null && currentCallback != null && (modelData.isRequested() || modelData.isModelStarted())) {
                    if (currentCallback.asBinder() != callback.asBinder()) {
                        Slog.w(TAG, "Attempting stopRecognition for another recognition");
                        return Integer.MIN_VALUE;
                    }
                    modelData.setRequested(false);
                    int status = updateRecognitionLocked(modelData, isRecognitionAllowed(), false);
                    if (status != 0) {
                        return status;
                    }
                    modelData.setLoaded();
                    modelData.clearCallback();
                    modelData.setRecognitionConfig(null);
                    if (!computeRecognitionRequestedLocked()) {
                        internalClearGlobalStateLocked();
                    }
                    return status;
                }
                Slog.w(TAG, "Attempting stopRecognition without a successful startRecognition");
                return Integer.MIN_VALUE;
            }
            Slog.w(TAG, "Attempting stopRecognition without the capability");
            return Integer.MIN_VALUE;
        }
    }

    private int tryStopAndUnloadLocked(ModelData modelData, boolean stopModel, boolean unloadModel) {
        int status = 0;
        if (modelData.isModelNotLoaded()) {
            return 0;
        }
        if (stopModel && modelData.isModelStarted() && (status = stopRecognitionLocked(modelData, false)) != 0) {
            Slog.w(TAG, "stopRecognition failed: " + status);
            return status;
        }
        if (unloadModel && modelData.isModelLoaded()) {
            Slog.d(TAG, "Unloading previously loaded stale model.");
            SoundTriggerModule soundTriggerModule = this.mModule;
            if (soundTriggerModule == null) {
                return Integer.MIN_VALUE;
            }
            status = soundTriggerModule.unloadSoundModel(modelData.getHandle());
            MetricsLogger.count(this.mContext, "sth_unloading_stale_model", 1);
            if (status != 0) {
                Slog.w(TAG, "unloadSoundModel call failed with " + status);
            } else {
                modelData.clearState();
            }
        }
        return status;
    }

    public SoundTrigger.ModuleProperties getModuleProperties() {
        return this.mModuleProperties;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int unloadKeyphraseSoundModel(int keyphraseId) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_unload_keyphrase_sound_model", 1);
            ModelData modelData = getKeyphraseModelDataLocked(keyphraseId);
            if (this.mModule != null && modelData != null && modelData.isModelLoaded() && modelData.isKeyphraseModel()) {
                modelData.setRequested(false);
                int status = updateRecognitionLocked(modelData, isRecognitionAllowed(), false);
                if (status != 0) {
                    Slog.w(TAG, "Stop recognition failed for keyphrase ID:" + status);
                }
                int status2 = this.mModule.unloadSoundModel(modelData.getHandle());
                if (status2 != 0) {
                    Slog.w(TAG, "unloadKeyphraseSoundModel call failed with " + status2);
                }
                removeKeyphraseModelLocked(keyphraseId);
                return status2;
            }
            return Integer.MIN_VALUE;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int unloadGenericSoundModel(UUID modelId) {
        int status;
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_unload_generic_sound_model", 1);
            if (modelId != null && this.mModule != null) {
                ModelData modelData = this.mModelDataMap.get(modelId);
                if (modelData != null && modelData.isGenericModel()) {
                    if (!modelData.isModelLoaded()) {
                        Slog.i(TAG, "Unload: Given generic model is not loaded:" + modelId);
                        return 0;
                    }
                    if (modelData.isModelStarted() && (status = stopRecognitionLocked(modelData, false)) != 0) {
                        Slog.w(TAG, "stopGenericRecognition failed: " + status);
                    }
                    if (this.mModule == null) {
                        return Integer.MIN_VALUE;
                    }
                    int status2 = this.mModule.unloadSoundModel(modelData.getHandle());
                    if (status2 != 0) {
                        Slog.w(TAG, "unloadGenericSoundModel() call failed with " + status2);
                        Slog.w(TAG, "unloadGenericSoundModel() force-marking model as unloaded.");
                    }
                    this.mModelDataMap.remove(modelId);
                    dumpModelStateLocked();
                    return status2;
                }
                Slog.w(TAG, "Unload error: Attempting unload invalid generic model with id:" + modelId);
                return Integer.MIN_VALUE;
            }
            return Integer.MIN_VALUE;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public boolean isRecognitionRequested(UUID modelId) {
        boolean z;
        synchronized (this.mLock) {
            ModelData modelData = this.mModelDataMap.get(modelId);
            z = modelData != null && modelData.isRequested();
        }
        return z;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getGenericModelState(UUID modelId) {
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_get_generic_model_state", 1);
            if (modelId != null && this.mModule != null) {
                ModelData modelData = this.mModelDataMap.get(modelId);
                if (modelData != null && modelData.isGenericModel()) {
                    if (!modelData.isModelLoaded()) {
                        Slog.i(TAG, "GetGenericModelState: Given generic model is not loaded:" + modelId);
                        return Integer.MIN_VALUE;
                    } else if (!modelData.isModelStarted()) {
                        Slog.i(TAG, "GetGenericModelState: Given generic model is not started:" + modelId);
                        return Integer.MIN_VALUE;
                    } else {
                        return this.mModule.getModelState(modelData.getHandle());
                    }
                }
                Slog.w(TAG, "GetGenericModelState error: Invalid generic model id:" + modelId);
                return Integer.MIN_VALUE;
            }
            return Integer.MIN_VALUE;
        }
    }

    int getKeyphraseModelState(UUID modelId) {
        Slog.w(TAG, "GetKeyphraseModelState error: Not implemented");
        return Integer.MIN_VALUE;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int setParameter(UUID modelId, int modelParam, int value) {
        int parameterLocked;
        synchronized (this.mLock) {
            parameterLocked = setParameterLocked(this.mModelDataMap.get(modelId), modelParam, value);
        }
        return parameterLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int setKeyphraseParameter(int keyphraseId, int modelParam, int value) {
        int parameterLocked;
        synchronized (this.mLock) {
            parameterLocked = setParameterLocked(getKeyphraseModelDataLocked(keyphraseId), modelParam, value);
        }
        return parameterLocked;
    }

    private int setParameterLocked(ModelData modelData, int modelParam, int value) {
        MetricsLogger.count(this.mContext, "sth_set_parameter", 1);
        if (this.mModule == null) {
            return SoundTrigger.STATUS_NO_INIT;
        }
        if (modelData == null || !modelData.isModelLoaded()) {
            Slog.i(TAG, "SetParameter: Given model is not loaded:" + modelData);
            return SoundTrigger.STATUS_BAD_VALUE;
        }
        return this.mModule.setParameter(modelData.getHandle(), modelParam, value);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getParameter(UUID modelId, int modelParam) {
        int parameterLocked;
        synchronized (this.mLock) {
            parameterLocked = getParameterLocked(this.mModelDataMap.get(modelId), modelParam);
        }
        return parameterLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public int getKeyphraseParameter(int keyphraseId, int modelParam) {
        int parameterLocked;
        synchronized (this.mLock) {
            parameterLocked = getParameterLocked(getKeyphraseModelDataLocked(keyphraseId), modelParam);
        }
        return parameterLocked;
    }

    private int getParameterLocked(ModelData modelData, int modelParam) {
        MetricsLogger.count(this.mContext, "sth_get_parameter", 1);
        if (this.mModule == null) {
            throw new UnsupportedOperationException("SoundTriggerModule not initialized");
        }
        if (modelData == null) {
            throw new IllegalArgumentException("Invalid model id");
        }
        if (!modelData.isModelLoaded()) {
            throw new UnsupportedOperationException("Given model is not loaded:" + modelData);
        }
        return this.mModule.getParameter(modelData.getHandle(), modelParam);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SoundTrigger.ModelParamRange queryParameter(UUID modelId, int modelParam) {
        SoundTrigger.ModelParamRange queryParameterLocked;
        synchronized (this.mLock) {
            queryParameterLocked = queryParameterLocked(this.mModelDataMap.get(modelId), modelParam);
        }
        return queryParameterLocked;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SoundTrigger.ModelParamRange queryKeyphraseParameter(int keyphraseId, int modelParam) {
        SoundTrigger.ModelParamRange queryParameterLocked;
        synchronized (this.mLock) {
            queryParameterLocked = queryParameterLocked(getKeyphraseModelDataLocked(keyphraseId), modelParam);
        }
        return queryParameterLocked;
    }

    private SoundTrigger.ModelParamRange queryParameterLocked(ModelData modelData, int modelParam) {
        MetricsLogger.count(this.mContext, "sth_query_parameter", 1);
        if (this.mModule == null) {
            return null;
        }
        if (modelData == null) {
            Slog.w(TAG, "queryParameter: Invalid model id");
            return null;
        } else if (!modelData.isModelLoaded()) {
            Slog.i(TAG, "queryParameter: Given model is not loaded:" + modelData);
            return null;
        } else {
            return this.mModule.queryParameter(modelData.getHandle(), modelParam);
        }
    }

    public void onRecognition(SoundTrigger.RecognitionEvent event) {
        if (event == null) {
            Slog.w(TAG, "Null recognition event!");
        } else if (!(event instanceof SoundTrigger.KeyphraseRecognitionEvent) && !(event instanceof SoundTrigger.GenericRecognitionEvent)) {
            Slog.w(TAG, "Invalid recognition event type (not one of generic or keyphrase)!");
        } else {
            Slog.d(TAG, "onRecognition: " + event);
            synchronized (this.mLock) {
                int i = event.status;
                if (i != 0) {
                    if (i == 1) {
                        onRecognitionAbortLocked(event);
                    } else if (i == 2) {
                        onRecognitionFailureLocked();
                    } else if (i != 3) {
                    }
                }
                if (isKeyphraseRecognitionEvent(event)) {
                    onKeyphraseRecognitionSuccessLocked((SoundTrigger.KeyphraseRecognitionEvent) event);
                } else {
                    onGenericRecognitionSuccessLocked((SoundTrigger.GenericRecognitionEvent) event);
                }
            }
        }
    }

    private boolean isKeyphraseRecognitionEvent(SoundTrigger.RecognitionEvent event) {
        return event instanceof SoundTrigger.KeyphraseRecognitionEvent;
    }

    private void onGenericRecognitionSuccessLocked(SoundTrigger.GenericRecognitionEvent event) {
        MetricsLogger.count(this.mContext, "sth_generic_recognition_event", 1);
        if (event.status != 0 && event.status != 3) {
            return;
        }
        ModelData model = getModelDataForLocked(event.soundModelHandle);
        if (model == null || !model.isGenericModel()) {
            Slog.w(TAG, "Generic recognition event: Model does not exist for handle: " + event.soundModelHandle);
            return;
        }
        IRecognitionStatusCallback callback = model.getCallback();
        if (callback == null) {
            Slog.w(TAG, "Generic recognition event: Null callback for model handle: " + event.soundModelHandle);
            return;
        }
        if (event.status != 3) {
            model.setStopped();
        }
        try {
            callback.onGenericSoundTriggerDetected(event);
        } catch (DeadObjectException e) {
            forceStopAndUnloadModelLocked(model, e);
            return;
        } catch (RemoteException e2) {
            Slog.w(TAG, "RemoteException in onGenericSoundTriggerDetected", e2);
        }
        Slog.i(TAG, "Generic Recognition Success");
        SoundTrigger.RecognitionConfig config = model.getRecognitionConfig();
        if (config == null) {
            Slog.w(TAG, "Generic recognition event: Null RecognitionConfig for model handle: " + event.soundModelHandle);
            return;
        }
        model.setRequested(config.allowMultipleTriggers);
        if (model.isRequested()) {
            updateRecognitionLocked(model, isRecognitionAllowed(), true);
        }
    }

    public void onSoundModelUpdate(SoundTrigger.SoundModelEvent event) {
        if (event == null) {
            Slog.w(TAG, "Invalid sound model event!");
            return;
        }
        Slog.d(TAG, "onSoundModelUpdate: " + event);
        synchronized (this.mLock) {
            MetricsLogger.count(this.mContext, "sth_sound_model_updated", 1);
            onSoundModelUpdatedLocked(event);
        }
    }

    public void onServiceStateChange(int state) {
        Slog.d(TAG, "onServiceStateChange, state: " + state);
        synchronized (this.mLock) {
            onServiceStateChangedLocked(1 == state);
        }
    }

    public void onServiceDied() {
        Slog.e(TAG, "onServiceDied!!");
        MetricsLogger.count(this.mContext, "sth_service_died", 1);
        this.mServiceDisabled = false;
        synchronized (this.mLock) {
            onServiceDiedLocked();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onCallStateChangedLocked(boolean callActive) {
        if (this.mCallActive == callActive) {
            return;
        }
        this.mCallActive = callActive;
        updateAllRecognitionsLocked(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPowerSaveModeChangedLocked(boolean isPowerSaveMode) {
        if (this.mIsPowerSaveMode == isPowerSaveMode) {
            return;
        }
        this.mIsPowerSaveMode = isPowerSaveMode;
        if (isVivoRecognition()) {
            return;
        }
        updateAllRecognitionsLocked(true);
    }

    private void onSoundModelUpdatedLocked(SoundTrigger.SoundModelEvent event) {
    }

    private void onServiceStateChangedLocked(boolean disabled) {
        if (disabled == this.mServiceDisabled) {
            return;
        }
        this.mServiceDisabled = disabled;
        updateAllRecognitionsLocked(true);
    }

    private void onRecognitionAbortLocked(SoundTrigger.RecognitionEvent event) {
        Slog.w(TAG, "Recognition aborted");
        MetricsLogger.count(this.mContext, "sth_recognition_aborted", 1);
        ModelData modelData = getModelDataForLocked(event.soundModelHandle);
        if (modelData != null && modelData.isModelStarted()) {
            modelData.setStopped();
            try {
                modelData.getCallback().onRecognitionPaused();
            } catch (DeadObjectException e) {
                forceStopAndUnloadModelLocked(modelData, e);
            } catch (RemoteException e2) {
                Slog.w(TAG, "RemoteException in onRecognitionPaused", e2);
            }
        }
    }

    private void onRecognitionFailureLocked() {
        Slog.w(TAG, "Recognition failure");
        MetricsLogger.count(this.mContext, "sth_recognition_failure_event", 1);
        try {
            sendErrorCallbacksToAllLocked(Integer.MIN_VALUE);
        } finally {
            internalClearGlobalStateLocked();
        }
    }

    private int getKeyphraseIdFromEvent(SoundTrigger.KeyphraseRecognitionEvent event) {
        if (event == null) {
            Slog.w(TAG, "Null RecognitionEvent received.");
            return Integer.MIN_VALUE;
        }
        SoundTrigger.KeyphraseRecognitionExtra[] keyphraseExtras = event.keyphraseExtras;
        if (keyphraseExtras == null || keyphraseExtras.length == 0) {
            Slog.w(TAG, "Invalid keyphrase recognition event!");
            return Integer.MIN_VALUE;
        }
        return keyphraseExtras[0].id;
    }

    private void onKeyphraseRecognitionSuccessLocked(SoundTrigger.KeyphraseRecognitionEvent event) {
        Slog.i(TAG, "Recognition success");
        MetricsLogger.count(this.mContext, "sth_keyphrase_recognition_event", 1);
        int keyphraseId = getKeyphraseIdFromEvent(event);
        ModelData modelData = getKeyphraseModelDataLocked(keyphraseId);
        if (modelData == null || !modelData.isKeyphraseModel()) {
            Slog.e(TAG, "Keyphase model data does not exist for ID:" + keyphraseId);
        } else if (modelData.getCallback() == null) {
            Slog.w(TAG, "Received onRecognition event without callback for keyphrase model.");
        } else {
            if (event.status != 3) {
                modelData.setStopped();
            }
            try {
                modelData.getCallback().onKeyphraseDetected(event);
            } catch (DeadObjectException e) {
                forceStopAndUnloadModelLocked(modelData, e);
                return;
            } catch (RemoteException e2) {
                Slog.w(TAG, "RemoteException in onKeyphraseDetected", e2);
            }
            Slog.i(TAG, "Keyphrase Recognition Success");
            SoundTrigger.RecognitionConfig config = modelData.getRecognitionConfig();
            if (config != null) {
                modelData.setRequested(config.allowMultipleTriggers);
            }
            if (modelData.isRequested()) {
                updateRecognitionLocked(modelData, isRecognitionAllowed(), true);
            }
        }
    }

    private void updateAllRecognitionsLocked(boolean notify) {
        boolean isAllowed = isRecognitionAllowed();
        ArrayList<ModelData> modelDatas = new ArrayList<>(this.mModelDataMap.values());
        Iterator<ModelData> it = modelDatas.iterator();
        while (it.hasNext()) {
            ModelData modelData = it.next();
            updateRecognitionLocked(modelData, isAllowed, notify);
        }
    }

    private int updateRecognitionLocked(ModelData model, boolean isAllowed, boolean notify) {
        boolean start = model.isRequested() && isAllowed;
        if (start == model.isModelStarted()) {
            return 0;
        }
        if (start) {
            int status = prepareForRecognition(model);
            if (status != 0) {
                return status;
            }
            return startRecognitionLocked(model, notify);
        }
        return stopRecognitionLocked(model, notify);
    }

    private void onServiceDiedLocked() {
        try {
            MetricsLogger.count(this.mContext, "sth_service_died", 1);
            sendErrorCallbacksToAllLocked(SoundTrigger.STATUS_DEAD_OBJECT);
        } finally {
            internalClearModelStateLocked();
            internalClearGlobalStateLocked();
            SoundTriggerModule soundTriggerModule = this.mModule;
            if (soundTriggerModule != null) {
                soundTriggerModule.detach();
                this.mModule = null;
            }
        }
    }

    private void internalClearGlobalStateLocked() {
        Slog.d(TAG, "internalClearGlobalStateLocked ");
        long token = Binder.clearCallingIdentity();
        try {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
            Binder.restoreCallingIdentity(token);
            PowerSaveModeListener powerSaveModeListener = this.mPowerSaveModeListener;
            if (powerSaveModeListener != null) {
                this.mContext.unregisterReceiver(powerSaveModeListener);
                this.mPowerSaveModeListener = null;
            }
            this.mRecognitionRequested = false;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(token);
            throw th;
        }
    }

    private void internalClearModelStateLocked() {
        Slog.d(TAG, "internalClearModelStateLocked ");
        for (ModelData modelData : this.mModelDataMap.values()) {
            modelData.clearState();
        }
    }

    /* loaded from: classes2.dex */
    class MyCallStateListener extends PhoneStateListener {
        MyCallStateListener() {
        }

        @Override // android.telephony.PhoneStateListener
        public void onCallStateChanged(int state, String arg1) {
            Slog.d(SoundTriggerHelper.TAG, "onCallStateChanged: " + state);
            if (SoundTriggerHelper.this.mHandler != null) {
                synchronized (SoundTriggerHelper.this.mLock) {
                    SoundTriggerHelper.this.mHandler.removeMessages(0);
                    Message msg = SoundTriggerHelper.this.mHandler.obtainMessage(0, state, 0);
                    SoundTriggerHelper.this.mHandler.sendMessageDelayed(msg, 2 == state ? 0L : 1000L);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes2.dex */
    public class PowerSaveModeListener extends BroadcastReceiver {
        PowerSaveModeListener() {
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(intent.getAction())) {
                boolean active = SoundTriggerHelper.this.mPowerManager.getPowerSaveState(8).batterySaverEnabled;
                Slog.d(SoundTriggerHelper.TAG, "onPowerSaveModeChanged: " + active);
                synchronized (SoundTriggerHelper.this.mLock) {
                    SoundTriggerHelper.this.onPowerSaveModeChangedLocked(active);
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this.mLock) {
            pw.print("  module properties=");
            pw.println((Object) (this.mModuleProperties == null ? "null" : this.mModuleProperties));
            pw.print("  call active=");
            pw.println(this.mCallActive);
            pw.print("  power save mode active=");
            pw.println(this.mIsPowerSaveMode);
            pw.print("  service disabled=");
            pw.println(this.mServiceDisabled);
        }
    }

    private void initializeTelephonyAndPowerStateListeners() {
        if (this.mRecognitionRequested) {
            return;
        }
        long token = Binder.clearCallingIdentity();
        try {
            this.mCallActive = this.mTelephonyManager.getCallState() == 2;
            this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
            if (this.mPowerSaveModeListener == null) {
                PowerSaveModeListener powerSaveModeListener = new PowerSaveModeListener();
                this.mPowerSaveModeListener = powerSaveModeListener;
                this.mContext.registerReceiver(powerSaveModeListener, new IntentFilter("android.os.action.POWER_SAVE_MODE_CHANGED"));
            }
            this.mIsPowerSaveMode = this.mPowerManager.getPowerSaveState(8).batterySaverEnabled;
            this.mRecognitionRequested = true;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    private void sendErrorCallbacksToAllLocked(int errorCode) {
        for (ModelData modelData : this.mModelDataMap.values()) {
            IRecognitionStatusCallback callback = modelData.getCallback();
            if (callback != null) {
                try {
                    callback.onError(errorCode);
                } catch (RemoteException e) {
                    Slog.w(TAG, "RemoteException sendErrorCallbacksToAllLocked for model handle " + modelData.getHandle(), e);
                }
            }
        }
    }

    private void forceStopAndUnloadModelLocked(ModelData modelData, Exception exception) {
        forceStopAndUnloadModelLocked(modelData, exception, null);
    }

    private void forceStopAndUnloadModelLocked(ModelData modelData, Exception exception, Iterator modelDataIterator) {
        if (exception != null) {
            Slog.e(TAG, "forceStopAndUnloadModel", exception);
        }
        if (this.mModule == null) {
            return;
        }
        if (modelData.isModelStarted()) {
            Slog.d(TAG, "Stopping previously started dangling model " + modelData.getHandle());
            if (this.mModule.stopRecognition(modelData.getHandle()) != 0) {
                modelData.setStopped();
                modelData.setRequested(false);
            } else {
                Slog.e(TAG, "Failed to stop model " + modelData.getHandle());
            }
        }
        if (modelData.isModelLoaded()) {
            Slog.d(TAG, "Unloading previously loaded dangling model " + modelData.getHandle());
            if (this.mModule.unloadSoundModel(modelData.getHandle()) == 0) {
                if (modelDataIterator != null) {
                    modelDataIterator.remove();
                } else {
                    this.mModelDataMap.remove(modelData.getModelId());
                }
                Iterator it = this.mKeyphraseUuidMap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<Integer, UUID> pair = it.next();
                    if (pair.getValue().equals(modelData.getModelId())) {
                        it.remove();
                    }
                }
                modelData.clearState();
                return;
            }
            Slog.e(TAG, "Failed to unload model " + modelData.getHandle());
        }
    }

    private void stopAndUnloadDeadModelsLocked() {
        Iterator it = this.mModelDataMap.entrySet().iterator();
        while (it.hasNext()) {
            ModelData modelData = it.next().getValue();
            if (modelData.isModelLoaded() && (modelData.getCallback() == null || (modelData.getCallback().asBinder() != null && !modelData.getCallback().asBinder().pingBinder()))) {
                Slog.w(TAG, "Removing model " + modelData.getHandle() + " that has no clients");
                forceStopAndUnloadModelLocked(modelData, null, it);
            }
        }
    }

    private ModelData getOrCreateGenericModelDataLocked(UUID modelId) {
        ModelData modelData = this.mModelDataMap.get(modelId);
        if (modelData == null) {
            ModelData modelData2 = ModelData.createGenericModelData(modelId);
            this.mModelDataMap.put(modelId, modelData2);
            return modelData2;
        } else if (!modelData.isGenericModel()) {
            Slog.e(TAG, "UUID already used for non-generic model.");
            return null;
        } else {
            return modelData;
        }
    }

    private void removeKeyphraseModelLocked(int keyphraseId) {
        UUID uuid = this.mKeyphraseUuidMap.get(Integer.valueOf(keyphraseId));
        if (uuid == null) {
            return;
        }
        this.mModelDataMap.remove(uuid);
        this.mKeyphraseUuidMap.remove(Integer.valueOf(keyphraseId));
    }

    private ModelData getKeyphraseModelDataLocked(int keyphraseId) {
        UUID uuid = this.mKeyphraseUuidMap.get(Integer.valueOf(keyphraseId));
        if (uuid == null) {
            return null;
        }
        return this.mModelDataMap.get(uuid);
    }

    private ModelData createKeyphraseModelDataLocked(UUID modelId, int keyphraseId) {
        this.mKeyphraseUuidMap.remove(Integer.valueOf(keyphraseId));
        this.mModelDataMap.remove(modelId);
        this.mKeyphraseUuidMap.put(Integer.valueOf(keyphraseId), modelId);
        ModelData modelData = ModelData.createKeyphraseModelData(modelId);
        this.mModelDataMap.put(modelId, modelData);
        return modelData;
    }

    private ModelData getModelDataForLocked(int modelHandle) {
        for (ModelData model : this.mModelDataMap.values()) {
            if (model.getHandle() == modelHandle) {
                return model;
            }
        }
        return null;
    }

    private boolean isVivoRecognition() {
        HashMap<UUID, ModelData> hashMap = this.mModelDataMap;
        if (hashMap == null) {
            Slog.d(TAG, "mModelDataMap == null");
            return false;
        }
        String[] vivoUuid = {"68ab2d40-e860-11e3-95ef-0002a5d5c51b", "68ac2eb5-c2b3-3c20-8eba-0dce55ee9e47", "ec9b1092-fcdd-37ed-bf8e-eb6e7ce43240", "00000000-0000-0000-0000-4d544b5f3031", "00000000-0000-0002-0000-4d544b5f3034"};
        for (ModelData modelData : hashMap.values()) {
            UUID uuid = modelData.getModelId();
            if (uuid != null) {
                for (String str : vivoUuid) {
                    if (str.equals(uuid.toString())) {
                        Slog.d(TAG, "vivo uuid");
                        return true;
                    }
                }
                continue;
            }
        }
        Slog.d(TAG, "not vivo uuid");
        return false;
    }

    private boolean isRecognitionAllowed() {
        if (!this.mRecognitionRequested) {
            this.mCallActive = this.mTelephonyManager.getCallState() == 2;
            this.mIsPowerSaveMode = this.mPowerManager.getPowerSaveState(8).batterySaverEnabled;
        }
        Slog.i(TAG, "mCallActive=" + this.mCallActive + ",mServiceDisabled=" + this.mServiceDisabled + ",mIsPowerSaveMode=" + this.mIsPowerSaveMode);
        return isVivoRecognition() ? (this.mCallActive || this.mServiceDisabled) ? false : true : (this.mCallActive || this.mServiceDisabled || this.mIsPowerSaveMode) ? false : true;
    }

    private int startRecognitionLocked(ModelData modelData, boolean notify) {
        IRecognitionStatusCallback callback = modelData.getCallback();
        SoundTrigger.RecognitionConfig config = modelData.getRecognitionConfig();
        if (callback == null || !modelData.isModelLoaded() || config == null) {
            Slog.w(TAG, "startRecognition: Bad data passed in.");
            MetricsLogger.count(this.mContext, "sth_start_recognition_error", 1);
            return Integer.MIN_VALUE;
        } else if (!isRecognitionAllowed()) {
            Slog.w(TAG, "startRecognition requested but not allowed.");
            MetricsLogger.count(this.mContext, "sth_start_recognition_not_allowed", 1);
            return 0;
        } else {
            SoundTriggerModule soundTriggerModule = this.mModule;
            if (soundTriggerModule == null) {
                return Integer.MIN_VALUE;
            }
            int status = soundTriggerModule.startRecognition(modelData.getHandle(), config);
            if (status != 0) {
                Slog.w(TAG, "startRecognition failed with " + status);
                MetricsLogger.count(this.mContext, "sth_start_recognition_error", 1);
                if (notify) {
                    try {
                        callback.onError(status);
                    } catch (DeadObjectException e) {
                        forceStopAndUnloadModelLocked(modelData, e);
                    } catch (RemoteException e2) {
                        Slog.w(TAG, "RemoteException in onError", e2);
                    }
                }
            } else {
                Slog.i(TAG, "startRecognition successful.");
                MetricsLogger.count(this.mContext, "sth_start_recognition_success", 1);
                modelData.setStarted();
                if (notify) {
                    try {
                        callback.onRecognitionResumed();
                    } catch (DeadObjectException e3) {
                        forceStopAndUnloadModelLocked(modelData, e3);
                    } catch (RemoteException e4) {
                        Slog.w(TAG, "RemoteException in onRecognitionResumed", e4);
                    }
                }
            }
            Slog.d(TAG, "Model being started :" + modelData.toString());
            return status;
        }
    }

    private int stopRecognitionLocked(ModelData modelData, boolean notify) {
        if (this.mModule == null) {
            return Integer.MIN_VALUE;
        }
        IRecognitionStatusCallback callback = modelData.getCallback();
        int status = this.mModule.stopRecognition(modelData.getHandle());
        if (status != 0) {
            Slog.w(TAG, "stopRecognition call failed with " + status);
            MetricsLogger.count(this.mContext, "sth_stop_recognition_error", 1);
            if (notify) {
                try {
                    callback.onError(status);
                } catch (DeadObjectException e) {
                    forceStopAndUnloadModelLocked(modelData, e);
                } catch (RemoteException e2) {
                    Slog.w(TAG, "RemoteException in onError", e2);
                }
            }
        } else {
            Slog.i(TAG, "stopRecognition successful.");
            modelData.setStopped();
            MetricsLogger.count(this.mContext, "sth_stop_recognition_success", 1);
            if (notify) {
                try {
                    callback.onRecognitionPaused();
                } catch (DeadObjectException e3) {
                    forceStopAndUnloadModelLocked(modelData, e3);
                } catch (RemoteException e4) {
                    Slog.w(TAG, "RemoteException in onRecognitionPaused", e4);
                }
            }
        }
        Slog.d(TAG, "Model being stopped :" + modelData.toString());
        return status;
    }

    private void dumpModelStateLocked() {
        for (UUID modelId : this.mModelDataMap.keySet()) {
            ModelData modelData = this.mModelDataMap.get(modelId);
            Slog.i(TAG, "Model :" + modelData.toString());
        }
    }

    private boolean computeRecognitionRequestedLocked() {
        if (this.mModuleProperties == null || this.mModule == null) {
            this.mRecognitionRequested = false;
            return false;
        }
        for (ModelData modelData : this.mModelDataMap.values()) {
            if (modelData.isRequested()) {
                this.mRecognitionRequested = true;
                return true;
            }
        }
        this.mRecognitionRequested = false;
        return false;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ModelData {
        static final int MODEL_LOADED = 1;
        static final int MODEL_NOTLOADED = 0;
        static final int MODEL_STARTED = 2;
        private int mModelHandle;
        private UUID mModelId;
        private int mModelState;
        private int mModelType;
        private boolean mRequested = false;
        private IRecognitionStatusCallback mCallback = null;
        private SoundTrigger.RecognitionConfig mRecognitionConfig = null;
        private SoundTrigger.SoundModel mSoundModel = null;

        private ModelData(UUID modelId, int modelType) {
            this.mModelType = -1;
            this.mModelId = modelId;
            this.mModelType = modelType;
        }

        static ModelData createKeyphraseModelData(UUID modelId) {
            return new ModelData(modelId, 0);
        }

        static ModelData createGenericModelData(UUID modelId) {
            return new ModelData(modelId, 1);
        }

        static ModelData createModelDataOfUnknownType(UUID modelId) {
            return new ModelData(modelId, -1);
        }

        synchronized void setCallback(IRecognitionStatusCallback callback) {
            this.mCallback = callback;
        }

        synchronized IRecognitionStatusCallback getCallback() {
            return this.mCallback;
        }

        synchronized boolean isModelLoaded() {
            boolean z;
            z = true;
            if (this.mModelState != 1) {
                if (this.mModelState != 2) {
                    z = false;
                }
            }
            return z;
        }

        synchronized boolean isModelNotLoaded() {
            return this.mModelState == 0;
        }

        synchronized void setStarted() {
            this.mModelState = 2;
        }

        synchronized void setStopped() {
            this.mModelState = 1;
        }

        synchronized void setLoaded() {
            this.mModelState = 1;
        }

        synchronized boolean isModelStarted() {
            return this.mModelState == 2;
        }

        synchronized void clearState() {
            this.mModelState = 0;
            this.mRecognitionConfig = null;
            this.mRequested = false;
            this.mCallback = null;
        }

        synchronized void clearCallback() {
            this.mCallback = null;
        }

        synchronized void setHandle(int handle) {
            this.mModelHandle = handle;
        }

        synchronized void setRecognitionConfig(SoundTrigger.RecognitionConfig config) {
            this.mRecognitionConfig = config;
        }

        synchronized int getHandle() {
            return this.mModelHandle;
        }

        synchronized UUID getModelId() {
            return this.mModelId;
        }

        synchronized SoundTrigger.RecognitionConfig getRecognitionConfig() {
            return this.mRecognitionConfig;
        }

        synchronized boolean isRequested() {
            return this.mRequested;
        }

        synchronized void setRequested(boolean requested) {
            this.mRequested = requested;
        }

        synchronized void setSoundModel(SoundTrigger.SoundModel soundModel) {
            this.mSoundModel = soundModel;
        }

        synchronized SoundTrigger.SoundModel getSoundModel() {
            return this.mSoundModel;
        }

        synchronized int getModelType() {
            return this.mModelType;
        }

        synchronized boolean isKeyphraseModel() {
            return this.mModelType == 0;
        }

        synchronized boolean isGenericModel() {
            return this.mModelType == 1;
        }

        synchronized String stateToString() {
            int i = this.mModelState;
            if (i != 0) {
                if (i != 1) {
                    if (i == 2) {
                        return "STARTED";
                    }
                    return "Unknown state";
                }
                return "LOADED";
            }
            return "NOT_LOADED";
        }

        synchronized String requestedToString() {
            StringBuilder sb;
            sb = new StringBuilder();
            sb.append("Requested: ");
            sb.append(this.mRequested ? "Yes" : "No");
            return sb.toString();
        }

        synchronized String callbackToString() {
            StringBuilder sb;
            sb = new StringBuilder();
            sb.append("Callback: ");
            sb.append(this.mCallback != null ? this.mCallback.asBinder() : "null");
            return sb.toString();
        }

        synchronized String uuidToString() {
            return "UUID: " + this.mModelId;
        }

        public synchronized String toString() {
            return "Handle: " + this.mModelHandle + "\nModelState: " + stateToString() + "\n" + requestedToString() + "\n" + callbackToString() + "\n" + uuidToString() + "\n" + modelTypeToString();
        }

        synchronized String modelTypeToString() {
            String type;
            type = null;
            int i = this.mModelType;
            if (i == -1) {
                type = "Unknown";
            } else if (i == 0) {
                type = "Keyphrase";
            } else if (i == 1) {
                type = "Generic";
            }
            return "Model type: " + type + "\n";
        }
    }
}