package com.android.server.integrity.parser;

/* loaded from: classes.dex */
public class RuleIndexRange {
    private int mEndIndex;
    private int mStartIndex;

    public RuleIndexRange(int startIndex, int endIndex) {
        this.mStartIndex = startIndex;
        this.mEndIndex = endIndex;
    }

    public int getStartIndex() {
        return this.mStartIndex;
    }

    public int getEndIndex() {
        return this.mEndIndex;
    }

    public boolean equals(Object object) {
        return this.mStartIndex == ((RuleIndexRange) object).getStartIndex() && this.mEndIndex == ((RuleIndexRange) object).getEndIndex();
    }

    public String toString() {
        return String.format("Range{%d, %d}", Integer.valueOf(this.mStartIndex), Integer.valueOf(this.mEndIndex));
    }
}