package com.android.server.am.frozen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.SparseArray;
import com.android.server.am.VivoFrozenPackageSupervisor;
import com.vivo.statistics.sdk.GatherManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public class FrozenDataManager extends BroadcastReceiver implements Observer {
    static final boolean DEBUG = SystemProperties.get("persist.sys.log.ctrl", "no").equalsIgnoreCase("yes");
    private static final int LOOPER_TIME = 3600000;
    private static final int MAX_BEAN = 2;
    private static final String TAG = "frozen";
    private static final int WHAT_FROZEN = 1;
    private static final int WHAT_TRIGGER = 0;
    private static final int WHAT_UNFROZEN = 2;
    private BgHandler mHandler;

    private FrozenDataManager() {
    }

    public static FrozenDataManager getInstance() {
        return Holder.INSTANCE;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        BgHandler bgHandler = this.mHandler;
        if (bgHandler == null) {
            return;
        }
        bgHandler.trigger(0);
    }

    @Override // java.util.Observer
    public void update(Observable observable, Object o) {
        if (o instanceof FrozenDataInfo) {
            FrozenDataInfo data = (FrozenDataInfo) o;
            if (data.state == 1) {
                frozen(data);
            } else {
                unfrozen(data);
            }
        }
    }

    public void init(Context context, Looper l) {
        BgHandler bgHandler = new BgHandler(l);
        this.mHandler = bgHandler;
        bgHandler.sendEmptyMessageDelayed(0, 3600000L);
        new IntentFilter("android.intent.action.ACTION_SHUTDOWN");
        VivoFrozenPackageSupervisor.getInstance().addObserver(this);
    }

    private void frozen(FrozenDataInfo data) {
        BgHandler bgHandler = this.mHandler;
        if (bgHandler != null) {
            bgHandler.sendMessage(bgHandler.obtainMessage(1, data));
        }
    }

    private void unfrozen(FrozenDataInfo data) {
        BgHandler bgHandler = this.mHandler;
        if (bgHandler != null) {
            bgHandler.sendMessage(bgHandler.obtainMessage(2, data));
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class Holder {
        private static final FrozenDataManager INSTANCE = new FrozenDataManager();

        private Holder() {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class BgHandler extends Handler {
        private final SparseArray<HashMap<String, FrozenDataBean>> mDataBeansList;
        private FrozenQuicker mFrozenQuicker;
        private GatherManager mGatherManager;

        public BgHandler(Looper l) {
            super(l);
            this.mDataBeansList = new SparseArray<>(3);
            this.mGatherManager = GatherManager.getInstance();
            this.mFrozenQuicker = FrozenQuicker.getInstance();
            this.mDataBeansList.put(0, new HashMap<>());
            this.mDataBeansList.put(1, new HashMap<>());
            this.mDataBeansList.put(2, new HashMap<>());
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i = msg.what;
            if (i == 0) {
                VSlog.d(FrozenDataManager.TAG, "Receive msg trigger.");
                removeMessages(0);
                sendEmptyMessageDelayed(0, 3600000L);
                trigger(2);
            } else if (i == 1) {
                FrozenDataInfo data = (FrozenDataInfo) msg.obj;
                int uid = data.uid;
                int caller = data.caller;
                String pkg = data.pkgName;
                HashMap<String, FrozenDataBean> map = this.mDataBeansList.get(caller);
                if (FrozenDataManager.DEBUG) {
                    VSlog.d(FrozenDataManager.TAG, "Receive msg frozen, data: " + data.toString());
                }
                if (map == null) {
                    return;
                }
                FrozenDataBean bean = map.get(pkg);
                long bgBeginTime = this.mFrozenQuicker.getBeginTime(uid, pkg);
                if (bean == null) {
                    map.put(pkg, new FrozenDataBean(uid, pkg, caller, bgBeginTime));
                } else {
                    bean.updateFrozenTime(bgBeginTime);
                }
            } else if (i == 2) {
                FrozenDataInfo data2 = (FrozenDataInfo) msg.obj;
                HashMap<String, FrozenDataBean> map2 = this.mDataBeansList.get(data2.caller);
                if (FrozenDataManager.DEBUG) {
                    VSlog.d(FrozenDataManager.TAG, "Receive msg unfrozen, data: " + data2.toString());
                }
                if (map2 == null) {
                    return;
                }
                FrozenDataBean bean2 = map2.get(data2.pkgName);
                if (bean2 == null) {
                    VSlog.d(FrozenDataManager.TAG, "Receive msg unfrozen, not send data pkgName = " + data2.pkgName);
                    return;
                }
                bean2.unfrozen(data2.unfrozenReason);
                sendEmptyMessage(0);
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void trigger(int threshold) {
            for (int i = 0; i < this.mDataBeansList.size(); i++) {
                HashMap<String, FrozenDataBean> beans = this.mDataBeansList.valueAt(i);
                VSlog.d(FrozenDataManager.TAG, "Frozen from " + FrozenDataInfo.convertCaller(i) + ", total size = " + beans.size() + ", threshold = " + threshold);
                if (beans.size() > threshold) {
                    ArrayList<String> list = new ArrayList<>(beans.size());
                    Iterator<Map.Entry<String, FrozenDataBean>> it = beans.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, FrozenDataBean> entry = it.next();
                        FrozenDataBean bean = entry.getValue();
                        if (bean.canUpload()) {
                            list.add(bean.toJSONString());
                            it.remove();
                        } else {
                            boolean z = FrozenDataManager.DEBUG;
                        }
                    }
                    VSlog.d(FrozenDataManager.TAG, "trigger size = " + list.size() + ", surplus size = " + beans.size());
                    if (list.size() > 0) {
                        this.mGatherManager.gather(FrozenDataManager.TAG, new Object[]{list});
                    }
                }
            }
        }
    }
}