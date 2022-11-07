package com.vivo.services.phonelock;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Base64;
import com.vivo.common.utils.VLog;
import com.vivo.face.common.data.Constants;
import com.vivo.framework.phonelock.VivoPhoneLockUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import vivo.app.phonelock.IVivoPhoneLockService;

/* loaded from: classes.dex */
public class VivoPhoneLockService extends IVivoPhoneLockService.Stub {
    private static final String COMMERCIAL = "commercial";
    private static final String COMPLETED = "2";
    private static final int COMPLETED_INT = 2;
    private static final String CUSTOMIZE = "customize";
    private static final String CUSTOMLOCK = "customlock";
    private static final String DISABLE = "0";
    private static final String ENABLE = "1";
    private static final String ERROR_CODE = "-1";
    private static final String IS_ACTIVE = "isActive";
    private static final String IS_IN_FACTORY_MODE = "isInFactoryMode";
    private static final String IS_SIM_LOCK_ENABLE = "isSimlockEnable";
    private static final String IS_SIM_LOCK_NEED_PW = "isSimlockNeedPassword";
    private static final int LOCKED = 1;
    private static final String MAX_LOCK_NUM = "maxLockNum";
    private static final String OLD_COMERCIAL_PROP = "ro.product.customize.comercial";
    private static final String OLD_CUSTOMIZE_PROP = "ro.product.customize.bbk";
    private static final String OLD_IS_SIM_LOCK_NEED_PW = "ro.vivo.sim_lock.needpw";
    private static final String OLD_PHONE_LOCK_ENABLE_PROP = "ro.vivo.phonelock.enabled";
    private static final String OLD_PHONE_LOCK_STATUS_PROP = "persist.vivo.custom.phonelock";
    private static final String OLD_SIM_LOCK_ENABLE_PROP = "ro.vivo.sim_lock";
    private static final String PHONE_LOCK_ENABLE = "phoneLockEnable";
    private static final String PHONE_LOCK_PERMISSION = "com.vivo.permission.PHONE_LOCK_PERMISSION";
    private static final String PHONE_LOCK_PW = "phoneLockPasswd";
    private static final String SIM_LOCK_NEED_PW = "1";
    private static final int SIM_LOCK_NEED_PW_FLAG = 1;
    private static final String SIM_LOCK_NO_NEED_PW = "0";
    private static final int SIM_LOCK_NO_NEED_PW_FLAG = 0;
    private static final String SIM_PW = "simpass";
    private static final String TAG = "VivoPhoneLockService";
    private static final String TRY_UNLOCK_NUM = "tryUnlockNum";
    private static final int UNLOCKED = 0;
    private static final int VERIFY_ERROR = -1;
    private static final int VERIFY_FAIL = 1;
    private static final int VERIFY_SUCCESS = 0;
    private static final String VGC_COMERCIAL_PROP = "ro.vgc.customize.comercial";
    private static final String VTRUST = "VTRUST:";
    private static List<String> fieldList;
    private static final boolean isOverseas = SystemProperties.get("ro.vivo.product.overseas", "no").equals("yes");
    private static volatile VivoPhoneLockService sInstance;
    private Context mContext;
    private VivoPhoneLockUtils mPhoneLockFieldUtil;
    private String phoneLockEnableCache = null;
    private String simLockEnableCache = null;

    public static VivoPhoneLockService getInstance(Context context) {
        if (sInstance == null) {
            synchronized (VivoPhoneLockService.class) {
                if (sInstance == null) {
                    sInstance = new VivoPhoneLockService(context);
                }
            }
        }
        return sInstance;
    }

    public VivoPhoneLockService(Context context) {
        this.mContext = context;
        this.mPhoneLockFieldUtil = VivoPhoneLockUtils.getInstance(context);
        initFieldList();
    }

    public int setPhoneLockFieldWithToken(String field, String value, String token) throws RemoteException {
        VLog.d(TAG, "set Phone Lock Field With Tok");
        checkPermission(PHONE_LOCK_PERMISSION);
        byte[] bArr = new byte[0];
        try {
            String data = VTRUST + field + ":" + value;
            byte[] decodeData = data.getBytes("utf-8");
            int ret = this.mPhoneLockFieldUtil.setField(CUSTOMLOCK, token, decodeData);
            if (PHONE_LOCK_ENABLE.equals(field)) {
                VLog.d(TAG, "setPhoneLockFieldWithToken:erase PhoneLock Enable Cache");
                this.phoneLockEnableCache = null;
            }
            if (IS_SIM_LOCK_ENABLE.equals(field)) {
                VLog.d(TAG, "setPhoneLockFieldWithToken:erase SimLock Enable Cache");
                this.simLockEnableCache = null;
            }
            return ret;
        } catch (Exception e) {
            VLog.e(TAG, "Error:setPhoneLockFieldWithToken");
            return -1;
        }
    }

