package com.android.server.networktime;

import android.app.AlarmManager;
import android.app.AppGlobals;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.net.Network;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Time;
import android.timezone.CountryTimeZones;
import android.timezone.TimeZoneFinder;
import android.util.NtpTrustedTime;
import com.android.server.NetworkTimeUpdateService;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import vivo.app.VivoFrameworkFactory;
import vivo.app.vgc.AbsVivoVgcManager;

/* loaded from: classes.dex */
public class VivoNetworkTimeUpdateServiceImpl implements IVivoNetworkTimeUpdateService {
    private static final String DECRYPT_STATE = "trigger_restart_framework";
    private static final String TAG = "VivoNetworkTimeUpdateServiceImpl";
    private static final int UNIX_YEAR = 1970;
    private AlarmManager alarmManager;
    private Context mContext;
    private NetworkTimeUpdateService networktimeUpdateService;
    private NtpTrustedTime ntpTrustedTime;
    private TimeZoneFinder timeZoneFinder;

    public VivoNetworkTimeUpdateServiceImpl(NetworkTimeUpdateService networktimeUpService, NtpTrustedTime ntpTime, AlarmManager alarmMgr, Context context) {
        this.networktimeUpdateService = networktimeUpService;
        this.ntpTrustedTime = ntpTime;
        this.alarmManager = alarmMgr;
        this.mContext = context;
    }

    public void initDefaultTime() {
        boolean isRtcReset = false;
        boolean isFirstBoot = false;
        try {
            boolean z = true;
            if (Calendar.getInstance().get(1) != UNIX_YEAR) {
                z = false;
            }
            isRtcReset = z;
            IPackageManager pm = AppGlobals.getPackageManager();
            isFirstBoot = pm.isFirstBoot();
            VLog.i(TAG, "isRtcReset: " + isRtcReset + " isFirstBoot: " + isFirstBoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isFirstBoot || isRtcReset) {
            this.timeZoneFinder = TimeZoneFinder.getInstance();
            setDefaultTime();
        }
        setSecureNtp();
    }

    private void setDefaultTime() {
        CountryTimeZones countryTimeZones = this.timeZoneFinder.lookupCountryTimeZones(getCountryCode().toLowerCase());
        if (countryTimeZones == null) {
            VLog.i(TAG, "CountryTimeZones is empty, just set Asia/Shanghai");
            setTime("Asia/Shanghai");
            return;
        }
        setTime(countryTimeZones.getDefaultTimeZoneId());
    }

    private String getCountryCode() {
        char c;
        String countryCode = SystemProperties.get("ro.product.customize.bbk", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).toUpperCase();
        VLog.i(TAG, "originCode == " + countryCode);
        String defaultCode = Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK;
        int hashCode = countryCode.hashCode();
        if (hashCode == 64717) {
            if (countryCode.equals("AFR")) {
                c = 2;
            }
            c = 65535;
        } else if (hashCode != 68513) {
            if (hashCode == 76174 && countryCode.equals("MDE")) {
                c = 1;
            }
            c = 65535;
        } else {
            if (countryCode.equals("EEA")) {
                c = 0;
            }
            c = 65535;
        }
        if (c == 0) {
            defaultCode = "DE";
        } else if (c == 1) {
            defaultCode = "SA";
        } else if (c == 2) {
            defaultCode = "TZ";
        }
        if (!TextUtils.isEmpty(defaultCode)) {
            AbsVivoVgcManager vivoVgcManager = null;
            if (VivoFrameworkFactory.getFrameworkFactoryImpl() != null) {
                vivoVgcManager = VivoFrameworkFactory.getFrameworkFactoryImpl().getVivoVgcManager();
            }
            if (vivoVgcManager != null) {
                countryCode = vivoVgcManager.getString("settings_default_region", defaultCode);
            }
        }
        if (countryCode.equalsIgnoreCase("UK")) {
            countryCode = "GB";
        }
        VLog.i(TAG, "adjustedCode == " + countryCode);
        return countryCode;
    }

    private void setTime(String timeZoneId) {
        String tempString = SystemProperties.get("ro.kernel.qemu", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        boolean isEmulator = "1".equals(tempString);
        if (isEmulator) {
            VLog.d(TAG, "isEmulator:" + tempString);
            return;
        }
        String decryptState = SystemProperties.get("vold.decrypt", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
        VLog.d(TAG, "decryptState:" + decryptState);
        VLog.i(TAG, "setDefaultTime Service init calendar zone");
        TimeZone zone = TimeZone.getTimeZone(timeZoneId);
        Calendar calendar = Calendar.getInstance(zone);
        String buildDate = SystemProperties.get("ro.build.date.utc", "1412771212");
        VLog.i(TAG, "setDefaultTime Service buildTime = " + buildDate);
        calendar.setTime(new Date(Long.parseLong(buildDate) * 1000));
        if (calendar.get(2) + 1 <= 6) {
            calendar.set(2, 0);
            calendar.set(5, 2);
        } else {
            calendar.set(2, 6);
            calendar.set(5, 1);
        }
        calendar.set(11, 8);
        calendar.set(12, 0);
        calendar.set(13, 0);
        calendar.set(14, 0);
        long when = calendar.getTimeInMillis();
        if (Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK.equals(decryptState) || DECRYPT_STATE.equals(decryptState)) {
            this.alarmManager.setTimeZone(timeZoneId);
            Time today = new Time(timeZoneId);
            today.set(when);
            VLog.d(TAG, "First boot set the clock");
            SystemClock.setCurrentTimeMillis(today.toMillis(false));
            String currentTimeZone = SystemProperties.get("persist.sys.timezone", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
            VLog.d(TAG, "currentTimeZone == " + currentTimeZone + " ,currentTime == " + today.toString());
        }
    }

    public void showOnPollNetworkTimeEvent(int event, Network network) {
        if (network == null) {
            VLog.d(TAG, "defaultNetwork == null when call onPollNetworkTime.");
        }
        VLog.d(TAG, "onPollNetworkTime event == " + event);
    }

    public void setCurrentTime(long currentTimeMills) {
        VLog.d(TAG, "set current time by network time now ! time == " + currentTimeMills);
    }

    public void enforceForceRefresh(int event, int autoTimeEvent) {
        if (event == autoTimeEvent) {
            VLog.d(TAG, "Stale NTP fix; forcing refresh");
            this.ntpTrustedTime.forceRefresh();
        }
    }

    public boolean shouldIgnoreNitzTime() {
        return true;
    }

    private void setSecureNtp() {
        Context context = this.mContext;
        if (context == null) {
            return;
        }
        ContentResolver resolver = context.getContentResolver();
        String secureServer = Settings.Global.getString(resolver, "ntp_server");
        if (TextUtils.isEmpty(secureServer)) {
            VLog.i(TAG, "Secure server is null.");
            Settings.Global.putString(resolver, "ntp_server", "0.pool.ntp.org");
        }
    }
}