package com.android.server.am.firewall;

import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class VivoSpecialRuleItem {
    private List<String> blackList;
    private List<String> componentList;
    private int kind;
    private String name;
    private List<String> whiteList;

    public VivoSpecialRuleItem(int kind, String name, List<String> whiteList, List<String> blackList, List<String> componentList) {
        this.kind = kind;
        this.name = name;
        this.whiteList = whiteList;
        this.blackList = blackList;
        this.componentList = componentList;
        if (whiteList == null) {
            this.whiteList = new ArrayList();
        }
        if (this.blackList == null) {
            this.blackList = new ArrayList();
        }
        if (this.componentList == null) {
            this.componentList = new ArrayList();
        }
    }

    public boolean checkActivityComponentState(String callerPackage, String topComponent) {
        if (this.componentList.contains(topComponent)) {
            return false;
        }
        return this.blackList.contains(callerPackage) || this.blackList.contains("*");
    }

    public boolean isWhiteListContains(String packageName) {
        if (this.whiteList.contains(packageName) || this.whiteList.contains("*")) {
            return true;
        }
        return false;
    }

    public boolean isWhiteListEmpty() {
        return this.whiteList.isEmpty();
    }

    public boolean isBlackListContains(String packageName) {
        if (this.blackList.contains(packageName) || this.blackList.contains("*")) {
            return true;
        }
        return false;
    }

    public boolean isBlackListEmpty() {
        return this.blackList.isEmpty();
    }

    public String toString() {
        return "kind:" + this.kind + ",key:" + this.name + ",whitelist:" + this.whiteList.toString() + ",blacklist:" + this.blackList.toString() + ",component:" + this.componentList.toString();
    }
}