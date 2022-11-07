package com.android.server.locksettings;

import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/* loaded from: classes.dex */
public class PasswordSlotManager {
    private static final String GSI_RUNNING_PROP = "ro.gsid.image_running";
    private static final String SLOT_MAP_DIR = "/metadata/password_slots";
    private static final String TAG = "PasswordSlotManager";
    private Set<Integer> mActiveSlots;
    private Map<Integer, String> mSlotMap;

    protected String getSlotMapDir() {
        return SLOT_MAP_DIR;
    }

    protected int getGsiImageNumber() {
        return SystemProperties.getInt(GSI_RUNNING_PROP, 0);
    }

    public void refreshActiveSlots(Set<Integer> activeSlots) throws RuntimeException {
        if (this.mSlotMap == null) {
            this.mActiveSlots = new HashSet(activeSlots);
            return;
        }
        HashSet<Integer> slotsToDelete = new HashSet<>();
        for (Map.Entry<Integer, String> entry : this.mSlotMap.entrySet()) {
            if (entry.getValue().equals(getMode())) {
                slotsToDelete.add(entry.getKey());
            }
        }
        Iterator<Integer> it = slotsToDelete.iterator();
        while (it.hasNext()) {
            Integer slot = it.next();
            this.mSlotMap.remove(slot);
        }
        for (Integer slot2 : activeSlots) {
            this.mSlotMap.put(slot2, getMode());
        }
        saveSlotMap();
    }

    public void markSlotInUse(int slot) throws RuntimeException {
        ensureSlotMapLoaded();
        if (this.mSlotMap.containsKey(Integer.valueOf(slot)) && !this.mSlotMap.get(Integer.valueOf(slot)).equals(getMode())) {
            throw new IllegalStateException("password slot " + slot + " is not available");
        }
        this.mSlotMap.put(Integer.valueOf(slot), getMode());
        saveSlotMap();
    }

    public void markSlotDeleted(int slot) throws RuntimeException {
        ensureSlotMapLoaded();
        if (this.mSlotMap.containsKey(Integer.valueOf(slot)) && !this.mSlotMap.get(Integer.valueOf(slot)).equals(getMode())) {
            throw new IllegalStateException("password slot " + slot + " cannot be deleted");
        }
        this.mSlotMap.remove(Integer.valueOf(slot));
        saveSlotMap();
    }

    public Set<Integer> getUsedSlots() {
        ensureSlotMapLoaded();
        return Collections.unmodifiableSet(this.mSlotMap.keySet());
    }

    private File getSlotMapFile() {
        return Paths.get(getSlotMapDir(), "slot_map").toFile();
    }

    private String getMode() {
        int gsiIndex = getGsiImageNumber();
        if (gsiIndex > 0) {
            return "gsi" + gsiIndex;
        }
        return WatchlistLoggingHandler.WatchlistEventKeys.HOST;
    }

    protected Map<Integer, String> loadSlotMap(InputStream stream) throws IOException {
        HashMap<Integer, String> map = new HashMap<>();
        Properties props = new Properties();
        props.load(stream);
        for (String slotString : props.stringPropertyNames()) {
            int slot = Integer.parseInt(slotString);
            String owner = props.getProperty(slotString);
            map.put(Integer.valueOf(slot), owner);
        }
        return map;
    }

    private Map<Integer, String> loadSlotMap() {
        File file = getSlotMapFile();
        if (file.exists()) {
            try {
                FileInputStream stream = new FileInputStream(file);
                Map<Integer, String> loadSlotMap = loadSlotMap(stream);
                stream.close();
                return loadSlotMap;
            } catch (Exception e) {
                Slog.e(TAG, "Could not load slot map file", e);
            }
        }
        return new HashMap();
    }

    private void ensureSlotMapLoaded() {
        if (this.mSlotMap == null) {
            this.mSlotMap = loadSlotMap();
            Set<Integer> set = this.mActiveSlots;
            if (set != null) {
                refreshActiveSlots(set);
                this.mActiveSlots = null;
            }
        }
    }

    protected void saveSlotMap(OutputStream stream) throws IOException {
        if (this.mSlotMap == null) {
            return;
        }
        Properties props = new Properties();
        for (Map.Entry<Integer, String> entry : this.mSlotMap.entrySet()) {
            props.setProperty(entry.getKey().toString(), entry.getValue());
        }
        props.store(stream, "");
    }

    private void saveSlotMap() {
        if (this.mSlotMap == null) {
            return;
        }
        if (!getSlotMapFile().getParentFile().exists()) {
            Slog.w(TAG, "Not saving slot map, " + getSlotMapDir() + " does not exist");
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(getSlotMapFile());
            saveSlotMap(fos);
            fos.close();
        } catch (IOException e) {
            Slog.e(TAG, "failed to save password slot map", e);
        }
    }
}