package com.vivo.services.proxy;

import com.vivo.face.common.data.Constants;

/* loaded from: classes.dex */
public class DualInt {
    public int mInt1;
    public int mInt2;

    public String toString() {
        return Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK + this.mInt1 + "/" + this.mInt2;
    }
}