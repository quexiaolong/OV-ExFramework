package com.android.server.wm;

import android.graphics.Path;
import android.graphics.PointF;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class VivoBezierUtil {
    private static final int FRAME = 1000;
    private static final String TAG = "Launcher.BezierUtil";

    private static float deCasteljauX(int i, int j, float t, ArrayList<PointF> controlPoints) {
        if (i == 1) {
            return ((1.0f - t) * controlPoints.get(j).x) + (controlPoints.get(j + 1).x * t);
        }
        return ((1.0f - t) * deCasteljauX(i - 1, j, t, controlPoints)) + (deCasteljauX(i - 1, j + 1, t, controlPoints) * t);
    }

    private static float deCasteljauY(int i, int j, float t, ArrayList<PointF> controlPoints) {
        if (i == 1) {
            return ((1.0f - t) * controlPoints.get(j).y) + (controlPoints.get(j + 1).y * t);
        }
        return ((1.0f - t) * deCasteljauY(i - 1, j, t, controlPoints)) + (deCasteljauY(i - 1, j + 1, t, controlPoints) * t);
    }

    private static ArrayList<PointF> buildBezierPoints(ArrayList<PointF> controlPoints) {
        ArrayList<PointF> points = new ArrayList<>();
        int order = controlPoints.size() - 1;
        for (float t = 0.0f; t <= 1.0f; t += 0.001f) {
            points.add(new PointF(deCasteljauX(order, 0, t, controlPoints), deCasteljauY(order, 0, t, controlPoints)));
        }
        return points;
    }

    public static Path buildPath(PointF... points) {
        ArrayList<PointF> controlPoints = new ArrayList<>();
        Path bezierPath = new Path();
        controlPoints.add(new PointF(0.0f, 0.0f));
        for (PointF point : points) {
            controlPoints.add(point);
        }
        controlPoints.add(new PointF(1.0f, 1.0f));
        ArrayList<PointF> bezierPoints = buildBezierPoints(controlPoints);
        bezierPath.reset();
        bezierPath.moveTo(0.0f, 0.0f);
        Iterator<PointF> it = bezierPoints.iterator();
        while (it.hasNext()) {
            PointF point2 = it.next();
            bezierPath.lineTo(point2.x, point2.y);
        }
        bezierPath.lineTo(1.0f, 1.0f);
        return bezierPath;
    }
}