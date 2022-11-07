package com.vivo.services.rms.appmng.namelist;

/* loaded from: classes.dex */
public class OomNode {
    public int adj;
    public int procState;
    public int schedGroup;

    public OomNode() {
    }

    public OomNode(int adj, int state, int sched) {
        this.adj = adj;
        this.procState = state;
        this.schedGroup = sched;
    }
}