package com.android.server.media;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.IntArray;
import android.util.Log;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class AudioPlayerStateMonitor {
    private static boolean DEBUG = true;
    private static String TAG = "AudioPlayerStateMonitor";
    private static AudioPlayerStateMonitor sInstance;
    private final Object mLock = new Object();
    private final Map<OnAudioPlayerActiveStateChangedListener, MessageHandler> mListenerMap = new ArrayMap();
    final Set<Integer> mActiveAudioUids = new ArraySet();
    ArrayMap<Integer, AudioPlaybackConfiguration> mPrevActiveAudioPlaybackConfigs = new ArrayMap<>();
    final IntArray mSortedAudioPlaybackClientUids = new IntArray();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public interface OnAudioPlayerActiveStateChangedListener {
        void onAudioPlayerActiveStateChanged(AudioPlaybackConfiguration audioPlaybackConfiguration, boolean z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static final class MessageHandler extends Handler {
        private static final int MSG_AUDIO_PLAYER_ACTIVE_STATE_CHANGED = 1;
        private final OnAudioPlayerActiveStateChangedListener mListener;

        MessageHandler(Looper looper, OnAudioPlayerActiveStateChangedListener listener) {
            super(looper);
            this.mListener = listener;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                this.mListener.onAudioPlayerActiveStateChanged((AudioPlaybackConfiguration) msg.obj, msg.arg1 != 0);
            }
        }

        void sendAudioPlayerActiveStateChangedMessage(AudioPlaybackConfiguration config, boolean isRemoved) {
            obtainMessage(1, isRemoved ? 1 : 0, 0, config).sendToTarget();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static AudioPlayerStateMonitor getInstance(Context context) {
        AudioPlayerStateMonitor audioPlayerStateMonitor;
        synchronized (AudioPlayerStateMonitor.class) {
            if (sInstance == null) {
                sInstance = new AudioPlayerStateMonitor(context);
            }
            audioPlayerStateMonitor = sInstance;
        }
        return audioPlayerStateMonitor;
    }

    private AudioPlayerStateMonitor(Context context) {
        AudioManager am = (AudioManager) context.getSystemService("audio");
        am.registerAudioPlaybackCallback(new AudioManagerPlaybackListener(), null);
    }

    public void registerListener(OnAudioPlayerActiveStateChangedListener listener, Handler handler) {
        synchronized (this.mLock) {
            this.mListenerMap.put(listener, new MessageHandler(handler == null ? Looper.myLooper() : handler.getLooper(), listener));
        }
    }

    public void unregisterListener(OnAudioPlayerActiveStateChangedListener listener) {
        synchronized (this.mLock) {
            this.mListenerMap.remove(listener);
        }
    }

    public IntArray getSortedAudioPlaybackClientUids() {
        IntArray sortedAudioPlaybackClientUids = new IntArray();
        synchronized (this.mLock) {
            sortedAudioPlaybackClientUids.addAll(this.mSortedAudioPlaybackClientUids);
        }
        return sortedAudioPlaybackClientUids;
    }

    public boolean isPlaybackActive(int uid) {
        boolean contains;
        synchronized (this.mLock) {
            contains = this.mActiveAudioUids.contains(Integer.valueOf(uid));
        }
        return contains;
    }

    public void cleanUpAudioPlaybackUids(int mediaButtonSessionUid) {
        synchronized (this.mLock) {
            int userId = UserHandle.getUserHandleForUid(mediaButtonSessionUid).getIdentifier();
            for (int i = this.mSortedAudioPlaybackClientUids.size() - 1; i >= 0 && this.mSortedAudioPlaybackClientUids.get(i) != mediaButtonSessionUid; i--) {
                int uid = this.mSortedAudioPlaybackClientUids.get(i);
                if (userId == UserHandle.getUserHandleForUid(uid).getIdentifier() && !isPlaybackActive(uid)) {
                    this.mSortedAudioPlaybackClientUids.remove(i);
                }
            }
        }
    }

    public void dump(Context context, PrintWriter pw, String prefix) {
        synchronized (this.mLock) {
            pw.println(prefix + "Audio playback (lastly played comes first)");
            String indent = prefix + "  ";
            for (int i = 0; i < this.mSortedAudioPlaybackClientUids.size(); i++) {
                int uid = this.mSortedAudioPlaybackClientUids.get(i);
                pw.print(indent + "uid=" + uid + " packages=");
                String[] packages = context.getPackageManager().getPackagesForUid(uid);
                if (packages != null && packages.length > 0) {
                    for (int j = 0; j < packages.length; j++) {
                        pw.print(packages[j] + " ");
                    }
                }
                pw.println();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void sendAudioPlayerActiveStateChangedMessageLocked(AudioPlaybackConfiguration config, boolean isRemoved) {
        for (MessageHandler messageHandler : this.mListenerMap.values()) {
            messageHandler.sendAudioPlayerActiveStateChangedMessage(config, isRemoved);
        }
    }

    /* loaded from: classes.dex */
    private class AudioManagerPlaybackListener extends AudioManager.AudioPlaybackCallback {
        private AudioManagerPlaybackListener() {
        }

        @Override // android.media.AudioManager.AudioPlaybackCallback
        public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
            synchronized (AudioPlayerStateMonitor.this.mLock) {
                AudioPlayerStateMonitor.this.mActiveAudioUids.clear();
                ArrayMap<Integer, AudioPlaybackConfiguration> activeAudioPlaybackConfigs = new ArrayMap<>();
                for (AudioPlaybackConfiguration config : configs) {
                    if (config.isActive()) {
                        AudioPlayerStateMonitor.this.mActiveAudioUids.add(Integer.valueOf(config.getClientUid()));
                        activeAudioPlaybackConfigs.put(Integer.valueOf(config.getPlayerInterfaceId()), config);
                    }
                }
                for (int i = 0; i < activeAudioPlaybackConfigs.size(); i++) {
                    AudioPlaybackConfiguration config2 = activeAudioPlaybackConfigs.valueAt(i);
                    int uid = config2.getClientUid();
                    if (!AudioPlayerStateMonitor.this.mPrevActiveAudioPlaybackConfigs.containsKey(Integer.valueOf(config2.getPlayerInterfaceId()))) {
                        if (AudioPlayerStateMonitor.DEBUG) {
                            Log.d(AudioPlayerStateMonitor.TAG, "Found a new active media playback. " + config2);
                        }
                        int index = AudioPlayerStateMonitor.this.mSortedAudioPlaybackClientUids.indexOf(uid);
                        if (index != 0) {
                            if (index > 0) {
                                AudioPlayerStateMonitor.this.mSortedAudioPlaybackClientUids.remove(index);
                            }
                            AudioPlayerStateMonitor.this.mSortedAudioPlaybackClientUids.add(0, uid);
                        }
                    }
                }
                if (AudioPlayerStateMonitor.this.mActiveAudioUids.size() > 0 && !AudioPlayerStateMonitor.this.mActiveAudioUids.contains(Integer.valueOf(AudioPlayerStateMonitor.this.mSortedAudioPlaybackClientUids.get(0)))) {
                    int firstActiveUid = -1;
                    int firatActiveUidIndex = -1;
                    int i2 = 1;
                    while (true) {
                        if (i2 >= AudioPlayerStateMonitor.this.mSortedAudioPlaybackClientUids.size()) {
                            break;
                        }
                        int uid2 = AudioPlayerStateMonitor.this.mSortedAudioPlaybackClientUids.get(i2);
                        if (!AudioPlayerStateMonitor.this.mActiveAudioUids.contains(Integer.valueOf(uid2))) {
                            i2++;
                        } else {
                            firatActiveUidIndex = i2;
                            firstActiveUid = uid2;
                            break;
                        }
                    }
                    for (int i3 = firatActiveUidIndex; i3 > 0; i3--) {
                        AudioPlayerStateMonitor.this.mSortedAudioPlaybackClientUids.set(i3, AudioPlayerStateMonitor.this.mSortedAudioPlaybackClientUids.get(i3 - 1));
                    }
                    AudioPlayerStateMonitor.this.mSortedAudioPlaybackClientUids.set(0, firstActiveUid);
                }
                Iterator<AudioPlaybackConfiguration> it = configs.iterator();
                while (true) {
                    boolean wasActive = true;
                    if (!it.hasNext()) {
                        break;
                    }
                    AudioPlaybackConfiguration config3 = it.next();
                    int pii = config3.getPlayerInterfaceId();
                    if (AudioPlayerStateMonitor.this.mPrevActiveAudioPlaybackConfigs.remove(Integer.valueOf(pii)) == null) {
                        wasActive = false;
                    }
                    if (wasActive != config3.isActive()) {
                        AudioPlayerStateMonitor.this.sendAudioPlayerActiveStateChangedMessageLocked(config3, false);
                    }
                }
                for (AudioPlaybackConfiguration config4 : AudioPlayerStateMonitor.this.mPrevActiveAudioPlaybackConfigs.values()) {
                    AudioPlayerStateMonitor.this.sendAudioPlayerActiveStateChangedMessageLocked(config4, true);
                }
                AudioPlayerStateMonitor.this.mPrevActiveAudioPlaybackConfigs = activeAudioPlaybackConfigs;
            }
        }
    }
}