package com.android.server.audio;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.PrintWriterPrinter;
import com.android.internal.util.XmlUtils;
import com.android.server.audio.AudioEventLogger;
import com.vivo.common.utils.VLog;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class SoundEffectsHelper {
    private static final String ASSET_FILE_VERSION = "1.0";
    private static final String ATTR_ASSET_FILE = "file";
    private static final String ATTR_ASSET_ID = "id";
    private static final String ATTR_GROUP_NAME = "name";
    private static final String ATTR_VERSION = "version";
    private static final int EFFECT_NOT_IN_SOUND_POOL = 0;
    private static final String GROUP_TOUCH_SOUNDS = "touch_sounds";
    private static final int MSG_LOAD_EFFECTS = 0;
    private static final int MSG_LOAD_EFFECTS_TIMEOUT = 3;
    private static final int MSG_PLAY_EFFECT = 2;
    private static final int MSG_UNLOAD_EFFECTS = 1;
    private static final int NUM_SOUNDPOOL_CHANNELS = 4;
    private static final int SOUND_EFFECTS_LOAD_TIMEOUT_MS = 15000;
    private static final String SOUND_EFFECTS_PATH = "/media/audio/ui/";
    private static final String TAG = "AS.SfxHelper";
    private static final String TAG_ASSET = "asset";
    private static final String TAG_AUDIO_ASSETS = "audio_assets";
    private static final String TAG_GROUP = "group";
    private final Context mContext;
    private final int mSfxAttenuationDb;
    private SfxHandler mSfxHandler;
    private SfxWorker mSfxWorker;
    private SoundPool mSoundPool;
    private SoundPoolLoader mSoundPoolLoader;
    private final AudioEventLogger mSfxLogger = new AudioEventLogger(20, "Sound Effects Loading");
    private final List<Resource> mResources = new ArrayList();
    private final int[] mEffects = new int[10];

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnEffectsLoadCompleteHandler {
        void run(boolean z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class Resource {
        final String mFileName;
        boolean mLoaded;
        int mSampleId = 0;

        Resource(String fileName) {
            this.mFileName = fileName;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SoundEffectsHelper(Context context) {
        this.mContext = context;
        this.mSfxAttenuationDb = context.getResources().getInteger(17694907);
        startWorker();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void loadSoundEffects(OnEffectsLoadCompleteHandler onComplete) {
        sendMsg(0, 0, 0, onComplete, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void unloadSoundEffects() {
        sendMsg(1, 0, 0, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void playSoundEffect(int effect, int volume) {
        sendMsg(2, effect, volume, null, 0);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void dump(PrintWriter pw, String prefix) {
        if (this.mSfxHandler != null) {
            pw.println(prefix + "Message handler (watch for unhandled messages):");
            this.mSfxHandler.dump(new PrintWriterPrinter(pw), "  ");
        } else {
            pw.println(prefix + "Message handler is null");
        }
        pw.println(prefix + "Default attenuation (dB): " + this.mSfxAttenuationDb);
        this.mSfxLogger.dump(pw);
    }

    private void startWorker() {
        SfxWorker sfxWorker = new SfxWorker();
        this.mSfxWorker = sfxWorker;
        sfxWorker.start();
        synchronized (this) {
            while (this.mSfxHandler == null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Log.w(TAG, "Interrupted while waiting " + this.mSfxWorker.getName() + " to start");
                }
            }
        }
    }

    private void sendMsg(int msg, int arg1, int arg2, Object obj, int delayMs) {
        SfxHandler sfxHandler = this.mSfxHandler;
        sfxHandler.sendMessageDelayed(sfxHandler.obtainMessage(msg, arg1, arg2, obj), delayMs);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void logEvent(String msg) {
        this.mSfxLogger.log(new AudioEventLogger.StringEvent(msg));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onLoadSoundEffects(OnEffectsLoadCompleteHandler onComplete) {
        synchronized (this) {
            if (this.mSoundPoolLoader != null) {
                this.mSoundPoolLoader.addHandler(onComplete);
            } else if (this.mSoundPool != null) {
                if (onComplete != null) {
                    onComplete.run(true);
                }
            } else {
                logEvent("effects loading started");
                this.mSoundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder().setUsage(13).setContentType(4).build()).build();
                loadTouchSoundAssets();
                SoundPoolLoader soundPoolLoader = new SoundPoolLoader();
                this.mSoundPoolLoader = soundPoolLoader;
                soundPoolLoader.addHandler(new OnEffectsLoadCompleteHandler() { // from class: com.android.server.audio.SoundEffectsHelper.1
                    @Override // com.android.server.audio.SoundEffectsHelper.OnEffectsLoadCompleteHandler
                    public void run(boolean success) {
                        SoundEffectsHelper.this.mSoundPoolLoader = null;
                        if (!success) {
                            Log.w(SoundEffectsHelper.TAG, "onLoadSoundEffects(), Error while loading samples");
                            SoundEffectsHelper.this.onUnloadSoundEffects();
                        }
                    }
                });
                this.mSoundPoolLoader.addHandler(onComplete);
                int resourcesToLoad = 0;
                for (Resource res : this.mResources) {
                    String filePath = getResourceFilePath(res);
                    int sampleId = this.mSoundPool.load(filePath, 0);
                    if (sampleId > 0) {
                        res.mSampleId = sampleId;
                        res.mLoaded = false;
                        resourcesToLoad++;
                    } else {
                        logEvent("effect " + filePath + " rejected by SoundPool");
                        StringBuilder sb = new StringBuilder();
                        sb.append("SoundPool could not load file: ");
                        sb.append(filePath);
                        Log.w(TAG, sb.toString());
                    }
                }
                if (resourcesToLoad > 0) {
                    sendMsg(3, 0, 0, null, 15000);
                } else {
                    logEvent("effects loading completed, no effects to load");
                    this.mSoundPoolLoader.onComplete(true);
                }
            }
        }
    }

    void onUnloadSoundEffects() {
        synchronized (this) {
            if (this.mSoundPool == null) {
                return;
            }
            if (this.mSoundPoolLoader != null) {
                this.mSoundPoolLoader.addHandler(new OnEffectsLoadCompleteHandler() { // from class: com.android.server.audio.SoundEffectsHelper.2
                    @Override // com.android.server.audio.SoundEffectsHelper.OnEffectsLoadCompleteHandler
                    public void run(boolean success) {
                        SoundEffectsHelper.this.onUnloadSoundEffects();
                    }
                });
            }
            logEvent("effects unloading started");
            for (Resource res : this.mResources) {
                if (res.mSampleId != 0) {
                    this.mSoundPool.unload(res.mSampleId);
                }
            }
            this.mSoundPool.release();
            this.mSoundPool = null;
            logEvent("effects unloading completed");
        }
    }

    void onPlaySoundEffect(int effect, int volume) {
        float volFloat;
        if (this.mSoundPool == null) {
            return;
        }
        if (effect >= 10 || effect < 0) {
            Log.w(TAG, "AudioService effectType value " + effect + " out of range");
            return;
        }
        if (volume < 0) {
            volFloat = 1.0f;
        } else {
            float volFloat2 = volume;
            volFloat = volFloat2 / 1000.0f;
        }
        int[] iArr = this.mEffects;
        if (effect >= iArr.length) {
            VLog.e(TAG, "onPlaySoundEffect fail!!! effect:" + effect + " mEffects.length:" + this.mEffects.length);
            Exception e = new Exception("Effect ArrayIndexOutOfBoundsException");
            e.printStackTrace();
            return;
        }
        Resource res = this.mResources.get(iArr[effect]);
        if (this.mSoundPool != null && res.mSampleId != 0 && res.mLoaded) {
            this.mSoundPool.play(res.mSampleId, volFloat, volFloat, 0, 0, 1.0f);
            return;
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            String filePath = getResourceFilePath(res);
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.setAudioStreamType(1);
            mediaPlayer.prepare();
            mediaPlayer.setVolume(volFloat);
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { // from class: com.android.server.audio.SoundEffectsHelper.3
                @Override // android.media.MediaPlayer.OnCompletionListener
                public void onCompletion(MediaPlayer mp) {
                    SoundEffectsHelper.cleanupPlayer(mp);
                }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() { // from class: com.android.server.audio.SoundEffectsHelper.4
                @Override // android.media.MediaPlayer.OnErrorListener
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    SoundEffectsHelper.cleanupPlayer(mp);
                    return true;
                }
            });
            mediaPlayer.start();
        } catch (IOException ex) {
            Log.w(TAG, "MediaPlayer IOException: " + ex);
        } catch (IllegalArgumentException ex2) {
            Log.w(TAG, "MediaPlayer IllegalArgumentException: " + ex2);
        } catch (IllegalStateException ex3) {
            Log.w(TAG, "MediaPlayer IllegalStateException: " + ex3);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void cleanupPlayer(MediaPlayer mp) {
        if (mp != null) {
            try {
                mp.stop();
                mp.release();
            } catch (IllegalStateException ex) {
                Log.w(TAG, "MediaPlayer IllegalStateException: " + ex);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public String getResourceFilePath(Resource res) {
        String filePath = Environment.getProductDirectory() + SOUND_EFFECTS_PATH + res.mFileName;
        if (!new File(filePath).isFile()) {
            return Environment.getRootDirectory() + SOUND_EFFECTS_PATH + res.mFileName;
        }
        return filePath;
    }

    private void loadTouchSoundAssetDefaults() {
        int defaultResourceIdx = this.mResources.size();
        this.mResources.add(new Resource("Effect_Tick.ogg"));
        int i = 0;
        while (true) {
            int[] iArr = this.mEffects;
            if (i < iArr.length) {
                iArr[i] = defaultResourceIdx;
                i++;
            } else {
                return;
            }
        }
    }

    private void loadTouchSoundAssets() {
        XmlResourceParser parser = null;
        if (this.mResources.isEmpty()) {
            loadTouchSoundAssetDefaults();
            try {
                try {
                    try {
                        parser = this.mContext.getResources().getXml(18284545);
                        XmlUtils.beginDocument(parser, TAG_AUDIO_ASSETS);
                        String version = parser.getAttributeValue(null, ATTR_VERSION);
                        boolean inTouchSoundsGroup = false;
                        if (ASSET_FILE_VERSION.equals(version)) {
                            while (true) {
                                XmlUtils.nextElement(parser);
                                String element = parser.getName();
                                if (element == null) {
                                    break;
                                } else if (element.equals(TAG_GROUP)) {
                                    String name = parser.getAttributeValue(null, "name");
                                    if (GROUP_TOUCH_SOUNDS.equals(name)) {
                                        inTouchSoundsGroup = true;
                                        break;
                                    }
                                }
                            }
                            while (inTouchSoundsGroup) {
                                XmlUtils.nextElement(parser);
                                String element2 = parser.getName();
                                if (element2 == null || !element2.equals(TAG_ASSET)) {
                                    break;
                                }
                                String id = parser.getAttributeValue(null, ATTR_ASSET_ID);
                                String file = parser.getAttributeValue(null, ATTR_ASSET_FILE);
                                try {
                                    Field field = AudioManager.class.getField(id);
                                    int fx = field.getInt(null);
                                    this.mEffects[fx] = findOrAddResourceByFileName(file);
                                } catch (Exception e) {
                                    Log.w(TAG, "Invalid touch sound ID: " + id);
                                }
                            }
                        }
                        if (parser == null) {
                            return;
                        }
                    } catch (Resources.NotFoundException e2) {
                        Log.w(TAG, "audio assets file not found", e2);
                        if (parser == null) {
                            return;
                        }
                    } catch (IOException e3) {
                        Log.w(TAG, "I/O exception reading touch sound assets", e3);
                        if (parser == null) {
                            return;
                        }
                    }
                } catch (XmlPullParserException e4) {
                    Log.w(TAG, "XML parser exception reading touch sound assets", e4);
                    if (parser == null) {
                        return;
                    }
                }
                parser.close();
            } catch (Throwable th) {
                if (parser != null) {
                    parser.close();
                }
                throw th;
            }
        }
    }

    private int findOrAddResourceByFileName(String fileName) {
        for (int i = 0; i < this.mResources.size(); i++) {
            if (this.mResources.get(i).mFileName.equals(fileName)) {
                return i;
            }
        }
        int result = this.mResources.size();
        this.mResources.add(new Resource(fileName));
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public Resource findResourceBySampleId(int sampleId) {
        for (Resource res : this.mResources) {
            if (res.mSampleId == sampleId) {
                return res;
            }
        }
        return null;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SfxWorker extends Thread {
        SfxWorker() {
            super("AS.SfxWorker");
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            Looper.prepare();
            synchronized (SoundEffectsHelper.this) {
                SoundEffectsHelper.this.mSfxHandler = new SfxHandler();
                SoundEffectsHelper.this.notify();
            }
            Looper.loop();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SfxHandler extends Handler {
        private SfxHandler() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                SoundEffectsHelper.this.onLoadSoundEffects((OnEffectsLoadCompleteHandler) msg.obj);
            } else if (i == 1) {
                SoundEffectsHelper.this.onUnloadSoundEffects();
            } else if (i != 2) {
                if (i == 3 && SoundEffectsHelper.this.mSoundPoolLoader != null) {
                    SoundEffectsHelper.this.mSoundPoolLoader.onTimeout();
                }
            } else {
                final int effect = msg.arg1;
                final int volume = msg.arg2;
                SoundEffectsHelper.this.onLoadSoundEffects(new OnEffectsLoadCompleteHandler() { // from class: com.android.server.audio.SoundEffectsHelper.SfxHandler.1
                    @Override // com.android.server.audio.SoundEffectsHelper.OnEffectsLoadCompleteHandler
                    public void run(boolean success) {
                        if (success) {
                            SoundEffectsHelper.this.onPlaySoundEffect(effect, volume);
                        }
                    }
                });
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SoundPoolLoader implements SoundPool.OnLoadCompleteListener {
        private List<OnEffectsLoadCompleteHandler> mLoadCompleteHandlers = new ArrayList();

        SoundPoolLoader() {
            SoundEffectsHelper.this.mSoundPool.setOnLoadCompleteListener(this);
        }

        void addHandler(OnEffectsLoadCompleteHandler handler) {
            if (handler != null) {
                this.mLoadCompleteHandlers.add(handler);
            }
        }

        @Override // android.media.SoundPool.OnLoadCompleteListener
        public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
            if (status != 0) {
                Resource res = SoundEffectsHelper.this.findResourceBySampleId(sampleId);
                String filePath = res != null ? SoundEffectsHelper.this.getResourceFilePath(res) : "with unknown sample ID " + sampleId;
                SoundEffectsHelper.this.logEvent("effect " + filePath + " loading failed, status " + status);
                Log.w(SoundEffectsHelper.TAG, "onLoadSoundEffects(), Error " + status + " while loading sample " + filePath);
                onComplete(false);
                return;
            }
            int remainingToLoad = 0;
            for (Resource res2 : SoundEffectsHelper.this.mResources) {
                if (res2.mSampleId == sampleId && !res2.mLoaded) {
                    SoundEffectsHelper.this.logEvent("effect " + res2.mFileName + " loaded");
                    res2.mLoaded = true;
                }
                if (res2.mSampleId != 0 && !res2.mLoaded) {
                    remainingToLoad++;
                }
            }
            if (remainingToLoad == 0) {
                onComplete(true);
            }
        }

        void onTimeout() {
            onComplete(false);
        }

        void onComplete(boolean success) {
            if (SoundEffectsHelper.this.mSoundPool != null) {
                SoundEffectsHelper.this.mSoundPool.setOnLoadCompleteListener(null);
            }
            for (OnEffectsLoadCompleteHandler handler : this.mLoadCompleteHandlers) {
                handler.run(success);
            }
            SoundEffectsHelper soundEffectsHelper = SoundEffectsHelper.this;
            StringBuilder sb = new StringBuilder();
            sb.append("effects loading ");
            sb.append(success ? "completed" : "failed");
            soundEffectsHelper.logEvent(sb.toString());
        }
    }
}