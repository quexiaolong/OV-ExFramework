package com.vivo.services.configurationManager;

import android.content.ContentValues;
import java.util.ArrayList;
import java.util.List;
import vivo.app.configuration.ContentValuesList;
import vivo.app.configuration.StringList;
import vivo.app.configuration.Switch;

/* loaded from: classes.dex */
public class ListConvertHelper {
    private static final String TAG = "ConfigurationManager";

    public static List<Object> convertSwitchList2ObjectList(List<Switch> src) {
        List<Object> to = new ArrayList<>();
        if (src == null) {
            return to;
        }
        for (Switch s : src) {
            to.add(s);
        }
        return to;
    }

    public static List<Object> convertStringList2ObjectList(List<StringList> src) {
        List<Object> to = new ArrayList<>();
        if (src == null) {
            return to;
        }
        for (StringList s : src) {
            to.add(s);
        }
        return to;
    }

    public static List<Object> convertContentValuesList2ObjectList(List<ContentValuesList> src) {
        List<Object> to = new ArrayList<>();
        if (src == null) {
            return to;
        }
        for (ContentValuesList s : src) {
            to.add(s);
        }
        return to;
    }

    public static List<Switch> convertObjectList2SwitchList(List<Object> src) {
        List<Switch> to = new ArrayList<>();
        if (src == null) {
            return to;
        }
        for (Object o : src) {
            to.add((Switch) o);
        }
        return to;
    }

    public static List<StringList> convertObjectList2StringList(List<Object> src) {
        List<StringList> to = new ArrayList<>();
        if (src == null) {
            return to;
        }
        for (Object o : src) {
            to.add((StringList) o);
        }
        return to;
    }

    public static List<ContentValuesList> convertObjectList2ContentValuesList(List<Object> src) {
        List<ContentValuesList> to = new ArrayList<>();
        if (src == null) {
            return to;
        }
        for (Object o : src) {
            to.add((ContentValuesList) o);
        }
        return to;
    }

    public static boolean compareStringList(List<String> old, List<String> current) {
        if (old != null || current == null) {
            if (old != null && current == null) {
                return true;
            }
            if (old == null && current == null) {
                return false;
            }
            if (old.size() != current.size()) {
                return true;
            }
            for (int i = 0; i < old.size(); i++) {
                if (!old.get(i).equals(current.get(i))) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:19:0x002a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static boolean compareContentValuesList(java.util.HashMap<java.lang.String, android.content.ContentValues> r7, java.util.HashMap<java.lang.String, android.content.ContentValues> r8) {
        /*
            r0 = 0
            if (r7 != 0) goto L6
            if (r8 == 0) goto L6
            return r0
        L6:
            r1 = 1
            if (r7 == 0) goto Lc
            if (r8 != 0) goto Lc
            return r1
        Lc:
            if (r7 != 0) goto L11
            if (r8 != 0) goto L11
            return r0
        L11:
            int r2 = r7.size()
            int r3 = r8.size()
            if (r2 == r3) goto L1c
            return r1
        L1c:
            java.util.Set r2 = r7.entrySet()
            java.util.Iterator r2 = r2.iterator()
        L24:
            boolean r3 = r2.hasNext()
            if (r3 == 0) goto L51
            java.lang.Object r3 = r2.next()
            java.util.Map$Entry r3 = (java.util.Map.Entry) r3
            java.lang.Object r4 = r3.getKey()
            java.lang.String r4 = (java.lang.String) r4
            boolean r5 = r8.containsKey(r4)
            if (r5 == 0) goto L50
            java.lang.Object r5 = r3.getValue()
            android.content.ContentValues r5 = (android.content.ContentValues) r5
            java.lang.Object r6 = r8.get(r4)
            android.content.ContentValues r6 = (android.content.ContentValues) r6
            boolean r5 = compareContentValues(r5, r6)
            if (r5 != 0) goto L4f
            return r1
        L4f:
            goto L24
        L50:
            return r1
        L51:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vivo.services.configurationManager.ListConvertHelper.compareContentValuesList(java.util.HashMap, java.util.HashMap):boolean");
    }

    private static boolean compareContentValues(ContentValues old, ContentValues current) {
        if (old == null && current != null) {
            return false;
        }
        if (old == null || current != null) {
            if (old == null && current == null) {
                return true;
            }
            if (old.keySet().size() != current.keySet().size()) {
                return false;
            }
            for (String key : old.keySet()) {
                if (!old.getAsString(key).equals(current.getAsString(key))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static boolean isSwitchRepeated(List<Switch> list, Switch w) {
        String name;
        if (list == null || w == null || (name = w.getName()) == null) {
            return false;
        }
        for (Switch tmp : list) {
            if (name.equals(tmp.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isStringListRepeated(List<StringList> list, StringList sl) {
        String name;
        if (list == null || sl == null || (name = sl.getName()) == null) {
            return false;
        }
        for (StringList tmp : list) {
            if (name.equals(tmp.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isContentValuesListRepeated(List<ContentValuesList> list, ContentValuesList sl) {
        String name;
        if (list == null || sl == null || (name = sl.getName()) == null) {
            return false;
        }
        for (ContentValuesList tmp : list) {
            if (name.equals(tmp.getName())) {
                return true;
            }
        }
        return false;
    }
}