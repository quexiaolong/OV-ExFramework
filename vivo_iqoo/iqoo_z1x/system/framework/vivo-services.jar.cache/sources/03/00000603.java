package com.vivo.services.artkeeper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import com.vivo.face.common.data.Constants;
import vivo.util.VivoThemeUtil;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class VivoArtKeeperWarnDialog implements DialogInterface.OnCancelListener {
    private static final String TAG = "VivoArtKeeperService.Dialog";
    private Context mContext;
    private Handler mHandler;
    private static VivoArtKeeperWarnDialog sInstance = null;
    public static boolean SERIES_IQOO = SystemProperties.get("ro.vivo.product.series", Constants.Setting.ARD9_FACE_UNLOCK_FAST_UNLOCK).equals("IQOO");
    private AlertDialog mAlertDialog = null;
    private Boolean mSyncDialog = true;
    private Boolean checkBoxStatus = false;
    final String artAppEnable = "artpp_enabled";

    private VivoArtKeeperWarnDialog(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public static synchronized VivoArtKeeperWarnDialog getInstance(Context context, Handler handler) {
        VivoArtKeeperWarnDialog vivoArtKeeperWarnDialog;
        synchronized (VivoArtKeeperWarnDialog.class) {
            if (sInstance == null) {
                sInstance = new VivoArtKeeperWarnDialog(context, handler);
            }
            vivoArtKeeperWarnDialog = sInstance;
        }
        return vivoArtKeeperWarnDialog;
    }

    public void ShowArtKeeperWarnDialog() {
        View layout;
        synchronized (this.mSyncDialog) {
            if (this.mAlertDialog != null) {
                Slog.d(TAG, "already show dialog exit");
                return;
            }
            try {
                ConfirmDialogListener listener = new ConfirmDialogListener();
                LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService("layout_inflater");
                if (SERIES_IQOO) {
                    layout = inflater.inflate(50528360, (ViewGroup) null);
                } else {
                    layout = inflater.inflate(50528359, (ViewGroup) null);
                }
                CheckBox rememberCB = (CheckBox) layout.findViewById(51183811);
                rememberCB.setOnCheckedChangeListener(listener);
                AlertDialog create = new AlertDialog.Builder(this.mContext, VivoThemeUtil.getSystemThemeStyle(VivoThemeUtil.ThemeType.DIALOG_SLIDE)).setTitle(51249716).setView(layout).setNegativeButton(51249711, listener).setPositiveButton(51249714, listener).setOnCancelListener(this).setOnKeyListener(new DialogInterface.OnKeyListener() { // from class: com.vivo.services.artkeeper.VivoArtKeeperWarnDialog.1
                    @Override // android.content.DialogInterface.OnKeyListener
                    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                        if (keyCode == 3 || keyCode == 4) {
                            Slog.d(VivoArtKeeperWarnDialog.TAG, "disable keyCode:" + keyCode);
                            return true;
                        }
                        return true;
                    }
                }).create();
                this.mAlertDialog = create;
                create.getWindow().setType(2003);
                this.mAlertDialog.getWindow().getAttributes().flags |= 536870912;
                this.mAlertDialog.show();
            } catch (Exception e) {
                Slog.e(TAG, "Failed show dialog");
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public synchronized void dismiss() {
        synchronized (this.mSyncDialog) {
            if (this.mAlertDialog != null) {
                this.mAlertDialog.dismiss();
            }
            this.mAlertDialog = null;
        }
    }

    @Override // android.content.DialogInterface.OnCancelListener
    public void onCancel(DialogInterface dialog) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ConfirmDialogListener implements DialogInterface.OnClickListener, CompoundButton.OnCheckedChangeListener {
        ConfirmDialogListener() {
        }

        @Override // android.content.DialogInterface.OnClickListener
        public void onClick(DialogInterface dialog, int which) {
            if (which == -1) {
                try {
                    Settings.Global.putInt(VivoArtKeeperWarnDialog.this.mContext.getContentResolver(), "artpp_enabled", 1);
                } catch (Exception e) {
                    Slog.d(VivoArtKeeperWarnDialog.TAG, "Failed to write artAppEnable:" + e);
                }
            } else if (which == -2) {
                try {
                    if (VivoArtKeeperWarnDialog.this.checkBoxStatus.booleanValue()) {
                        Settings.Global.putInt(VivoArtKeeperWarnDialog.this.mContext.getContentResolver(), "artpp_enabled", -1);
                    }
                } catch (Exception e2) {
                    Slog.d(VivoArtKeeperWarnDialog.TAG, " Failed to write artAppEnable:" + e2);
                }
            }
            VivoArtKeeperWarnDialog.this.dismiss();
            Slog.d(VivoArtKeeperWarnDialog.TAG, " which = " + which);
        }

        @Override // android.widget.CompoundButton.OnCheckedChangeListener
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Slog.e(VivoArtKeeperWarnDialog.TAG, "onCheckedChanged + " + buttonView + " isChecked " + isChecked);
            VivoArtKeeperWarnDialog.this.checkBoxStatus = Boolean.valueOf(isChecked);
        }
    }
}