package com.android.server.soundtrigger;

import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger;
import java.io.FileDescriptor;
import java.io.PrintWriter;

/* loaded from: classes2.dex */
public abstract class SoundTriggerInternal {
    public static final int STATUS_ERROR = Integer.MIN_VALUE;
    public static final int STATUS_OK = 0;

    public abstract void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    public abstract SoundTrigger.ModuleProperties getModuleProperties();

    public abstract int getParameter(int i, int i2);

    public abstract SoundTrigger.ModelParamRange queryParameter(int i, int i2);

    public abstract int setParameter(int i, int i2, int i3);

    public abstract int startRecognition(int i, SoundTrigger.KeyphraseSoundModel keyphraseSoundModel, IRecognitionStatusCallback iRecognitionStatusCallback, SoundTrigger.RecognitionConfig recognitionConfig);

    public abstract int stopRecognition(int i, IRecognitionStatusCallback iRecognitionStatusCallback);

    public abstract int unloadKeyphraseModel(int i);
}