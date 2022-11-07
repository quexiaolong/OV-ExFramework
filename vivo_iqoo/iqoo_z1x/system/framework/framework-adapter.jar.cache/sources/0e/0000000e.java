package com.vivo.adapter;

import android.content.Context;
import android.os.SystemProperties;
import com.vivo.adapter.aee.AbsExceptionLogAdapter;
import com.vivo.adapter.aee.ExceptionLogAdapterImpl;
import com.vivo.adapter.cpuinfo.AbsCpuInfo;
import com.vivo.adapter.cpuinfo.CpuInfoImpl;
import com.vivo.adapter.hello.AbsHello;
import com.vivo.adapter.hello.HelloImpl;
import com.vivo.adapter.securecamera.AbsSecureCameraManager;
import com.vivo.adapter.securecamera.SecureCameraManagerImpl;

/* loaded from: classes.dex */
public class FrameworkAdapterFactoryImpl extends FrameworkAdapterFactory {
    public AbsHello getHello() {
        return new HelloImpl();
    }

    public AbsExceptionLogAdapter getExceptionLogAdapter() {
        if (SystemProperties.get("ro.vendor.have_aee_feature").equals("1")) {
            return ExceptionLogAdapterImpl.getInstance();
        }
        return null;
    }

    public AbsCpuInfo getCpuInfoImpl() {
        return new CpuInfoImpl();
    }

    public AbsSecureCameraManager getSecureCameraManagerImpl(Context context) {
        return new SecureCameraManagerImpl(context);
    }
}