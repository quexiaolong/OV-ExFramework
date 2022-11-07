package com.vivo.services.sarpower;

import java.util.Arrays;
import java.util.HashMap;

/* loaded from: classes.dex */
public class ConfigList {
    public String[] commandsBody;
    public String[] commandsHead;
    public String[] commandsOnC2K;
    public String model;
    public String[] wcommandsBody;
    public String[] wcommandsHead;
    public String[] wcommandsOnC2K;
    public String resetGSM = null;
    public String resetC2K = null;

    public ConfigList(String model, String[] commands, String[] wcommands, String[] commandsOnC2K, String[] wcommandsOnC2K) {
        this.model = model;
        this.commandsHead = commands;
        this.wcommandsHead = wcommands;
        this.commandsOnC2K = commandsOnC2K;
        this.wcommandsOnC2K = wcommandsOnC2K;
    }

    public ConfigList(String model, HashMap<String, String[]> commands, HashMap<String, String[]> wcommands, String[] commandsOnC2K, String[] wcommandsOnC2K) {
        this.model = model;
        this.commandsHead = commands.get("head");
        this.commandsBody = commands.get("body");
        this.wcommandsHead = wcommands.get("head");
        this.wcommandsBody = wcommands.get("body");
        this.commandsOnC2K = commandsOnC2K;
        this.wcommandsOnC2K = wcommandsOnC2K;
    }

    public ConfigList(String model) {
        this.model = model;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("model:" + this.model);
        sb.append("\nHead:" + Arrays.toString(this.commandsHead));
        sb.append("\nBody:" + Arrays.toString(this.commandsBody));
        sb.append("\nC2K:" + Arrays.toString(this.commandsOnC2K));
        sb.append("\nwHead:" + Arrays.toString(this.wcommandsHead));
        sb.append("\nwBody:" + Arrays.toString(this.wcommandsBody));
        sb.append("\nwC2K:" + Arrays.toString(this.wcommandsOnC2K));
        return sb.toString();
    }
}