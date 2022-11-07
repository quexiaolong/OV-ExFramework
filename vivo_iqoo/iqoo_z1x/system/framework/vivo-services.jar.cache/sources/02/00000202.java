package com.android.server.display.color;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.vivo.face.common.data.Constants;
import com.vivo.services.rms.sdk.Consts;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import vivo.util.VSlog;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class ExynosDisplayUtils {
    private static final String TAG = "ExynosDisplayUtils";

    private ExynosDisplayUtils() {
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String getStringFromFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            VSlog.e(TAG, fileName + " File not found");
            return null;
        } else if (!file.isFile()) {
            VSlog.e(TAG, fileName + " is not File");
            return null;
        } else {
            InputStream in = null;
            byte[] buffer = new byte[Consts.ProcessStates.FOCUS];
            int length = 0;
            for (int i = 0; i < 1024; i++) {
                buffer[i] = 0;
            }
            try {
                try {
                    if (fileName != null) {
                        try {
                            in = new FileInputStream(new File(fileName));
                        } catch (Exception e) {
                            VSlog.e(TAG, "Exception : " + e + " , in : " + in + " , value : " + value + " , length : " + length);
                            e.printStackTrace();
                            if (in != null) {
                                in.close();
                            }
                        }
                    }
                    if (in != null) {
                        length = in.read(buffer);
                        value = length > 0 ? new String(buffer, 0, length - 1, StandardCharsets.UTF_8) : null;
                        in.close();
                    }
                    if (in != null) {
                        in.close();
                    }
                } catch (Throwable th) {
                    if (0 != 0) {
                        try {
                            in.close();
                        } catch (Exception e2) {
                            VSlog.e(TAG, "File Close error");
                        }
                    }
                    throw th;
                }
            } catch (Exception e3) {
                VSlog.e(TAG, "File Close error");
            }
            return value;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean sysfsWrite(String sysfs, int value) {
        FileOutputStream out = null;
        File myfile = new File(sysfs);
        try {
            if (myfile.exists()) {
                try {
                    FileOutputStream out2 = new FileOutputStream(myfile);
                    out2.write(Integer.toString(value).getBytes());
                    out2.close();
                    return true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
            }
            return false;
        } catch (IOException e2) {
            e2.printStackTrace();
            try {
                out.close();
            } catch (Exception err) {
                err.printStackTrace();
            }
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean sysfsWriteSting(String sysfs, String value) {
        FileOutputStream out = null;
        File myfile = new File(sysfs);
        if (myfile.exists()) {
            try {
                try {
                    FileOutputStream out2 = new FileOutputStream(myfile);
                    out2.write(value.getBytes());
                    out2.close();
                    return true;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return false;
                }
            } catch (IOException e2) {
                e2.printStackTrace();
                try {
                    out.close();
                } catch (Exception err) {
                    err.printStackTrace();
                }
                return false;
            }
        }
        return false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static boolean existFile(String file_path) {
        File file = new File(file_path);
        if (!file.exists()) {
            VSlog.e(TAG, file_path + " File not found");
            return false;
        } else if (!file.isFile()) {
            VSlog.e(TAG, file_path + " is not File");
            return false;
        } else {
            return true;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:123:0x0220 A[Catch: Exception -> 0x0224, TRY_ENTER, TRY_LEAVE, TryCatch #13 {Exception -> 0x0224, blocks: (B:123:0x0220, B:131:0x0235), top: B:148:0x005f }] */
    /* JADX WARN: Removed duplicated region for block: B:131:0x0235 A[Catch: Exception -> 0x0224, TRY_ENTER, TRY_LEAVE, TryCatch #13 {Exception -> 0x0224, blocks: (B:123:0x0220, B:131:0x0235), top: B:148:0x005f }] */
    /* JADX WARN: Removed duplicated region for block: B:150:0x023f A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:171:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:172:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static java.lang.String[] parserXML(java.lang.String r26, java.lang.String r27, java.lang.String r28) {
        /*
            Method dump skipped, instructions count: 586
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.ExynosDisplayUtils.parserXML(java.lang.String, java.lang.String, java.lang.String):java.lang.String[]");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:128:0x0255 A[Catch: Exception -> 0x0259, TRY_ENTER, TRY_LEAVE, TryCatch #12 {Exception -> 0x0259, blocks: (B:128:0x0255, B:136:0x026a), top: B:153:0x0036 }] */
    /* JADX WARN: Removed duplicated region for block: B:136:0x026a A[Catch: Exception -> 0x0259, TRY_ENTER, TRY_LEAVE, TryCatch #12 {Exception -> 0x0259, blocks: (B:128:0x0255, B:136:0x026a), top: B:153:0x0036 }] */
    /* JADX WARN: Removed duplicated region for block: B:148:0x0274 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:166:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:167:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static java.lang.String[] parserTuneXML(java.lang.String r28, java.lang.String r29, java.lang.String r30) {
        /*
            Method dump skipped, instructions count: 639
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.ExynosDisplayUtils.parserTuneXML(java.lang.String, java.lang.String, java.lang.String):java.lang.String[]");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String[] parserXMLAttribute(String xml_path, String node_name, String attr_name) {
        FileInputStream fis = null;
        File file = new File(xml_path);
        String str = null;
        if (!file.isFile()) {
            VSlog.e(TAG, xml_path + " File not found");
            return null;
        }
        try {
            try {
                try {
                    fis = new FileInputStream(xml_path);
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput(fis, null);
                    int eventType = xpp.getEventType();
                    List<String> attrArray = new ArrayList<>();
                    int eventType2 = eventType;
                    while (eventType2 != 1) {
                        if (eventType2 != 0 && eventType2 != 1) {
                            if (eventType2 == 2) {
                                String startTag = xpp.getName();
                                if (startTag.equals(node_name)) {
                                    String attrValue = xpp.getAttributeValue(str, attr_name);
                                    attrArray.add(attrValue);
                                }
                            } else if (eventType2 == 3) {
                                String endTag = xpp.getName();
                                if (endTag.equals(node_name)) {
                                }
                            }
                        }
                        try {
                            eventType2 = xpp.next();
                            str = null;
                        } catch (IOException e) {
                            e = e;
                            e.printStackTrace();
                            if (fis != null) {
                                fis.close();
                                return null;
                            }
                            return null;
                        } catch (XmlPullParserException e2) {
                            e = e2;
                            e.printStackTrace();
                            if (fis != null) {
                                fis.close();
                                return null;
                            }
                            return null;
                        }
                    }
                    if (attrArray.size() == 0) {
                        try {
                            fis.close();
                        } catch (Exception e3) {
                            VSlog.e(TAG, "File Close error");
                        }
                        return null;
                    }
                    String[] return_array = new String[attrArray.size()];
                    for (int i = 0; i < attrArray.size(); i++) {
                        return_array[i] = attrArray.get(i);
                        return_array[i] = return_array[i].trim();
                    }
                    try {
                        fis.close();
                    } catch (Exception e4) {
                        VSlog.e(TAG, "File Close error");
                    }
                    return return_array;
                } catch (IOException e5) {
                    e = e5;
                } catch (XmlPullParserException e6) {
                    e = e6;
                } catch (Throwable th) {
                    th = th;
                    Throwable th2 = th;
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (Exception e7) {
                            VSlog.e(TAG, "File Close error");
                        }
                    }
                    throw th2;
                }
            } catch (Exception e8) {
                VSlog.e(TAG, "File Close error");
                return null;
            }
        } catch (Throwable th3) {
            th = th3;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:109:0x017f A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:127:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:128:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:83:0x0160 A[Catch: Exception -> 0x0164, TRY_ENTER, TRY_LEAVE, TryCatch #7 {Exception -> 0x0164, blocks: (B:83:0x0160, B:91:0x0175), top: B:107:0x0034 }] */
    /* JADX WARN: Removed duplicated region for block: B:91:0x0175 A[Catch: Exception -> 0x0164, TRY_ENTER, TRY_LEAVE, TryCatch #7 {Exception -> 0x0164, blocks: (B:83:0x0160, B:91:0x0175), top: B:107:0x0034 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static java.lang.String[] parserXMLALText(java.lang.String r23, java.lang.String r24, int r25, java.lang.String r26) {
        /*
            Method dump skipped, instructions count: 394
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.ExynosDisplayUtils.parserXMLALText(java.lang.String, java.lang.String, int, java.lang.String):java.lang.String[]");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String[] parserXMLNodeText(String xml_path, String node) {
        FileInputStream fis = null;
        File file = new File(xml_path);
        if (!file.isFile()) {
            VSlog.e(TAG, xml_path + " File not found");
            return null;
        }
        try {
            try {
                try {
                    fis = new FileInputStream(xml_path);
                    XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput(fis, null);
                    int eventType = xpp.getEventType();
                    List<String> input = new ArrayList<>();
                    int eventType2 = 0;
                    for (int eventType3 = eventType; eventType3 != 1; eventType3 = xpp.next()) {
                        if (eventType3 != 0 && eventType3 != 1) {
                            if (eventType3 == 2) {
                                String startTag = xpp.getName();
                                if (startTag.equals(node)) {
                                    eventType2 = 1;
                                }
                            } else if (eventType3 == 3) {
                                String endTag = xpp.getName();
                                if (endTag.equals(node)) {
                                    eventType2 = 0;
                                }
                            } else if (eventType3 == 4) {
                                if (eventType2 == 1) {
                                    input.add(xpp.getText());
                                }
                            }
                        }
                    }
                    if (input.size() == 0) {
                        try {
                            fis.close();
                        } catch (Exception e) {
                            VSlog.e(TAG, "File Close error");
                        }
                        return null;
                    }
                    String[] return_array = new String[input.size()];
                    for (int i = 0; i < input.size(); i++) {
                        return_array[i] = input.get(i);
                        return_array[i] = return_array[i].trim();
                    }
                    try {
                        fis.close();
                    } catch (Exception e2) {
                        VSlog.e(TAG, "File Close error");
                    }
                    return return_array;
                } catch (IOException e3) {
                    e3.printStackTrace();
                    if (fis != null) {
                        fis.close();
                        return null;
                    }
                    return null;
                } catch (XmlPullParserException e4) {
                    e4.printStackTrace();
                    if (fis != null) {
                        fis.close();
                        return null;
                    }
                    return null;
                }
            } catch (Exception e5) {
                VSlog.e(TAG, "File Close error");
                return null;
            }
        } catch (Throwable ee) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e6) {
                    VSlog.e(TAG, "File Close error");
                }
            }
            throw ee;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:112:0x01b5 A[Catch: Exception -> 0x01b9, TRY_ENTER, TRY_LEAVE, TryCatch #13 {Exception -> 0x01b9, blocks: (B:112:0x01b5, B:120:0x01ca), top: B:140:0x0036 }] */
    /* JADX WARN: Removed duplicated region for block: B:120:0x01ca A[Catch: Exception -> 0x01b9, TRY_ENTER, TRY_LEAVE, TryCatch #13 {Exception -> 0x01b9, blocks: (B:112:0x01b5, B:120:0x01ca), top: B:140:0x0036 }] */
    /* JADX WARN: Removed duplicated region for block: B:134:0x01d4 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:159:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:160:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static java.lang.String[] parserFactoryXMLALText(java.lang.String r25, java.lang.String r26, java.lang.String r27, int r28, java.lang.String r29) {
        /*
            Method dump skipped, instructions count: 479
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.ExynosDisplayUtils.parserFactoryXMLALText(java.lang.String, java.lang.String, java.lang.String, int, java.lang.String):java.lang.String[]");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Unreachable block: B:33:0x00a6
        	at jadx.core.dex.visitors.blocks.BlockProcessor.checkForUnreachableBlocks(BlockProcessor.java:81)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.processBlocksTree(BlockProcessor.java:47)
        	at jadx.core.dex.visitors.blocks.BlockProcessor.visit(BlockProcessor.java:39)
        */
    static java.lang.String[] parserFactoryXMLAttribute(java.lang.String r22, java.lang.String r23, java.lang.String r24, java.lang.String r25) {
        /*
            Method dump skipped, instructions count: 430
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.ExynosDisplayUtils.parserFactoryXMLAttribute(java.lang.String, java.lang.String, java.lang.String, java.lang.String):java.lang.String[]");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:122:0x0202 A[Catch: Exception -> 0x0206, TRY_ENTER, TRY_LEAVE, TryCatch #12 {Exception -> 0x0206, blocks: (B:122:0x0202, B:130:0x0215), top: B:147:0x0035 }] */
    /* JADX WARN: Removed duplicated region for block: B:130:0x0215 A[Catch: Exception -> 0x0206, TRY_ENTER, TRY_LEAVE, TryCatch #12 {Exception -> 0x0206, blocks: (B:122:0x0202, B:130:0x0215), top: B:147:0x0035 }] */
    /* JADX WARN: Removed duplicated region for block: B:150:0x021f A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:160:? A[RETURN, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:161:? A[RETURN, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static java.lang.String[] parserFactoryXMLText(java.lang.String r26, java.lang.String r27, java.lang.String r28, int r29, int r30) {
        /*
            Method dump skipped, instructions count: 554
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.color.ExynosDisplayUtils.parserFactoryXMLText(java.lang.String, java.lang.String, java.lang.String, int, int):java.lang.String[]");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void sendEmptyUpdate() {
        if (ExynosDisplayATC.TUNE_MODE) {
            VSlog.d(TAG, "sendEmptyUpdate");
        }
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (flinger != null) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(0);
                flinger.transact(Constants.CMD.CMD_HIDL_INITIALIZATION, data, null, 0);
                data.recycle();
            }
        } catch (RemoteException e) {
            VSlog.d(TAG, "failed to sendEmptyUpdate");
        }
    }
}