    public String getPhoneLockField(String field, String defaultValue) throws RemoteException {
        VLog.d(TAG, "get Phone Lock Field");
        try {
            byte[] value = this.mPhoneLockFieldUtil.getField(CUSTOMLOCK);
            byte[] retByte = this.mPhoneLockFieldUtil.getFieldFromData(field, value);
            if (retByte != null) {
                return getStrFromByte(retByte);
            }
            return defaultValue;
        } catch (Exception e) {
            VLog.e(TAG, "Error:getPhoneLockField");
            return ERROR_CODE;
        }
    }

    public int setPhoneLockByteWithToken(String field, byte[] value, String token) throws RemoteException {
        VLog.d(TAG, "set Phone Lock Byte With Tok");
        checkPermission(PHONE_LOCK_PERMISSION);
        String strValue = VTRUST + field + ":" + getStrFromByte(value);
        try {
            int ret = this.mPhoneLockFieldUtil.setField(CUSTOMLOCK, token, strValue.getBytes("utf-8"));
            return ret;
        } catch (Exception e) {
            VLog.e(TAG, "Error:setPhoneLockByteWithToken");
            return -1;
        }
    }

    public byte[] getPhoneLockByte(String field, byte[] defaultValue) throws RemoteException {
        VLog.d(TAG, "get Phone Lock Byte");
        try {
            byte[] value = this.mPhoneLockFieldUtil.getField(CUSTOMLOCK);
            byte[] retByte = this.mPhoneLockFieldUtil.getFieldFromData(field, value);
            if (retByte != null) {
                return removeNullCharFromByte(retByte);
            }
        } catch (Exception e) {
            VLog.e(TAG, "Error:getPhoneLockByte");
        }
        return defaultValue;
    }

    public int setPhoneLockEnable(String enable, String token) throws RemoteException {
        VLog.d(TAG, "setPhoneLockEnable");
        checkPermission(PHONE_LOCK_PERMISSION);
        try {
            int ret = setPhoneLockFieldWithToken(PHONE_LOCK_ENABLE, enable, token);
            return ret;
        } catch (Exception e) {
            VLog.e(TAG, "Error:setPhoneLockEnable");
            return -1;
        }
    }

    public int isPhoneLockedEnable() throws RemoteException {
        VLog.d(TAG, "isPhoneLockedEnable");
        if (!isOverseas) {
            VLog.d(TAG, "Phone Locked Enable Only Support Overseas Device");
            return 0;
        }
        VLog.d(TAG, "Overseas Device");
        String oldEnableStatus = SystemProperties.get(OLD_PHONE_LOCK_ENABLE_PROP, "0");
        if ("1".equals(oldEnableStatus)) {
            return 1;
        }
        if (this.phoneLockEnableCache == null) {
            try {
                this.phoneLockEnableCache = getPhoneLockField(PHONE_LOCK_ENABLE, "0");
            } catch (Exception e) {
                VLog.e(TAG, "Error:read isPhoneLockEnable");
                return -1;
            }
        }
        if ("1".equals(this.phoneLockEnableCache)) {
            VLog.d(TAG, "isPhoneLockedEnable === 1");
            return 1;
        }
        VLog.d(TAG, "isPhoneLockedEnable === 0");
        return 0;
    }

    public int isPhoneLocked() throws RemoteException {
        VLog.d(TAG, "isPhoneLocked");
        try {
            return getPhoneLockFlag();
        } catch (Exception e) {
            VLog.e(TAG, "Error:isPhoneLocked");
            return -1;
        }
    }

    public int setPhoneLockPwWithAuth(String password, String token) throws RemoteException {
        VLog.d(TAG, "setPhoneLockPwWithAuth");
        checkPermission(PHONE_LOCK_PERMISSION);
        try {
            byte[] decodeData = Base64.decode(password, 11);
            return this.mPhoneLockFieldUtil.setFieldWithCipher(PHONE_LOCK_PW, token, decodeData);
        } catch (Exception e) {
            VLog.e(TAG, "Error:setPhoneLockPwWithAuth");
            return -1;
        }
    }

