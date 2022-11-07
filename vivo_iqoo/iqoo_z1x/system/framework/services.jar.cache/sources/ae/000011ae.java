package com.android.server.locksettings;

import com.android.internal.util.Preconditions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

/* loaded from: classes.dex */
class RebootEscrowData {
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int CURRENT_VERSION = 1;
    private final byte[] mBlob;
    private final byte[] mIv;
    private final RebootEscrowKey mKey;
    private final byte mSpVersion;
    private final byte[] mSyntheticPassword;

    private RebootEscrowData(byte spVersion, byte[] iv, byte[] syntheticPassword, byte[] blob, RebootEscrowKey key) {
        this.mSpVersion = spVersion;
        this.mIv = iv;
        this.mSyntheticPassword = syntheticPassword;
        this.mBlob = blob;
        this.mKey = key;
    }

    public byte getSpVersion() {
        return this.mSpVersion;
    }

    public byte[] getIv() {
        return this.mIv;
    }

    public byte[] getSyntheticPassword() {
        return this.mSyntheticPassword;
    }

    public byte[] getBlob() {
        return this.mBlob;
    }

    public RebootEscrowKey getKey() {
        return this.mKey;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static RebootEscrowData fromEncryptedData(RebootEscrowKey key, byte[] blob) throws IOException {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(blob);
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(blob));
        int version = dis.readInt();
        if (version != 1) {
            throw new IOException("Unsupported version " + version);
        }
        byte spVersion = dis.readByte();
        int ivSize = dis.readInt();
        if (ivSize < 0 || ivSize > 32) {
            throw new IOException("IV out of range: " + ivSize);
        }
        byte[] iv = new byte[ivSize];
        dis.readFully(iv);
        int cipherTextSize = dis.readInt();
        if (cipherTextSize < 0) {
            throw new IOException("Invalid cipher text size: " + cipherTextSize);
        }
        byte[] cipherText = new byte[cipherTextSize];
        dis.readFully(cipherText);
        try {
            Cipher c = Cipher.getInstance(CIPHER_ALGO);
            c.init(2, key.getKey(), new IvParameterSpec(iv));
            byte[] syntheticPassword = c.doFinal(cipherText);
            return new RebootEscrowData(spVersion, iv, syntheticPassword, blob, key);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            throw new IOException("Could not decrypt ciphertext", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static RebootEscrowData fromSyntheticPassword(RebootEscrowKey key, byte spVersion, byte[] syntheticPassword) throws IOException {
        Preconditions.checkNotNull(syntheticPassword);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(1, key.getKey());
            byte[] cipherText = cipher.doFinal(syntheticPassword);
            byte[] iv = cipher.getIV();
            dos.writeInt(1);
            dos.writeByte(spVersion);
            dos.writeInt(iv.length);
            dos.write(iv);
            dos.writeInt(cipherText.length);
            dos.write(cipherText);
            return new RebootEscrowData(spVersion, iv, syntheticPassword, bos.toByteArray(), key);
        } catch (InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e) {
            throw new IOException("Could not encrypt reboot escrow data", e);
        }
    }
}