package com.android.server.am.firewall;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/* loaded from: classes.dex */
public class VivoAppRuleItem {
    private static final int TYPE_ACTIVITY = 1;
    private static final int TYPE_ALL = 7;
    private static final int TYPE_PROVIDER = 4;
    private static final int TYPE_SERVICE = 2;
    private boolean mAllowBringup;
    private String mPackageName;
    private int mSystemType;
    private int mType1Limit;
    private int mType2Limit;
    private int mType3Limit;
    private int mType4Limit;
    private boolean hasRuleType1 = false;
    private boolean hasRuleType2 = false;
    private boolean hasRuleType3 = false;
    private boolean hasRuleType4 = false;
    private boolean hasRuleType6 = false;
    private boolean hasRuleType7 = false;
    private HashMap<String, Boolean> mBringupHashMap = new HashMap<>();
    private List<String> mType1PackageList = new ArrayList();
    private List<String> mType2PackageList = new ArrayList();
    private List<String> mType3PackageList = new ArrayList();
    private List<String> mType4PackageList = new ArrayList();
    private List<String> mType1ComponentList = new ArrayList();
    private List<String> mType2ComponentList = new ArrayList();

    public VivoAppRuleItem(String packageName) {
        this.mPackageName = packageName;
    }

    public boolean isAllowToBringup(String type, String pkgName) {
        return (this.hasRuleType3 && hasTypeLimit(3, type)) ? !this.mType3PackageList.contains(pkgName) : this.hasRuleType4 && hasTypeLimit(4, type) && this.mType4PackageList.contains(pkgName);
    }

    public boolean isAllowToBeBringedup(String type, String pkgName, String className) {
        if (this.hasRuleType1 && hasTypeLimit(1, type)) {
            return (this.mType1PackageList.contains(pkgName) || this.mType1ComponentList.contains(className)) ? false : true;
        } else if (this.hasRuleType2 && hasTypeLimit(2, type)) {
            return this.mType2PackageList.contains(pkgName) || this.mType2ComponentList.contains(className);
        } else {
            return this.mAllowBringup;
        }
    }

    public boolean isAllowToBeBringedupBySystem() {
        if (this.hasRuleType6) {
            return true;
        }
        if (this.hasRuleType7) {
            return false;
        }
        return this.mAllowBringup;
    }

    public void setRuleType(int ruleType, boolean hasType) {
        if (ruleType == 1) {
            this.hasRuleType1 = hasType;
        } else if (ruleType == 2) {
            this.hasRuleType2 = hasType;
        } else if (ruleType == 3) {
            this.hasRuleType3 = hasType;
        } else if (ruleType == 4) {
            this.hasRuleType4 = hasType;
        } else if (ruleType == 6) {
            this.hasRuleType6 = hasType;
        } else if (ruleType == 7) {
            this.hasRuleType7 = hasType;
        }
    }

    public boolean hasType(int type) {
        if (type == 0) {
            return this.mBringupHashMap.size() > 0;
        } else if (type != 1) {
            if (type != 2) {
                if (type != 3) {
                    if (type != 4) {
                        if (type != 6) {
                            if (type != 7) {
                                return false;
                            }
                            return this.hasRuleType7;
                        }
                        return this.hasRuleType6;
                    }
                    return this.hasRuleType4;
                }
                return this.hasRuleType3;
            }
            return this.hasRuleType2;
        } else {
            return this.hasRuleType1;
        }
    }

    public void addToTypeList(int type, String packageName) {
        if (type == 1) {
            if (this.hasRuleType1) {
                this.mType1PackageList.add(packageName);
            }
        } else if (type == 2) {
            if (this.hasRuleType2) {
                this.mType2PackageList.add(packageName);
            }
        } else if (type == 3) {
            if (this.hasRuleType3) {
                this.mType3PackageList.add(packageName);
            }
        } else if (type == 4 && this.hasRuleType4) {
            this.mType4PackageList.add(packageName);
        }
    }

    public void removeFromTypeList(int type, String packageName) {
        if (type < 1 || type > 4) {
            return;
        }
        if (type == 1) {
            if (this.hasRuleType1 && this.mType1PackageList.contains(packageName)) {
                this.mType1PackageList.remove(packageName);
            }
        } else if (type == 2) {
            if (this.hasRuleType2 && this.mType2PackageList.contains(packageName)) {
                this.mType2PackageList.remove(packageName);
            }
        } else if (type == 3) {
            if (this.hasRuleType3 && this.mType3PackageList.contains(packageName)) {
                this.mType3PackageList.remove(packageName);
            }
        } else if (type == 4 && this.hasRuleType4 && this.mType4PackageList.contains(packageName)) {
            this.mType4PackageList.remove(packageName);
        }
    }