    public int verifyPhoneLockPwWithAuth(String password, String token) throws RemoteException {
        VLog.d(TAG, "verifyPhoneLockPwWithAuth");
        try {
            byte[] decodeData = Base64.decode(password, 11);
            return this.mPhoneLockFieldUtil.setFieldWithCipher(PHONE_LOCK_PW, token, decodeData);
        } catch (Exception e) {
            VLog.e(TAG, "Error:verifyPhoneLockPwWithAuth");
            return -1;
        }
    }

    public int verifyPhoneLockPw(String password) throws RemoteException {
        VLog.d(TAG, "verifyPhoneLockPw");
        try {
            return this.mPhoneLockFieldUtil.setField(PHONE_LOCK_PW, password);
        } catch (Exception e) {
            VLog.e(TAG, "Error:verifyPhoneLockPw");
            return -1;
        }
    }

    public int setSimLockedEnable(String enable, String token) throws RemoteException {
        checkPermission(PHONE_LOCK_PERMISSION);
        VLog.d(TAG, "setSimLockedEnable");
        try {
            int ret = setPhoneLockFieldWithToken(IS_SIM_LOCK_ENABLE, enable, token);
            return ret;
        } catch (Exception e) {
            VLog.e(TAG, "Error:setSimLockedEnable");
            return -1;
        }
    }

    public int isSimLockedEnable() throws RemoteException {
        VLog.d(TAG, "isSimLockedEnable");
        if (!isOverseas) {
            VLog.d(TAG, "Sim Locked Enable Only Support Overseas Device");
            return 0;
        }
        VLog.d(TAG, "Overseas Device");
        String oldSimLockStatus = SystemProperties.get(OLD_SIM_LOCK_ENABLE_PROP, "0");
        if ("1".equals(oldSimLockStatus)) {
            return 1;
        }
        if (this.simLockEnableCache == null) {
            try {
                this.simLockEnableCache = getPhoneLockField(IS_SIM_LOCK_ENABLE, "0");
            } catch (Exception e) {
                VLog.e(TAG, "Error:read isSimLockedEnable");
                return -1;
            }
        }
        return "1".equals(this.simLockEnableCache) ? 1 : 0;
    }

    public int isSimLocked() throws RemoteException {
        VLog.d(TAG, "isSimLocked");
        if (isSimLockNeedPassword() == 1) {
            try {
                return this.mPhoneLockFieldUtil.hasSet(SIM_PW) ? 1 : 0;
            } catch (Exception e) {
                VLog.e(TAG, "Error:isSimLocked");
                return -1;
            }
        }
        return 1;
    }

    public int setSimLockPwWithAuth(String hck, String token) throws RemoteException {
        VLog.d(TAG, "setSimLockPwWithAuth");
        checkPermission(PHONE_LOCK_PERMISSION);
        try {
            byte[] decodeData = hck.getBytes("utf-8");
            int ret = this.mPhoneLockFieldUtil.setField(SIM_PW, token, decodeData);
            return ret;
        } catch (Exception e) {
            VLog.e(TAG, "pin toBytes occur error");
            return -1;
        }
    }

    public int verifySimLock(String pin) throws RemoteException {
        VLog.d(TAG, "verifySimLock");
        try {
            return this.mPhoneLockFieldUtil.setField(SIM_PW, pin);
        } catch (Exception e) {
            VLog.e(TAG, "Error:verifySimLock");
            return -1;
        }
    }

    public String getTokenForAuth(String key) throws RemoteException {
        VLog.d(TAG, "get tok for auth");
        try {
            return this.mPhoneLockFieldUtil.getTokenForAuth(key);
        } catch (Exception e) {
            VLog.e(TAG, "Error:getTokenForAuth");
            return ERROR_CODE;
        }
    }

    public int isPhoneActivated() throws RemoteException {
        VLog.d(TAG, "isPhoneActivated");
        try {
            String ret = getPhoneLockField(IS_ACTIVE, "1");
            if ("1".equals(ret)) {
                return 1;
            }
            if ("2".equals(ret)) {
                return 2;
            }
            return 0;
        } catch (Exception e) {
            VLog.e(TAG, "Error:isPhoneActivated");
            return -1;
        }
    }

    public int setPhoneActivate(String isActivate, String token) throws RemoteException {
        VLog.d(TAG, "setPhoneActivate");
        checkPermission(PHONE_LOCK_PERMISSION);
        try {
            return setPhoneLockFieldWithToken(IS_ACTIVE, isActivate, token);
        } catch (Exception e) {
            VLog.e(TAG, "Error:setPhoneActivate");
            return -1;
        }
    }

