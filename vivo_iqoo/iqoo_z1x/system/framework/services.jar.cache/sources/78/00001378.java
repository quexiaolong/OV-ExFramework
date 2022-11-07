package com.android.server.notification;

import android.app.NotificationHistory;
import android.graphics.drawable.Icon;
import android.text.TextUtils;
import android.util.Slog;
import android.util.proto.ProtoInputStream;
import android.util.proto.ProtoOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* loaded from: classes.dex */
final class NotificationHistoryProtoHelper {
    private static final String TAG = "NotifHistoryProto";

    private NotificationHistoryProtoHelper() {
    }

    private static List<String> readStringPool(ProtoInputStream proto) throws IOException {
        List<String> stringPool;
        long token = proto.start(1146756268033L);
        if (proto.nextField(1120986464257L)) {
            stringPool = new ArrayList<>(proto.readInt(1120986464257L));
        } else {
            stringPool = new ArrayList<>();
        }
        while (proto.nextField() != -1) {
            if (proto.getFieldNumber() == 2) {
                stringPool.add(proto.readString(2237677961218L));
            }
        }
        proto.end(token);
        return stringPool;
    }

    private static void writeStringPool(ProtoOutputStream proto, NotificationHistory notifications) {
        long token = proto.start(1146756268033L);
        String[] pooledStrings = notifications.getPooledStringsToWrite();
        proto.write(1120986464257L, pooledStrings.length);
        for (String str : pooledStrings) {
            proto.write(2237677961218L, str);
        }
        proto.end(token);
    }

    private static void readNotification(ProtoInputStream proto, List<String> stringPool, NotificationHistory notifications, NotificationHistoryFilter filter) throws IOException {
        long token = proto.start(2246267895811L);
        try {
            try {
                NotificationHistory.HistoricalNotification notification = readNotification(proto, stringPool);
                if (filter.matchesPackageAndChannelFilter(notification) && filter.matchesCountFilter(notifications)) {
                    notifications.addNotificationToWrite(notification);
                }
            } catch (Exception e) {
                Slog.e(TAG, "Error reading notification", e);
            }
        } finally {
            proto.end(token);
        }
    }

    private static NotificationHistory.HistoricalNotification readNotification(ProtoInputStream parser, List<String> stringPool) throws IOException {
        NotificationHistory.HistoricalNotification.Builder notification = new NotificationHistory.HistoricalNotification.Builder();
        String pkg = null;
        while (true) {
            switch (parser.nextField()) {
                case -1:
                    return notification.build();
                case 1:
                    pkg = parser.readString(1138166333441L);
                    notification.setPackage(pkg);
                    stringPool.add(pkg);
                    break;
                case 2:
                    String pkg2 = stringPool.get(parser.readInt(1120986464258L) - 1);
                    pkg = pkg2;
                    notification.setPackage(pkg);
                    break;
                case 3:
                    String channelName = parser.readString(1138166333443L);
                    notification.setChannelName(channelName);
                    stringPool.add(channelName);
                    break;
                case 4:
                    notification.setChannelName(stringPool.get(parser.readInt(1120986464260L) - 1));
                    break;
                case 5:
                    String channelId = parser.readString(1138166333445L);
                    notification.setChannelId(channelId);
                    stringPool.add(channelId);
                    break;
                case 6:
                    notification.setChannelId(stringPool.get(parser.readInt(1120986464262L) - 1));
                    break;
                case 7:
                    notification.setUid(parser.readInt(1120986464263L));
                    break;
                case 8:
                    notification.setUserId(parser.readInt(1120986464264L));
                    break;
                case 9:
                    notification.setPostedTimeMs(parser.readLong(1112396529673L));
                    break;
                case 10:
                    notification.setTitle(parser.readString(1138166333450L));
                    break;
                case 11:
                    notification.setText(parser.readString(1138166333451L));
                    break;
                case 12:
                    long iconToken = parser.start(1146756268044L);
                    loadIcon(parser, notification, pkg);
                    parser.end(iconToken);
                    break;
                case 13:
                    String conversationId = parser.readString(1138166333453L);
                    notification.setConversationId(conversationId);
                    stringPool.add(conversationId);
                    break;
                case 14:
                    notification.setConversationId(stringPool.get(parser.readInt(1120986464270L) - 1));
                    break;
            }
        }
    }

