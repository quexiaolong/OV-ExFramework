package com.vivo.services.rms.sdk.args;

import com.vivo.common.utils.VLog;
import com.vivo.services.rms.sdk.ObjectCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/* loaded from: classes.dex */
public class ArgsFactory {
    public static final String TAG = "ObjectFactory";
    public static final HashMap<String, ObjectCache<Args>> sCaches = new HashMap<>();

    public static void addClass(Class<? extends Args> clazz, int maxSize) {
        if (clazz == null) {
            return;
        }
        String className = clazz.getSimpleName();
        ObjectCache<Args> cache = sCaches.get(className);
        if (cache != null) {
            VLog.e(TAG, String.format("addClass %s fail.", className));
        } else {
            sCaches.put(className, new ObjectCache<>(clazz, maxSize));
        }
    }

    public static void recycle(Args obj) {
        if (obj == null) {
            return;
        }
        obj.recycle();
        String className = obj.getClass().getSimpleName();
        ObjectCache<Args> cache = sCaches.get(className);
        if (cache == null) {
            VLog.e(TAG, String.format("recycle %s fail.", className));
        } else {
            cache.put((ObjectCache<Args>) obj);
        }
    }

    public static void recycle(ArrayList<? extends Args> objs) {
        if (objs.isEmpty()) {
            return;
        }
        Iterator<? extends Args> it = objs.iterator();
        while (it.hasNext()) {
            Args obj = it.next();
            recycle(obj);
        }
    }

    public static Args create(String className) {
        ObjectCache<Args> cache = sCaches.get(className);
        if (cache == null) {
            VLog.e(TAG, String.format("create %s fail.", className));
            return null;
        }
        return cache.pop();
    }
}