    public String getCustomize() throws RemoteException {
        VLog.d(TAG, "getCustomize");
        return getFieldFromProp(OLD_CUSTOMIZE_PROP);
    }

    public String getCommercial() throws RemoteException {
        VLog.d(TAG, "getCommercial");
        byte[] retByte = null;
        String result = null;
        try {
            byte[] value = this.mPhoneLockFieldUtil.getField(CUSTOMLOCK);
            retByte = this.mPhoneLockFieldUtil.getFieldFromData(COMMERCIAL, value);
        } catch (Exception e) {
            VLog.e(TAG, "Error:getCommercial");
        }
        if (retByte != null) {
            result = getStrFromByte(retByte);
            VLog.d(TAG, "getCommercial: value === " + result);
        }
        if (TextUtils.isEmpty(result)) {
            String vgcCommercial = getFieldFromProp(VGC_COMERCIAL_PROP);
            String oldCommercial = getFieldFromProp(OLD_COMERCIAL_PROP);
            return TextUtils.isEmpty(vgcCommercial) ? oldCommercial : vgcCommercial;
        }
        return result;
    }

    public int setCommercial(String value, String token) throws RemoteException {
        VLog.d(TAG, "setCommercial");
        checkPermission(PHONE_LOCK_PERMISSION);
        try {
            return setPhoneLockFieldWithToken(COMMERCIAL, value, token);
        } catch (Exception e) {
            VLog.e(TAG, "Error:setCommercial");
            return -1;
        }
    }

    public int isSimLockNeedPassword() throws RemoteException {
        VLog.d(TAG, "is Sim Lock Need PW");
        byte[] retByte = null;
        String result = null;
        try {
            byte[] value = this.mPhoneLockFieldUtil.getField(CUSTOMLOCK);
            retByte = this.mPhoneLockFieldUtil.getFieldFromData(IS_SIM_LOCK_NEED_PW, value);
        } catch (Exception e) {
            VLog.e(TAG, "Error:isSimLockNeedPassword");
        }
        if (retByte != null) {
            result = getStrFromByte(retByte);
            VLog.d(TAG, "is Sim Lock Need PW: value === " + result);
        }
        if (TextUtils.isEmpty(result)) {
            String oldStatus = getFieldFromProp(OLD_IS_SIM_LOCK_NEED_PW);
            if ("0".equals(oldStatus)) {
                return 0;
            }
            return "1".equals(oldStatus) ? 1 : -1;
        } else if ("0".equals(result)) {
            return 0;
        } else {
            return "1".equals(result) ? 1 : -1;
        }
    }

    public int setSimLockNeedPassword(String isNeed, String token) throws RemoteException {
        VLog.d(TAG, "set Sim Lock Need PW");
        checkPermission(PHONE_LOCK_PERMISSION);
        try {
            return setPhoneLockFieldWithToken(IS_SIM_LOCK_NEED_PW, isNeed, token);
        } catch (Exception e) {
            VLog.e(TAG, "Error:setSimLockNeedPassword");
            return -1;
        }
    }

    private static void initFieldList() {
        ArrayList arrayList = new ArrayList();
        fieldList = arrayList;
        arrayList.add(IS_ACTIVE);
        fieldList.add(PHONE_LOCK_PW);
        fieldList.add(SIM_PW);
        fieldList.add(TRY_UNLOCK_NUM);
        fieldList.add(IS_SIM_LOCK_NEED_PW);
        fieldList.add(PHONE_LOCK_ENABLE);
        fieldList.add(IS_SIM_LOCK_ENABLE);
        fieldList.add(COMMERCIAL);
        fieldList.add(MAX_LOCK_NUM);
        fieldList.add(IS_IN_FACTORY_MODE);
        fieldList.add(CUSTOMIZE);
    }

    private static boolean isContainedInFieldList(String key) {
        List<String> list = fieldList;
        if (list == null) {
            return false;
        }
        if (list.contains(key)) {
            return true;
        }
        VLog.d(TAG, key + " is not Contained In FieldList");
        return false;
    }

    private String getStrFromByte(byte[] buffer) {
        if (buffer == null) {
            return null;
        }
        int length = 0;
        int i = 0;
        while (true) {
            try {
                if (i >= buffer.length) {
                    break;
                } else if (buffer[i] != 0) {
                    i++;
                } else {
                    length = i;
                    break;
                }
            } catch (Exception e) {
                VLog.e(TAG, "Error:getStrFromByte");
                return null;
            }
        }
        return new String(buffer, 0, length, "UTF-8");
    }

