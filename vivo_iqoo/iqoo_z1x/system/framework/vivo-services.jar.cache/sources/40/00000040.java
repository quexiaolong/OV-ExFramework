package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.FileUtils;
import android.os.Message;
import android.os.UserHandle;
import com.android.server.DropBoxManagerService;
import com.vivo.services.rms.sdk.Consts;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: classes.dex */
public class VivoDbmsImpl implements IVivoDbms {
    private static final int MSG_SEND_CLOUD_DIAGNOSIS = 3;
    private static final String TAG = "VivoDropboxManagerServiceImpl";
    private final DropBoxManagerService.DropBoxManagerBroadcastHandler mHandler;

    public VivoDbmsImpl(DropBoxManagerService.DropBoxManagerBroadcastHandler mHandler) {
        this.mHandler = mHandler;
    }

    public void handleCloudDiagnosisMessage(Message msg, Context context) {
        if (msg.what == 3) {
            context.sendBroadcastAsUser((Intent) msg.obj, UserHandle.CURRENT);
        }
    }

    public void sendCloudDiagnosisMessage(String tag, long time) {
        if ("data_app_anr".equals(tag) || "system_app_anr".equals(tag) || "system_server_watchdog".equals(tag) || "system_server_anr".equals(tag) || "system_server_crash".equals(tag) || "data_app_crash".equals(tag) || "system_app_crash".equals(tag) || "data_app_native_crash".equals(tag) || "system_app_native_crash".equals(tag) || "SYSTEM_TOMBSTONE".equals(tag) || "system_server_native_crash".equals(tag) || "backup_crash".equals(tag)) {
            if ("system_server_watchdog".equals(tag) || "system_server_anr".equals(tag) || "system_server_crash".equals(tag) || "system_server_native_crash".equals(tag)) {
                File dropboxFile = new File("/data/logsystem_tmp");
                if (!dropboxFile.exists()) {
                    dropboxFile.mkdirs();
                }
                FileUtils.setPermissions(dropboxFile.getPath(), 493, -1, -1);
                deleteFurthestFile("/data/logsystem_tmp", 50);
                String fileName = "/data/system/dropbox/" + tag + "@" + String.valueOf(time) + ".txt.gz";
                String newFileName = "/data/logsystem_tmp/" + tag + "@" + String.valueOf(time) + ".txt.gz";
                File oldDropboxFile = new File(fileName);
                File logsystemFile = new File(newFileName);
                try {
                    if (!logsystemFile.exists()) {
                        logsystemFile.createNewFile();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (oldDropboxFile.exists()) {
                    bufferedCopy(oldDropboxFile, logsystemFile);
                }
                FileUtils.setPermissions(logsystemFile.getPath(), 420, -1, -1);
            }
            Intent cloudIntent = new Intent("com.vivo.intent.action.CLOUD_DIAGNOSIS");
            cloudIntent.addFlags(67108864);
            cloudIntent.putExtra("attr", 2);
            cloudIntent.setPackage("com.bbk.iqoo.logsystem");
            cloudIntent.putExtra("tag", tag);
            cloudIntent.putExtra("time", time);
            DropBoxManagerService.DropBoxManagerBroadcastHandler dropBoxManagerBroadcastHandler = this.mHandler;
            dropBoxManagerBroadcastHandler.sendMessage(dropBoxManagerBroadcastHandler.obtainMessage(3, cloudIntent));
        }
    }

    private void bufferedCopy(File sourceFile, File targetFile) {
        InputStream bis = null;
        OutputStream bos = null;
        try {
            try {
                bis = new BufferedInputStream(new FileInputStream(sourceFile));
                bos = new BufferedOutputStream(new FileOutputStream(targetFile));
                byte[] b = new byte[Consts.ProcessStates.FOCUS];
                while (true) {
                    int len = bis.read(b);
                    if (len == -1) {
                        break;
                    }
                    bos.write(b, 0, len);
                }
                bos.flush();
                bis.close();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
            if (bis != null) {
                bis.close();
            }
            if (bos != null) {
            }
        }
        bos.close();
    }

    private void deleteFurthestFile(String fileDir, int max) {
        File[] listFiles;
        try {
            File dir = new File(fileDir);
            if (!dir.exists()) {
                return;
            }
            int currentNum = 0;
            if (dir.exists()) {
                currentNum = dir.listFiles().length;
            }
            if (currentNum > max) {
                Map<String, String> map = new HashMap<>();
                List<String> list = new ArrayList<>();
                for (File f : dir.listFiles()) {
                    String subFileName = f.getName().substring(f.getName().indexOf("@") + 1);
                    map.put(subFileName, f.getAbsolutePath());
                    list.add(subFileName);
                }
                Collections.sort(list);
                for (int j = 0; j < list.size() - max; j++) {
                    new File(map.get(list.get(j))).delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPemmissionEntryFile(DropBoxManagerService.EntryFile entry, File dir) {
        File file = entry.getFile(dir);
        File parentDir = file.getParentFile();
        if (!file.exists()) {
            file.mkdirs();
        }
        FileUtils.setPermissions(parentDir.getPath(), 493, -1, -1);
        FileUtils.setPermissions(file.getPath(), 420, -1, -1);
    }
}