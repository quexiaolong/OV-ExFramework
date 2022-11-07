package com.android.server.media;

import android.app.ActivityThread;
import android.hardware.tv.cec.V1_0.CecMessageType;
import android.media.MediaMetadata;
import android.media.session.ISessionManager;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.KeyEvent;
import com.android.server.usb.descriptors.UsbTerminalTypes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class MediaShellCommand extends ShellCommand {
    private static final String PACKAGE_NAME = "";
    private static MediaSessionManager sMediaSessionManager;
    private static ActivityThread sThread;
    private PrintWriter mErrorWriter;
    private ISessionManager mSessionService;
    private PrintWriter mWriter;

    public int onCommand(String cmd) {
        this.mWriter = getOutPrintWriter();
        this.mErrorWriter = getErrPrintWriter();
        if (TextUtils.isEmpty(cmd)) {
            return handleDefaultCommands(cmd);
        }
        if (sThread == null) {
            Looper.prepare();
            ActivityThread systemMain = ActivityThread.systemMain();
            sThread = systemMain;
            sMediaSessionManager = (MediaSessionManager) systemMain.getSystemContext().getSystemService("media_session");
        }
        ISessionManager asInterface = ISessionManager.Stub.asInterface(ServiceManager.checkService("media_session"));
        this.mSessionService = asInterface;
        if (asInterface == null) {
            throw new IllegalStateException("Can't connect to media session service; is the system running?");
        }
        try {
            if (cmd.equals("dispatch")) {
                runDispatch();
                return 0;
            } else if (cmd.equals("list-sessions")) {
                runListSessions();
                return 0;
            } else if (cmd.equals("monitor")) {
                runMonitor();
                return 0;
            } else if (cmd.equals("volume")) {
                runVolume();
                return 0;
            } else {
                showError("Error: unknown command '" + cmd + "'");
                return -1;
            }
        } catch (Exception e) {
            showError(e.toString());
            return -1;
        }
    }

    public void onHelp() {
        this.mWriter.println("usage: media_session [subcommand] [options]");
        this.mWriter.println("       media_session dispatch KEY");
        this.mWriter.println("       media_session dispatch KEY");
        this.mWriter.println("       media_session list-sessions");
        this.mWriter.println("       media_session monitor <tag>");
        this.mWriter.println("       media_session volume [options]");
        this.mWriter.println();
        this.mWriter.println("media_session dispatch: dispatch a media key to the system.");
        this.mWriter.println("                KEY may be: play, pause, play-pause, mute, headsethook,");
        this.mWriter.println("                stop, next, previous, rewind, record, fast-forword.");
        this.mWriter.println("media_session list-sessions: print a list of the current sessions.");
        this.mWriter.println("media_session monitor: monitor updates to the specified session.");
        this.mWriter.println("                       Use the tag from list-sessions.");
        PrintWriter printWriter = this.mWriter;
        printWriter.println("media_session volume:  " + VolumeCtrl.USAGE);
        this.mWriter.println();
    }

    private void sendMediaKey(KeyEvent event) {
        try {
            this.mSessionService.dispatchMediaKeyEvent("", false, event, false);
        } catch (RemoteException e) {
        }
    }

    private void runMonitor() throws Exception {
        String id = getNextArgRequired();
        if (id == null) {
            showError("Error: must include a session id");
            return;
        }
        boolean success = false;
        try {
            List<MediaController> controllers = sMediaSessionManager.getActiveSessions(null);
            Iterator<MediaController> it = controllers.iterator();
            while (true) {
                if (!it.hasNext()) {
                    break;
                }
                MediaController controller = it.next();
                if (controller != null) {
                    try {
                        if (id.equals(controller.getTag())) {
                            ControllerMonitor monitor = new ControllerMonitor(controller);
                            monitor.run();
                            success = true;
                            break;
                        }
                        continue;
                    } catch (RemoteException e) {
                    }
                }
            }
        } catch (Exception e2) {
            PrintWriter printWriter = this.mErrorWriter;
            printWriter.println("***Error monitoring session*** " + e2.getMessage());
        }
        if (!success) {
            PrintWriter printWriter2 = this.mErrorWriter;
            printWriter2.println("No session found with id " + id);
        }
    }

    private void runDispatch() throws Exception {
        int keycode;
        String cmd = getNextArgRequired();
        if ("play".equals(cmd)) {
            keycode = CecMessageType.SYSTEM_AUDIO_MODE_STATUS;
        } else if ("pause".equals(cmd)) {
            keycode = 127;
        } else if ("play-pause".equals(cmd)) {
            keycode = 85;
        } else if ("mute".equals(cmd)) {
            keycode = 91;
        } else if ("headsethook".equals(cmd)) {
            keycode = 79;
        } else if ("stop".equals(cmd)) {
            keycode = 86;
        } else if ("next".equals(cmd)) {
            keycode = 87;
        } else if ("previous".equals(cmd)) {
            keycode = 88;
        } else if ("rewind".equals(cmd)) {
            keycode = 89;
        } else if ("record".equals(cmd)) {
            keycode = 130;
        } else if ("fast-forward".equals(cmd)) {
            keycode = 90;
        } else {
            showError("Error: unknown dispatch code '" + cmd + "'");
            return;
        }
        long now = SystemClock.uptimeMillis();
        int i = keycode;
        sendMediaKey(new KeyEvent(now, now, 0, i, 0, 0, -1, 0, 0, UsbTerminalTypes.TERMINAL_USB_STREAMING));
        sendMediaKey(new KeyEvent(now, now, 1, i, 0, 0, -1, 0, 0, UsbTerminalTypes.TERMINAL_USB_STREAMING));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public void showError(String errMsg) {
        onHelp();
        this.mErrorWriter.println(errMsg);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ControllerCallback extends MediaController.Callback {
        ControllerCallback() {
        }

        @Override // android.media.session.MediaController.Callback
        public void onSessionDestroyed() {
            MediaShellCommand.this.mWriter.println("onSessionDestroyed. Enter q to quit.");
        }

        @Override // android.media.session.MediaController.Callback
        public void onSessionEvent(String event, Bundle extras) {
            PrintWriter printWriter = MediaShellCommand.this.mWriter;
            printWriter.println("onSessionEvent event=" + event + ", extras=" + extras);
        }

        @Override // android.media.session.MediaController.Callback
        public void onPlaybackStateChanged(PlaybackState state) {
            PrintWriter printWriter = MediaShellCommand.this.mWriter;
            printWriter.println("onPlaybackStateChanged " + state);
        }

        @Override // android.media.session.MediaController.Callback
        public void onMetadataChanged(MediaMetadata metadata) {
            String mmString;
            if (metadata == null) {
                mmString = null;
            } else {
                mmString = "title=" + metadata.getDescription();
            }
            MediaShellCommand.this.mWriter.println("onMetadataChanged " + mmString);
        }

        @Override // android.media.session.MediaController.Callback
        public void onQueueChanged(List<MediaSession.QueueItem> queue) {
            String str;
            PrintWriter printWriter = MediaShellCommand.this.mWriter;
            StringBuilder sb = new StringBuilder();
            sb.append("onQueueChanged, ");
            if (queue == null) {
                str = "null queue";
            } else {
                str = " size=" + queue.size();
            }
            sb.append(str);
            printWriter.println(sb.toString());
        }

        @Override // android.media.session.MediaController.Callback
        public void onQueueTitleChanged(CharSequence title) {
            PrintWriter printWriter = MediaShellCommand.this.mWriter;
            printWriter.println("onQueueTitleChange " + ((Object) title));
        }

        @Override // android.media.session.MediaController.Callback
        public void onExtrasChanged(Bundle extras) {
            PrintWriter printWriter = MediaShellCommand.this.mWriter;
            printWriter.println("onExtrasChanged " + extras);
        }

        @Override // android.media.session.MediaController.Callback
        public void onAudioInfoChanged(MediaController.PlaybackInfo info) {
            PrintWriter printWriter = MediaShellCommand.this.mWriter;
            printWriter.println("onAudioInfoChanged " + info);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class ControllerMonitor {
        private final MediaController mController;
        private final ControllerCallback mControllerCallback;

        ControllerMonitor(MediaController controller) {
            this.mController = controller;
            this.mControllerCallback = new ControllerCallback();
        }

        void printUsageMessage() {
            try {
                PrintWriter printWriter = MediaShellCommand.this.mWriter;
                printWriter.println("V2Monitoring session " + this.mController.getTag() + "...  available commands: play, pause, next, previous");
            } catch (RuntimeException e) {
                MediaShellCommand.this.mWriter.println("Error trying to monitor session!");
            }
            MediaShellCommand.this.mWriter.println("(q)uit: finish monitoring");
        }

        void run() throws RemoteException {
            printUsageMessage();
            HandlerThread cbThread = new HandlerThread("MediaCb") { // from class: com.android.server.media.MediaShellCommand.ControllerMonitor.1
                @Override // android.os.HandlerThread
                protected void onLooperPrepared() {
                    try {
                        ControllerMonitor.this.mController.registerCallback(ControllerMonitor.this.mControllerCallback);
                    } catch (RuntimeException e) {
                        MediaShellCommand.this.mErrorWriter.println("Error registering monitor callback");
                    }
                }
            };
            cbThread.start();
            try {
                try {
                    try {
                        InputStreamReader converter = new InputStreamReader(System.in);
                        BufferedReader in = new BufferedReader(converter);
                        while (true) {
                            String line = in.readLine();
                            if (line == null) {
                                break;
                            }
                            boolean addNewline = true;
                            if (line.length() > 0) {
                                if ("q".equals(line) || "quit".equals(line)) {
                                    break;
                                } else if ("play".equals(line)) {
                                    dispatchKeyCode(CecMessageType.SYSTEM_AUDIO_MODE_STATUS);
                                } else if ("pause".equals(line)) {
                                    dispatchKeyCode(127);
                                } else if ("next".equals(line)) {
                                    dispatchKeyCode(87);
                                } else if ("previous".equals(line)) {
                                    dispatchKeyCode(88);
                                } else {
                                    PrintWriter printWriter = MediaShellCommand.this.mErrorWriter;
                                    printWriter.println("Invalid command: " + line);
                                }
                            } else {
                                addNewline = false;
                            }
                            synchronized (this) {
                                if (addNewline) {
                                    System.out.println("");
                                }
                                printUsageMessage();
                            }
                        }
                        cbThread.getLooper().quit();
                        this.mController.unregisterCallback(this.mControllerCallback);
                    } catch (IOException e) {
                        e.printStackTrace();
                        cbThread.getLooper().quit();
                        this.mController.unregisterCallback(this.mControllerCallback);
                    }
                } catch (Throwable th) {
                    cbThread.getLooper().quit();
                    try {
                        this.mController.unregisterCallback(this.mControllerCallback);
                    } catch (Exception e2) {
                    }
                    throw th;
                }
            } catch (Exception e3) {
            }
        }

        private void dispatchKeyCode(int keyCode) {
            long now = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(now, now, 0, keyCode, 0, 0, -1, 0, 0, UsbTerminalTypes.TERMINAL_USB_STREAMING);
            KeyEvent up = new KeyEvent(now, now, 1, keyCode, 0, 0, -1, 0, 0, UsbTerminalTypes.TERMINAL_USB_STREAMING);
            try {
                this.mController.dispatchMediaButtonEvent(down);
                this.mController.dispatchMediaButtonEvent(up);
            } catch (RuntimeException e) {
                PrintWriter printWriter = MediaShellCommand.this.mErrorWriter;
                printWriter.println("Failed to dispatch " + keyCode);
            }
        }
    }

    private void runListSessions() {
        this.mWriter.println("Sessions:");
        try {
            List<MediaController> controllers = sMediaSessionManager.getActiveSessions(null);
            for (MediaController controller : controllers) {
                if (controller != null) {
                    try {
                        PrintWriter printWriter = this.mWriter;
                        printWriter.println("  tag=" + controller.getTag() + ", package=" + controller.getPackageName());
                    } catch (RuntimeException e) {
                    }
                }
            }
        } catch (Exception e2) {
            this.mErrorWriter.println("***Error listing sessions***");
        }
    }

    private void runVolume() throws Exception {
        VolumeCtrl.run(this);
    }
}