    private byte[] removeNullCharFromByte(byte[] buffer) {
        if (buffer == null) {
            return null;
        }
        int length = 0;
        int i = 0;
        while (true) {
            try {
                if (i >= buffer.length) {
                    break;
                } else if (buffer[i] != 0) {
                    i++;
                } else {
                    length = i;
                    break;
                }
            } catch (Exception e) {
                VLog.e(TAG, "Error:removeNullCharFromByte");
                return null;
            }
        }
        return Arrays.copyOf(buffer, length);
    }

    private int getPhoneLockFlag() {
        VLog.d(TAG, "getPhoneLockFlag");
        try {
            if (isPhoneLockedEnable() != 1) {
                return 0;
            }
            return this.mPhoneLockFieldUtil.hasSet(PHONE_LOCK_PW) ? 1 : 0;
        } catch (Exception e) {
            VLog.e(TAG, "Error:getPhoneLockFlag");
            return 0;
        }
    }

    private String getFieldFromProp(String field) {
        return SystemProperties.get(field, Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK);
    }

    private void checkPermission(String permission) {
        Context context = this.mContext;
        context.enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }

    public byte[] getPhoneLockBuffer() throws RemoteException {
        VLog.d(TAG, "get Phone Lock buffer");
        try {
            byte[] value = this.mPhoneLockFieldUtil.getField(CUSTOMLOCK);
            return value;
        } catch (Exception e) {
            VLog.e(TAG, "Error:getPhoneLockBuffer");
            return null;
        }
    }

    public String getPhoneLockFieldFromBuffer(byte[] buffer, String field) throws RemoteException {
        VLog.d(TAG, "getPhoneLockFieldFromBuffer");
        if (buffer == null) {
            VLog.d(TAG, "getPhoneLockFieldFromBuffer,buffer is null");
            return null;
        }
        byte[] retByte = null;
        try {
            retByte = this.mPhoneLockFieldUtil.getFieldFromData(field, buffer);
        } catch (Exception e) {
            VLog.e(TAG, "Error:getPhoneLockFieldFromBuffer");
        }
        if (retByte == null) {
            return null;
        }
        return getStrFromByte(retByte);
    }

    public String getDeviceInfo(String field, String defaultValue) throws RemoteException {
        VLog.d(TAG, "get Device Info");
        if (field == null) {
            VLog.d(TAG, "get Device Info == null");
            return defaultValue;
        }
        String value = null;
        try {
            value = this.mPhoneLockFieldUtil.getInfo(field);
        } catch (Exception e) {
            VLog.e(TAG, "Error:getDeviceInfo");
        }
        if (TextUtils.isEmpty(value)) {
            return defaultValue;
        }
        return value;
    }

    public int setCommonFieldWithToken(String field, String value, String token) throws RemoteException {
        VLog.d(TAG, "set Common Field With Tok");
        checkPermission(PHONE_LOCK_PERMISSION);
        byte[] bArr = new byte[0];
        try {
            byte[] decodeData = value.getBytes("utf-8");
            int ret = this.mPhoneLockFieldUtil.setField(field, token, decodeData);
            return ret;
        } catch (Exception e) {
            VLog.e(TAG, "Error:setCommonFieldWithToken");
            return -1;
        }
    }

    public String getCommonField(String field, String defaultValue) throws RemoteException {
        VLog.d(TAG, "get Common Field");
        try {
            String value = this.mPhoneLockFieldUtil.readField(field);
            return value;
        } catch (Exception e) {
            VLog.e(TAG, "Error:getCommonField");
            return ERROR_CODE;
        }
    }

    public int setCommonByteWithToken(String field, byte[] value, String token) throws RemoteException {
        VLog.d(TAG, "set Common Byte With Tok");
        checkPermission(PHONE_LOCK_PERMISSION);
        try {
            int ret = this.mPhoneLockFieldUtil.setField(field, token, value);
            return ret;
        } catch (Exception e) {
            VLog.e(TAG, "Error:setCommonByteWithToken");
            return -1;
        }
    }

    public byte[] getCommonByte(String field, byte[] defaultValue) throws RemoteException {
        VLog.d(TAG, "get Common Byte");
        try {
            byte[] value = this.mPhoneLockFieldUtil.getField(field);
            return value;
        } catch (Exception e) {
            VLog.e(TAG, "Error:getCommonByte");
            return defaultValue;
        }
    }
}