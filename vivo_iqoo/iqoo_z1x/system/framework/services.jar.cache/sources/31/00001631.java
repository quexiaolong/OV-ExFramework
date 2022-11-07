package com.android.server.pm;

import android.os.Bundle;
import android.os.UserManager;
import android.util.SparseArray;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

/* loaded from: classes.dex */
public class RestrictionsSet {
    private static final String TAG_RESTRICTIONS = "restrictions";
    private static final String TAG_RESTRICTIONS_USER = "restrictions_user";
    private static final String USER_ID = "user_id";
    private final SparseArray<Bundle> mUserRestrictions = new SparseArray<>(0);

    public RestrictionsSet() {
    }

    public RestrictionsSet(int userId, Bundle restrictions) {
        if (restrictions.isEmpty()) {
            throw new IllegalArgumentException("empty restriction bundle cannot be added.");
        }
        this.mUserRestrictions.put(userId, restrictions);
    }

    public boolean updateRestrictions(int userId, Bundle restrictions) {
        boolean changed = !UserRestrictionsUtils.areEqual(this.mUserRestrictions.get(userId), restrictions);
        if (!changed) {
            return false;
        }
        if (!UserRestrictionsUtils.isEmpty(restrictions)) {
            this.mUserRestrictions.put(userId, restrictions);
        } else {
            this.mUserRestrictions.delete(userId);
        }
        return true;
    }

    public void moveRestriction(RestrictionsSet destRestrictions, String restriction) {
        int i = 0;
        while (i < this.mUserRestrictions.size()) {
            int userId = this.mUserRestrictions.keyAt(i);
            Bundle from = this.mUserRestrictions.valueAt(i);
            if (UserRestrictionsUtils.contains(from, restriction)) {
                from.remove(restriction);
                Bundle to = destRestrictions.getRestrictions(userId);
                if (to == null) {
                    Bundle to2 = new Bundle();
                    to2.putBoolean(restriction, true);
                    destRestrictions.updateRestrictions(userId, to2);
                } else {
                    to.putBoolean(restriction, true);
                }
                if (from.isEmpty()) {
                    this.mUserRestrictions.removeAt(i);
                    i--;
                }
            }
            i++;
        }
    }

    public boolean isEmpty() {
        return this.mUserRestrictions.size() == 0;
    }

    public Bundle mergeAll() {
        Bundle result = new Bundle();
        for (int i = 0; i < this.mUserRestrictions.size(); i++) {
            UserRestrictionsUtils.merge(result, this.mUserRestrictions.valueAt(i));
        }
        return result;
    }

    public List<UserManager.EnforcingUser> getEnforcingUsers(String restriction, int deviceOwnerUserId) {
        List<UserManager.EnforcingUser> result = new ArrayList<>();
        for (int i = 0; i < this.mUserRestrictions.size(); i++) {
            if (UserRestrictionsUtils.contains(this.mUserRestrictions.valueAt(i), restriction)) {
                result.add(getEnforcingUser(this.mUserRestrictions.keyAt(i), deviceOwnerUserId));
            }
        }
        return result;
    }

    private UserManager.EnforcingUser getEnforcingUser(int userId, int deviceOwnerUserId) {
        int source;
        if (deviceOwnerUserId == userId) {
            source = 2;
        } else {
            source = 4;
        }
        return new UserManager.EnforcingUser(userId, source);
    }

    public Bundle getRestrictions(int userId) {
        return this.mUserRestrictions.get(userId);
    }

    public boolean remove(int userId) {
        boolean hasUserRestriction = this.mUserRestrictions.contains(userId);
        this.mUserRestrictions.remove(userId);
        return hasUserRestriction;
    }

    public void removeAllRestrictions() {
        this.mUserRestrictions.clear();
    }

    public void writeRestrictions(XmlSerializer serializer, String outerTag) throws IOException {
        serializer.startTag(null, outerTag);
        for (int i = 0; i < this.mUserRestrictions.size(); i++) {
            serializer.startTag(null, TAG_RESTRICTIONS_USER);
            serializer.attribute(null, USER_ID, String.valueOf(this.mUserRestrictions.keyAt(i)));
            UserRestrictionsUtils.writeRestrictions(serializer, this.mUserRestrictions.valueAt(i), TAG_RESTRICTIONS);
            serializer.endTag(null, TAG_RESTRICTIONS_USER);
        }
        serializer.endTag(null, outerTag);
    }

    public static RestrictionsSet readRestrictions(XmlPullParser parser, String outerTag) throws IOException, XmlPullParserException {
        RestrictionsSet restrictionsSet = new RestrictionsSet();
        int userId = 0;
        while (true) {
            int type = parser.next();
            if (type != 1) {
                String tag = parser.getName();
                if (type == 3 && outerTag.equals(tag)) {
                    return restrictionsSet;
                }
                if (type == 2 && TAG_RESTRICTIONS_USER.equals(tag)) {
                    userId = Integer.parseInt(parser.getAttributeValue(null, USER_ID));
                } else if (type == 2 && TAG_RESTRICTIONS.equals(tag)) {
                    Bundle restrictions = UserRestrictionsUtils.readRestrictions(parser);
                    restrictionsSet.updateRestrictions(userId, restrictions);
                }
            } else {
                throw new XmlPullParserException("restrictions cannot be read as xml is malformed.");
            }
        }
    }

    public void dumpRestrictions(PrintWriter pw, String prefix) {
        boolean noneSet = true;
        for (int i = 0; i < this.mUserRestrictions.size(); i++) {
            pw.println(prefix + "User Id: " + this.mUserRestrictions.keyAt(i));
            StringBuilder sb = new StringBuilder();
            sb.append(prefix);
            sb.append("  ");
            UserRestrictionsUtils.dumpRestrictions(pw, sb.toString(), this.mUserRestrictions.valueAt(i));
            noneSet = false;
        }
        if (noneSet) {
            pw.println(prefix + "none");
        }
    }

    public boolean containsKey(int userId) {
        return this.mUserRestrictions.contains(userId);
    }

    public int size() {
        return this.mUserRestrictions.size();
    }

    public int keyAt(int index) {
        return this.mUserRestrictions.keyAt(index);
    }

    public Bundle valueAt(int index) {
        return this.mUserRestrictions.valueAt(index);
    }
}