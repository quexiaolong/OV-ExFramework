package com.android.server;

import android.content.Context;
import com.android.server.input.InputManagerService;
import com.android.server.policy.PhoneWindowManager;
import com.android.server.utils.TimingsTraceAndSlog;

/* loaded from: classes.dex */
public interface ISystemServerExt {
    default void initFontsForserializeFontMap() {
    }

    default void initSystemServer(Context systemContext) {
    }

    default void setDataNormalizationManager() {
    }

    default void addOplusDevicePolicyService() {
    }

    default void waitForFutureNoInterrupt() {
    }

    default void startBootstrapServices() {
    }

    default void startCoreServices() {
    }

    default InputManagerService getInputManagerService(Context context) {
        return new InputManagerService(context);
    }

    default PhoneWindowManager getSubPhoneWindowManager() {
        return new PhoneWindowManager();
    }

    default boolean startJobSchedulerService() {
        return false;
    }

    default void addLinearmotorVibratorService(Context context) {
    }

    default void addStorageHealthInfoService(Context context) {
    }

    default void startOtherServices() {
    }

    default void linearVibratorSystemReady() {
    }

    default void systemReady() {
    }

    default void systemRunning() {
    }

    default void startUsageStatsService(SystemServiceManager systemServiceManager) {
    }

    default void writeAgingCriticalEvent() {
    }

    default void setBootstage(boolean start) {
    }

    default void startDynamicFilterService(SystemServiceManager systemServiceManager) {
    }

    default void dynamicFilterServiceSystemReady(TimingsTraceAndSlog t) {
    }

    default void addCabcService(Context context, TimingsTraceAndSlog t) {
    }

    default void addOplusTestService(Context context) {
    }
}