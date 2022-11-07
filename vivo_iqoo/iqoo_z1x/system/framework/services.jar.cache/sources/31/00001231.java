package com.android.server.media;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.media.ICodecCallback;
import android.media.IMediaResourceMonitor;
import android.os.Binder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.Slog;
import com.android.server.SystemService;

/* loaded from: classes.dex */
public class MediaResourceMonitorService extends SystemService {
    private static final String SERVICE_NAME = "media_resource_monitor";
    private final MediaResourceMonitorImpl mMediaResourceMonitorImpl;
    private static final String TAG = "MediaResourceMonitor";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    public MediaResourceMonitorService(Context context) {
        super(context);
        this.mMediaResourceMonitorImpl = new MediaResourceMonitorImpl();
    }

    @Override // com.android.server.SystemService
    public void onStart() {
        publishBinderService(SERVICE_NAME, this.mMediaResourceMonitorImpl);
    }

    /* loaded from: classes.dex */
    class MediaResourceMonitorImpl extends IMediaResourceMonitor.Stub {
        RemoteCallbackList<ICodecCallback> mRegisteredCodecCallbacks = new RemoteCallbackList<>();

        MediaResourceMonitorImpl() {
        }

        public void notifyResourceGranted(int pid, int type) throws RemoteException {
            if (MediaResourceMonitorService.DEBUG) {
                Slog.d(MediaResourceMonitorService.TAG, "notifyResourceGranted(pid=" + pid + ", type=" + type + ")");
            }
            long identity = Binder.clearCallingIdentity();
            try {
                String[] pkgNames = getPackageNamesFromPid(pid);
                if (pkgNames == null) {
                    return;
                }
                UserManager manager = (UserManager) MediaResourceMonitorService.this.getContext().getSystemService("user");
                int[] userIds = manager.getEnabledProfileIds(ActivityManager.getCurrentUser());
                if (userIds != null && userIds.length != 0) {
                    Intent intent = new Intent("android.intent.action.MEDIA_RESOURCE_GRANTED");
                    intent.putExtra("android.intent.extra.PACKAGES", pkgNames);
                    intent.putExtra("android.intent.extra.MEDIA_RESOURCE_TYPE", type);
                    for (int userId : userIds) {
                        MediaResourceMonitorService.this.getContext().sendBroadcastAsUser(intent, UserHandle.of(userId), "android.permission.RECEIVE_MEDIA_RESOURCE_USAGE");
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(identity);
            }
        }

        private String[] getPackageNamesFromPid(int pid) {
            try {
                for (ActivityManager.RunningAppProcessInfo proc : ActivityManager.getService().getRunningAppProcesses()) {
                    if (proc.pid == pid) {
                        return proc.pkgList;
                    }
                }
                return null;
            } catch (RemoteException e) {
                Slog.w(MediaResourceMonitorService.TAG, "ActivityManager.getRunningAppProcesses() failed");
                return null;
            }
        }

        public void notifyStreamStart(int uid, int streamId, long timestamp) {
            sendStreamStartToCallbacks(uid, streamId, timestamp);
        }

        public void notifyStreamStop(int uid, int streamId, long timestamp) {
            sendStreamStopToCallbacks(uid, streamId, timestamp);
        }

        public void notifyFrameUpdate(int uid, int streamId, long frameId, long timestamp, boolean hasEndFlag) {
            sendFrameUpdateToCallbacks(uid, streamId, frameId, timestamp, hasEndFlag);
        }

        public void registerCodecCallback(ICodecCallback callback) {
            int uid;
            try {
                uid = callback.getUid();
            } catch (RemoteException e) {
                uid = -1;
            }
            if (uid >= 0) {
                this.mRegisteredCodecCallbacks.register(callback, Integer.valueOf(uid));
            }
        }

        public void unregisterCodecCallback(ICodecCallback callback) {
            this.mRegisteredCodecCallbacks.unregister(callback);
        }

        public void sendStreamStartToCallbacks(int uid, int streamId, long timestamp) {
            int i = this.mRegisteredCodecCallbacks.beginBroadcast();
            while (i > 0) {
                i--;
                try {
                    if (this.mRegisteredCodecCallbacks.getBroadcastCookie(i).equals(Integer.valueOf(uid))) {
                        this.mRegisteredCodecCallbacks.getBroadcastItem(i).onStreamStart(uid, streamId, timestamp);
                    }
                } catch (RemoteException e) {
                }
            }
            this.mRegisteredCodecCallbacks.finishBroadcast();
        }

        public void sendStreamStopToCallbacks(int uid, int streamId, long timestamp) {
            int i = this.mRegisteredCodecCallbacks.beginBroadcast();
            while (i > 0) {
                i--;
                try {
                    if (this.mRegisteredCodecCallbacks.getBroadcastCookie(i).equals(Integer.valueOf(uid))) {
                        this.mRegisteredCodecCallbacks.getBroadcastItem(i).onStreamEnd(uid, streamId, timestamp);
                    }
                } catch (RemoteException e) {
                }
            }
            this.mRegisteredCodecCallbacks.finishBroadcast();
        }

        public void sendFrameUpdateToCallbacks(int uid, int streamId, long frameId, long timestamp, boolean hasEndFlag) {
            int i = this.mRegisteredCodecCallbacks.beginBroadcast();
            while (i > 0) {
                int i2 = i - 1;
                try {
                    if (this.mRegisteredCodecCallbacks.getBroadcastCookie(i2).equals(Integer.valueOf(uid))) {
                        this.mRegisteredCodecCallbacks.getBroadcastItem(i2).onFrameUpdate(uid, streamId, frameId, timestamp, hasEndFlag);
                    }
                } catch (RemoteException e) {
                }
                i = i2;
            }
            this.mRegisteredCodecCallbacks.finishBroadcast();
        }
    }
}