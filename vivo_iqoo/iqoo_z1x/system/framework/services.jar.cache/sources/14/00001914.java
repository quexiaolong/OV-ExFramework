package com.android.server.soundtrigger_middleware;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/* loaded from: classes2.dex */
class ObjectPrinter {
    public static final int kDefaultMaxCollectionLength = 16;

    ObjectPrinter() {
    }

    static String print(Object obj) {
        return print(obj, false, 16);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static String print(Object obj, boolean deep, int maxCollectionLength) {
        StringBuilder builder = new StringBuilder();
        print(builder, obj, deep, maxCollectionLength);
        return builder.toString();
    }

    static String printPublicFields(Object obj, boolean deep, int maxCollectionLength) {
        StringBuilder builder = new StringBuilder();
        printPublicFields(builder, obj, deep, maxCollectionLength);
        return builder.toString();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static void print(StringBuilder builder, Object obj, boolean deep, int maxCollectionLength) {
        try {
            if (obj == null) {
                builder.append("null");
            } else if (obj instanceof Boolean) {
                builder.append(obj.toString());
            } else if (obj instanceof Number) {
                builder.append(obj.toString());
            } else if (obj instanceof Character) {
                builder.append('\'');
                builder.append(obj.toString());
                builder.append('\'');
            } else if (obj instanceof String) {
                builder.append('\"');
                builder.append(obj.toString());
                builder.append('\"');
            } else {
                Class cls = obj.getClass();
                if (Collection.class.isAssignableFrom(cls)) {
                    Collection collection = (Collection) obj;
                    builder.append("[ ");
                    int length = collection.size();
                    boolean isLong = false;
                    int i = 0;
                    Iterator it = collection.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        Object child = it.next();
                        if (i > 0) {
                            builder.append(", ");
                        }
                        if (i >= maxCollectionLength) {
                            isLong = true;
                            break;
                        } else {
                            print(builder, child, deep, maxCollectionLength);
                            i++;
                        }
                    }
                    if (isLong) {
                        builder.append("... (+");
                        builder.append(length - maxCollectionLength);
                        builder.append(" entries)");
                    }
                    builder.append(" ]");
                } else if (Map.class.isAssignableFrom(cls)) {
                    Map<?, ?> map = (Map) obj;
                    builder.append("< ");
                    int length2 = map.size();
                    boolean isLong2 = false;
                    int i2 = 0;
                    Iterator<Map.Entry<?, ?>> it2 = map.entrySet().iterator();
                    while (true) {
                        if (!it2.hasNext()) {
                            break;
                        }
                        Map.Entry<?, ?> child2 = it2.next();
                        if (i2 > 0) {
                            builder.append(", ");
                        }
                        if (i2 >= maxCollectionLength) {
                            isLong2 = true;
                            break;
                        }
                        print(builder, child2.getKey(), deep, maxCollectionLength);
                        builder.append(": ");
                        print(builder, child2.getValue(), deep, maxCollectionLength);
                        i2++;
                    }
                    if (isLong2) {
                        builder.append("... (+");
                        builder.append(length2 - maxCollectionLength);
                        builder.append(" entries)");
                    }
                    builder.append(" >");
                } else if (cls.isArray()) {
                    builder.append("[ ");
                    int length3 = Array.getLength(obj);
                    boolean isLong3 = false;
                    int i3 = 0;
                    while (true) {
                        if (i3 >= length3) {
                            break;
                        }
                        if (i3 > 0) {
                            builder.append(", ");
                        }
                        if (i3 >= maxCollectionLength) {
                            isLong3 = true;
                            break;
                        } else {
                            print(builder, Array.get(obj, i3), deep, maxCollectionLength);
                            i3++;
                        }
                    }
                    if (isLong3) {
                        builder.append("... (+");
                        builder.append(length3 - maxCollectionLength);
                        builder.append(" entries)");
                    }
                    builder.append(" ]");
                } else if (!deep) {
                    builder.append(obj.toString());
                } else {
                    printPublicFields(builder, obj, deep, maxCollectionLength);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void printPublicFields(StringBuilder builder, Object obj, boolean deep, int maxCollectionLength) {
        Field[] declaredFields;
        try {
            Class cls = obj.getClass();
            builder.append("{ ");
            boolean first = true;
            for (Field fld : cls.getDeclaredFields()) {
                int mod = fld.getModifiers();
                if ((mod & 1) != 0 && (mod & 8) == 0) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append(", ");
                    }
                    builder.append(fld.getName());
                    builder.append(": ");
                    print(builder, fld.get(obj), deep, maxCollectionLength);
                }
            }
            builder.append(" }");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}