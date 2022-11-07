package com.android.server;

import android.content.Context;
import android.multidisplay.MultiDisplayManager;
import android.util.ArrayMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import vivo.util.VSlog;

/* loaded from: classes.dex */
public final class VCarConfigManager {
    private static VCarConfigManager sInstance;
    private Context mContext;
    final ArrayMap<String, ArrayList<String>> mValuesMap = new ArrayMap<>();

    private VCarConfigManager() {
    }

    public static synchronized VCarConfigManager getInstance() {
        VCarConfigManager vCarConfigManager;
        synchronized (VCarConfigManager.class) {
            if (sInstance == null) {
                sInstance = new VCarConfigManager();
            }
            vCarConfigManager = sInstance;
        }
        return vCarConfigManager;
    }

    public static synchronized VCarConfigManager getInstance(Context ctx) {
        VCarConfigManager vCarConfigManager;
        synchronized (VCarConfigManager.class) {
            vCarConfigManager = getInstance();
        }
        return vCarConfigManager;
    }

    public void clear() {
        synchronized (VCarConfigManager.class) {
            this.mValuesMap.clear();
        }
    }

    public void updateConfigs(List<String> list) {
        if (!MultiDisplayManager.SUPPORT_CAR_NETWORKING) {
            return;
        }
        synchronized (VCarConfigManager.class) {
            if (list == null) {
                this.mValuesMap.clear();
                return;
            }
            this.mValuesMap.clear();
            for (String temp : list) {
                String[] parser = temp.split(":");
                if (parser != null && parser.length == 2) {
                    String type = parser[0];
                    String value = parser[1];
                    ArrayList<String> mylist = this.mValuesMap.get(type);
                    if (mylist == null) {
                        mylist = new ArrayList<>();
                        this.mValuesMap.put(type, mylist);
                    }
                    mylist.add(value);
                }
            }
            for (String key : this.mValuesMap.keySet()) {
                VSlog.d("VCar", "---------------key: " + key);
                ArrayList<String> values = this.mValuesMap.get(key);
                if (values != null) {
                    Iterator<String> it = values.iterator();
                    while (it.hasNext()) {
                        String value2 = it.next();
                        VSlog.d("VCar", "value= " + value2);
                    }
                }
            }
        }
    }

    public ArrayList<String> get(String type) {
        ArrayList<String> arrayList;
        synchronized (VCarConfigManager.class) {
            arrayList = this.mValuesMap.get(type);
        }
        return arrayList;
    }
}