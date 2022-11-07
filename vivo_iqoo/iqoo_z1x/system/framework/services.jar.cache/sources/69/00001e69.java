package com.android.server.wm;

/* loaded from: classes2.dex */
public interface IVivoDisplayRotation {
    boolean deferUpdateRotationForSplit(int i, int i2);

    boolean directReturnLastRotationInFreeform(int i);

    void dummy();

    int getSensorOrientation();

    boolean isRotationChanging();

    boolean needSensorRunningForSuggestion();

    int rotationForCast();

    void sendProposedRotationChangeToMultiWindowInternal(int i, int i2, boolean z);

    void setRotationChanging(boolean z);

    /* loaded from: classes2.dex */
    public interface IVivoDisplayRotationExport {
        IVivoDisplayRotation getVivoInjectInstance();

        default void dummyExport() {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().dummy();
            }
        }

        default int getSensorOrientation() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().getSensorOrientation();
            }
            return -1;
        }

        default boolean isRotationChanging() {
            if (getVivoInjectInstance() != null) {
                return getVivoInjectInstance().isRotationChanging();
            }
            return false;
        }

        default void setRotationChanging(boolean rotationChanging) {
            if (getVivoInjectInstance() != null) {
                getVivoInjectInstance().setRotationChanging(rotationChanging);
            }
        }
    }
}