    public ArrayList<String> getTypeList(int type) {
        if (type < 1 || type > 4) {
            return null;
        }
        if (type == 1) {
            if (this.hasRuleType1) {
                return (ArrayList) this.mType1PackageList;
            }
            return null;
        } else if (type == 2) {
            if (this.hasRuleType2) {
                return (ArrayList) this.mType2PackageList;
            }
            return null;
        } else if (type == 3) {
            if (this.hasRuleType3) {
                return (ArrayList) this.mType3PackageList;
            }
            return null;
        } else if (type == 4 && this.hasRuleType4) {
            return (ArrayList) this.mType4PackageList;
        } else {
            return null;
        }
    }

    public void putBringupRule(String packageName, boolean allowBringup) {
        this.mBringupHashMap.put(packageName, Boolean.valueOf(allowBringup));
    }

    public HashMap<String, Boolean> getBringupRule() {
        return this.mBringupHashMap;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public boolean isAllowBringup() {
        return this.mAllowBringup;
    }

    public void setAllowBringup(boolean allowed) {
        this.mAllowBringup = allowed;
    }

    public void setSystemType(int systemType) {
        this.mSystemType |= systemType;
    }

    public void setTypeLimit(int ruleType, int typeLimit) {
        typeLimit = (typeLimit <= 0 || typeLimit > 7) ? 7 : 7;
        if (ruleType == 1) {
            this.mType1Limit = typeLimit;
        } else if (ruleType == 2) {
            this.mType2Limit = typeLimit;
        } else if (ruleType == 3) {
            this.mType3Limit = typeLimit;
        } else if (ruleType == 4) {
            this.mType4Limit = typeLimit;
        }
    }

    private boolean hasTypeLimit(int ruleType, String type) {
        int typeLimit;
        if (ruleType != 1) {
            if (ruleType == 2) {
                typeLimit = this.mType2Limit;
            } else if (ruleType == 3) {
                typeLimit = this.mType3Limit;
            } else if (ruleType != 4) {
                return false;
            } else {
                typeLimit = this.mType4Limit;
            }
        } else {
            typeLimit = this.mType1Limit;
        }
        char c = 65535;
        int hashCode = type.hashCode();
        if (hashCode != -1655966961) {
            if (hashCode != -987494927) {
                if (hashCode == 1984153269 && type.equals(VivoFirewall.TYPE_SERVICE)) {
                    c = 1;
                }
            } else if (type.equals(VivoFirewall.TYPE_PROVIDER)) {
                c = 2;
            }
        } else if (type.equals(VivoFirewall.TYPE_ACTIVITY)) {
            c = 0;
        }
        return c != 0 ? c != 1 ? c == 2 && (typeLimit & 4) != 0 : (typeLimit & 2) != 0 : (typeLimit & 1) != 0;
    }

    public void addToComponentList(int type, String componentName) {
        if (type == 1) {
            if (this.hasRuleType1) {
                this.mType1ComponentList.add(componentName);
            }
        } else if (type == 2 && this.hasRuleType2) {
            this.mType2ComponentList.add(componentName);
        }
    }

    public void dump(PrintWriter pw) {
        pw.println(this.mPackageName + " " + this.mAllowBringup);
        if (this.mSystemType != 0) {
            pw.println("systemFlag: " + this.mSystemType);
        }
        if (this.hasRuleType1) {
            pw.println("has type 1");
            pw.println("typeLimit: " + this.mType1Limit);
            for (String item : this.mType1PackageList) {
                pw.println("  package:" + item);
            }
            for (String item2 : this.mType1ComponentList) {
                pw.println("  component:" + item2);
            }
        }
        if (this.hasRuleType2) {
            pw.println("has type 2");
            pw.println("typeLimit: " + this.mType2Limit);
            for (String item3 : this.mType2PackageList) {
                pw.println("  package:" + item3);
            }
            for (String item4 : this.mType2ComponentList) {
                pw.println("  component:" + item4);
            }
        }
        if (this.hasRuleType3) {
            pw.println("has type 3");
            pw.println("typeLimit: " + this.mType3Limit);
            for (String item5 : this.mType3PackageList) {
                pw.println("  package:" + item5);
            }
        }
        if (this.hasRuleType4) {
            pw.println("has type 4");
            pw.println("typeLimit: " + this.mType4Limit);
            for (String item6 : this.mType4PackageList) {
                pw.println("  package:" + item6);
            }
        }
        if (this.hasRuleType6) {
            pw.println("has type 6");
        }
        if (this.hasRuleType7) {
            pw.println("has type 7");
        }
    }

    public int getSystemType() {
        return this.mSystemType;
    }
}