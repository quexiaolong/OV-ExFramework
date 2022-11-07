package com.android.server.wm.utils;

import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.RegionIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/* loaded from: classes2.dex */
public class RegionUtils {
    private RegionUtils() {
    }

    public static void rectListToRegion(List<Rect> rects, Region outRegion) {
        outRegion.setEmpty();
        int n = rects.size();
        for (int i = 0; i < n; i++) {
            outRegion.union(rects.get(i));
        }
    }

    public static void forEachRect(Region region, Consumer<Rect> rectConsumer) {
        RegionIterator it = new RegionIterator(region);
        Rect rect = new Rect();
        while (it.next(rect)) {
            rectConsumer.accept(rect);
        }
    }

    public static void forEachRectReverse(Region region, Consumer<Rect> rectConsumer) {
        RegionIterator it = new RegionIterator(region);
        ArrayList<Rect> rects = new ArrayList<>();
        Rect rect = new Rect();
        while (it.next(rect)) {
            rects.add(new Rect(rect));
        }
        Collections.reverse(rects);
        rects.forEach(rectConsumer);
    }
}