    private static void loadIcon(ProtoInputStream parser, NotificationHistory.HistoricalNotification.Builder notification, String pkg) throws IOException {
        String str;
        int iconType = 0;
        int imageResourceId = 0;
        String imageResourceIdPackage = null;
        byte[] imageByteData = null;
        int imageByteDataLength = 0;
        int imageByteDataOffset = 0;
        String imageUri = null;
        while (true) {
            switch (parser.nextField()) {
                case -1:
                    if (iconType == 3) {
                        if (imageByteData != null) {
                            notification.setIcon(Icon.createWithData(imageByteData, imageByteDataOffset, imageByteDataLength));
                            return;
                        }
                        return;
                    } else if (iconType == 2) {
                        if (imageResourceId != 0) {
                            if (imageResourceIdPackage != null) {
                                str = imageResourceIdPackage;
                            } else {
                                str = pkg;
                            }
                            notification.setIcon(Icon.createWithResource(str, imageResourceId));
                            return;
                        }
                        return;
                    } else if (iconType == 4 && imageUri != null) {
                        notification.setIcon(Icon.createWithContentUri(imageUri));
                        return;
                    } else {
                        return;
                    }
                case 1:
                    iconType = parser.readInt(1159641169921L);
                    break;
                case 2:
                    parser.readString(1138166333442L);
                    break;
                case 3:
                    imageResourceId = parser.readInt(1120986464259L);
                    break;
                case 4:
                    imageResourceIdPackage = parser.readString(1138166333444L);
                    break;
                case 5:
                    imageByteData = parser.readBytes(1151051235333L);
                    break;
                case 6:
                    imageByteDataLength = parser.readInt(1120986464262L);
                    break;
                case 7:
                    imageByteDataOffset = parser.readInt(1120986464263L);
                    break;
                case 8:
                    imageUri = parser.readString(1138166333448L);
                    break;
            }
        }
    }

    private static void writeIcon(ProtoOutputStream proto, NotificationHistory.HistoricalNotification notification) {
        long token = proto.start(1146756268044L);
        proto.write(1159641169921L, notification.getIcon().getType());
        int type = notification.getIcon().getType();
        if (type == 2) {
            proto.write(1120986464259L, notification.getIcon().getResId());
            if (!notification.getPackage().equals(notification.getIcon().getResPackage())) {
                proto.write(1138166333444L, notification.getIcon().getResPackage());
            }
        } else if (type == 3) {
            proto.write(1151051235333L, notification.getIcon().getDataBytes());
            proto.write(1120986464262L, notification.getIcon().getDataLength());
            proto.write(1120986464263L, notification.getIcon().getDataOffset());
        } else if (type == 4) {
            proto.write(1138166333448L, notification.getIcon().getUriString());
        }
        proto.end(token);
    }

    private static void writeNotification(ProtoOutputStream proto, String[] stringPool, NotificationHistory.HistoricalNotification notification) {
        long token = proto.start(2246267895811L);
        int packageIndex = Arrays.binarySearch(stringPool, notification.getPackage());
        if (packageIndex >= 0) {
            proto.write(1120986464258L, packageIndex + 1);
        } else {
            Slog.w(TAG, "notification package name (" + notification.getPackage() + ") not found in string cache");
            proto.write(1138166333441L, notification.getPackage());
        }
        int channelNameIndex = Arrays.binarySearch(stringPool, notification.getChannelName());
        if (channelNameIndex >= 0) {
            proto.write(1120986464260L, channelNameIndex + 1);
        } else {
            Slog.w(TAG, "notification channel name (" + notification.getChannelName() + ") not found in string cache");
            proto.write(1138166333443L, notification.getChannelName());
        }
        int channelIdIndex = Arrays.binarySearch(stringPool, notification.getChannelId());
        if (channelIdIndex >= 0) {
            proto.write(1120986464262L, channelIdIndex + 1);
        } else {
            Slog.w(TAG, "notification channel id (" + notification.getChannelId() + ") not found in string cache");
            proto.write(1138166333445L, notification.getChannelId());
        }
        if (!TextUtils.isEmpty(notification.getConversationId())) {
            int conversationIdIndex = Arrays.binarySearch(stringPool, notification.getConversationId());
            if (conversationIdIndex >= 0) {
                proto.write(1120986464270L, conversationIdIndex + 1);
            } else {
                Slog.w(TAG, "notification conversation id (" + notification.getConversationId() + ") not found in string cache");
                proto.write(1138166333453L, notification.getConversationId());
            }
        }
        proto.write(1120986464263L, notification.getUid());
        proto.write(1120986464264L, notification.getUserId());
        proto.write(1112396529673L, notification.getPostedTimeMs());
        proto.write(1138166333450L, notification.getTitle());
        proto.write(1138166333451L, notification.getText());
        writeIcon(proto, notification);
        proto.end(token);
    }

    public static void read(InputStream in, NotificationHistory notifications, NotificationHistoryFilter filter) throws IOException {
        ProtoInputStream proto = new ProtoInputStream(in);
        List<String> stringPool = new ArrayList<>();
        while (true) {
            int nextField = proto.nextField();
            if (nextField == -1) {
                break;
            } else if (nextField == 1) {
                stringPool = readStringPool(proto);
            } else if (nextField == 3) {
                readNotification(proto, stringPool, notifications, filter);
            }
        }
        if (filter.isFiltering()) {
            notifications.poolStringsFromNotifications();
        } else {
            notifications.addPooledStrings(stringPool);
        }
    }

    public static void write(OutputStream out, NotificationHistory notifications, int version) {
        ProtoOutputStream proto = new ProtoOutputStream(out);
        proto.write(1120986464258L, version);
        writeStringPool(proto, notifications);
        List<NotificationHistory.HistoricalNotification> notificationsToWrite = notifications.getNotificationsToWrite();
        int count = notificationsToWrite.size();
        for (int i = 0; i < count; i++) {
            writeNotification(proto, notifications.getPooledStringsToWrite(), notificationsToWrite.get(i));
        }
        proto.flush();
    }
}